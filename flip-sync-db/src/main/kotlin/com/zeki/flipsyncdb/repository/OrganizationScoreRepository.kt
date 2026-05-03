package com.zeki.flipsyncdb.repository

import com.zeki.flipsyncdb.entity.Organization
import com.zeki.flipsyncdb.entity.OrganizationScore
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OrganizationScoreRepository : JpaRepository<OrganizationScore, Long> {
    @Query(
        """
        select score
        from OrganizationScore score
        where score.organization.id = :organizationId
          and (:title is null or score.title like concat('%', :title, '%'))
          and (:singer is null or score.singer like concat('%', :singer, '%'))
          and (:code is null or score.code like concat('%', :code, '%'))
          and (
            :uploadedUserName is null or exists (
              select 1
              from User user
              where user.id = score.uploadedUserId
                and user.name like concat('%', :uploadedUserName, '%')
            )
          )
        """
    )
    fun searchByOrganization(
        @Param("organizationId") organizationId: Long,
        @Param("title") title: String?,
        @Param("singer") singer: String?,
        @Param("code") code: String?,
        @Param("uploadedUserName") uploadedUserName: String?,
        pageable: Pageable
    ): Page<OrganizationScore>

    fun findByIdAndOrganization_Id(id: Long, organizationId: Long): OrganizationScore?

    fun deleteByUploadedUserId(uploadedUserId: Long)
    fun deleteByOrganization(organization: Organization)
}
