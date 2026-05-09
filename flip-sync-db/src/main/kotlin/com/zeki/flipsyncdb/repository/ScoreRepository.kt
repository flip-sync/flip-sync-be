package com.zeki.flipsyncdb.repository

import com.zeki.flipsyncdb.entity.Group
import com.zeki.flipsyncdb.entity.Organization
import com.zeki.flipsyncdb.entity.Score
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ScoreRepository : JpaRepository<Score, Long> {
    fun deleteByUploadedUserId(uploadedUserId: Long)

    @Modifying
    @Query("delete from Score s where s.group = :group")
    fun deleteByGroup(@Param("group") group: Group)

    @Modifying
    @Query("delete from Score s where s.group.organization = :organization")
    fun deleteByGroup_Organization(@Param("organization") organization: Organization)
}
