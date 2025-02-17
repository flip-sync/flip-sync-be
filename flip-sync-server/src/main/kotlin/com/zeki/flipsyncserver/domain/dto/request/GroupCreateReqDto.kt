package com.zeki.flipsyncserver.domain.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(name = "Group Create Req Dto")
data class GroupCreateReqDto(
    @Schema(description = "그룹 이름", example = "test")
    @NotBlank
    val name: String
)
