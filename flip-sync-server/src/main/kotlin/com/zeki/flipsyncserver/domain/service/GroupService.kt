package com.zeki.flipsyncserver.domain.service

import com.zeki.flipsyncdb.entity.Group
import com.zeki.flipsyncdb.repository.GroupRepository
import com.zeki.flipsyncserver.config.security.UserDetailsImpl
import com.zeki.flipsyncserver.domain.dto.request.GroupCreateReqDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroupService(
    private val groupRepository: GroupRepository,
    private val getUserEntityService: GetUserEntityService
) {
    @Transactional
    fun createGroup(userDetail: UserDetailsImpl, reqDto: GroupCreateReqDto): Long {
        val userEntity = getUserEntityService.getUserByUsername(userDetail.username)
        val group = Group.create(reqDto.name, userEntity)

        groupRepository.save(group)
        return group.id!!
    }

}
