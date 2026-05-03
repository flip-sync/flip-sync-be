package com.zeki.flipsyncdb.dto

import com.querydsl.core.annotations.QueryProjection
import com.zeki.common.util.CustomUtils.toStringDateTime
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Score page response")
class ScoreGetPageResDto @QueryProjection constructor(
    id: Long,
    uploadedUserId: Long,
    thumbnail: String,
    title: String,
    singer: String,
    code: String,
    uploadedUserName: String,
    uploadedUserProfileImageUrl: String?,
    createdAt: LocalDateTime,
    modifiedAt: LocalDateTime,
) {
    @Schema(description = "Score id", example = "1")
    val id: Long = id

    @Schema(description = "Uploader user id", example = "12")
    val uploadedUserId: Long = uploadedUserId

    @Schema(description = "Thumbnail url", example = "https://example.com/score.jpg")
    val thumbnail: String = thumbnail

    @Schema(description = "Score title", example = "Moon River")
    val title: String = title

    @Schema(description = "Singer", example = "Audrey Hepburn")
    val singer: String = singer

    @Schema(description = "Code", example = "C")
    val code: String = code

    @Schema(description = "Uploader name", example = "Alice")
    val uploadedUserName: String = uploadedUserName

    @Schema(description = "Uploader profile image url", example = "https://example.com/profile.png")
    val uploadedUserProfileImageUrl: String? = uploadedUserProfileImageUrl

    @Schema(description = "Created at", example = "2026-05-02 12:00:00")
    val createdAt: String = createdAt.toStringDateTime()

    @Schema(description = "Modified at", example = "2026-05-02 12:30:00")
    val modifiedAt: String = modifiedAt.toStringDateTime()
}
