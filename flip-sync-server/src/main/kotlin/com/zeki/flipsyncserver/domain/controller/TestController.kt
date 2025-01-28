package com.zeki.flipsyncserver.domain.controller

import com.zeki.flipsyncserver.domain.service.EmailService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TestController(
    private val emailService: EmailService
) {

    @GetMapping("/test")
    fun test() {

    }
}