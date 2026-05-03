package com.zeki.flipsyncserver.domain.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Organization Member Res Dto")
data class OrganizationMemberResDto(
    @Schema(description = "유저 PK", example = "1")
    val userId: Long,
    @Schema(description = "이메일", example = "member@flipsync.dev")
    val email: String,
    @Schema(description = "닉네임", example = "정경태")
    val name: String,
    @Schema(description = "프로필 이미지", example = "https://example.com/profile.png")
    val profileImageUrl: String? = null,
    @Schema(description = "조직 내 역할", example = "LEADER")
    val role: String,
    @Schema(description = "가입일", example = "2026-05-03 12:00:00")
    val joinedAt: String
)
