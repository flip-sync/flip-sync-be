package com.zeki.flipsyncserver.domain.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(name = "User Update Email Request Dto")
data class UserUpdateEmailReqDto(
    @Schema(description = "변경할 이메일", example = "next@flipsync.app")
    @field:Email
    @field:NotBlank
    @field:Size(max = 255)
    val email: String
)
