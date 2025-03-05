package com.zeki.flipsyncserver.domain.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Score Get Page Request DTO")
data class ScoreGetPageReqDto(
    @Schema(description = "노래 제목", example = "사랑은 늘 도망가")
    val title: String?,
    @Schema(description = "가수", example = "김수희")
    val singer: String?,
    @Schema(description = "코드", example = "C 코드")
    val code: String?,
    @Schema(description = "업로드한 유저 이름", example = "장경태")
    val uploadedUserName: String?,
)
