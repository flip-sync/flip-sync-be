package com.zeki.flipsyncdb.repository

import com.zeki.flipsyncdb.entity.Organization
import org.springframework.data.jpa.repository.JpaRepository

interface OrganizationRepository : JpaRepository<Organization, Long> {
    fun existsByInviteCode(inviteCode: String): Boolean
    fun findByInviteCode(inviteCode: String): Organization?
    fun findByNameAndCreator_Id(name: String, creatorId: Long): Organization?
}
