package com.zeki.flipsyncserver.domain.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Score Get Detail Res Dto")
data class ScoreGetDetailResDto(
    @Schema(description = "악보 ID", example = "1")
    val id: Long,
    @Schema(description = "악보 제목", example = "주님 큰 영광 받으소서")
    val title: String,
    @Schema(description = "가수 또는 아티스트", example = "Various Artists")
    val singer: String,
    @Schema(description = "악보 코드", example = "C")
    val code: String,
    @Schema(description = "업로드한 유저 ID", example = "1")
    val uploadedUserId: Long,
    @Schema(description = "업로드한 유저 이름", example = "방장 계정")
    val uploadedUserName: String,
    @Schema(description = "악보 이미지 목록")
    val scoreImageList: List<ScoreImageResDto>
) {
    @Schema(name = "Score Get Detail Res Dto - Image")
    data class ScoreImageResDto(
        @Schema(description = "악보 이미지 ID", example = "1")
        val id: Long,
        @Schema(description = "악보 이미지 URL", example = "https://example.com/image.jpg")
        val url: String,
        @Schema(description = "악보 이미지 순서", example = "1")
        val order: Int
    )
}
