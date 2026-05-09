package com.zeki.flipsyncserver.domain.dto.response

data class AppVersionPolicyResDto(
    val platform: String,
    val latestVersion: String,
    val latestBuildVersion: Int,
    val minimumBuildVersion: Int,
    val storeUrl: String,
    val forceUpdateMessage: String,
    val optionalUpdateMessage: String
)
