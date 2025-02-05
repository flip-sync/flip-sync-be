package com.zeki.flipsyncserver.domain.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Group Create Req Dto")
data class GroupCreateReqDto(
    @Schema(description = "그룹 이름", example = "test")
    val name: String
)
