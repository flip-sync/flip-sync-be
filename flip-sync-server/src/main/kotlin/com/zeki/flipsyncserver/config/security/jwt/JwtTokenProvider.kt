package com.zeki.flipsyncserver.config.security.jwt

import com.zeki.flipsyncdb.entity.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.spec.SecretKeySpec

@Component
class JwtTokenProvider(
    private val userDetailsService: UserDetailsService
) {

    // TODO: yml에서 읽어오기
    private var secretKey: String = "your-very-strong-secret-key-32bytes!"
    private var secretKeySpec: SecretKeySpec = SecretKeySpec(secretKey.toByteArray(), SignatureAlgorithm.HS256.jcaName)
    private val headerName: String = "Token"
    private val expriationTime: Long = 1000 * 60 * 60 * 24

    fun createToken(userId: Long, email: String, name: String, role: User.UserRole): String {
        val claims: Claims = Jwts.claims().setSubject(userId.toString())
        claims.put("email", email)
        claims.put("name", name)
        claims.put("role", role)


        val now: Date = Date()
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(Date(now.time + expriationTime))
            .signWith(secretKeySpec)
            .compact()
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
        val userId = this.getUsername(token)
        return userDetailsService.loadUserByUsername(userId)
    }
}