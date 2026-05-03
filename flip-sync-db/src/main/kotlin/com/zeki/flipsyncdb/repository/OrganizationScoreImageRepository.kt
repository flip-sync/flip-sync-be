package com.zeki.flipsyncdb.repository

import com.zeki.flipsyncdb.entity.Organization
import com.zeki.flipsyncdb.entity.OrganizationScoreImage
import org.springframework.data.jpa.repository.JpaRepository

interface OrganizationScoreImageRepository : JpaRepository<OrganizationScoreImage, Long> {
    fun deleteByOrganizationScore_UploadedUserId(uploadedUserId: Long)
    fun deleteByOrganizationScore_Organization(organization: Organization)
}
