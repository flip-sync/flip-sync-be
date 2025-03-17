package com.zeki.flipsyncserver.domain.service

import com.zeki.common.exception.ApiException
import com.zeki.common.exception.ResponseCode
import com.zeki.flipsyncdb.dto.ScoreGetPageResDto
import com.zeki.flipsyncdb.entity.Score
import com.zeki.flipsyncdb.entity.ScoreImage
import com.zeki.flipsyncdb.repository.ScoreRepository
import com.zeki.flipsyncserver.config.security.UserDetailsImpl
import com.zeki.flipsyncserver.domain.dto.request.ScoreGetPageReqDto
import com.zeki.flipsyncserver.domain.dto.request.SocreCreateReqDto
import com.zeki.flipsyncserver.domain.dto.response.ScoreGetDetailResDto
import com.zeki.flipsyncserver.domain.service.join.ScoreJoinRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ScoreService(
    private val scoreRepository: ScoreRepository,
    private val scoreJoinRepository: ScoreJoinRepository,

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

    @Transactional(readOnly = true)
    fun getPageScore(
        userDetail: UserDetailsImpl,
        groupId: Long,
        reqDto: ScoreGetPageReqDto,
        pageable: Pageable
    ): Page<ScoreGetPageResDto> {
        val userEntity = getUserEntityService.getUserByUsername(userDetail.username)
        val groupEntity = groupService.getGroupEntity(groupId)

        if (!groupEntity.groupUserList.map { it.users.id }.contains(userEntity.id)) {
            throw ApiException(ResponseCode.FORBIDDEN, "그룹에 속하지 않은 사용자입니다.")
        }

        return scoreJoinRepository.getPageScore(groupId, reqDto, pageable)

    }

    @Transactional(readOnly = true)
    fun getDetailScore(
        userDetail: UserDetailsImpl,
        groupId: Long,
        scoreId: Long
    ): ScoreGetDetailResDto {
        val userEntity = getUserEntityService.getUserByUsername(userDetail.username)
        val groupEntity = groupService.getGroupEntity(groupId)

        if (!groupEntity.groupUserList.map { it.users.id }.contains(userEntity.id)) {
            throw ApiException(ResponseCode.FORBIDDEN, "그룹에 속하지 않은 사용자입니다.")
        }

        val scoreEntity = scoreRepository.findById(scoreId)
            .orElseThrow { ApiException(ResponseCode.RESOURCE_NOT_FOUND) }

        val uploadedUser = getUserEntityService.getUserById(scoreEntity.uploadedUserId)
        return ScoreGetDetailResDto(
            id = scoreEntity.id!!,
            title = scoreEntity.title,
            singer = scoreEntity.singer,
            code = scoreEntity.code,
            uploadedUserId = scoreEntity.uploadedUserId,
            uploadedUserName = uploadedUser.name,
            scoreImageList = scoreEntity.scoreImageList.map {
                ScoreGetDetailResDto.ScoreImageResDto(
                    id = it.id!!,
                    url = it.url,
                    order = it.order
                )
            }
        )

    }

}
