package com.zeki.flipsyncdb.entity

import com.zeki.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint


@Entity
@Table(
    name = "users",
    schema = "flip_sync",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_users_username", columnNames = ["username"])
    ]
)
class User private constructor(
    username: String,
    password: String,
    name: String,
    role: UserRole = UserRole.USER
) : BaseEntity() {
    enum class UserRole {
        ADMIN, USER
    }

    @Column(name = "username", length = 255)
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

    @Column(name = "bio", length = 100)
    var bio: String? = null
        protected set

    @Column(name = "profile_image_url", length = 500)
    var profileImageUrl: String? = null
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

    fun updatePassword(password: String) {
        this.password = password
    }

    fun updateUsername(username: String) {
        this.username = username
    }

    fun updateProfile(name: String, bio: String?) {
        this.name = name
        this.bio = bio?.trim()?.ifBlank { null }
    }

    fun updateProfileImage(profileImageUrl: String?) {
        this.profileImageUrl = profileImageUrl?.trim()?.ifBlank { null }
    }
}
