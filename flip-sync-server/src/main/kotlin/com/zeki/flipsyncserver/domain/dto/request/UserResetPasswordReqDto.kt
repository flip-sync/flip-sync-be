package com.zeki.flipsyncserver.domain.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue

@Schema(name = "User Reset Password Req Dto")
data class UserResetPasswordReqDto(
    @Schema(description = "이메일", example = "test@test.com")
    val email: String,

    @Schema(description = "비밀번호", example = "test1234")
    val password: String,

    @Schema(description = "비밀번호", example = "test1234")
    val passwordConfirm: String
) {

    @Schema(hidden = true)
    @AssertTrue(message = "비밀번호가 일치하지 않습니다.")
    fun isPasswordMatch(): Boolean {
        return password == passwordConfirm
    }
    
}
