package com.zeki.flipsyncserver.domain.service

import com.zeki.common.exception.ApiException
import com.zeki.common.exception.ResponseCode
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
import com.zeki.flipsyncdb.repository.UserRepository
import com.zeki.flipsyncserver.config.security.UserDetailsImpl
import com.zeki.flipsyncserver.domain.dto.request.GroupCreateReqDto
import com.zeki.flipsyncserver.domain.dto.request.SocreCreateReqDto
import com.zeki.flipsyncserver.support.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Base64

class ScoreUploadContractTest : IntegrationTest() {

    @Autowired
    private lateinit var scoreService: ScoreService

    @Autowired
    private lateinit var organizationScoreService: OrganizationScoreService

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var scoreRepository: ScoreRepository

    @Autowired
    private lateinit var scoreImageRepository: SocreImageRepository

    @Autowired
    private lateinit var organizationScoreRepository: OrganizationScoreRepository

    @Autowired
    private lateinit var organizationScoreImageRepository: OrganizationScoreImageRepository

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var groupUserRepository: GroupUserRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    @Autowired
    private lateinit var organizationMemberRepository: OrganizationMemberRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @BeforeEach
    fun setUp() {
        organizationScoreImageRepository.deleteAll()
        organizationScoreRepository.deleteAll()
        scoreImageRepository.deleteAll()
        scoreRepository.deleteAll()
        groupUserRepository.deleteAll()
        groupRepository.deleteAll()
        organizationMemberRepository.deleteAll()
        organizationRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `createScore stores images and returns detail images by order`() {
        val context = createOrganizationWithLeader("score-create")
        val groupId = createRoom(context)

        val scoreId = scoreService.createScore(
            userDetail = context.leader.details,
            organizationId = context.organization.id!!,
            groupId = groupId,
            reqDto = createScoreRequest(
                image(2, "second.png"),
                image(1, "first.png")
            )
        )

        val detail = scoreService.getDetailScore(
            userDetail = context.leader.details,
            organizationId = context.organization.id!!,
            groupId = groupId,
            scoreId = scoreId
        )

        assertThat(scoreRepository.count()).isEqualTo(1)
        assertThat(scoreImageRepository.count()).isEqualTo(2)
        assertThat(detail.scoreImageList.map { it.order }).containsExactly(1, 2)
    }

    @Test
    fun `createScore rejects non image file and does not persist score`() {
        val context = createOrganizationWithLeader("score-invalid")
        val groupId = createRoom(context)

        val exception = assertThrows<ApiException> {
            scoreService.createScore(
                userDetail = context.leader.details,
                organizationId = context.organization.id!!,
                groupId = groupId,
                reqDto = createScoreRequest(textFile(1, "score.txt"))
            )
        }

        assertThat(exception.responseCode).isEqualTo(ResponseCode.BAD_REQUEST)
        assertThat(scoreRepository.count()).isZero()
        assertThat(scoreImageRepository.count()).isZero()
    }

    @Test
    fun `createScore rejects users who have not joined the room before uploading`() {
        val context = createOrganizationWithLeader("score-not-joined")
        val member = createOrganizationMember(context.organization, "score-not-joined-member")
        val groupId = createRoom(context)

        val exception = assertThrows<ApiException> {
            scoreService.createScore(
                userDetail = member.details,
                organizationId = context.organization.id!!,
                groupId = groupId,
                reqDto = createScoreRequest(image(1, "first.png"))
            )
        }

        assertThat(exception.responseCode).isEqualTo(ResponseCode.FORBIDDEN)
        assertThat(scoreRepository.count()).isZero()
        assertThat(scoreImageRepository.count()).isZero()
    }

    @Test
    fun `createOrganizationScore stores score library images`() {
        val context = createOrganizationWithLeader("organization-score")

        val scoreId = organizationScoreService.createOrganizationScore(
            userDetail = context.leader.details,
            organizationId = context.organization.id!!,
            reqDto = createScoreRequest(
                image(2, "second.png"),
                image(1, "first.png")
            )
        )

        val detail = organizationScoreService.getDetailOrganizationScore(
            userDetail = context.leader.details,
            organizationId = context.organization.id!!,
            scoreId = scoreId
        )

        assertThat(organizationScoreRepository.count()).isEqualTo(1)
        assertThat(organizationScoreImageRepository.count()).isEqualTo(2)
        assertThat(detail.scoreImageList.map { it.order }).containsExactly(1, 2)
    }

    @Test
    fun `sendOrganizationScoreToGroup copies score library images into room score`() {
        val context = createOrganizationWithLeader("organization-score-send")
        val groupId = createRoom(context)
        val organizationScoreId = organizationScoreService.createOrganizationScore(
            userDetail = context.leader.details,
            organizationId = context.organization.id!!,
            reqDto = createScoreRequest(
                image(2, "second.png"),
                image(1, "first.png")
            )
        )

        val roomScoreId = organizationScoreService.sendOrganizationScoreToGroup(
            userDetail = context.leader.details,
            organizationId = context.organization.id!!,
            scoreId = organizationScoreId,
            groupId = groupId
        )

        val detail = scoreService.getDetailScore(
            userDetail = context.leader.details,
            organizationId = context.organization.id!!,
            groupId = groupId,
            scoreId = roomScoreId
        )

        assertThat(scoreRepository.count()).isEqualTo(1)
        assertThat(scoreImageRepository.count()).isEqualTo(2)
        assertThat(detail.scoreImageList.map { it.order }).containsExactly(1, 2)
    }

    private fun createScoreRequest(vararg images: SocreCreateReqDto.ScoreImageReqDto): SocreCreateReqDto {
        return SocreCreateReqDto(
            title = "테스트 악보",
            singer = "테스트 가수",
            code = "C",
            imageList = images.toList()
        )
    }

    private fun image(order: Int, fileName: String): SocreCreateReqDto.ScoreImageReqDto {
        return SocreCreateReqDto.ScoreImageReqDto(
            file = MockMultipartFile("file", fileName, "image/png", pngBytes()),
            order = order
        )
    }

    private fun textFile(order: Int, fileName: String): SocreCreateReqDto.ScoreImageReqDto {
        return SocreCreateReqDto.ScoreImageReqDto(
            file = MockMultipartFile("file", fileName, "text/plain", "not an image".toByteArray()),
            order = order
        )
    }

    private fun pngBytes(): ByteArray {
        return Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII="
        )
    }

    private fun createRoom(context: GroupTestContext): Long {
        return groupService.createGroup(
            userDetail = context.leader.details,
            organizationId = context.organization.id!!,
            reqDto = GroupCreateReqDto(
                name = "악보 테스트 방",
                maxMemberCount = 4,
                password = null
            )
        )
    }

    private fun createOrganizationWithLeader(prefix: String): GroupTestContext {
        val leader = createUser("$prefix-leader")
        val organization = organizationRepository.save(
            Organization.create(
                name = "$prefix 조직",
                inviteCode = "INV-${System.nanoTime()}",
                creator = leader.user
            )
        )
        organizationMemberRepository.save(
            OrganizationMember.create(
                organization = organization,
                user = leader.user,
                role = OrganizationMember.OrganizationRole.LEADER
            )
        )

        return GroupTestContext(organization = organization, leader = leader)
    }

    private fun createOrganizationMember(organization: Organization, prefix: String): TestUser {
        val member = createUser(prefix)
        organizationMemberRepository.save(
            OrganizationMember.create(
                organization = organization,
                user = member.user,
                role = OrganizationMember.OrganizationRole.MEMBER
            )
        )
        return member
    }

    private fun createUser(prefix: String): TestUser {
        val user = userRepository.save(
            User.create(
                username = "$prefix-${System.nanoTime()}@flipsync.test",
                password = passwordEncoder.encode("Password1234!"),
                name = "${prefix.take(16)} 사용자"
            )
        )
        return TestUser(user = user, details = UserDetailsImpl.create(user))
    }

    private data class GroupTestContext(
        val organization: Organization,
        val leader: TestUser
    )

    private data class TestUser(
        val user: User,
        val details: UserDetailsImpl
    )
}
