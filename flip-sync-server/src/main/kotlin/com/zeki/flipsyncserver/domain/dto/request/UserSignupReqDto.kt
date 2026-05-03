package com.zeki.flipsyncserver.domain.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(name = "User Signup Request Dto")
data class UserSignupReqDto(
    @Schema(description = "이메일", example = "test@test.com")
    @field:Email
    @field:NotBlank
    @field:Size(max = 255)
    val email: String,

    @Schema(description = "비밀번호", example = "1234")
    @NotBlank
    val password: String,

    @Schema(description = "비밀번호 확인", example = "1234")
    @NotBlank
    val passwordConfirm: String,

    @Schema(description = "이름", example = "홍길동")
    @NotBlank
    @field:Size(max = 30)
    val name: String,
) {

    @AssertTrue(message = "비밀번호가 일치하지 않습니다.")
    @Schema(hidden = true)
    fun isPasswordMatch(): Boolean {
        return password == passwordConfirm
    }

}
