package com.zeki.flipsyncserver.domain.service

import com.zeki.common.exception.ApiException
import com.zeki.common.exception.ResponseCode
import com.zeki.common.util.CustomUtils.toStringDateTime
import com.zeki.flipsyncdb.entity.Group
import com.zeki.flipsyncdb.entity.GroupUser
import com.zeki.flipsyncdb.repository.GroupRepository
import com.zeki.flipsyncdb.repository.GroupUserRepository
import com.zeki.flipsyncdb.repository.ScoreRepository
import com.zeki.flipsyncdb.repository.SocreImageRepository
import com.zeki.flipsyncserver.config.security.UserDetailsImpl
import com.zeki.flipsyncserver.domain.dto.request.GroupCreateReqDto
import com.zeki.flipsyncserver.domain.dto.request.GroupJoinReqDto
import com.zeki.flipsyncserver.domain.dto.response.GroupGetDetailResDto
import com.zeki.flipsyncserver.domain.dto.response.GroupInviteInfoResDto
import com.zeki.flipsyncserver.domain.dto.response.GroupGetListResDto
import com.zeki.flipsyncserver.domain.dto.response.GroupUsersGetListResDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroupService(
    private val groupRepository: GroupRepository,
    private val groupUserRepository: GroupUserRepository,
    private val scoreRepository: ScoreRepository,
    private val socreImageRepository: SocreImageRepository,
    private val getUserEntityService: GetUserEntityService,
    private val organizationAccessService: OrganizationAccessService,
    private val passwordEncoder: PasswordEncoder
) {
    @Transactional
    fun createGroup(userDetail: UserDetailsImpl, organizationId: Long, reqDto: GroupCreateReqDto): Long {
        val userEntity = getUserEntityService.getUserByUsername(userDetail.username)
        val organization = organizationAccessService.getAccessibleOrganization(userDetail, organizationId)
        val normalizedPassword = reqDto.password?.trim().orEmpty()
        val encodedPassword = normalizedPassword.takeIf { it.isNotBlank() }?.let { passwordEncoder.encode(it) }
        val group = Group.create(
            name = reqDto.name,
            creator = userEntity,
            organization = organization,
            maxMemberCount = reqDto.maxMemberCount,
            roomPassword = encodedPassword
        )

        groupRepository.save(group)
        groupUserRepository.save(GroupUser.create(group, userEntity))
        return group.id!!
    }

    @Transactional(readOnly = true)
    fun getMyGroupList(
        userDetail: UserDetailsImpl,
        organizationId: Long,
        pageable: Pageable
    ): Page<GroupGetListResDto> {
        val userEntity = getUserEntityService.getUserByUsername(userDetail.username)
        organizationAccessService.getAccessibleOrganization(userDetail, organizationId)
        val groupUsersPage = groupUserRepository.findByUsersAndGroup_Organization_Id(userEntity, organizationId, pageable)

        return groupUsersPage.map { it.group }.map { group ->
            GroupGetListResDto(
                id = group.id!!,
                organizationId = group.organization.id!!,
                name = group.name,
                creatorId = group.creator.id!!,
                creatorName = group.creator.name,
                currentMemberCount = group.groupUserList.size,
                maxMemberCount = group.maxMemberCount,
                hasPassword = group.hasPassword()
            )
        }
    }

    @Transactional(readOnly = true)
    fun getGroupList(
        userDetail: UserDetailsImpl,
        organizationId: Long,
        pageable: Pageable
    ): Page<GroupGetListResDto> {
        organizationAccessService.getAccessibleOrganization(userDetail, organizationId)
        val allGroupPage = groupRepository.findAllByOrganization_Id(organizationId, pageable)

        return allGroupPage.map { group ->
            GroupGetListResDto(
                id = group.id!!,
                organizationId = group.organization.id!!,
                name = group.name,
                creatorId = group.creator.id!!,
                creatorName = group.creator.name,
                currentMemberCount = group.groupUserList.size,
                maxMemberCount = group.maxMemberCount,
                hasPassword = group.hasPassword()
            )
        }
    }

    @Transactional
    fun joinGroup(userDetail: UserDetailsImpl, organizationId: Long, reqDto: GroupJoinReqDto) {
        val userEntity = getUserEntityService.getUserByUsername(userDetail.username)
        val group = getGroupEntity(userDetail, organizationId, reqDto.groupId)

        val existsByUsersAndGroup = groupUserRepository.existsByUsersAndGroup(userEntity, group)
        if (existsByUsersAndGroup) {
            throw ApiException(ResponseCode.CONFLICT_DATA)
        }

        if (group.groupUserList.size >= group.maxMemberCount) {
            throw ApiException(ResponseCode.CONFLICT_DATA, "ROOM_MEMBER_LIMIT_REACHED")
        }

        if (group.hasPassword()) {
            val password = reqDto.password?.trim().orEmpty()
            if (!passwordEncoder.matches(password, group.roomPassword.orEmpty())) {
                throw ApiException(ResponseCode.FORBIDDEN, "INVALID_ROOM_PASSWORD")
            }
        }

        groupUserRepository.save(GroupUser.create(group = group, user = userEntity))
    }

    @Transactional
    fun leaveGroup(userDetail: UserDetailsImpl, organizationId: Long, groupId: Long, delegateUserId: Long? = null) {
        val userEntity = getUserEntityService.getUserByUsername(userDetail.username)
        val group = getGroupEntity(userDetail, organizationId, groupId)
        val groupUsersList = groupUserRepository.findByGroup_IdAndUsers_Id(groupId, userEntity.id!!)

        if (groupUsersList.isEmpty()) {
            throw ApiException(ResponseCode.RESOURCE_NOT_FOUND)
        }

        val joinedMember = groupUsersList.first()
        val groupMembers = groupUserRepository.findByGroup_Id(groupId)

        if (groupMembers.size == 1) {
            deleteGroupFully(group)
            return
        }

        if (group.creator.id == userEntity.id && groupMembers.size > 1) {
            if (delegateUserId == null) {
                throw ApiException(ResponseCode.UNMODIFIABLE_INFORMATION, "ROOM_OWNER_DELEGATE_REQUIRED")
            }

            transferGroupOwnerInternal(group, userEntity.id!!, delegateUserId)
        }

        groupUserRepository.delete(joinedMember)
    }

    @Transactional
    fun transferGroupOwner(
        userDetail: UserDetailsImpl,
        organizationId: Long,
        groupId: Long,
        delegateUserId: Long
    ) {
        val userEntity = getUserEntityService.getUserByUsername(userDetail.username)
        val group = getGroupEntity(userDetail, organizationId, groupId)

        transferGroupOwnerInternal(group, userEntity.id!!, delegateUserId)
    }

    @Transactional
    fun deleteGroup(userDetail: UserDetailsImpl, organizationId: Long, groupId: Long) {
        val userEntity = getUserEntityService.getUserByUsername(userDetail.username)
        val group = getGroupEntity(userDetail, organizationId, groupId)

        if (group.creator.id != userEntity.id) {
            throw ApiException(ResponseCode.FORBIDDEN, "ROOM_OWNER_ONLY")
        }

        deleteGroupFully(group)
    }

    @Transactional(readOnly = true)
    fun getGroupUsersList(
        userDetail: UserDetailsImpl,
        organizationId: Long,
        groupId: Long
    ): List<GroupUsersGetListResDto> {
        getUserEntityService.getUserByUsername(userDetail.username)
        val group = getGroupEntity(userDetail, organizationId, groupId)

        return group.groupUserList.map {
            GroupUsersGetListResDto(
                id = it.users.id!!,
                name = it.users.name,
                profileImageUrl = it.users.profileImageUrl,
                joinedAt = it.createdAt.toStringDateTime()
            )
        }
    }

    @Transactional(readOnly = true)
    fun getGroupDetail(
        userDetail: UserDetailsImpl,
        organizationId: Long,
        groupId: Long
    ): GroupGetDetailResDto {
        val userEntity = getUserEntityService.getUserByUsername(userDetail.username)
        val group = getGroupEntity(userDetail, organizationId, groupId)

        if (!group.groupUserList.map { it.users.id }.contains(userEntity.id)) {
            throw ApiException(ResponseCode.FORBIDDEN)
        }

        return GroupGetDetailResDto(
            id = group.id!!,
            organizationId = group.organization.id!!,
            name = group.name,
            creatorId = group.creator.id!!,
            creatorName = group.creator.name,
            currentUserId = userEntity.id!!,
            currentUserName = userEntity.name,
            currentUserProfileImageUrl = userEntity.profileImageUrl,
            currentUserIsCreator = group.creator.id == userEntity.id,
            currentMemberCount = group.groupUserList.size,
            maxMemberCount = group.maxMemberCount,
            hasPassword = group.hasPassword()
        )
    }

    @Transactional(readOnly = true)
    fun getGroupInviteInfo(userDetail: UserDetailsImpl, groupId: Long): GroupInviteInfoResDto {
        val userEntity = getUserEntityService.getUserByUsername(userDetail.username)
        val group = groupRepository.findById(groupId)
            .orElseThrow { ApiException(ResponseCode.RESOURCE_NOT_FOUND) }
        val organization = group.organization
        val organizationMember = organizationAccessService.getAccessibleOrganizationMember(userDetail, organization.id)
        val joined = groupUserRepository.existsByUsersAndGroup(userEntity, group)

        return GroupInviteInfoResDto(
            groupId = group.id!!,
            groupName = group.name,
            organizationId = organization.id!!,
            organizationName = organization.name,
            organizationInviteCode = organization.inviteCode,
            organizationCreatorId = organization.creator.id!!,
            organizationCreatorName = organization.creator.name,
            organizationMemberCount = organization.memberList.size.toLong(),
            organizationRole = organizationMember.role.name,
            organizationIsLeader = organizationMember.role.name == "LEADER",
            creatorId = group.creator.id!!,
            creatorName = group.creator.name,
            currentMemberCount = group.groupUserList.size,
            maxMemberCount = group.maxMemberCount,
            hasPassword = group.hasPassword(),
            joined = joined
        )
    }

    @Transactional(readOnly = true)
    fun getGroupDetailForSocket(userDetail: UserDetailsImpl, groupId: Long): GroupGetDetailResDto {
        val userEntity = getUserEntityService.getUserByUsername(userDetail.username)
        val group = groupRepository.findById(groupId)
            .orElseThrow { ApiException(ResponseCode.RESOURCE_NOT_FOUND) }

        if (!group.groupUserList.map { it.users.id }.contains(userEntity.id)) {
            throw ApiException(ResponseCode.FORBIDDEN)
        }

        return GroupGetDetailResDto(
            id = group.id!!,
            organizationId = group.organization.id!!,
            name = group.name,
            creatorId = group.creator.id!!,
            creatorName = group.creator.name,
            currentUserId = userEntity.id!!,
            currentUserName = userEntity.name,
            currentUserProfileImageUrl = userEntity.profileImageUrl,
            currentUserIsCreator = group.creator.id == userEntity.id,
            currentMemberCount = group.groupUserList.size,
            maxMemberCount = group.maxMemberCount,
            hasPassword = group.hasPassword()
        )
    }

    @Transactional(readOnly = true)
    fun getGroupEntity(userDetail: UserDetailsImpl, organizationId: Long, groupId: Long): Group {
        organizationAccessService.getAccessibleOrganization(userDetail, organizationId)
        val group = groupRepository.findById(groupId)
            .orElseThrow { ApiException(ResponseCode.RESOURCE_NOT_FOUND) }

        if (group.organization.id != organizationId) {
            throw ApiException(ResponseCode.FORBIDDEN, "선택된 조직의 방이 아닙니다.")
        }

        return group
    }

    private fun transferGroupOwnerInternal(group: Group, currentOwnerId: Long, delegateUserId: Long) {
        if (group.creator.id != currentOwnerId) {
            throw ApiException(ResponseCode.FORBIDDEN, "ROOM_OWNER_ONLY")
        }

        if (delegateUserId == currentOwnerId) {
            throw ApiException(ResponseCode.BAD_REQUEST, "ROOM_OWNER_DELEGATE_SELF")
        }

        val delegateMember = groupUserRepository.findByGroup_IdAndUsers_Id(group.id!!, delegateUserId)
            .firstOrNull()
            ?: throw ApiException(ResponseCode.RESOURCE_NOT_FOUND, "ROOM_OWNER_DELEGATE_NOT_FOUND")

        group.updateCreator(delegateMember.users)
    }

    private fun deleteGroupFully(group: Group) {
        socreImageRepository.deleteByScore_Group(group)
        scoreRepository.deleteByGroup(group)
        groupUserRepository.deleteAll(groupUserRepository.findByGroup_Id(group.id!!))
        groupRepository.delete(group)
    }
}
