package com.zeki.flipsyncserver.domain.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email

@Schema(name = "User Login Request Dto")
data class UserLoginReqDto(
    @Schema(description = "이메일", example = "ojy9612@gmail.com")
    @Email
    val email: String,
    @Schema(description = "비밀번호", example = "1234")
    val password: String
)
