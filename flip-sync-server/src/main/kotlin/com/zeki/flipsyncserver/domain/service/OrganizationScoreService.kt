package com.zeki.flipsyncserver.domain.service

import com.zeki.common.exception.ApiException
import com.zeki.common.exception.ResponseCode
import com.zeki.common.util.CustomUtils.toStringDateTime
import com.zeki.flipsyncdb.dto.ScoreGetPageResDto
import com.zeki.flipsyncdb.entity.OrganizationScore
import com.zeki.flipsyncdb.entity.OrganizationScoreImage
import com.zeki.flipsyncdb.entity.Score
import com.zeki.flipsyncdb.entity.ScoreImage
import com.zeki.flipsyncdb.repository.OrganizationScoreRepository
import com.zeki.flipsyncdb.repository.ScoreRepository
import com.zeki.flipsyncserver.config.security.UserDetailsImpl
import com.zeki.flipsyncserver.config.websocket.GroupScoreSummaryPayload
import com.zeki.flipsyncserver.config.websocket.GroupScoreSyncMessage
import com.zeki.flipsyncserver.config.websocket.GroupScoreSyncWebSocketHandler
import com.zeki.flipsyncserver.domain.dto.request.ScoreGetPageReqDto
import com.zeki.flipsyncserver.domain.dto.request.SocreCreateReqDto
import com.zeki.flipsyncserver.domain.dto.response.ScoreGetDetailResDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Service
class OrganizationScoreService(
    private val organizationScoreRepository: OrganizationScoreRepository,
    private val scoreRepository: ScoreRepository,
    private val organizationAccessService: OrganizationAccessService,
    private val getUserEntityService: GetUserEntityService,
    private val groupService: GroupService,
    private val s3Service: S3Service,
    private val groupScoreSyncWebSocketHandler: GroupScoreSyncWebSocketHandler
) {
    @Transactional
    fun createOrganizationScore(
        userDetail: UserDetailsImpl,
        organizationId: Long,
        reqDto: SocreCreateReqDto
    ): Long {
        val userEntity = organizationAccessService.getUserEntity(userDetail)
        val organization = organizationAccessService.getAccessibleOrganization(userDetail, organizationId)

        val organizationScore = OrganizationScore.create(
            organization = organization,
            title = reqDto.title,
            code = reqDto.code,
            singer = reqDto.singer,
            uploadedUserId = userEntity.id!!
        )

        reqDto.imageList.map {
            OrganizationScoreImage.create(
                organizationScore = organizationScore,
                order = it.order,
                url = s3Service.createUrl(it.file, "score")
            )
        }.toList()

        organizationScoreRepository.save(organizationScore)
        return organizationScore.id!!
    }

    @Transactional(readOnly = true)
    fun getPageOrganizationScore(
        userDetail: UserDetailsImpl,
        organizationId: Long,
        reqDto: ScoreGetPageReqDto,
        pageable: Pageable
    ): Page<ScoreGetPageResDto> {
        organizationAccessService.getAccessibleOrganization(userDetail, organizationId)

        return organizationScoreRepository.searchByOrganization(
            organizationId = organizationId,
            title = reqDto.title?.trim()?.takeIf { it.isNotBlank() },
            singer = reqDto.singer?.trim()?.takeIf { it.isNotBlank() },
            code = reqDto.code?.trim()?.takeIf { it.isNotBlank() },
            uploadedUserName = reqDto.uploadedUserName?.trim()?.takeIf { it.isNotBlank() },
            pageable = pageable
        ).map { it.toPageResponse() }
    }

    @Transactional(readOnly = true)
    fun getDetailOrganizationScore(
        userDetail: UserDetailsImpl,
        organizationId: Long,
        scoreId: Long
    ): ScoreGetDetailResDto {
        organizationAccessService.getAccessibleOrganization(userDetail, organizationId)
        val organizationScore = organizationScoreRepository.findByIdAndOrganization_Id(scoreId, organizationId)
            ?: throw ApiException(ResponseCode.RESOURCE_NOT_FOUND)

        val uploadedUser = getUserEntityService.getUserById(organizationScore.uploadedUserId)
        return ScoreGetDetailResDto(
            id = organizationScore.id!!,
            title = organizationScore.title,
            singer = organizationScore.singer,
            code = organizationScore.code,
            uploadedUserId = organizationScore.uploadedUserId,
            uploadedUserName = uploadedUser.name,
            scoreImageList = organizationScore.scoreImageList
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

    @Transactional
    fun deleteOrganizationScore(
        userDetail: UserDetailsImpl,
        organizationId: Long,
        scoreId: Long
    ) {
        val userEntity = organizationAccessService.getUserEntity(userDetail)
        organizationAccessService.getAccessibleOrganization(userDetail, organizationId)

        val organizationScore = organizationScoreRepository.findByIdAndOrganization_Id(scoreId, organizationId)
            ?: throw ApiException(ResponseCode.RESOURCE_NOT_FOUND)

        if (organizationScore.uploadedUserId != userEntity.id) {
            throw ApiException(ResponseCode.FORBIDDEN, "본인이 등록한 악보만 삭제할 수 있습니다.")
        }

        organizationScoreRepository.delete(organizationScore)
    }

    @Transactional
    fun sendOrganizationScoreToGroup(
        userDetail: UserDetailsImpl,
        organizationId: Long,
        scoreId: Long,
        groupId: Long
    ): Long {
        val userEntity = organizationAccessService.getUserEntity(userDetail)
        val groupEntity = groupService.getGroupEntity(userDetail, organizationId, groupId)

        if (!groupEntity.groupUserList.map { it.users.id }.contains(userEntity.id)) {
            throw ApiException(ResponseCode.FORBIDDEN, "해당 방에 참여하지 않은 사용자입니다.")
        }

        val organizationScore = organizationScoreRepository.findByIdAndOrganization_Id(scoreId, organizationId)
            ?: throw ApiException(ResponseCode.RESOURCE_NOT_FOUND)

        val roomScore = Score.create(
            group = groupEntity,
            title = organizationScore.title,
            code = organizationScore.code,
            singer = organizationScore.singer,
            uploadedUserId = userEntity.id!!
        )

        organizationScore.scoreImageList
            .sortedBy { it.order }
            .forEach {
                ScoreImage.create(
                    score = roomScore,
                    order = it.order,
                    url = it.url
                )
            }

        scoreRepository.save(roomScore)
        val firstScoreImageUrl = roomScore.scoreImageList
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
                            scoreId = roomScore.id!!,
                            pageIndex = 0,
                            active = true,
                            triggeredByUserId = userEntity.id,
                            triggeredByUserName = userEntity.name,
                            scoreSummary = GroupScoreSummaryPayload(
                                id = roomScore.id!!,
                                uploadedUserId = roomScore.uploadedUserId,
                                thumbnail = firstScoreImageUrl,
                                title = roomScore.title,
                                singer = roomScore.singer,
                                code = roomScore.code,
                                uploadedUserName = userEntity.name,
                                uploadedUserProfileImageUrl = userEntity.profileImageUrl,
                                createdAt = roomScore.createdAt.toStringDateTime(),
                                modifiedAt = roomScore.modifiedAt.toStringDateTime()
                            )
                        )
                    )
                }
            })
        }

        return roomScore.id!!
    }

    private fun OrganizationScore.toPageResponse(): ScoreGetPageResDto {
        val uploadedUser = getUserEntityService.getUserById(uploadedUserId)
        val thumbnailUrl = scoreImageList.minByOrNull { it.order }?.url.orEmpty()

        return ScoreGetPageResDto(
            id = id!!,
            uploadedUserId = uploadedUserId,
            thumbnail = thumbnailUrl,
            title = title,
            singer = singer,
            code = code,
            uploadedUserName = uploadedUser.name,
            uploadedUserProfileImageUrl = uploadedUser.profileImageUrl,
            createdAt = createdAt,
            modifiedAt = modifiedAt
        )
    }
}
