package com.zeki.flipsyncserver.config.security.jwt

import com.zeki.common.exception.ApiException
import com.zeki.common.exception.ResponseCode
import com.zeki.flipsyncdb.entity.User
import com.zeki.flipsyncdb.repository.UserRepository
import com.zeki.flipsyncserver.config.security.UserDetailsImpl
import com.zeki.flipsyncserver.domain.dto.response.TokenResDto
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.spec.SecretKeySpec

@Component
class JwtTokenProvider(
    private val userDetailsService: UserDetailsService,
    private val userRepository: UserRepository,

    @Value("\${jwt.secret}")
    private val secretKey: String,

    @Value("\${jwt.header}")
    private val headerName: String
) {

    private val secretKeySpec: SecretKeySpec = SecretKeySpec(secretKey.toByteArray(), SignatureAlgorithm.HS256.jcaName)

    companion object {
        const val ACCESS_TOKEN_EXPIRE_TIME = 30 * 60 * 1000L     // 30분
        const val REFRESH_TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L    // 7일
    }

    private fun generateAccessToken(user: User): String {
        val claims: Claims = Jwts.claims().setSubject(user.id.toString())
        claims.put("email", user.username)
        claims.put("name", user.name)
        claims.put("role", user.role)

        val now = Date()

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(Date(now.time + ACCESS_TOKEN_EXPIRE_TIME))
            .signWith(secretKeySpec)
            .compact()
    }

    fun createToken(user: User): TokenResDto {
        // Access Token 생성
        val accessToken = this.generateAccessToken(user)

        val now = Date()
        // Refresh Token 생성
        val refreshToken = Jwts.builder()
            .setExpiration(Date(now.time + REFRESH_TOKEN_EXPIRE_TIME))
            .signWith(secretKeySpec)
            .compact()

        user.updateRefreshToken(refreshToken)

        return TokenResDto(
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessTokenExpiresIn = ACCESS_TOKEN_EXPIRE_TIME
        )
    }

    fun regenerateAccessToken(refreshToken: String): TokenResDto? {
        // Refresh Token 검증
        if (!validateToken(refreshToken)) {
            throw ApiException(ResponseCode.EXPIRED_TOKEN)
        }

        // 기존 사용자 정보 추출
        val userDetails = this.getUserDetailsByRefreshToken(refreshToken)
        val user = userDetails as UserDetailsImpl

        // 새로운 Access Token만 생성
        val newAccessToken = this.generateAccessToken(
            userRepository.findById(user.userPk).orElseThrow { ApiException(ResponseCode.RESOURCE_NOT_FOUND) },
        )

        // 기존 Refresh Token 유지
        return TokenResDto(
            accessToken = newAccessToken,
            refreshToken = refreshToken,  // 기존 리프레시 토큰 재사용
            accessTokenExpiresIn = ACCESS_TOKEN_EXPIRE_TIME
        )
    }

    // 토큰에서 회원 정보 추출
    fun getUsername(token: String?): String? {
        val body = Jwts.parserBuilder()
            .setSigningKey(secretKeySpec)
            .build()
            .parseClaimsJws(token)
            .body

        return body["email"] as String?
    }

    // JWT 토큰에서 인증 정보 조회
    fun getAuthentication(token: String?): Authentication {
        val userDetails = userDetailsService.loadUserByUsername(this.getUsername(token))
        return UsernamePasswordAuthenticationToken(userDetails, "", userDetails.authorities)
    }


    // Request의 Header에서 token 값을 가져옵니다. "TOKEN" : "TOKEN값'
    fun resolveToken(request: HttpServletRequest): String? {
        return request.getHeader(headerName)
    }

    // 토큰의 유효성 + 만료일자 확인
    fun validateToken(jwtToken: String?): Boolean {
        val claims = Jwts.parserBuilder()
            .setSigningKey(secretKeySpec)
            .build()
            .parseClaimsJws(jwtToken)

        return !claims.body.expiration.before(Date())
    }

    fun getUserDetails(token: String?): UserDetails {
        val userId = this.getUsername(token) ?: throw ApiException(ResponseCode.UNAUTHORIZED)
        return userDetailsService.loadUserByUsername(userId)
    }

    fun getUserDetailsByRefreshToken(refreshToken: String): UserDetails {
        val user = userRepository.findOneByRefreshToken(refreshToken) ?: throw ApiException(ResponseCode.UNAUTHORIZED)
        return userDetailsService.loadUserByUsername(user.username)
    }
}