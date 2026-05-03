package com.zeki.flipsyncserver.domain.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(name = "User Login Request Dto")
data class UserLoginReqDto(
    @Schema(description = "이메일", example = "ojy9612@gmail.com")
    @field:Email
    @field:NotBlank
    @field:Size(max = 255)
    val email: String,
    @Schema(description = "비밀번호", example = "1234")
    val password: String
)
