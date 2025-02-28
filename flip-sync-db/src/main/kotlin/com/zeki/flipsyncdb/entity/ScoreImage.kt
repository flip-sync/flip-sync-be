package com.zeki.flipsyncdb.entity

import com.zeki.common.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "score_image", schema = "flip_sync")
class ScoreImage private constructor(
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
        fun create(score: Score, order: Int, url: String): ScoreImage {
            return ScoreImage(score, order, url)
        }
    }
}