package com.zeki.flipsyncserver.config.security.jwt

import com.zeki.common.exception.ResponseCode
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.SignatureException
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.GenericFilterBean

class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : GenericFilterBean() {
    val ATTRIBUTE_NAME: String = "exception"

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val token: String? = jwtTokenProvider.resolveToken(request as HttpServletRequest)


        try {
            when {
                token == null -> request.setAttribute(ATTRIBUTE_NAME, ResponseCode.UNAUTHORIZED.code)
                jwtTokenProvider.validateToken(token) -> {
                    val authentication: Authentication = jwtTokenProvider.getAuthentication(token)
                    SecurityContextHolder.getContext().authentication = authentication
                }

                else -> request.setAttribute(ATTRIBUTE_NAME, ResponseCode.WRONG_TYPE_TOKEN.code)
            }

        } catch (e: Exception) {
            when (e) {
                is SecurityException,
                is MalformedJwtException,
                is IllegalArgumentException,
                is SignatureException -> request.setAttribute(ATTRIBUTE_NAME, ResponseCode.WRONG_TYPE_TOKEN.code)

                is ExpiredJwtException -> request.setAttribute(ATTRIBUTE_NAME, ResponseCode.EXPIRED_TOKEN.code)
                is UnsupportedJwtException -> request.setAttribute(ATTRIBUTE_NAME, ResponseCode.UNSUPPORTED_TOKEN.code)
                else -> request.setAttribute(ATTRIBUTE_NAME, ResponseCode.UNAUTHORIZED.code)
            }
        }

        chain.doFilter(request, response)
    }
}