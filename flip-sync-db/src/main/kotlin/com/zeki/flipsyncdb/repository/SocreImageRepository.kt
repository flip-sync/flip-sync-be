package com.zeki.flipsyncdb.repository

import com.zeki.flipsyncdb.entity.Group
import com.zeki.flipsyncdb.entity.Organization
import com.zeki.flipsyncdb.entity.ScoreImage
import org.springframework.data.jpa.repository.JpaRepository

interface SocreImageRepository : JpaRepository<ScoreImage, Long> {
    fun deleteByScore_UploadedUserId(uploadedUserId: Long)
    fun deleteByScore_Group(group: Group)
    fun deleteByScore_Group_Organization(organization: Organization)
}
