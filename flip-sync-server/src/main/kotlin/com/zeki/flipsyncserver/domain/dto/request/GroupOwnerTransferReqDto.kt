package com.zeki.flipsyncserver.domain.dto.request

import jakarta.validation.constraints.Min

data class GroupOwnerTransferReqDto(
    @field:Min(1)
    val delegateUserId: Long
)
