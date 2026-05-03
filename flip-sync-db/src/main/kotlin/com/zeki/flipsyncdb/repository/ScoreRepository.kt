package com.zeki.flipsyncdb.repository

import com.zeki.flipsyncdb.entity.Group
import com.zeki.flipsyncdb.entity.Organization
import com.zeki.flipsyncdb.entity.Score
import org.springframework.data.jpa.repository.JpaRepository

interface ScoreRepository : JpaRepository<Score, Long> {
    fun deleteByUploadedUserId(uploadedUserId: Long)
    fun deleteByGroup(group: Group)
    fun deleteByGroup_Organization(organization: Organization)
}
