package com.zeki.flipsyncdb.repository

import com.zeki.flipsyncdb.entity.Organization
import com.zeki.flipsyncdb.entity.OrganizationMember
import com.zeki.flipsyncdb.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface OrganizationMemberRepository : JpaRepository<OrganizationMember, Long> {
    fun findByUsersOrderByCreatedAtAsc(users: User): MutableList<OrganizationMember>
    fun findByOrganization_IdOrderByCreatedAtAsc(organizationId: Long): MutableList<OrganizationMember>
    fun findByUsersAndOrganization(users: User, organization: Organization): OrganizationMember?
    fun existsByUsersAndOrganization(users: User, organization: Organization): Boolean
    fun countByUsers(users: User): Long
    fun countByOrganization_Id(organizationId: Long): Long
    fun deleteByUsers(users: User)
    fun deleteByOrganization(organization: Organization)
}
