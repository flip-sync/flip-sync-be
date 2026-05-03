package com.zeki.flipsyncdb.entity

import com.zeki.common.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "organizations",
    schema = "flip_sync",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_organizations_invite_code", columnNames = ["invite_code"])
    ]
)
class Organization private constructor(
    name: String,
    inviteCode: String,
    creator: User
) : BaseEntity() {
    @Column(name = "name", length = 50, nullable = false)
    var name: String = name
        protected set

    @Column(name = "invite_code", length = 20, nullable = false)
    var inviteCode: String = inviteCode
        protected set

    @JoinColumn(name = "creator_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    var creator: User = creator
        protected set

    @OneToMany(mappedBy = "organization", fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    var memberList: MutableList<OrganizationMember> = mutableListOf()
        protected set

    companion object {
        fun create(name: String, inviteCode: String, creator: User): Organization {
            return Organization(
                name = name.trim(),
                inviteCode = inviteCode.trim(),
                creator = creator
            )
        }
    }

    fun updateName(name: String) {
        this.name = name.trim()
    }

    fun updateCreator(creator: User) {
        this.creator = creator
    }
}
