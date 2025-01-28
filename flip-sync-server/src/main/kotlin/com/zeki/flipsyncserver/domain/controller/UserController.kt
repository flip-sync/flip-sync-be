package com.zeki.flipsyncserver.domain.controller

import com.zeki.common.dto.CommonResDto
import com.zeki.flipsyncserver.domain.dto.request.UserLoginReqDto
import com.zeki.flipsyncserver.domain.dto.request.UserSignupReqDto
import com.zeki.flipsyncserver.domain.dto.request.UserVerifyEmailReqDto
import com.zeki.flipsyncserver.domain.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

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
            ApiResponse(responseCode = "409", ref = "#/components/responses/CONFLICT_DATA"),
        ]
    )
    @Operation(summary = "회원가입", description = "", security = [])
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    fun signup(
        @RequestBody reqDto: UserSignupReqDto
    ): CommonResDto<Long> {

        val data = userService.signup(reqDto)

        return CommonResDto.success(data)
    }


    @ApiResponses(
        value = [
            ApiResponse(responseCode = "401", ref = "#/components/responses/EMAIL_VERIFY_UNAUTHORIZED"),
            ApiResponse(responseCode = "404", ref = "#/components/responses/EMAIL_VERIFY_NOT_FOUND"),
        ]
    )
    @Operation(summary = "이메일 인증 요청", description = "", security = [])
    @GetMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun verifyEmail(
        @RequestParam email: String
    ): CommonResDto<Unit> {

        userService.createVerifyEmail(email)

        return CommonResDto.success()
    }


    @ApiResponses(
        value = [
            ApiResponse(responseCode = "401", ref = "#/components/responses/EMAIL_VERIFY_UNAUTHORIZED"),
            ApiResponse(responseCode = "404", ref = "#/components/responses/EMAIL_VERIFY_NOT_FOUND"),
        ]
    )
    @Operation(summary = "이메일 인증 확인", description = "", security = [])
    @PostMapping("/verify-email/check")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun verifyEmailCheck(
        @RequestBody reqDto: UserVerifyEmailReqDto
    ): CommonResDto<Unit> {

        userService.checkVerifyEmail(reqDto)

        return CommonResDto.success()
    }


    @ApiResponses(
        value = [
            ApiResponse(responseCode = "401", ref = "#/components/responses/UNAUTHORIZED"),
            ApiResponse(responseCode = "404", ref = "#/components/responses/RESOURCE_NOT_FOUND"),
        ]
    )
    @Operation(summary = "로그인", description = "", security = [])
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    fun login(
        @RequestBody reqDto: UserLoginReqDto
    ): CommonResDto<String> {
        userService.login(reqDto.email, reqDto.password)
        return CommonResDto.success()
    }
}