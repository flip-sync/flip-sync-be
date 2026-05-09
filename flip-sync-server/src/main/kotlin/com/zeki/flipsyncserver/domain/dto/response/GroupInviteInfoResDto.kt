package com.zeki.flipsyncserver.domain.dto.response

data class GroupInviteInfoResDto(
    val groupId: Long,
    val groupName: String,
    val organizationId: Long,
    val organizationName: String,
    val organizationInviteCode: String,
    val organizationCreatorId: Long,
    val organizationCreatorName: String,
    val organizationMemberCount: Long,
    val organizationRole: String,
    val organizationIsLeader: Boolean,
    val creatorId: Long,
    val creatorName: String,
    val currentMemberCount: Int,
    val maxMemberCount: Int,
    val hasPassword: Boolean,
    val joined: Boolean
)
