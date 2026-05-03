package com.zeki.flipsyncserver.domain.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(name = "Organization Create Req Dto")
data class OrganizationCreateReqDto(
    @Schema(description = "조직 이름", example = "서울예대 밴드팀")
    @field:NotBlank
    @field:Size(max = 50)
    val name: String
)
