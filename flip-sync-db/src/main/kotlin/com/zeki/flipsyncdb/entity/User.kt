package com.zeki.flipsyncdb.entity

import com.zeki.common.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "users", schema = "flip_sync")
class User private constructor(
    username: String,
    name: String
) : BaseEntity() {
    @Column(name = "username", length = 30)
    var username: String = username

    @Column(name = "name", length = 30)
    var name: String = name

    companion object {
        fun create(username: String, name: String): User {
            return User(username, name)
        }
    }
}