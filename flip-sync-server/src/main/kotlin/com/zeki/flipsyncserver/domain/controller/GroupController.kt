package com.zeki.flipsyncserver.domain.controller

import com.zeki.common.dto.CommonResDto
import com.zeki.flipsyncserver.config.security.UserDetailsImpl
import com.zeki.flipsyncserver.domain.dto.request.GroupCreateReqDto
import com.zeki.flipsyncserver.domain.dto.request.GroupJoinReqDto
import com.zeki.flipsyncserver.domain.dto.request.GroupOwnerTransferReqDto
import com.zeki.flipsyncserver.domain.dto.response.GroupGetDetailResDto
import com.zeki.flipsyncserver.domain.dto.response.GroupInviteInfoResDto
import com.zeki.flipsyncserver.domain.dto.response.GroupGetListResDto
import com.zeki.flipsyncserver.domain.dto.response.GroupUsersGetListResDto
import com.zeki.flipsyncserver.domain.service.GroupService
import com.zeki.flipsyncserver.domain.service.OrganizationHeader
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/group")
@Tag(name = "02. Group", description = "Group API")
class GroupController(
    private val groupService: GroupService
) {
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "401", ref = "#/components/responses/UNAUTHORIZED"),
            ApiResponse(responseCode = "404", ref = "#/components/responses/RESOURCE_NOT_FOUND"),
            ApiResponse(responseCode = "409", ref = "#/components/responses/CONFLICT_DATA")
        ]
    )
    @Operation(
        summary = "방 생성",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @PostMapping("")
    fun createGroup(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @RequestHeader(OrganizationHeader.NAME) organizationId: Long,
        @Valid @RequestBody reqDto: GroupCreateReqDto
    ): CommonResDto<Long> {
        return CommonResDto.success(groupService.createGroup(userDetail, organizationId, reqDto))
    }

    @Operation(
        summary = "내가 참여 중인 방 목록 조회",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @GetMapping("/my")
    fun getMyGroupList(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @RequestHeader(OrganizationHeader.NAME) organizationId: Long,
        @ParameterObject @PageableDefault pageable: Pageable
    ): CommonResDto<Page<GroupGetListResDto>> {
        return CommonResDto.success(groupService.getMyGroupList(userDetail, organizationId, pageable))
    }

    @Operation(
        summary = "선택된 조직의 방 목록 조회",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @GetMapping("")
    fun getGroupList(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @RequestHeader(OrganizationHeader.NAME) organizationId: Long,
        @ParameterObject @PageableDefault pageable: Pageable
    ): CommonResDto<Page<GroupGetListResDto>> {
        return CommonResDto.success(groupService.getGroupList(userDetail, organizationId, pageable))
    }

    @Operation(
        summary = "방 상세 조회",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @GetMapping("/{groupId}")
    fun getGroupDetail(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @RequestHeader(OrganizationHeader.NAME) organizationId: Long,
        @PathVariable groupId: Long
    ): CommonResDto<GroupGetDetailResDto> {
        return CommonResDto.success(groupService.getGroupDetail(userDetail, organizationId, groupId))
    }

    @Operation(
        summary = "방 초대 정보 및 멤버 검증",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @GetMapping("/invite/{groupId}")
    fun getGroupInviteInfo(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @PathVariable groupId: Long
    ): CommonResDto<GroupInviteInfoResDto> {
        return CommonResDto.success(groupService.getGroupInviteInfo(userDetail, groupId))
    }

    @ApiResponses(
        value = [
            ApiResponse(responseCode = "401", ref = "#/components/responses/UNAUTHORIZED"),
            ApiResponse(responseCode = "404", ref = "#/components/responses/RESOURCE_NOT_FOUND"),
            ApiResponse(responseCode = "409", ref = "#/components/responses/CONFLICT_DATA")
        ]
    )
    @Operation(
        summary = "방 참여",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @PostMapping("/join")
    fun joinGroup(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @RequestHeader(OrganizationHeader.NAME) organizationId: Long,
        @Valid @RequestBody reqDto: GroupJoinReqDto
    ): CommonResDto<Unit> {
        groupService.joinGroup(userDetail, organizationId, reqDto)
        return CommonResDto.success()
    }

    @ApiResponses(
        value = [
            ApiResponse(responseCode = "401", ref = "#/components/responses/UNAUTHORIZED"),
            ApiResponse(responseCode = "403", ref = "#/components/responses/UNMODIFIABLE_INFORMATION"),
            ApiResponse(responseCode = "404", ref = "#/components/responses/RESOURCE_NOT_FOUND")
        ]
    )
    @Operation(
        summary = "방 나가기",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @DeleteMapping("/leave")
    fun leaveGroup(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @RequestHeader(OrganizationHeader.NAME) organizationId: Long,
        @RequestParam groupId: Long,
        @RequestParam(required = false) delegateUserId: Long?
    ): CommonResDto<Unit> {
        groupService.leaveGroup(userDetail, organizationId, groupId, delegateUserId)
        return CommonResDto.success()
    }

    @Operation(
        summary = "방장 위임",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @PatchMapping("/{groupId}/owner")
    fun transferGroupOwner(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @RequestHeader(OrganizationHeader.NAME) organizationId: Long,
        @PathVariable groupId: Long,
        @Valid @RequestBody reqDto: GroupOwnerTransferReqDto
    ): CommonResDto<Unit> {
        groupService.transferGroupOwner(userDetail, organizationId, groupId, reqDto.delegateUserId)
        return CommonResDto.success()
    }

    @Operation(
        summary = "방 삭제",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @DeleteMapping("/{groupId}")
    fun deleteGroup(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @RequestHeader(OrganizationHeader.NAME) organizationId: Long,
        @PathVariable groupId: Long
    ): CommonResDto<Unit> {
        groupService.deleteGroup(userDetail, organizationId, groupId)
        return CommonResDto.success()
    }

    @Operation(
        summary = "방 참여 멤버 목록 조회",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @GetMapping("/users")
    fun getGroupUsersList(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @RequestHeader(OrganizationHeader.NAME) organizationId: Long,
        @RequestParam groupId: Long
    ): CommonResDto<List<GroupUsersGetListResDto>> {
        return CommonResDto.success(groupService.getGroupUsersList(userDetail, organizationId, groupId))
    }
}
