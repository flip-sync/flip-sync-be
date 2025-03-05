package com.zeki.flipsyncserver.domain.service.join

import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import com.zeki.flipsyncdb.dto.QScoreGetPageResDto
import com.zeki.flipsyncdb.dto.ScoreGetPageResDto
import com.zeki.flipsyncdb.entity.QScore.score
import com.zeki.flipsyncdb.entity.QScoreImage.scoreImage
import com.zeki.flipsyncdb.entity.QUser.user
import com.zeki.flipsyncserver.domain.dto.request.ScoreGetPageReqDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class ScoreJoinRepository(
    private val jpaQueryFactory: JPAQueryFactory
) {
    fun getPageScore(
        groupId: Long,
        reqDto: ScoreGetPageReqDto,
        pageable: Pageable
    ): Page<ScoreGetPageResDto> {
        val builder = BooleanBuilder()

        if (reqDto.title != null) {
            builder.and(score.title.contains(reqDto.title))
        }

        if (reqDto.singer != null) {
            builder.and(score.singer.contains(reqDto.singer))
        }

        if (reqDto.code != null) {
            builder.and(score.code.contains(reqDto.code))
        }

        if (reqDto.uploadedUserName != null) {
            builder.and(user.name.contains(reqDto.uploadedUserName))
        }

        builder.and(score.group.id.eq(groupId))

        val mainQuery = jpaQueryFactory.select(
            QScoreGetPageResDto(
                score.id,
                scoreImage.url,
                score.title,
                score.singer,
                score.code,
                user.name,
                score.createdAt,
                score.modifiedAt
            )
        )
            .from(score)
            .leftJoin(score.scoreImageList, scoreImage)
            .leftJoin(user).on(score.uploadedUserId.eq(user.id))
            .where(builder)
            .groupBy(score.id)

        // Pageable의 sort 정보를 이용한 정렬 로직
        pageable.sort.forEach { order ->
            val path =
                when (order.property) {
                    "id" -> score.id
                    "title" -> score.title
                    "singer" -> score.singer
                    "code" -> score.code
                    "uploadedUserName" -> user.name
                    "createdAt" -> score.createdAt
                    "modifiedAt" -> score.modifiedAt
                    else -> null
                }

            path?.let {
                if (order.isAscending) {
                    mainQuery.orderBy(it.asc())
                } else {
                    mainQuery.orderBy(it.desc())
                }
            }
        }

        val contents = mainQuery
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val totalCount = jpaQueryFactory
            .select(score.count())
            .from(score)
            .where(builder)
            .fetchOne() ?: 0

        return PageImpl(contents, pageable, totalCount)
    }

}