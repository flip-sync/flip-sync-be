package com.zeki.flipsyncdb.repository

import com.zeki.flipsyncdb.entity.Group
import com.zeki.flipsyncdb.entity.Organization
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface GroupRepository : JpaRepository<Group, Long> {
    fun findByCreator_Id(creatorId: Long): MutableList<Group>
    fun findAllByOrganization_Id(organizationId: Long, pageable: Pageable): Page<Group>
    fun deleteByOrganization(organization: Organization)
}
