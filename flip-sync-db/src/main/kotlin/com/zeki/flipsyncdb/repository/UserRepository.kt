package com.zeki.flipsyncdb.repository

import com.zeki.flipsyncdb.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByUsername(username: String): User?
}