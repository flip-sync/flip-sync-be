package com.zeki.flipsyncserver.domain.dto.response

data class TokenResDto(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresIn: Long,
)