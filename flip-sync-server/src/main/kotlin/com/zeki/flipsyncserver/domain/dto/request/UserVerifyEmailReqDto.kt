package com.zeki.flipsyncserver.domain.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "User Verify Email Request Dto")
data class UserVerifyEmailReqDto(
    @Schema(description = "이메일", example = "test@test.com")
    val email: String,
    @Schema(description = "인증코드", example = "123456")
    val code: String
)
