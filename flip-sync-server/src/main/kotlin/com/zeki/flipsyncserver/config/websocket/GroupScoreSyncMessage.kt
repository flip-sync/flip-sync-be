package com.zeki.flipsyncserver.config.websocket

data class GroupScoreSyncMessage(
    val type: String,
    val scoreId: Long? = null,
    val pageIndex: Int? = null,
    val active: Boolean? = null,
    val triggeredByUserId: Long? = null,
    val triggeredByUserName: String? = null,
    val connectedUsers: List<GroupConnectedUser>? = null,
    val scoreSummary: GroupScoreSummaryPayload? = null,
    val clientTimestamp: Long? = null,
    val serverTimestamp: Long? = null
)

data class GroupConnectedUser(
    val userId: Long,
    val userName: String,
    val isCreator: Boolean,
    val profileImageUrl: String? = null
)

data class GroupScoreSummaryPayload(
    val id: Long,
    val uploadedUserId: Long,
    val thumbnail: String,
    val title: String,
    val singer: String,
    val code: String,
    val uploadedUserName: String,
    val uploadedUserProfileImageUrl: String? = null,
    val createdAt: String,
    val modifiedAt: String
)
