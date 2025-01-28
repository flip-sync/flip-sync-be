package com.zeki.flipsyncserver.config.security.jwt

import com.fasterxml.jackson.databind.ObjectMapper
import com.zeki.common.dto.CommonResDto
import com.zeki.common.exception.ResponseCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import java.io.IOException

class CustomAuthenticationEntryPoint : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        val exception = request.getAttribute("exception") as String?

        when (exception) {
            null -> setResponse(response, ResponseCode.UNAUTHORIZED)

            ResponseCode.UNAUTHORIZED.code -> setResponse(response, ResponseCode.UNAUTHORIZED)
            ResponseCode.WRONG_TYPE_TOKEN.code -> setResponse(response, ResponseCode.WRONG_TYPE_TOKEN)
            ResponseCode.EXPIRED_TOKEN.code -> setResponse(response, ResponseCode.EXPIRED_TOKEN)
            ResponseCode.UNSUPPORTED_TOKEN.code -> setResponse(response, ResponseCode.UNSUPPORTED_TOKEN)
            else -> setResponse(response, ResponseCode.ACCESS_DENIED)
        }
    }


    //한글 출력을 위해 writer 사용
    @Throws(IOException::class)
    private fun setResponse(response: HttpServletResponse, responseCode: ResponseCode) {
        val objectMapper = ObjectMapper()
        val res: CommonResDto<Any> = CommonResDto.error(responseCode.code, responseCode.defaultMessage)

        // response 설정
        response.characterEncoding = "utf-8"
        response.status = responseCode.status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write(objectMapper.writeValueAsString(res))
    }
}