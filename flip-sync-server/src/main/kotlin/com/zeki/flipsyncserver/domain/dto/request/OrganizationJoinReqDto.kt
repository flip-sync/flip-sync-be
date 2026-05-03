package com.zeki.flipsyncserver.domain.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(name = "Organization Join Req Dto")
data class OrganizationJoinReqDto(
    @Schema(description = "조직 초대 코드", example = "AB12CD34")
    @field:NotBlank
    @field:Size(max = 20)
    val inviteCode: String
)
