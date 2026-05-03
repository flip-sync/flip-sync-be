package com.zeki.flipsyncserver.domain.service

import com.zeki.common.exception.ApiException
import com.zeki.common.exception.ResponseCode
import com.zeki.flipsyncdb.entity.Organization
import com.zeki.flipsyncdb.entity.OrganizationMember
import com.zeki.flipsyncdb.entity.User
import com.zeki.flipsyncdb.repository.OrganizationMemberRepository
import com.zeki.flipsyncdb.repository.OrganizationRepository
import com.zeki.flipsyncserver.config.security.UserDetailsImpl
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrganizationAccessService(
    private val organizationRepository: OrganizationRepository,
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val getUserEntityService: GetUserEntityService
) {

    @Transactional(readOnly = true)
    fun getUserEntity(userDetail: UserDetailsImpl): User {
        return getUserEntityService.getUserByUsername(userDetail.username)
    }

    @Transactional(readOnly = true)
    fun getAccessibleOrganization(userDetail: UserDetailsImpl, organizationId: Long?): Organization {
        if (organizationId == null) {
            throw ApiException(ResponseCode.BAD_REQUEST, "조직을 선택해 주세요.")
        }

        val user = getUserEntity(userDetail)
        val organization = organizationRepository.findById(organizationId)
            .orElseThrow { ApiException(ResponseCode.RESOURCE_NOT_FOUND) }

        if (!organizationMemberRepository.existsByUsersAndOrganization(user, organization)) {
            throw ApiException(ResponseCode.FORBIDDEN, "해당 조직에 접근할 수 없습니다.")
        }

        return organization
    }

    @Transactional(readOnly = true)
    fun getAccessibleOrganizationMember(
        userDetail: UserDetailsImpl,
        organizationId: Long?
    ): OrganizationMember {
        val user = getUserEntity(userDetail)
        val organization = getAccessibleOrganization(userDetail, organizationId)

        return organizationMemberRepository.findByUsersAndOrganization(user, organization)
            ?: throw ApiException(ResponseCode.FORBIDDEN, "해당 조직에 접근할 수 없습니다.")
    }
}
