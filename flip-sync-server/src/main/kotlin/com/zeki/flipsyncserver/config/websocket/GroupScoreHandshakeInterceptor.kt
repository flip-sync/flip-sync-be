package com.zeki.flipsyncserver.config.websocket

import com.zeki.common.exception.ApiException
import com.zeki.common.exception.ResponseCode
import com.zeki.flipsyncserver.config.security.UserDetailsImpl
import com.zeki.flipsyncserver.config.security.jwt.JwtTokenProvider
import com.zeki.flipsyncserver.domain.service.GroupService
import org.springframework.http.HttpStatus
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

@Component
class GroupScoreHandshakeInterceptor(
    private val jwtTokenProvider: JwtTokenProvider,
    private val groupService: GroupService
) : HandshakeInterceptor {

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        val servletRequest = request as? ServletServerHttpRequest ?: run {
            response.setStatusCode(HttpStatus.BAD_REQUEST)
            return false
        }
        val token = servletRequest.servletRequest.getHeader("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()
            ?: servletRequest.servletRequest.getParameter("token")
                ?.removePrefix("Bearer ")
                ?.trim()

        if (token.isNullOrBlank()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED)
            return false
        }

        val groupId = request.uri.path.substringAfterLast("/").toLongOrNull()
        if (groupId == null) {
            response.setStatusCode(HttpStatus.BAD_REQUEST)
            return false
        }

        return try {
            val userDetail = jwtTokenProvider.getUserDetails(token) as? UserDetailsImpl
                ?: throw ApiException(ResponseCode.UNAUTHORIZED)
            val groupDetail = groupService.getGroupDetailForSocket(userDetail, groupId)

            attributes["groupId"] = groupId
            attributes["userId"] = userDetail.userPk
            attributes["userName"] = userDetail.name
            attributes["isCreator"] = groupDetail.currentUserIsCreator
            attributes["userProfileImageUrl"] = groupDetail.currentUserProfileImageUrl ?: ""
            true
        } catch (_: Exception) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED)
            false
        }
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) = Unit
}
