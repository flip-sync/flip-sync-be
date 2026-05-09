package com.zeki.flipsyncserver.domain.controller

import com.zeki.common.dto.CommonResDto
import com.zeki.flipsyncserver.config.security.UserDetailsImpl
import com.zeki.flipsyncserver.domain.dto.request.TokenReqDto
import com.zeki.flipsyncserver.domain.dto.request.UserDeleteAccountReqDto
import com.zeki.flipsyncserver.domain.dto.request.UserLoginReqDto
import com.zeki.flipsyncserver.domain.dto.request.UserResetPasswordReqDto
import com.zeki.flipsyncserver.domain.dto.request.UserSignupReqDto
import com.zeki.flipsyncserver.domain.dto.request.UserUpdateEmailReqDto
import com.zeki.flipsyncserver.domain.dto.request.UserUpdateProfileReqDto
import com.zeki.flipsyncserver.domain.dto.request.UserVerifyEmailReqDto
import com.zeki.flipsyncserver.domain.dto.response.TokenResDto
import com.zeki.flipsyncserver.domain.dto.response.UserProfileResDto
import com.zeki.flipsyncserver.domain.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/user")
@Tag(name = "01. User", description = "User API")
class UserController(
    private val userService: UserService
) {

    @ApiResponses(
        value = [
            ApiResponse(responseCode = "401", ref = "#/components/responses/EMAIL_VERIFY_UNAUTHORIZED"),
            ApiResponse(responseCode = "404", ref = "#/components/responses/EMAIL_VERIFY_NOT_FOUND"),
            ApiResponse(responseCode = "409", ref = "#/components/responses/CONFLICT_DATA")
        ]
    )
    @Operation(summary = "Sign up", description = "", security = [])
    @PostMapping("/signup")
    fun signup(
        @Valid @RequestBody reqDto: UserSignupReqDto
    ): CommonResDto<Long> {
        val data = userService.signup(reqDto)
        return CommonResDto.success(data)
    }

    @ApiResponses(
        value = [
            ApiResponse(responseCode = "401", ref = "#/components/responses/EMAIL_VERIFY_UNAUTHORIZED"),
            ApiResponse(responseCode = "404", ref = "#/components/responses/EMAIL_VERIFY_NOT_FOUND"),
            ApiResponse(responseCode = "429", ref = "#/components/responses/TOO_MANY_REQUESTS")
        ]
    )
    @Operation(summary = "Request email verification", description = "", security = [])
    @GetMapping("/verify-email")
    fun verifyEmail(
        @RequestParam email: String
    ): CommonResDto<Unit> {
        userService.createVerifyEmail(email)
        return CommonResDto.success()
    }

    @ApiResponses(
        value = [
            ApiResponse(responseCode = "401", ref = "#/components/responses/UNAUTHORIZED_EMAIL_VERIFY"),
            ApiResponse(responseCode = "404", ref = "#/components/responses/EMAIL_VERIFY_NOT_FOUND")
        ]
    )
    @Operation(summary = "Check email verification", description = "", security = [])
    @PostMapping("/verify-email/check")
    fun verifyEmailCheck(
        @Valid @RequestBody reqDto: UserVerifyEmailReqDto
    ): CommonResDto<Unit> {
        userService.checkVerifyEmail(reqDto)
        return CommonResDto.success()
    }

    @ApiResponses(
        value = [
            ApiResponse(responseCode = "401", ref = "#/components/responses/UNAUTHORIZED"),
            ApiResponse(responseCode = "404", ref = "#/components/responses/RESOURCE_NOT_FOUND")
        ]
    )
    @Operation(summary = "Login", description = "", security = [])
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    fun login(
        @Valid @RequestBody reqDto: UserLoginReqDto
    ): CommonResDto<TokenResDto> {
        val data = userService.login(reqDto.email, reqDto.password)
        return CommonResDto.success(data)
    }

    @ApiResponses(
        value = [
            ApiResponse(responseCode = "401", ref = "#/components/responses/EXPIRED_TOKEN"),
            ApiResponse(responseCode = "403", ref = "#/components/responses/FORBIDDEN")
        ]
    )
    @Operation(summary = "Refresh login token", description = "", security = [])
    @PostMapping("/login/refresh")
    fun loginRefresh(
        @RequestBody reqDto: TokenReqDto
    ): CommonResDto<TokenResDto> {
        val data = userService.loginRefresh(reqDto.refreshToken)
        return CommonResDto.success(data)
    }

    @ApiResponses(
        value = [
            ApiResponse(responseCode = "401", ref = "#/components/responses/UNAUTHORIZED"),
            ApiResponse(responseCode = "404", ref = "#/components/responses/RESOURCE_NOT_FOUND")
        ]
    )
    @Operation(summary = "Reset password", description = "", security = [])
    @PostMapping("/reset-password")
    fun resetPassword(
        @Valid @RequestBody reqDto: UserResetPasswordReqDto
    ): CommonResDto<Unit> {
        userService.resetPassword(reqDto)
        return CommonResDto.success()
    }

    @Operation(
        summary = "Get my profile",
        description = "",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @GetMapping("/me")
    fun getProfile(
        @AuthenticationPrincipal userDetail: UserDetailsImpl
    ): CommonResDto<UserProfileResDto> {
        val data = userService.getProfile(userDetail.username)
        return CommonResDto.success(data)
    }

    @Operation(
        summary = "Update my profile",
        description = "",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @PutMapping("/me")
    fun updateProfile(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @Valid @RequestBody reqDto: UserUpdateProfileReqDto
    ): CommonResDto<UserProfileResDto> {
        val data = userService.updateProfile(userDetail.username, reqDto)
        return CommonResDto.success(data)
    }

    @Operation(
        summary = "Update my profile image",
        description = "",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @PutMapping("/me/profile-image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateProfileImage(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @RequestPart("file") file: MultipartFile
    ): CommonResDto<UserProfileResDto> {
        val data = userService.updateProfileImage(userDetail.username, file)
        return CommonResDto.success(data)
    }

    @Operation(
        summary = "Update my email",
        description = "",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @PutMapping("/me/email")
    fun updateEmail(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @Valid @RequestBody reqDto: UserUpdateEmailReqDto
    ): CommonResDto<TokenResDto> {
        val data = userService.updateEmail(userDetail.username, reqDto)
        return CommonResDto.success(data)
    }

    @Operation(
        summary = "Delete my account",
        description = "",
        security = [SecurityRequirement(name = "Authorization")]
    )
    @DeleteMapping("/me")
    fun deleteAccount(
        @AuthenticationPrincipal userDetail: UserDetailsImpl,
        @Valid @RequestBody reqDto: UserDeleteAccountReqDto
    ): CommonResDto<Unit> {
        userService.deleteAccount(userDetail.username, reqDto)
        return CommonResDto.success()
    }
}
