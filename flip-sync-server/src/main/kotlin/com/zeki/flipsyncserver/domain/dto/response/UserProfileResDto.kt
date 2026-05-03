package com.zeki.flipsyncserver.domain.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "User Profile Response Dto")
data class UserProfileResDto(
    @Schema(description = "유저 ID", example = "1")
    val id: Long,

    @Schema(description = "이메일", example = "local@flipsync.dev")
    val email: String,

    @Schema(description = "이름", example = "Local Tester")
    val name: String,

    @Schema(description = "소속", example = "FlipSync")
    val organization: String? = null,

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.png")
    val profileImageUrl: String? = null
)
