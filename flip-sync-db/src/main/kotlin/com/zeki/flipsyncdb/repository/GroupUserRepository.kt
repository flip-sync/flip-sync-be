package com.zeki.flipsyncdb.repository

import com.zeki.flipsyncdb.entity.Group
import com.zeki.flipsyncdb.entity.GroupUser
import com.zeki.flipsyncdb.entity.Organization
import com.zeki.flipsyncdb.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GroupUserRepository : JpaRepository<GroupUser, Long> {
    fun findByUsers(users: User): MutableList<GroupUser>
    fun findByUsers(users: User, pageable: Pageable): Page<GroupUser>
    fun findByUsersAndGroup_Organization_Id(users: User, organizationId: Long, pageable: Pageable): Page<GroupUser>
    fun existsByUsersAndGroup(users: User, group: Group): Boolean
    fun existsByGroup_IdAndUsers_Id(groupId: Long, usersId: Long): Boolean
    fun countByGroup_Id(groupId: Long): Long
    fun findByGroup_IdAndUsers_Id(groupId: Long, usersId: Long): MutableList<GroupUser>
    fun findByGroup_Id(groupId: Long): MutableList<GroupUser>
    fun findByGroup_IdOrderByCreatedAtAsc(groupId: Long): MutableList<GroupUser>
    fun deleteByUsers(users: User)

    @Modifying
    @Query("delete from GroupUser gu where gu.group = :group")
    fun deleteByGroup(@Param("group") group: Group)

    @Modifying
    @Query("delete from GroupUser gu where gu.group.id = :groupId and gu.users.id = :usersId")
    fun deleteByGroup_IdAndUsers_Id(@Param("groupId") groupId: Long, @Param("usersId") usersId: Long): Int

    @Modifying
    @Query("delete from GroupUser gu where gu.group.organization = :organization")
    fun deleteByGroup_Organization(@Param("organization") organization: Organization)
}
