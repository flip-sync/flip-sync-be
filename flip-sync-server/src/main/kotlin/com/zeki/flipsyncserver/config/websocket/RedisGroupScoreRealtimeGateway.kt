package com.zeki.flipsyncserver.config.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator
import java.net.InetAddress
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private const val REALTIME_GROUP_EVENTS_CHANNEL = "flipsync:realtime:group-events"
private const val EVENT_TYPE_GROUP_MESSAGE = "GROUP_MESSAGE"
private const val EVENT_TYPE_PRESENCE_SYNC_REQUEST = "PRESENCE_SYNC_REQUEST"

private data class RealtimeGroupEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val originInstanceId: String,
    val groupId: Long,
    val type: String,
    val message: GroupScoreSyncMessage? = null,
    val createdAt: Long = System.currentTimeMillis()
)

private data class RedisPresenceSession(
    val sessionId: String,
    val groupId: Long,
    val userId: Long,
    val userName: String,
    val isCreator: Boolean,
    val profileImageUrl: String? = null,
    val instanceId: String,
    val lastSeenAt: Long
)

private data class RedisRealtimeSocketState(
    val session: WebSocketSession,
    @Volatile var lastSeenAt: Instant = Instant.now()
)

@Configuration
@ConditionalOnProperty(prefix = "flipsync.realtime", name = ["mode"], havingValue = "redis")
class RedisRealtimeConfig {

    @Bean
    fun redisMessageListenerContainer(
        connectionFactory: RedisConnectionFactory,
        redisGroupScoreRealtimeGateway: RedisGroupScoreRealtimeGateway
    ): RedisMessageListenerContainer =
        RedisMessageListenerContainer().apply {
            setConnectionFactory(connectionFactory)
            addMessageListener(redisGroupScoreRealtimeGateway, ChannelTopic(REALTIME_GROUP_EVENTS_CHANNEL))
        }
}

@Component
@ConditionalOnProperty(prefix = "flipsync.realtime", name = ["mode"], havingValue = "redis")
class RedisGroupScoreRealtimeGateway(
    private val objectMapper: ObjectMapper,
    private val redisTemplate: StringRedisTemplate,
    @Value("\${flipsync.realtime.instance-id:}") configuredInstanceId: String,
    @Value("\${flipsync.realtime.presence-session-ttl-seconds:120}") presenceSessionTtlSeconds: Long,
    @Value("\${flipsync.realtime.shared-view-ttl-minutes:10}") sharedViewTtlMinutes: Long
) : GroupScoreRealtimeGateway, MessageListener {

    private val instanceId = configuredInstanceId.ifBlank {
        runCatching { InetAddress.getLocalHost().hostName }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()
    }
    private val presenceSessionTtl = Duration.ofSeconds(presenceSessionTtlSeconds.coerceAtLeast(30))
    private val sharedViewTtl = Duration.ofMinutes(sharedViewTtlMinutes.coerceAtLeast(1))
    private val localRoomSessions = ConcurrentHashMap<Long, ConcurrentHashMap<String, RedisRealtimeSocketState>>()

    override fun registerSession(session: WebSocketSession) {
        val groupId = session.attributes["groupId"] as? Long ?: return
        val safeSession = session.toConcurrentSession()
        val sessions = localRoomSessions.computeIfAbsent(groupId) { ConcurrentHashMap() }

        sessions[safeSession.id] = RedisRealtimeSocketState(session = safeSession)
        touchPresence(safeSession)
        loadSharedState(groupId)?.let { sharedState ->
            sendToSession(safeSession, sharedState)
        }
        publishPresenceSyncRequest(groupId)
    }

    override fun unregisterSession(session: WebSocketSession) {
        val groupId = session.attributes["groupId"] as? Long ?: return
        val sessions = localRoomSessions[groupId] ?: return

        sessions.remove(session.id)
        if (sessions.isEmpty()) {
            localRoomSessions.remove(groupId)
        }

        removePresence(session)
        publishPresenceSyncRequest(groupId)
    }

    override fun handleIncomingMessage(session: WebSocketSession, payload: String) {
        val groupId = session.attributes["groupId"] as? Long ?: return
        val incoming = objectMapper.readValue(payload, GroupScoreSyncMessage::class.java)
        localRoomSessions[groupId]?.get(session.id)?.lastSeenAt = Instant.now()
        touchPresence(session)

        if (incoming.type == "CLIENT_PING") {
            sendToSession(
                session = localRoomSessions[groupId]?.get(session.id)?.session ?: session,
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
                saveSharedState(groupId, outgoing)
            } else {
                clearSharedState(groupId)
            }
        }

        publishGroupMessage(groupId, outgoing)
    }

    override fun broadcastToGroup(groupId: Long, message: GroupScoreSyncMessage) {
        publishGroupMessage(groupId, message)
    }

    override fun onMessage(message: Message, pattern: ByteArray?) {
        val payload = String(message.body, Charsets.UTF_8)
        val event = runCatching { objectMapper.readValue(payload, RealtimeGroupEvent::class.java) }.getOrNull()
            ?: return

        when (event.type) {
            EVENT_TYPE_GROUP_MESSAGE -> event.message?.let { localBroadcastToGroup(event.groupId, it) }
            EVENT_TYPE_PRESENCE_SYNC_REQUEST -> broadcastPresenceFromRedis(event.groupId)
        }
    }

    @Scheduled(fixedDelay = 60_000)
    fun cleanupStaleLocalSessions() {
        val now = Instant.now()
        val groupsToPublish = mutableSetOf<Long>()

        localRoomSessions.entries.removeIf { (groupId, sessions) ->
            val removed = sessions.entries.removeIf { (_, socketState) ->
                val stale = !socketState.session.isOpen ||
                    Duration.between(socketState.lastSeenAt, now) > presenceSessionTtl
                if (stale) {
                    removePresence(socketState.session)
                }
                stale
            }

            if (removed) {
                groupsToPublish += groupId
            }

            sessions.isEmpty()
        }

        groupsToPublish.forEach { groupId ->
            publishPresenceSyncRequest(groupId)
        }
    }

    private fun publishGroupMessage(groupId: Long, message: GroupScoreSyncMessage) {
        val event = RealtimeGroupEvent(
            originInstanceId = instanceId,
            groupId = groupId,
            type = EVENT_TYPE_GROUP_MESSAGE,
            message = message
        )

        if (!publishEvent(event)) {
            localBroadcastToGroup(groupId, message)
        }
    }

    private fun publishPresenceSyncRequest(groupId: Long) {
        publishEvent(
            RealtimeGroupEvent(
                originInstanceId = instanceId,
                groupId = groupId,
                type = EVENT_TYPE_PRESENCE_SYNC_REQUEST
            )
        )
    }

    private fun publishEvent(event: RealtimeGroupEvent): Boolean =
        runCatching {
            redisTemplate.convertAndSend(REALTIME_GROUP_EVENTS_CHANNEL, objectMapper.writeValueAsString(event))
        }.isSuccess

    private fun touchPresence(session: WebSocketSession) {
        val groupId = session.attributes["groupId"] as? Long ?: return
        val userId = session.attributes["userId"] as? Long ?: return
        val userName = session.attributes["userName"] as? String ?: return
        val now = System.currentTimeMillis()
        val presenceSession = RedisPresenceSession(
            sessionId = session.id,
            groupId = groupId,
            userId = userId,
            userName = userName,
            isCreator = session.attributes["isCreator"] as? Boolean ?: false,
            profileImageUrl = (session.attributes["userProfileImageUrl"] as? String)?.ifBlank { null },
            instanceId = instanceId,
            lastSeenAt = now
        )

        redisTemplate.opsForValue().set(
            presenceSessionKey(session.id),
            objectMapper.writeValueAsString(presenceSession),
            presenceSessionTtl
        )
        redisTemplate.opsForZSet().add(groupPresenceKey(groupId), session.id, now.toDouble())
    }

    private fun removePresence(session: WebSocketSession) {
        val groupId = session.attributes["groupId"] as? Long ?: return

        redisTemplate.delete(presenceSessionKey(session.id))
        redisTemplate.opsForZSet().remove(groupPresenceKey(groupId), session.id)
    }

    private fun broadcastPresenceFromRedis(groupId: Long) {
        removeExpiredPresence(groupId)
        val sessionIds = redisTemplate.opsForZSet().range(groupPresenceKey(groupId), 0, -1).orEmpty()
        if (sessionIds.isEmpty()) {
            localBroadcastToGroup(
                groupId = groupId,
                message = GroupScoreSyncMessage(
                    type = "PRESENCE_SYNC",
                    connectedUsers = emptyList(),
                    serverTimestamp = System.currentTimeMillis()
                )
            )
            return
        }

        val presenceValues = redisTemplate.opsForValue().multiGet(sessionIds.map(::presenceSessionKey)).orEmpty()
        val connectedUsers = presenceValues
            .mapNotNull { value ->
                runCatching { objectMapper.readValue(value, RedisPresenceSession::class.java) }.getOrNull()
            }
            .filter { it.groupId == groupId }
            .map {
                GroupConnectedUser(
                    userId = it.userId,
                    userName = it.userName,
                    isCreator = it.isCreator,
                    profileImageUrl = it.profileImageUrl
                )
            }
            .associateBy { it.userId }
            .values
            .sortedBy { it.userName }
            .toList()

        localBroadcastToGroup(
            groupId = groupId,
            message = GroupScoreSyncMessage(
                type = "PRESENCE_SYNC",
                connectedUsers = connectedUsers,
                serverTimestamp = System.currentTimeMillis()
            )
        )
    }

    private fun removeExpiredPresence(groupId: Long) {
        val staleBefore = System.currentTimeMillis() - presenceSessionTtl.toMillis()
        redisTemplate.opsForZSet().removeRangeByScore(groupPresenceKey(groupId), Double.NEGATIVE_INFINITY, staleBefore.toDouble())
    }

    private fun saveSharedState(groupId: Long, message: GroupScoreSyncMessage) {
        redisTemplate.opsForValue().set(sharedViewKey(groupId), objectMapper.writeValueAsString(message), sharedViewTtl)
    }

    private fun loadSharedState(groupId: Long): GroupScoreSyncMessage? =
        redisTemplate.opsForValue().get(sharedViewKey(groupId))?.let { payload ->
            runCatching { objectMapper.readValue(payload, GroupScoreSyncMessage::class.java) }.getOrNull()
        }

    private fun clearSharedState(groupId: Long) {
        redisTemplate.delete(sharedViewKey(groupId))
    }

    private fun localBroadcastToGroup(groupId: Long, message: GroupScoreSyncMessage) {
        val payload = objectMapper.writeValueAsString(message)
        localRoomSessions[groupId]?.values
            ?.map { it.session }
            ?.filter { it.isOpen }
            ?.forEach { session ->
                try {
                    session.sendMessage(TextMessage(payload))
                } catch (_: Exception) {
                    unregisterSession(session)
                }
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

    private fun groupPresenceKey(groupId: Long) = "flipsync:presence:group:$groupId"

    private fun presenceSessionKey(sessionId: String) = "flipsync:presence:session:$sessionId"

    private fun sharedViewKey(groupId: Long) = "flipsync:shared-view:group:$groupId"

    private fun WebSocketSession.toConcurrentSession(): WebSocketSession =
        if (this is ConcurrentWebSocketSessionDecorator) {
            this
        } else {
            ConcurrentWebSocketSessionDecorator(this, 10_000, 512 * 1024)
        }
}
