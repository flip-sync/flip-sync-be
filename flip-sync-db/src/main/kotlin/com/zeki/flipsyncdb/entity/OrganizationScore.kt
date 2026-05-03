package com.zeki.flipsyncdb.entity

import com.zeki.common.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "organization_score", schema = "flip_sync")
class OrganizationScore private constructor(
    organization: Organization,
    title: String,
    code: String,
    singer: String,
    uploadedUserId: Long
) : BaseEntity() {
    @JoinColumn(name = "organization_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    var organization: Organization = organization
        protected set

    @Column(name = "title", length = 50, nullable = false)
    var title: String = title
        protected set

    @Column(name = "code", length = 30, nullable = false)
    var code: String = code
        protected set

    @Column(name = "singer", length = 50, nullable = false)
    var singer: String = singer
        protected set

    @Column(name = "uploaded_user_id", nullable = false)
    var uploadedUserId: Long = uploadedUserId
        protected set

    @OneToMany(
        mappedBy = "organizationScore",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
        orphanRemoval = true
    )
    var scoreImageList: MutableList<OrganizationScoreImage> = mutableListOf()
        protected set

    companion object {
        fun create(
            organization: Organization,
            title: String,
            code: String,
            singer: String,
            uploadedUserId: Long
        ): OrganizationScore {
            return OrganizationScore(
                organization = organization,
                title = title,
                code = code,
                singer = singer,
                uploadedUserId = uploadedUserId
            )
        }
    }

    fun addScoreImage(scoreImage: OrganizationScoreImage) {
        scoreImageList.add(scoreImage)
        scoreImage.organizationScore = this
    }
}
