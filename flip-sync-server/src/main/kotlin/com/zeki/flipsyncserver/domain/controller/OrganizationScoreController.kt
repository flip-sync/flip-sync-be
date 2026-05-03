package com.zeki.flipsyncserver.domain.controller

import com.zeki.common.dto.CommonResDto
import com.zeki.flipsyncdb.dto.ScoreGetPageResDto
import com.zeki.flipsyncserver.config.security.UserDetailsImpl
import com.zeki.flipsyncserver.domain.dto.request.ScoreGetPageReqDto
import com.zeki.flipsyncserver.domain.dto.request.SocreCreateReqDto
import com.zeki.flipsyncserver.domain.dto.response.ScoreGetDetailResDto
import com.zeki.flipsyncserver.domain.service.OrganizationHeader
import com.zeki.flipsyncserver.domain.service.OrganizationScoreService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/organization/score")
@Tag(name = "05. Organization Score", description = "Organization score library API")
class OrganizationScoreController(
    private val organizationScoreService: OrganizationScoreService
) {
    @Operation(
        summary = "조직 악보 창고 등록",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @PostMapping("", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createOrganizationScore(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @RequestHeader(OrganizationHeader.NAME) organizationId: Long,
        @ModelAttribute @Valid reqDto: SocreCreateReqDto
    ): CommonResDto<Long> {
        return CommonResDto.success(
            organizationScoreService.createOrganizationScore(userDetail, organizationId, reqDto)
        )
    }

    @Operation(
        summary = "조직 악보 창고 목록 조회",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @GetMapping("")
    fun getPageOrganizationScore(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @RequestHeader(OrganizationHeader.NAME) organizationId: Long,
        @ParameterObject @ModelAttribute @Valid reqDto: ScoreGetPageReqDto,
        @ParameterObject @PageableDefault pageable: Pageable
    ): CommonResDto<Page<ScoreGetPageResDto>> {
        return CommonResDto.success(
            organizationScoreService.getPageOrganizationScore(userDetail, organizationId, reqDto, pageable)
        )
    }

    @Operation(
        summary = "조직 악보 창고 상세 조회",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @GetMapping("/{scoreId}")
    fun getDetailOrganizationScore(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @RequestHeader(OrganizationHeader.NAME) organizationId: Long,
        @PathVariable scoreId: Long
    ): CommonResDto<ScoreGetDetailResDto> {
        return CommonResDto.success(
            organizationScoreService.getDetailOrganizationScore(userDetail, organizationId, scoreId)
        )
    }

    @Operation(
        summary = "조직 악보 창고 삭제",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @DeleteMapping("/{scoreId}")
    fun deleteOrganizationScore(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @RequestHeader(OrganizationHeader.NAME) organizationId: Long,
        @PathVariable scoreId: Long
    ): CommonResDto<Unit> {
        organizationScoreService.deleteOrganizationScore(userDetail, organizationId, scoreId)
        return CommonResDto.success()
    }

    @Operation(
        summary = "조직 악보를 공유방으로 보내기",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @PostMapping("/{scoreId}/send/group/{groupId}")
    fun sendOrganizationScoreToGroup(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @RequestHeader(OrganizationHeader.NAME) organizationId: Long,
        @PathVariable scoreId: Long,
        @PathVariable groupId: Long
    ): CommonResDto<Long> {
        return CommonResDto.success(
            organizationScoreService.sendOrganizationScoreToGroup(userDetail, organizationId, scoreId, groupId)
        )
    }
}
