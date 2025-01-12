package com.zeki.common.controller

import com.zeki.common.dto.CommonResDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
class HealthCheckController {

    @GetMapping("")
    fun healthCheck(): CommonResDto<String> {
        return CommonResDto.success(
            LocalDateTime.now().toString()
        )
    }
}