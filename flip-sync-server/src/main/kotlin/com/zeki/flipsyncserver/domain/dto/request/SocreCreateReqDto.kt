package com.zeki.flipsyncserver.domain.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.web.multipart.MultipartFile


@Schema(name = "Score Create Req Dto")
data class SocreCreateReqDto(
    @Schema(description = "악보 제목", example = "고요한 거룩한 밤")
    val title: String,
    @Schema(description = "악보 가수", example = "현승범")
    val singer: String,
    @Schema(description = "악보 코드", example = "C")
    val code: String,

    @Size(min = 1)
    @Valid
    val imageList: List<ScoreImageReqDto> = emptyList()
) {

    @Schema(name = "Score Create Req Dto - Score Image Req Dto")
    data class ScoreImageReqDto(
        @Schema(description = "multipart file")
        val file: MultipartFile,
        @Schema(description = "순서", example = "1")
        val order: Int
    )
}
