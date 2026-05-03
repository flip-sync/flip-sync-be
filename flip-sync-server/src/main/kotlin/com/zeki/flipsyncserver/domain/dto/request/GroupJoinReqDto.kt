package com.zeki.flipsyncserver.domain.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(name = "Group Join Req Dto")
data class GroupJoinReqDto(
    @Schema(description = "방 PK", example = "1")
    @field:NotNull
    val groupId: Long,

    @Schema(description = "비밀방 비밀번호 8자리", example = "12345678")
    val password: String? = null
)
