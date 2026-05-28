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
import com.zeki.flipsyncdb.repository.UserRepository
import com.zeki.flipsyncserver.config.security.UserDetailsImpl
import com.zeki.flipsyncserver.domain.dto.request.GroupCreateReqDto
import com.zeki.flipsyncserver.domain.dto.request.GroupJoinReqDto
import com.zeki.flipsyncserver.support.IntegrationTest
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder

class GroupCreateJoinTest : IntegrationTest() {

    @Autowired
    private lateinit var groupService: GroupService

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

    @Autowired
    private lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        groupUserRepository.deleteAll()
        groupRepository.deleteAll()
        organizationMemberRepository.deleteAll()
        organizationRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `group create request accepts four digit password and rejects other lengths`() {
        val validViolations = validator.validate(
            GroupCreateReqDto(
                name = "비밀방",
                maxMemberCount = 4,
                password = "1234"
            )
        )
        val invalidViolations = validator.validate(
            GroupCreateReqDto(
                name = "비밀방",
                maxMemberCount = 4,
                password = "12345678"
            )
        )

        assertThat(validViolations).isEmpty()
        assertThat(invalidViolations).anyMatch { it.propertyPath.toString() == "password" }
    }

    @Test
    fun `createGroup stores four digit password and joins creator`() {
        val context = createOrganizationWithLeader("create")

        val groupId = groupService.createGroup(
            userDetail = context.leader.details,
            organizationId = context.organization.id!!,
            reqDto = GroupCreateReqDto(
                name = "합주방",
                maxMemberCount = 4,
                password = "1234"
            )
        )

        val group = groupRepository.findById(groupId).orElseThrow()
        assertThat(group.hasPassword()).isTrue()
        assertThat(passwordEncoder.matches("1234", group.roomPassword)).isTrue()
        assertThat(groupUserRepository.existsByGroup_IdAndUsers_Id(groupId, context.leader.user.id!!)).isTrue()
    }

    @Test
    fun `joinGroup accepts correct room password`() {
        val context = createOrganizationWithLeader("join")
        val member = createOrganizationMember(context.organization, "join-member")
        val groupId = createPrivateGroup(context)

        groupService.joinGroup(
            userDetail = member.details,
            organizationId = context.organization.id!!,
            reqDto = GroupJoinReqDto(groupId = groupId, password = "1234")
        )

        assertThat(groupUserRepository.existsByGroup_IdAndUsers_Id(groupId, member.user.id!!)).isTrue()
    }

    @Test
    fun `joinGroup rejects wrong room password`() {
        val context = createOrganizationWithLeader("wrong-password")
        val member = createOrganizationMember(context.organization, "wrong-password-member")
        val groupId = createPrivateGroup(context)

        val exception = assertThrows<ApiException> {
            groupService.joinGroup(
                userDetail = member.details,
                organizationId = context.organization.id!!,
                reqDto = GroupJoinReqDto(groupId = groupId, password = "9999")
            )
        }

        assertThat(exception.responseCode).isEqualTo(ResponseCode.FORBIDDEN)
        assertThat(exception.messages).isEqualTo("INVALID_ROOM_PASSWORD")
    }

    @Test
    fun `joinGroup rejects room member limit`() {
        val context = createOrganizationWithLeader("limit")
        val member = createOrganizationMember(context.organization, "limit-member")
        val groupId = groupService.createGroup(
            userDetail = context.leader.details,
            organizationId = context.organization.id!!,
            reqDto = GroupCreateReqDto(
                name = "정원 제한 방",
                maxMemberCount = 1,
                password = null
            )
        )

        val exception = assertThrows<ApiException> {
            groupService.joinGroup(
                userDetail = member.details,
                organizationId = context.organization.id!!,
                reqDto = GroupJoinReqDto(groupId = groupId)
            )
        }

        assertThat(exception.responseCode).isEqualTo(ResponseCode.CONFLICT_DATA)
        assertThat(exception.messages).isEqualTo("ROOM_MEMBER_LIMIT_REACHED")
    }

    private fun createPrivateGroup(context: GroupTestContext): Long {
        return groupService.createGroup(
            userDetail = context.leader.details,
            organizationId = context.organization.id!!,
            reqDto = GroupCreateReqDto(
                name = "비밀 합주방",
                maxMemberCount = 4,
                password = "1234"
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
                name = "$prefix 사용자"
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
