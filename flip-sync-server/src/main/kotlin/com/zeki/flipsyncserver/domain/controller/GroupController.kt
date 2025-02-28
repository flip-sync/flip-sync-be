package com.zeki.flipsyncserver.domain.controller

import com.zeki.common.dto.CommonResDto
import com.zeki.flipsyncserver.config.security.UserDetailsImpl
import com.zeki.flipsyncserver.domain.dto.request.GroupCreateReqDto
import com.zeki.flipsyncserver.domain.dto.response.GroupGetListResDto
import com.zeki.flipsyncserver.domain.dto.response.GroupUsersGetListResDto
import com.zeki.flipsyncserver.domain.service.GroupService
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
            ApiResponse(responseCode = "409", ref = "#/components/responses/CONFLICT_DATA"),
        ]
    )
    @Operation(
        summary = "그룹 생성",
        description = "",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @PostMapping("")
    fun createGroup(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @Valid @RequestBody reqDto: GroupCreateReqDto
    ): CommonResDto<Long> {
        val data = groupService.createGroup(userDetail, reqDto)
        return CommonResDto.success(data)
    }

    @ApiResponses(
        value = [
            ApiResponse(responseCode = "401", ref = "#/components/responses/UNAUTHORIZED"),
            ApiResponse(responseCode = "404", ref = "#/components/responses/RESOURCE_NOT_FOUND"),
        ]
    )
    @Operation(
        summary = "내가 속한 그룹 전체 조회",
        description = "",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @GetMapping("/my")
    public fun getMyGroupList(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @ParameterObject @PageableDefault pageable: Pageable
    ): CommonResDto<Page<GroupGetListResDto>> {
        val data = groupService.getMyGroupList(userDetail, pageable)
        return CommonResDto.success(data)
    }

    @ApiResponses(
        value = [
            ApiResponse(responseCode = "401", ref = "#/components/responses/UNAUTHORIZED"),
            ApiResponse(responseCode = "404", ref = "#/components/responses/RESOURCE_NOT_FOUND"),
        ]
    )
    @Operation(
        summary = "참여 가능한 그룹 전체 조회",
        description = "",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @GetMapping("")
    public fun getGroupList(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @ParameterObject @PageableDefault pageable: Pageable
    ): CommonResDto<Page<GroupGetListResDto>> {
        val data = groupService.getGroupList(userDetail, pageable)
        return CommonResDto.success(data)
    }

    @ApiResponses(
        value = [
            ApiResponse(responseCode = "401", ref = "#/components/responses/UNAUTHORIZED"),
            ApiResponse(responseCode = "404", ref = "#/components/responses/RESOURCE_NOT_FOUND"),
            ApiResponse(responseCode = "409", ref = "#/components/responses/CONFLICT_DATA"),
        ]
    )
    @Operation(
        summary = "그룹 참여",
        description = "",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @PostMapping("/join")
    public fun joinGroup(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @RequestParam groupId: Long
    ): CommonResDto<Unit> {
        groupService.joinGroup(userDetail, groupId)
        return CommonResDto.success()
    }

    @ApiResponses(
        value = [
            ApiResponse(responseCode = "401", ref = "#/components/responses/UNAUTHORIZED"),
            ApiResponse(
                responseCode = "403",
                ref = "#/components/responses/UNMODIFIABLE_INFORMATION"
            ),
            ApiResponse(responseCode = "404", ref = "#/components/responses/RESOURCE_NOT_FOUND"),
        ]
    )
    @Operation(
        summary = "그룹 떠나기",
        description = """
            그룹에 남은인원이 1명이면 그룹도 삭제됩니다.
            생성자 이면서 남은 인원이 2명이상 이라면 그룹을 떠날 수 없음. (403_1 에러 발생)
            """,
        security = [SecurityRequirement(name = "Authorization")]
    )
    @DeleteMapping("/leave")
    public fun leaveGroup(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @RequestParam groupId: Long
    ): CommonResDto<Unit> {
        groupService.leaveGroup(userDetail, groupId)
        return CommonResDto.success()
    }

    @ApiResponses(
        value = [
            ApiResponse(responseCode = "401", ref = "#/components/responses/UNAUTHORIZED"),
            ApiResponse(
                responseCode = "403",
                ref = "#/components/responses/UNMODIFIABLE_INFORMATION"
            ),
            ApiResponse(responseCode = "404", ref = "#/components/responses/RESOURCE_NOT_FOUND"),
        ]
    )
    @Operation(
        summary = "그룹 참여자 전체 조회",
        description = "",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @GetMapping("/users")
    public fun getGroupUsersList(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @RequestParam groupId: Long
    ): CommonResDto<List<GroupUsersGetListResDto>> {
        val data = groupService.getGroupUsersList(userDetail, groupId)
        return CommonResDto.success(data)
    }
}