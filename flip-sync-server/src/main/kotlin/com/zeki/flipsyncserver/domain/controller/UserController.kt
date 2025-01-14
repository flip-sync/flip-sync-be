package com.zeki.flipsyncserver.domain.controller

import com.zeki.common.dto.CommonResDto
import com.zeki.flipsyncserver.domain.dto.request.UserSignupReqDto
import com.zeki.flipsyncserver.domain.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/user")
@Tag(name = "01. User", description = "User API")
class UserController(
    private val userService: UserService
) {

    @Operation(
        summary = "회원가입",
        description = ""
    )
    @PostMapping("/signup")
    fun signup(
        @RequestBody reqDto: UserSignupReqDto
    ): CommonResDto<Long> {

        val data = userService.signup(reqDto)

        return CommonResDto.success(data)
    }

    @Operation(
        summary = "이메일 인증 요청",
        description = ""
    )
    @GetMapping("/verify-email")
    fun verifyEmail(
        @RequestParam email: String
    ): CommonResDto<Unit> {

        userService.createVerifyEmail(email)

        return CommonResDto.success()
    }

//    @Operation(
//        summary = "이메일 인증 확인",
//        description = ""
//    )
//    @PostMapping("/verify-email/check")
//    fun verifyEmailCheck(
//        @RequestParam email: String,
//        @RequestParam code: String
//    ): CommonResDto<Unit> {
//
//        userService.checkVerifyEmail(email, code)
//
//        return CommonResDto.success()
//    }
}