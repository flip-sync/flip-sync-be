package com.zeki.flipsyncdb.repository

import com.zeki.flipsyncdb.entity.Group
import com.zeki.flipsyncdb.entity.GroupUser
import com.zeki.flipsyncdb.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface GroupUserRepository : JpaRepository<GroupUser, Long> {
    fun findByUsers(users: User, pageable: Pageable): Page<GroupUser>
    fun existsByUsersAndGroup(users: User, group: Group): Boolean
    fun findByGroup_IdAndUsers_Id(groupId: Long, usersId: Long): MutableList<GroupUser>
    fun findByGroup_Id(groupId: Long): MutableList<GroupUser>
}