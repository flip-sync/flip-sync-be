package com.zeki.flipsyncserver.config.websocket

import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

@Component
class GroupScoreSyncWebSocketHandler(
    private val realtimeGateway: GroupScoreRealtimeGateway
) : TextWebSocketHandler() {

    override fun afterConnectionEstablished(session: WebSocketSession) {
        realtimeGateway.registerSession(session)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        realtimeGateway.handleIncomingMessage(session, message.payload)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        realtimeGateway.unregisterSession(session)
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        realtimeGateway.unregisterSession(session)
    }

    fun broadcastToGroup(groupId: Long, message: GroupScoreSyncMessage) {
        realtimeGateway.broadcastToGroup(groupId, message)
    }
}
