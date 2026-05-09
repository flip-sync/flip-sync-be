package com.zeki.flipsyncdb.repository

import com.zeki.flipsyncdb.entity.AppVersionPolicy
import org.springframework.data.jpa.repository.JpaRepository

interface AppVersionPolicyRepository : JpaRepository<AppVersionPolicy, Long> {
    fun findFirstByPlatformIgnoreCase(platform: String): AppVersionPolicy?
}
