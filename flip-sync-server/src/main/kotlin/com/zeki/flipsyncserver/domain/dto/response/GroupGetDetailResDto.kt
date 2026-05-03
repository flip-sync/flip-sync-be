package com.zeki.flipsyncserver.domain.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Group Get Detail Res Dto")
data class GroupGetDetailResDto(
    @Schema(description = "방 PK", example = "1")
    val id: Long,
    @Schema(description = "조직 PK", example = "1")
    val organizationId: Long,
    @Schema(description = "방 이름", example = "바이올린 파트방")
    val name: String,
    @Schema(description = "방장 PK", example = "1")
    val creatorId: Long,
    @Schema(description = "방장 이름", example = "방장 계정")
    val creatorName: String,
    @Schema(description = "현재 사용자 PK", example = "2")
    val currentUserId: Long,
    @Schema(description = "현재 사용자 이름", example = "게스트 계정")
    val currentUserName: String,
    @Schema(description = "현재 사용자 프로필 이미지", example = "https://example.com/profile.png")
    val currentUserProfileImageUrl: String? = null,
    @Schema(description = "현재 사용자가 방장인지 여부", example = "false")
    val currentUserIsCreator: Boolean,
    @Schema(description = "현재 참여 인원", example = "4")
    val currentMemberCount: Int,
    @Schema(description = "최대 참여 인원", example = "10")
    val maxMemberCount: Int,
    @Schema(description = "비밀방 여부", example = "true")
    val hasPassword: Boolean
)
