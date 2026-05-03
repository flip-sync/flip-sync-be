package com.zeki.flipsyncserver.config.websocket

import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class GroupScoreWebSocketConfig(
    private val groupScoreSyncWebSocketHandler: GroupScoreSyncWebSocketHandler,
    private val groupScoreHandshakeInterceptor: GroupScoreHandshakeInterceptor
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(groupScoreSyncWebSocketHandler, "/webchatws/group/*")
            .addInterceptors(groupScoreHandshakeInterceptor)
            .setAllowedOriginPatterns("*")
    }
}
