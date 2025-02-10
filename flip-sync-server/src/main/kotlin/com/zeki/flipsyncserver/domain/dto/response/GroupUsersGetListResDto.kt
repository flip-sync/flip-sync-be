package com.zeki.flipsyncserver.domain.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Group Users Get List Res Dto")
data class GroupUsersGetListResDto(
    @Schema(description = "유저 PK", example = "1")
    val id: Long,

    @Schema(description = "유저 이름", example = "오재영")
    val name: String,

    @Schema(description = "참여 일자", example = "2025-02-10 20:27:33")
    val joinedAt: String
)
