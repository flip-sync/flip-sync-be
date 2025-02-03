package com.zeki.flipsyncserver.domain.dto.request

import io.swagger.v3.oas.annotations.media.Schema


@Schema(name = "Token Req Dto")
data class TokenReqDto(
    val refreshToken: String
)
