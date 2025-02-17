package com.zeki.flipsyncserver.domain.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size

@Schema(name = "User Verify Email Request Dto")
data class UserVerifyEmailReqDto(
    @Schema(description = "이메일", example = "test@test.com")
    @Email
    val email: String,
    @Schema(description = "인증코드", example = "123456")
    @Size(min = 6, max = 6, message = "인증번호는 6자리 입니다.")
    val code: String
)
