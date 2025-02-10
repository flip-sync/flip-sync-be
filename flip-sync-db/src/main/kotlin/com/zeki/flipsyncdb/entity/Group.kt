package com.zeki.flipsyncdb.entity

import com.zeki.common.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "`group`", schema = "flip_sync")
@AttributeOverrides(
    AttributeOverride(name = "id", column = Column(name = "id", nullable = false)),
    AttributeOverride(name = "createdAt", column = Column(name = "created_at")),
    AttributeOverride(name = "modifiedAt", column = Column(name = "modified_at"))
)
class Group(
    name: String,
    creator: User
) : BaseEntity() {
    @Column(name = "name", length = 30)
    var name: String = name
        protected set

    @JoinColumn(name = "creator_id")
    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    var creator: User = creator
        protected set

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    var groupUserList: MutableList<GroupUser> = mutableListOf()

    companion object {
        fun create(name: String, creator: User): Group {
            return Group(name, creator)
        }
    }
}