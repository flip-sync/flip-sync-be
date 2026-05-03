package com.zeki.flipsyncserver.domain.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(name = "Group Create Req Dto")
data class GroupCreateReqDto(
    @Schema(description = "방 이름", example = "피아노 치는 방")
    @field:NotBlank
    @field:Size(max = 30)
    val name: String,

    @Schema(description = "최대 참여 인원", example = "10")
    @field:Min(1)
    @field:Max(10)
    val maxMemberCount: Int,

    @Schema(description = "비밀방 비밀번호 8자리", example = "12345678")
    @field:Pattern(regexp = "^\\d{8}$|^$", message = "비밀번호는 숫자 8자리여야 합니다.")
    val password: String? = null
)
