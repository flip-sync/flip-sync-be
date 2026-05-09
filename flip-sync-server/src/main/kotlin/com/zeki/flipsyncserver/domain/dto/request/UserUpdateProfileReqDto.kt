package com.zeki.flipsyncserver.domain.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(name = "User Update Profile Request Dto")
data class UserUpdateProfileReqDto(
    @Schema(description = "이름", example = "장경태")
    @field:NotBlank
    @field:Size(max = 30)
    val name: String
)
