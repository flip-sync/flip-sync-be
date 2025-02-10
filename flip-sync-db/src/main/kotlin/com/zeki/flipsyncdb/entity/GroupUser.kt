package com.zeki.flipsyncdb.entity

import com.zeki.common.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "group_users", schema = "flip_sync")
class GroupUser private constructor(
    group: Group,
    user: User
) : BaseEntity() {

    @JoinColumn(name = "group_id")
    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    var group: Group = group
        protected set

    @JoinColumn(name = "users_id")
    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    val users: User = user

    companion object {
        fun create(group: Group, user: User): GroupUser {
            return GroupUser(group, user).apply {
                group.groupUserList.add(this)
            }
        }
    }
}