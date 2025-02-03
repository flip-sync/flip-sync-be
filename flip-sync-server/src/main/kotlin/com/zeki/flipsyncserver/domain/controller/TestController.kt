package com.zeki.flipsyncserver.domain.controller

import com.zeki.flipsyncserver.config.security.UserDetailsImpl
import com.zeki.flipsyncserver.domain.service.EmailService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController()
@RequestMapping("/test")
class TestController(
    private val emailService: EmailService
) {

    @GetMapping("/token")
    @Operation(summary = "테스트", description = "", security = [SecurityRequirement(name = "Authorization")])
    fun test(
        @AuthenticationPrincipal userDetail: UserDetailsImpl
    ) {
        println("test")
    }
}