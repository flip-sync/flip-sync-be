package com.zeki.flipsyncserver.domain.service

import com.zeki.common.exception.ApiException
import com.zeki.common.exception.ResponseCode
import com.zeki.common.util.CustomUtils.toStringDateTime
import com.zeki.flipsyncdb.dto.ScoreGetPageResDto
import com.zeki.flipsyncdb.entity.Score
import com.zeki.flipsyncdb.entity.ScoreImage
import com.zeki.flipsyncdb.repository.ScoreRepository
import com.zeki.flipsyncserver.config.security.UserDetailsImpl
import com.zeki.flipsyncserver.config.websocket.GroupScoreSummaryPayload
import com.zeki.flipsyncserver.config.websocket.GroupScoreSyncMessage
import com.zeki.flipsyncserver.config.websocket.GroupScoreSyncWebSocketHandler
import com.zeki.flipsyncserver.domain.dto.request.ScoreGetPageReqDto
import com.zeki.flipsyncserver.domain.dto.request.SocreCreateReqDto
import com.zeki.flipsyncserver.domain.dto.response.ScoreGetDetailResDto
import com.zeki.flipsyncserver.domain.service.join.ScoreJoinRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Service
class ScoreService(
    private val scoreRepository: ScoreRepository,
    private val scoreJoinRepository: ScoreJoinRepository,
    private val getUserEntityService: GetUserEntityService,
    private val groupService: GroupService,
    private val s3Service: S3Service,
    private val groupScoreSyncWebSocketHandler: GroupScoreSyncWebSocketHandler
) {
    @Transactional
    fun createScore(
        userDetail: UserDetailsImpl,
        organizationId: Long,
        groupId: Long,
        reqDto: SocreCreateReqDto
    ): Long {
        val userEntity = getUserEntityService.getUserByUsername(userDetail.username)
        val groupEntity = groupService.getGroupEntity(userDetail, organizationId, groupId)

        if (!groupEntity.groupUserList.map { it.users.id }.contains(userEntity.id)) {
            throw ApiException(ResponseCode.FORBIDDEN, "해당 방에 참여하지 않은 사용자입니다.")
        }

        val score = Score.create(
            group = groupEntity,
            title = reqDto.title,
            code = reqDto.code,
            singer = reqDto.singer,
            uploadedUserId = userEntity.id!!
        )

        reqDto.imageList.map {
            ScoreImage.create(
                score = score,
                order = it.order,
                url = s3Service.createUrl(it.file, "score")
            )
        }.toList()

        scoreRepository.save(score)
        val firstScoreImageUrl = score.scoreImageList
            .minByOrNull { it.order }
            ?.url
            .orEmpty()

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() {
                    groupScoreSyncWebSocketHandler.broadcastToGroup(
                        groupId = groupId,
                        message = GroupScoreSyncMessage(
                            type = "SCORE_CREATED",
                            scoreId = score.id!!,
                            pageIndex = 0,
                            active = true,
                            triggeredByUserId = userEntity.id,
                            triggeredByUserName = userEntity.name,
                            scoreSummary = GroupScoreSummaryPayload(
                                id = score.id!!,
                                uploadedUserId = score.uploadedUserId,
                                thumbnail = firstScoreImageUrl,
                                title = score.title,
                                singer = score.singer,
                                code = score.code,
                                uploadedUserName = userEntity.name,
                                uploadedUserProfileImageUrl = userEntity.profileImageUrl,
                                createdAt = score.createdAt.toStringDateTime(),
                                modifiedAt = score.modifiedAt.toStringDateTime()
                            )
                        )
                    )
                }
            })
        }
        return score.id!!
    }

    @Transactional(readOnly = true)
    fun getPageScore(
        userDetail: UserDetailsImpl,
        organizationId: Long,
        groupId: Long,
        reqDto: ScoreGetPageReqDto,
        pageable: Pageable
    ): Page<ScoreGetPageResDto> {
        val userEntity = getUserEntityService.getUserByUsername(userDetail.username)
        val groupEntity = groupService.getGroupEntity(userDetail, organizationId, groupId)

        if (!groupEntity.groupUserList.map { it.users.id }.contains(userEntity.id)) {
            throw ApiException(ResponseCode.FORBIDDEN, "해당 방에 참여하지 않은 사용자입니다.")
        }

        return scoreJoinRepository.getPageScore(groupId, reqDto, pageable)
    }

    @Transactional(readOnly = true)
    fun getDetailScore(
        userDetail: UserDetailsImpl,
        organizationId: Long,
        groupId: Long,
        scoreId: Long
    ): ScoreGetDetailResDto {
        val userEntity = getUserEntityService.getUserByUsername(userDetail.username)
        val groupEntity = groupService.getGroupEntity(userDetail, organizationId, groupId)

        if (!groupEntity.groupUserList.map { it.users.id }.contains(userEntity.id)) {
            throw ApiException(ResponseCode.FORBIDDEN, "해당 방에 참여하지 않은 사용자입니다.")
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
            scoreImageList = scoreEntity.scoreImageList
                .sortedBy { it.order }
                .map {
                    ScoreGetDetailResDto.ScoreImageResDto(
                        id = it.id!!,
                        url = it.url,
                        order = it.order
                    )
                }
        )
    }
}
