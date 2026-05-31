package com.zeki.flipsyncserver.config.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

interface GroupScoreRealtimeGateway {
    fun registerSession(session: WebSocketSession)
    fun unregisterSession(session: WebSocketSession)
    fun handleIncomingMessage(session: WebSocketSession, payload: String)
    fun broadcastToGroup(groupId: Long, message: GroupScoreSyncMessage)
}

private data class GroupRealtimeRoomContext(
    val sessions: ConcurrentHashMap<String, GroupRealtimeSocketState> = ConcurrentHashMap(),
    @Volatile var sharedState: GroupScoreSyncMessage? = null,
    @Volatile var lastActivityAt: Instant = Instant.now()
)

private data class GroupRealtimeSocketState(
    val session: WebSocketSession,
    @Volatile var lastSeenAt: Instant = Instant.now()
)

@Component
@ConditionalOnProperty(prefix = "flipsync.realtime", name = ["mode"], havingValue = "in-memory", matchIfMissing = true)
class InMemoryGroupScoreRealtimeGateway(
    private val objectMapper: ObjectMapper
) : GroupScoreRealtimeGateway {

    companion object {
        private val STALE_SESSION_TIMEOUT: Duration = Duration.ofSeconds(90)
        private val EMPTY_ROOM_RETENTION: Duration = Duration.ofMinutes(10)
    }

    private val roomContexts = ConcurrentHashMap<Long, GroupRealtimeRoomContext>()

    override fun registerSession(session: WebSocketSession) {
        val groupId = session.attributes["groupId"] as? Long ?: return
        val safeSession = session.toConcurrentSession()
        val context = roomContexts.computeIfAbsent(groupId) { GroupRealtimeRoomContext() }

        context.sessions[safeSession.id] = GroupRealtimeSocketState(session = safeSession)
        context.lastActivityAt = Instant.now()

        context.sharedState?.let { sharedState ->
            sendToSession(safeSession, sharedState)
        }
        broadcastPresence(groupId)
    }

    override fun unregisterSession(session: WebSocketSession) {
        val groupId = session.attributes["groupId"] as? Long ?: return
        val context = roomContexts[groupId] ?: return

        context.sessions.remove(session.id)
        context.lastActivityAt = Instant.now()

        if (context.sessions.isEmpty()) {
            roomContexts.remove(groupId)
        } else {
            broadcastPresence(groupId)
        }
    }

    override fun handleIncomingMessage(session: WebSocketSession, payload: String) {
        val groupId = session.attributes["groupId"] as? Long ?: return
        val context = roomContexts.computeIfAbsent(groupId) { GroupRealtimeRoomContext() }
        val incoming = objectMapper.readValue(payload, GroupScoreSyncMessage::class.java)
        context.lastActivityAt = Instant.now()
        context.sessions[session.id]?.lastSeenAt = Instant.now()

        if (incoming.type == "CLIENT_PING") {
            sendToSession(
                session = context.sessions[session.id]?.session ?: session,
                message = GroupScoreSyncMessage(
                    type = "SERVER_PONG",
                    clientTimestamp = incoming.clientTimestamp,
                    serverTimestamp = System.currentTimeMillis()
                )
            )
            return
        }

        val isCreator = session.attributes["isCreator"] as? Boolean ?: false
        if (!isCreator) {
            return
        }

        val outgoing = incoming.copy(
            triggeredByUserId = session.attributes["userId"] as? Long,
            triggeredByUserName = session.attributes["userName"] as? String,
            serverTimestamp = System.currentTimeMillis()
        )

        if (outgoing.type == "SYNC_SCORE_VIEW") {
            if (outgoing.active == true && outgoing.scoreId != null) {
                context.sharedState = outgoing
            } else {
                context.sharedState = null
            }
        }

        broadcastToGroup(groupId, outgoing)
    }

    override fun broadcastToGroup(groupId: Long, message: GroupScoreSyncMessage) {
        val context = roomContexts[groupId] ?: return
        val payload = objectMapper.writeValueAsString(message)
        context.lastActivityAt = Instant.now()

        context.sessions.values
            .map { it.session }
            .filter { it.isOpen }
            .forEach { session ->
                try {
                    session.sendMessage(TextMessage(payload))
                } catch (_: Exception) {
                    unregisterSession(session)
                }
            }
    }

    private fun broadcastPresence(groupId: Long) {
        val context = roomContexts[groupId] ?: return
        val connectedUsers = context.sessions.values
            .map { it.session }
            .filter { it.isOpen }
            .mapNotNull { session ->
                val userId = session.attributes["userId"] as? Long ?: return@mapNotNull null
                val userName = session.attributes["userName"] as? String ?: return@mapNotNull null
                val isCreator = session.attributes["isCreator"] as? Boolean ?: false
                val profileImageUrl = (session.attributes["userProfileImageUrl"] as? String)?.ifBlank { null }

                GroupConnectedUser(
                    userId = userId,
                    userName = userName,
                    isCreator = isCreator,
                    profileImageUrl = profileImageUrl
                )
            }
            .associateBy { it.userId }
            .values
            .sortedBy { it.userName }
            .toList()

        broadcastToGroup(
            groupId = groupId,
            message = GroupScoreSyncMessage(
                type = "PRESENCE_SYNC",
                connectedUsers = connectedUsers,
                serverTimestamp = System.currentTimeMillis()
            )
        )
    }

    @Scheduled(fixedDelay = 60_000)
    fun cleanupStaleRooms() {
        val now = Instant.now()
        val groupsToBroadcast = mutableListOf<Long>()

        roomContexts.entries.removeIf { (groupId, context) ->
            val removedStaleSessions = context.sessions.entries.removeIf { (_, socketState) ->
                !socketState.session.isOpen ||
                    Duration.between(socketState.lastSeenAt, now) > STALE_SESSION_TIMEOUT
            }

            val shouldRemoveRoom = context.sessions.isEmpty() &&
                Duration.between(context.lastActivityAt, now) > EMPTY_ROOM_RETENTION

            if (removedStaleSessions && !shouldRemoveRoom && context.sessions.isNotEmpty()) {
                groupsToBroadcast += groupId
            }

            shouldRemoveRoom
        }

        groupsToBroadcast.forEach { groupId ->
            broadcastPresence(groupId)
        }
    }

    private fun sendToSession(session: WebSocketSession, message: GroupScoreSyncMessage) {
        if (!session.isOpen) {
            unregisterSession(session)
            return
        }

        try {
            session.sendMessage(TextMessage(objectMapper.writeValueAsString(message)))
        } catch (_: Exception) {
            unregisterSession(session)
        }
    }

    private fun WebSocketSession.toConcurrentSession(): WebSocketSession =
        if (this is ConcurrentWebSocketSessionDecorator) {
            this
        } else {
            ConcurrentWebSocketSessionDecorator(this, 10_000, 512 * 1024)
        }
}
