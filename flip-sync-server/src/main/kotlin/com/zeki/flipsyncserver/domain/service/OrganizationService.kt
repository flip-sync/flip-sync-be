package com.zeki.flipsyncserver.domain.service

import com.zeki.common.exception.ApiException
import com.zeki.common.exception.ResponseCode
import com.zeki.common.util.CustomUtils.toStringDateTime
import com.zeki.flipsyncdb.entity.Organization
import com.zeki.flipsyncdb.entity.OrganizationMember
import com.zeki.flipsyncdb.entity.User
import com.zeki.flipsyncdb.repository.GroupRepository
import com.zeki.flipsyncdb.repository.GroupUserRepository
import com.zeki.flipsyncdb.repository.OrganizationMemberRepository
import com.zeki.flipsyncdb.repository.OrganizationRepository
import com.zeki.flipsyncdb.repository.OrganizationScoreImageRepository
import com.zeki.flipsyncdb.repository.OrganizationScoreRepository
import com.zeki.flipsyncdb.repository.ScoreRepository
import com.zeki.flipsyncdb.repository.SocreImageRepository
import com.zeki.flipsyncserver.config.security.UserDetailsImpl
import com.zeki.flipsyncserver.domain.dto.request.OrganizationCreateReqDto
import com.zeki.flipsyncserver.domain.dto.request.OrganizationJoinReqDto
import com.zeki.flipsyncserver.domain.dto.response.OrganizationDetailResDto
import com.zeki.flipsyncserver.domain.dto.response.OrganizationMemberResDto
import com.zeki.flipsyncserver.domain.dto.response.OrganizationSummaryResDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrganizationService(
    private val organizationRepository: OrganizationRepository,
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val organizationAccessService: OrganizationAccessService,
    private val groupUserRepository: GroupUserRepository,
    private val groupRepository: GroupRepository,
    private val organizationScoreRepository: OrganizationScoreRepository,
    private val organizationScoreImageRepository: OrganizationScoreImageRepository,
    private val scoreRepository: ScoreRepository,
    private val socreImageRepository: SocreImageRepository
) {
    companion object {
        private const val MAX_ORGANIZATION_COUNT = 3L
        private val INVITE_CODE_CHAR_POOL = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }

    @Transactional(readOnly = true)
    fun getMyOrganizations(userDetail: UserDetailsImpl): List<OrganizationSummaryResDto> {
        val user = organizationAccessService.getUserEntity(userDetail)
        return organizationMemberRepository.findByUsersOrderByCreatedAtAsc(user)
            .map { member ->
                member.organization.toSummary(
                    role = member.role,
                    memberCount = organizationMemberRepository.countByOrganization_Id(member.organization.id!!)
                )
            }
    }

    @Transactional(readOnly = true)
    fun getOrganizationDetail(userDetail: UserDetailsImpl, organizationId: Long): OrganizationDetailResDto {
        val member = organizationAccessService.getAccessibleOrganizationMember(userDetail, organizationId)
        val organization = member.organization
        val members = organizationMemberRepository.findByOrganization_IdOrderByCreatedAtAsc(organizationId)
            .map { it.toMemberResponse() }

        return OrganizationDetailResDto(
            id = organization.id!!,
            name = organization.name,
            inviteCode = organization.inviteCode,
            creatorId = organization.creator.id!!,
            creatorName = organization.creator.name,
            role = member.role.name,
            isLeader = member.role == OrganizationMember.OrganizationRole.LEADER,
            memberCount = members.size.toLong(),
            members = members
        )
    }

    @Transactional
    fun createOrganization(
        userDetail: UserDetailsImpl,
        reqDto: OrganizationCreateReqDto
    ): OrganizationSummaryResDto {
        val user = organizationAccessService.getUserEntity(userDetail)
        validateOrganizationLimit(user)

        val organization = organizationRepository.save(
            Organization.create(
                name = reqDto.name,
                inviteCode = generateInviteCode(),
                creator = user
            )
        )
        organizationMemberRepository.save(
            OrganizationMember.create(
                organization = organization,
                user = user,
                role = OrganizationMember.OrganizationRole.LEADER
            )
        )

        return organization.toSummary(
            role = OrganizationMember.OrganizationRole.LEADER,
            memberCount = 1
        )
    }

    @Transactional
    fun joinOrganization(
        userDetail: UserDetailsImpl,
        reqDto: OrganizationJoinReqDto
    ): OrganizationSummaryResDto {
        val user = organizationAccessService.getUserEntity(userDetail)
        val inviteCode = reqDto.inviteCode.trim().uppercase()
        val organization = organizationRepository.findByInviteCode(inviteCode)
            ?: throw ApiException(ResponseCode.RESOURCE_NOT_FOUND, "유효한 초대 코드를 찾지 못했습니다.")

        organizationMemberRepository.findByUsersAndOrganization(user, organization)?.let { existingMember ->
            return organization.toSummary(
                role = existingMember.role,
                memberCount = organizationMemberRepository.countByOrganization_Id(organization.id!!)
            )
        }

        validateOrganizationLimit(user)
        val member = organizationMemberRepository.save(
            OrganizationMember.create(
                organization = organization,
                user = user,
                role = OrganizationMember.OrganizationRole.MEMBER
            )
        )

        return organization.toSummary(
            role = member.role,
            memberCount = organizationMemberRepository.countByOrganization_Id(organization.id!!)
        )
    }

    @Transactional
    fun deleteOrganization(userDetail: UserDetailsImpl, organizationId: Long) {
        val member = organizationAccessService.getAccessibleOrganizationMember(userDetail, organizationId)
        if (member.role != OrganizationMember.OrganizationRole.LEADER) {
            throw ApiException(ResponseCode.FORBIDDEN, "조직장만 조직을 삭제할 수 있습니다.")
        }

        val organization = member.organization
        organizationScoreImageRepository.deleteByOrganizationScore_Organization(organization)
        organizationScoreRepository.deleteByOrganization(organization)
        socreImageRepository.deleteByScore_Group_Organization(organization)
        scoreRepository.deleteByGroup_Organization(organization)
        groupUserRepository.deleteByGroup_Organization(organization)
        groupRepository.deleteByOrganization(organization)
        organizationMemberRepository.deleteByOrganization(organization)
        organizationRepository.delete(organization)
    }

    private fun validateOrganizationLimit(user: User) {
        val count = organizationMemberRepository.countByUsers(user)
        if (count >= MAX_ORGANIZATION_COUNT) {
            throw ApiException(ResponseCode.CONFLICT_DATA, "한 계정은 최대 3개의 소속만 가질 수 있습니다.")
        }
    }

    private fun generateInviteCode(): String {
        repeat(20) {
            val candidate = buildString {
                repeat(8) {
                    append(INVITE_CODE_CHAR_POOL.random())
                }
            }

            if (!organizationRepository.existsByInviteCode(candidate)) {
                return candidate
            }
        }

        throw ApiException(ResponseCode.INTERNAL_SERVER_ERROR, "조직 초대 코드를 생성하지 못했습니다.")
    }

    private fun Organization.toSummary(
        role: OrganizationMember.OrganizationRole,
        memberCount: Long
    ): OrganizationSummaryResDto {
        return OrganizationSummaryResDto(
            id = id!!,
            name = name,
            inviteCode = inviteCode,
            creatorId = creator.id!!,
            creatorName = creator.name,
            memberCount = memberCount,
            role = role.name,
            isLeader = role == OrganizationMember.OrganizationRole.LEADER
        )
    }

    private fun OrganizationMember.toMemberResponse(): OrganizationMemberResDto {
        return OrganizationMemberResDto(
            userId = users.id!!,
            email = users.username,
            name = users.name,
            profileImageUrl = users.profileImageUrl,
            role = role.name,
            joinedAt = createdAt.toStringDateTime()
        )
    }
}
