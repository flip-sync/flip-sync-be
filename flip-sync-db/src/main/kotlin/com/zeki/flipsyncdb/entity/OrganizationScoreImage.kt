package com.zeki.flipsyncdb.entity

import com.zeki.common.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "organization_score_image", schema = "flip_sync")
class OrganizationScoreImage private constructor(
    organizationScore: OrganizationScore,
    order: Int,
    url: String
) : BaseEntity() {
    @JoinColumn(name = "organization_score_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    var organizationScore: OrganizationScore = organizationScore

    @Column(name = "`order`", nullable = false)
    var order: Int = order
        protected set

    @Column(name = "url", nullable = false)
    var url: String = url
        protected set

    companion object {
        fun create(organizationScore: OrganizationScore, order: Int, url: String): OrganizationScoreImage {
            return OrganizationScoreImage(
                organizationScore = organizationScore,
                order = order,
                url = url
            ).apply {
                organizationScore.addScoreImage(this)
            }
        }
    }
}
