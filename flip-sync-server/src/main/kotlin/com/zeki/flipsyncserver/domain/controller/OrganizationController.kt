package com.zeki.flipsyncserver.domain.controller

import com.zeki.common.dto.CommonResDto
import com.zeki.flipsyncserver.config.security.UserDetailsImpl
import com.zeki.flipsyncserver.domain.dto.request.OrganizationCreateReqDto
import com.zeki.flipsyncserver.domain.dto.request.OrganizationJoinReqDto
import com.zeki.flipsyncserver.domain.dto.response.OrganizationDetailResDto
import com.zeki.flipsyncserver.domain.dto.response.OrganizationSummaryResDto
import com.zeki.flipsyncserver.domain.service.OrganizationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/organization")
@Tag(name = "03. Organization", description = "Organization API")
class OrganizationController(
    private val organizationService: OrganizationService
) {
    @Operation(
        summary = "내 조직 목록 조회",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @GetMapping("/my")
    fun getMyOrganizations(
        @AuthenticationPrincipal userDetail: UserDetailsImpl
    ): CommonResDto<List<OrganizationSummaryResDto>> {
        return CommonResDto.success(organizationService.getMyOrganizations(userDetail))
    }

    @Operation(
        summary = "조직 생성",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @PostMapping("")
    fun createOrganization(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @Valid @RequestBody reqDto: OrganizationCreateReqDto
    ): CommonResDto<OrganizationSummaryResDto> {
        return CommonResDto.success(organizationService.createOrganization(userDetail, reqDto))
    }

    @ApiResponses(
        value = [
            ApiResponse(responseCode = "404", ref = "#/components/responses/RESOURCE_NOT_FOUND"),
            ApiResponse(responseCode = "409", ref = "#/components/responses/CONFLICT_DATA")
        ]
    )
    @Operation(
        summary = "초대 코드로 조직 가입",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @PostMapping("/join")
    fun joinOrganization(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @Valid @RequestBody reqDto: OrganizationJoinReqDto
    ): CommonResDto<OrganizationSummaryResDto> {
        return CommonResDto.success(organizationService.joinOrganization(userDetail, reqDto))
    }

    @Operation(
        summary = "조직 상세 조회",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @GetMapping("/{organizationId}")
    fun getOrganizationDetail(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @PathVariable organizationId: Long
    ): CommonResDto<OrganizationDetailResDto> {
        return CommonResDto.success(organizationService.getOrganizationDetail(userDetail, organizationId))
    }

    @Operation(
        summary = "조직 삭제",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @DeleteMapping("/{organizationId}")
    fun deleteOrganization(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @PathVariable organizationId: Long
    ): CommonResDto<Unit> {
        organizationService.deleteOrganization(userDetail, organizationId)
        return CommonResDto.success()
    }
}
