package com.zeki.flipsyncserver.domain.controller

import com.zeki.common.dto.CommonResDto
import com.zeki.flipsyncserver.config.security.UserDetailsImpl
import com.zeki.flipsyncserver.domain.dto.request.SocreCreateReqDto
import com.zeki.flipsyncserver.domain.service.ScoreService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/group/{groupId}/score")
@Tag(name = "03. Score", description = "Score API")
class ScoreController(
    private val scoreService: ScoreService
) {

    @ApiResponses(
        value = [
            ApiResponse(responseCode = "401", ref = "#/components/responses/UNAUTHORIZED"),
            ApiResponse(responseCode = "404", ref = "#/components/responses/RESOURCE_NOT_FOUND"),
            ApiResponse(responseCode = "409", ref = "#/components/responses/CONFLICT_DATA"),
        ]
    )
    @Operation(
        summary = "악보 생성",
        description = "",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @PostMapping("")
    fun createScore(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @PathVariable groupId: Long,
        @RequestBody @Valid reqDto: SocreCreateReqDto
    ): CommonResDto<Long> {
        val data = scoreService.createScore(userDetail, groupId, reqDto)

        return CommonResDto.success(data)
    }
}