package com.zeki.flipsyncdb.entity

import com.zeki.common.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "socre_image", schema = "flip_sync")
class SocreImage private constructor(
    score: Score,
    order: Int,
    url: String
) : BaseEntity() {
    @JoinColumn(name = "score_id")
    @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    var score: Score = score

    @Column(name = "`order`")
    var order: Int = order
        protected set

    @Column(name = "url")
    var url: String = url
        protected set

    companion object {
        fun create(score: Score, order: Int, url: String): SocreImage {
            return SocreImage(score, order, url)
        }
    }
}