package com.zeki.flipsyncserver.domain.service

import com.zeki.common.exception.ApiException
import com.zeki.common.exception.ResponseCode
import com.zeki.common.util.CustomUtils.toStringDateTime
import com.zeki.flipsyncdb.entity.Group
import com.zeki.flipsyncdb.entity.GroupUser
import com.zeki.flipsyncdb.repository.GroupRepository
import com.zeki.flipsyncdb.repository.GroupUserRepository
import com.zeki.flipsyncserver.config.security.UserDetailsImpl
import com.zeki.flipsyncserver.domain.dto.request.GroupCreateReqDto
import com.zeki.flipsyncserver.domain.dto.response.GroupGetListResDto
import com.zeki.flipsyncserver.domain.dto.response.GroupUsersGetListResDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroupService(
    private val groupRepository: GroupRepository,
    private val groupUserRepository: GroupUserRepository,
    private val getUserEntityService: GetUserEntityService
) {
    @Transactional
    fun createGroup(userDetail: UserDetailsImpl, reqDto: GroupCreateReqDto): Long {
        val userEntity = getUserEntityService.getUserByUsername(userDetail.username)
        val group = Group.create(reqDto.name, userEntity)

        groupRepository.save(group)
        groupUserRepository.save(GroupUser.create(group, userEntity))
        return group.id!!
    }

    @Transactional(readOnly = true)
    fun getMyGroupList(userDetail: UserDetailsImpl): List<GroupGetListResDto> {
        val userEntity = getUserEntityService.getUserByUsername(userDetail.username)
        val groupUsersList = groupUserRepository.findByUsers(userEntity)

        return groupUsersList.stream().map {
            GroupGetListResDto(
                it.group.id!!,
                it.group.name,
                it.group.creator.id!!,
                it.group.creator.name
            )
        }.toList()
    }

    @Transactional(readOnly = true)
    fun getGroupList(userDetail: UserDetailsImpl): List<GroupGetListResDto> {
        getUserEntityService.getUserByUsername(userDetail.username)
        val allGroupList = groupRepository.findAll()

        return allGroupList.stream().map {
            GroupGetListResDto(
                it.id!!,
                it.name,
                it.creator.id!!,
                it.creator.name
            )
        }.toList()
    }

    @Transactional
    fun joinGroup(userDetail: UserDetailsImpl, groupId: Long) {
        val userEntity = getUserEntityService.getUserByUsername(userDetail.username)
        val group = groupRepository.findById(groupId)
            .orElseThrow { ApiException(ResponseCode.RESOURCE_NOT_FOUND) }

        val existsByUsersAndGroup = groupUserRepository.existsByUsersAndGroup(userEntity, group)
        if (existsByUsersAndGroup) throw ApiException(ResponseCode.CONFLICT_DATA)

        GroupUser.create(group = group, user = userEntity).let { groupUserRepository.save(it) }
    }

    @Transactional
    fun leaveGroup(userDetail: UserDetailsImpl, groupId: Long) {
        val userEntity = getUserEntityService.getUserByUsername(userDetail.username)
        val group = groupRepository.findById(groupId)
            .orElseThrow { ApiException(ResponseCode.RESOURCE_NOT_FOUND) }

        val groupUsersList = groupUserRepository.findByGroup_IdAndUsers_Id(groupId, userEntity.id!!)
        if (groupUsersList.isEmpty()) throw ApiException(ResponseCode.RESOURCE_NOT_FOUND)

        groupUsersList.get(0).let {
            val gUsersList = groupUserRepository.findByGroup_Id(groupId)
            // 생성자 이면서 남은 인원이 1명이 아니라면 그룹을 떠날 수 없음.
            if (group.creator.id == userEntity.id && gUsersList.size > 1) throw ApiException(
                ResponseCode.UNMODIFIABLE_INFORMATION
            )
            // 참여자 삭제
            groupUserRepository.delete(it)
            // 생성자라면 그룹 삭제
            if (gUsersList.size == 1) groupRepository.delete(gUsersList.get(0).group)
        }
    }

    @Transactional(readOnly = true)
    fun getGroupUsersList(
        userDetail: UserDetailsImpl,
        groupId: Long
    ): List<GroupUsersGetListResDto> {
        getUserEntityService.getUserByUsername(userDetail.username)
        val group = groupRepository.findById(groupId)
            .orElseThrow { ApiException(ResponseCode.RESOURCE_NOT_FOUND) }

        return group.groupUserList.stream().map {
            GroupUsersGetListResDto(
                id = it.users.id!!,
                name = it.users.name,
                joinedAt = it.createdAt.toStringDateTime()
            )
        }.toList()
    }

    fun getGroupEntity(groupId: Long): Group {
        return groupRepository.findById(groupId).orElseThrow {
            ApiException(ResponseCode.RESOURCE_NOT_FOUND)
        }
    }

}
