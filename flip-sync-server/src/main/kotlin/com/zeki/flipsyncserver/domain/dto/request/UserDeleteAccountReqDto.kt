package com.zeki.flipsyncserver.domain.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(name = "User Delete Account Request Dto")
data class UserDeleteAccountReqDto(
    @Schema(description = "현재 비밀번호", example = "password1234")
    @field:NotBlank
    @field:Size(max = 255)
    val password: String
)
