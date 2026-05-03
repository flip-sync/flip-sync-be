package com.zeki.flipsyncserver.domain.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Organization Detail Res Dto")
data class OrganizationDetailResDto(
    @Schema(description = "조직 PK", example = "1")
    val id: Long,
    @Schema(description = "조직 이름", example = "FlipSync Demo")
    val name: String,
    @Schema(description = "초대 코드", example = "AB12CD34")
    val inviteCode: String,
    @Schema(description = "조직장 PK", example = "1")
    val creatorId: Long,
    @Schema(description = "조직장 이름", example = "방장 계정")
    val creatorName: String,
    @Schema(description = "현재 사용자 역할", example = "LEADER")
    val role: String,
    @Schema(description = "현재 사용자가 조직장인지 여부", example = "true")
    val isLeader: Boolean,
    @Schema(description = "멤버 수", example = "2")
    val memberCount: Long,
    @Schema(description = "멤버 목록")
    val members: List<OrganizationMemberResDto>
)
