package com.zeki.flipsyncdb.dto

import com.querydsl.core.annotations.QueryProjection
import com.zeki.common.util.CustomUtils.toStringDateTime
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Score Get Page Response DTO")
class ScoreGetPageResDto @QueryProjection constructor(
    id: Long,
    thumbnail: String,
    title: String,
    singer: String,
    code: String,
    uploadedUserName: String,
    createdAt: LocalDateTime,
    modifiedAt: LocalDateTime,
) {
    @Schema(description = "악보 ID", example = "1")
    val id: Long = id

    @Schema(description = "대표 악보 이미지 URL", example = "https://example.com/score.jpg")
    val thumbnail: String = thumbnail

    @Schema(description = "악보 제목", example = "사랑은 늘 도망가")
    val title: String = title

    @Schema(description = "가수", example = "김수희")
    val singer: String = singer

    @Schema(description = "코드", example = "C 코드")
    val code: String = code

    @Schema(description = "업로드한 유저 이름", example = "장경태")
    val uploadedUserName: String = uploadedUserName

    @Schema(description = "생성일자", example = "2023-10-01 12:00:00")
    val createdAt: String = createdAt.toStringDateTime()

    @Schema(description = "수정일자", example = "2023-10-01 12:30:00")
    val modifiedAt: String = modifiedAt.toStringDateTime()
}
