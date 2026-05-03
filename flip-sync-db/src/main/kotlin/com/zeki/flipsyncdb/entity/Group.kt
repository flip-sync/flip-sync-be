package com.zeki.flipsyncdb.entity

import com.zeki.common.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "`group`",
    schema = "flip_sync",
    indexes = [
        Index(name = "group_creator_id_index", columnList = "creator_id"),
        Index(name = "group_organization_id_index", columnList = "organization_id")
    ]
)
@AttributeOverrides(
    AttributeOverride(name = "id", column = Column(name = "id", nullable = false)),
    AttributeOverride(name = "createdAt", column = Column(name = "created_at")),
    AttributeOverride(name = "modifiedAt", column = Column(name = "modified_at"))
)
class Group(
    name: String,
    creator: User,
    organization: Organization,
    maxMemberCount: Int,
    roomPassword: String?
) : BaseEntity() {
    @Column(name = "name", length = 30)
    var name: String = name
        protected set

    @JoinColumn(name = "creator_id")
    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    var creator: User = creator
        protected set

    @JoinColumn(name = "organization_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    var organization: Organization = organization
        protected set

    @Column(name = "max_member_count", nullable = false)
    var maxMemberCount: Int = maxMemberCount
        protected set

    @Column(name = "room_password", length = 255)
    var roomPassword: String? = roomPassword
        protected set

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    var groupUserList: MutableList<GroupUser> = mutableListOf()

    companion object {
        fun create(
            name: String,
            creator: User,
            organization: Organization,
            maxMemberCount: Int,
            roomPassword: String?
        ): Group {
            return Group(name, creator, organization, maxMemberCount, roomPassword)
        }
    }

    fun hasPassword(): Boolean = !roomPassword.isNullOrBlank()

    fun updateCreator(creator: User) {
        this.creator = creator
    }
}
