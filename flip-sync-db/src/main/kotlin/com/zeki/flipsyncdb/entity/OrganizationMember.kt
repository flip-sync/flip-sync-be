package com.zeki.flipsyncdb.entity

import com.zeki.common.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "organization_members",
    schema = "flip_sync",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_organization_members_organization_user",
            columnNames = ["organization_id", "users_id"]
        )
    ]
)
class OrganizationMember private constructor(
    organization: Organization,
    user: User,
    role: OrganizationRole
) : BaseEntity() {
    enum class OrganizationRole {
        LEADER, MEMBER
    }

    @JoinColumn(name = "organization_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    var organization: Organization = organization
        protected set

    @JoinColumn(name = "users_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    var users: User = user
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    var role: OrganizationRole = role
        protected set

    companion object {
        fun create(
            organization: Organization,
            user: User,
            role: OrganizationRole
        ): OrganizationMember {
            return OrganizationMember(
                organization = organization,
                user = user,
                role = role
            ).apply {
                organization.memberList.add(this)
            }
        }
    }

    fun updateRole(role: OrganizationRole) {
        this.role = role
    }
}
