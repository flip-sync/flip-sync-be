package com.zeki.flipsyncdb.entity

import com.zeki.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table


@Entity
@Table(name = "users", schema = "flip_sync")
class User private constructor(
    username: String,
    password: String,
    name: String,
    role: UserRole = UserRole.USER
) : BaseEntity() {
    enum class UserRole {
        ADMIN, USER
    }

    @Column(name = "username", length = 30)
    var username: String = username
        protected set

    @Column(name = "role", length = 10)
    var role: UserRole = role
        protected set

    @Column(name = "password", length = 255)
    var password: String = password
        protected set

    @Column(name = "name", length = 30)
    var name: String = name
        protected set

    @Column(name = "refresh_token", length = 500)
    var refreshToken: String? = null
        protected set

    companion object {
        fun create(username: String, password: String, name: String): User {
            return User(username, password, name)
        }
    }

    fun updateRefreshToken(refreshToken: String) {
        this.refreshToken = refreshToken
    }
}