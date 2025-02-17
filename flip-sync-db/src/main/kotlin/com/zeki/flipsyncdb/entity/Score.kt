package com.zeki.flipsyncdb.entity

import com.zeki.common.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "score", schema = "flip_sync")
class Score private constructor(
    group: Group,
    title: String,
    code: String,
    singer: String,
    uploadedUserId: Long
) : BaseEntity() {
    @JoinColumn(name = "group_id")
    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    var group: Group = group

    @Column(name = "title", length = 50)
    var title: String = title
        protected set

    @Column(name = "code", length = 30)
    var code: String = code
        protected set

    @Column(name = "singer", length = 50)
    var singer: String = singer
        protected set

    @Column(name = "uploaded_user_id")
    var uploadedUserId: Long = uploadedUserId
        protected set

    companion object {
        fun create(
            group: Group,
            title: String,
            code: String,
            singer: String,
            uploadedUserId: Long
        ): Score {
            return Score(group, title, code, singer, uploadedUserId)
        }
    }
}