package com.zeki.flipsyncserver.domain.service

import com.zeki.flipsyncdb.entity.Score
import com.zeki.flipsyncdb.entity.ScoreImage
import com.zeki.flipsyncdb.repository.ScoreRepository
import com.zeki.flipsyncserver.config.security.UserDetailsImpl
import com.zeki.flipsyncserver.domain.dto.request.SocreCreateReqDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ScoreService(
    private val scoreRepository: ScoreRepository,
    private val getUserEntityService: GetUserEntityService,
    private val groupService: GroupService,
    private val s3Service: S3Service
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

        // 악보 이미지 생성
        reqDto.imageList.map {
            ScoreImage.create(
                score = score,
                order = it.order,
                url = s3Service.createUrl(it.file, "score"),
            )
        }.toList()

        scoreRepository.save(score)
        return score.id!!
    }

}
