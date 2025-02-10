package com.zeki.flipsyncserver.domain.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Group Get List Res Dto")
data class GroupGetListResDto(
    @Schema(description = "그룹 PK", example = "1")
    val id: Long,
    @Schema(description = "그룹 이름", example = "그룹의 이름")
    val name: String,
    @Schema(description = "생성자 PK", example = "1")
    val creatorId: Long,
    @Schema(description = "그룹 생성자 이름", example = "오재영")
    val creatorName: String,
)
