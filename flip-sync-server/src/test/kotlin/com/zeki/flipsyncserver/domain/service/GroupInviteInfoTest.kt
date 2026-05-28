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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder

class GroupInviteInfoTest : IntegrationTest() {

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

    @BeforeEach
    fun setUp() {
        groupUserRepository.deleteAll()
        groupRepository.deleteAll()
        organizationMemberRepository.deleteAll()
        organizationRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `invite info rejects users outside the organization`() {
        val context = createOrganizationWithLeader("invite-forbidden")
        val outsider = createUser("invite-forbidden-outsider")
        val groupId = createRoom(context)

        val exception = assertThrows<ApiException> {
            groupService.getGroupInviteInfo(
                userDetail = outsider.details,
                groupId = groupId
            )
        }

        assertThat(exception.responseCode).isEqualTo(ResponseCode.FORBIDDEN)
    }

    @Test
    fun `invite info marks organization member as not joined`() {
        val context = createOrganizationWithLeader("invite-not-joined")
        val member = createOrganizationMember(context.organization, "invite-not-joined-member")
        val groupId = createRoom(context, password = "1234")

        val inviteInfo = groupService.getGroupInviteInfo(
            userDetail = member.details,
            groupId = groupId
        )

        assertThat(inviteInfo.groupId).isEqualTo(groupId)
        assertThat(inviteInfo.groupName).isEqualTo("초대 테스트 방")
        assertThat(inviteInfo.organizationId).isEqualTo(context.organization.id)
        assertThat(inviteInfo.organizationInviteCode).isEqualTo(context.organization.inviteCode)
        assertThat(inviteInfo.currentMemberCount).isEqualTo(1)
        assertThat(inviteInfo.maxMemberCount).isEqualTo(4)
        assertThat(inviteInfo.hasPassword).isTrue()
        assertThat(inviteInfo.joined).isFalse()
    }

    @Test
    fun `invite info marks already joined member as joined`() {
        val context = createOrganizationWithLeader("invite-joined")
        val member = createOrganizationMember(context.organization, "invite-joined-member")
        val groupId = createRoom(context)

        groupService.joinGroup(
            userDetail = member.details,
            organizationId = context.organization.id!!,
            reqDto = GroupJoinReqDto(groupId = groupId)
        )

        val inviteInfo = groupService.getGroupInviteInfo(
            userDetail = member.details,
            groupId = groupId
        )

        assertThat(inviteInfo.joined).isTrue()
        assertThat(inviteInfo.currentMemberCount).isEqualTo(2)
    }

    @Test
    fun `invite info rejects missing room`() {
        val context = createOrganizationWithLeader("invite-missing")

        val exception = assertThrows<ApiException> {
            groupService.getGroupInviteInfo(
                userDetail = context.leader.details,
                groupId = Long.MAX_VALUE
            )
        }

        assertThat(exception.responseCode).isEqualTo(ResponseCode.RESOURCE_NOT_FOUND)
    }

    private fun createRoom(context: GroupTestContext, password: String? = null): Long {
        return groupService.createGroup(
            userDetail = context.leader.details,
            organizationId = context.organization.id!!,
            reqDto = GroupCreateReqDto(
                name = "초대 테스트 방",
                maxMemberCount = 4,
                password = password
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
