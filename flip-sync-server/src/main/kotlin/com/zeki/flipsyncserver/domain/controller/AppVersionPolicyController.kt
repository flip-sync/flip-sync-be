package com.zeki.flipsyncserver.domain.controller

import com.zeki.common.dto.CommonResDto
import com.zeki.common.exception.ApiException
import com.zeki.common.exception.ResponseCode
import com.zeki.flipsyncdb.entity.User
import com.zeki.flipsyncserver.config.security.UserDetailsImpl
import com.zeki.flipsyncserver.domain.dto.request.AppVersionPolicyUpdateReqDto
import com.zeki.flipsyncserver.domain.dto.response.AppVersionPolicyResDto
import com.zeki.flipsyncserver.domain.service.AppVersionPolicyService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/app")
@Tag(name = "00. App", description = "Public app metadata API")
class AppVersionPolicyController(
    private val appVersionPolicyService: AppVersionPolicyService
) {
    @Operation(summary = "Get app version policy", description = "", security = [])
    @GetMapping("/version-policy")
    fun getVersionPolicy(
        @RequestParam(defaultValue = "android") platform: String
    ): CommonResDto<AppVersionPolicyResDto> {
        return CommonResDto.success(appVersionPolicyService.getPolicy(platform))
    }

    @Operation(
        summary = "Update app version policy",
        description = "Admin only",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @PutMapping("/version-policy")
    fun updateVersionPolicy(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @Valid @RequestBody reqDto: AppVersionPolicyUpdateReqDto
    ): CommonResDto<AppVersionPolicyResDto> {
        if (userDetail.role != User.UserRole.ADMIN) {
            throw ApiException(ResponseCode.FORBIDDEN)
        }

        return CommonResDto.success(appVersionPolicyService.upsertPolicy(reqDto))
    }
}
