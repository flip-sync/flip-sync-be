package com.zeki.flipsyncdb.repository

import com.zeki.flipsyncdb.entity.Group
import com.zeki.flipsyncdb.entity.Organization
import com.zeki.flipsyncdb.entity.ScoreImage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SocreImageRepository : JpaRepository<ScoreImage, Long> {
    fun deleteByScore_UploadedUserId(uploadedUserId: Long)

    @Modifying
    @Query("delete from ScoreImage si where si.score.group = :group")
    fun deleteByScore_Group(@Param("group") group: Group)

    @Modifying
    @Query("delete from ScoreImage si where si.score.group.organization = :organization")
    fun deleteByScore_Group_Organization(@Param("organization") organization: Organization)
}
