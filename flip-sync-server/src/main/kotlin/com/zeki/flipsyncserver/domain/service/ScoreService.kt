package com.zeki.flipsyncserver.domain.service

import com.zeki.flipsyncdb.entity.Score
import com.zeki.flipsyncdb.repository.ScoreRepository
import com.zeki.flipsyncserver.config.security.UserDetailsImpl
import com.zeki.flipsyncserver.domain.dto.request.SocreCreateReqDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ScoreService(
    private val scoreRepository: ScoreRepository,
    private val getUserEntityService: GetUserEntityService,
    private val groupService: GroupService
) {

    @Transactional
    fun createScore(userDetail: UserDetailsImpl, groupId: Long, reqDto: SocreCreateReqDto): Long {
        val userEntity = getUserEntityService.getUserByUsername(userDetail.username)
        val groupEntity = groupService.getGroupEntity(groupId)

        val score = Score.create(
            group = groupEntity,
            title = reqDto.title,
            code = reqDto.code,
            singer = reqDto.singer,
            uploadedUserId = userEntity.id!!
        )

        // TODO image 처리

        scoreRepository.save(score)
        return score.id!!
    }

}
