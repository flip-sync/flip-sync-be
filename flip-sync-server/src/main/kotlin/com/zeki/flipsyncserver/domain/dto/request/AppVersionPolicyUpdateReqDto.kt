package com.zeki.flipsyncserver.domain.dto.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class AppVersionPolicyUpdateReqDto(
    @field:NotBlank
    val platform: String = "android",
    @field:NotBlank
    val latestVersion: String,
    @field:Min(1)
    val latestBuildVersion: Int,
    @field:Min(1)
    val minimumBuildVersion: Int,
    @field:NotBlank
    val storeUrl: String,
    val forceUpdateMessage: String? = null,
    val optionalUpdateMessage: String? = null
)
