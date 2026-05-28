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

class GroupLifecyclePermissionTest : IntegrationTest() {

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
    fun `owner leaving alone deletes room`() {
        val context = createOrganizationWithLeader("leave-alone")
        val groupId = createRoom(context)

        groupService.leaveGroup(
            userDetail = context.leader.details,
            organizationId = context.organization.id!!,
            groupId = groupId
        )

        assertThat(groupRepository.findById(groupId)).isEmpty()
        assertThat(groupUserRepository.countByGroup_Id(groupId)).isZero()
    }

    @Test
    fun `owner leaving with members requires delegate`() {
        val context = createOrganizationWithLeader("delegate-required")
        val member = createJoinedMember(context, "delegate-required-member")

        val exception = assertThrows<ApiException> {
            groupService.leaveGroup(
                userDetail = context.leader.details,
                organizationId = context.organization.id!!,
                groupId = context.groupId!!,
                delegateUserId = null
            )
        }

        assertThat(member.user.id).isNotNull()
        assertThat(exception.responseCode).isEqualTo(ResponseCode.UNMODIFIABLE_INFORMATION)
        assertThat(exception.messages).isEqualTo("ROOM_OWNER_DELEGATE_REQUIRED")
    }

    @Test
    fun `owner leaving with delegate transfers owner and removes previous owner`() {
        val context = createOrganizationWithLeader("delegate")
        val member = createJoinedMember(context, "delegate-member")
        val groupId = context.groupId!!

        groupService.leaveGroup(
            userDetail = context.leader.details,
            organizationId = context.organization.id!!,
            groupId = groupId,
            delegateUserId = member.user.id
        )

        val group = groupRepository.findById(groupId).orElseThrow()
        assertThat(group.creator.id).isEqualTo(member.user.id)
        assertThat(groupUserRepository.existsByGroup_IdAndUsers_Id(groupId, context.leader.user.id!!)).isFalse()
        assertThat(groupUserRepository.existsByGroup_IdAndUsers_Id(groupId, member.user.id!!)).isTrue()
    }

    @Test
    fun `only owner can delete room`() {
        val context = createOrganizationWithLeader("delete-owner-only")
        val member = createJoinedMember(context, "delete-owner-only-member")

        val exception = assertThrows<ApiException> {
            groupService.deleteGroup(
                userDetail = member.details,
                organizationId = context.organization.id!!,
                groupId = context.groupId!!
            )
        }

        assertThat(exception.responseCode).isEqualTo(ResponseCode.FORBIDDEN)
        assertThat(exception.messages).isEqualTo("ROOM_OWNER_ONLY")
        assertThat(groupRepository.findById(context.groupId!!)).isPresent()
    }

    @Test
    fun `owner can delete room`() {
        val context = createOrganizationWithLeader("delete")
        createJoinedMember(context, "delete-member")

        groupService.deleteGroup(
            userDetail = context.leader.details,
            organizationId = context.organization.id!!,
            groupId = context.groupId!!
        )

        assertThat(groupRepository.findById(context.groupId!!)).isEmpty()
        assertThat(groupUserRepository.countByGroup_Id(context.groupId!!)).isZero()
    }

    @Test
    fun `owner can kick member`() {
        val context = createOrganizationWithLeader("kick")
        val member = createJoinedMember(context, "kick-member")

        groupService.kickGroupMember(
            userDetail = context.leader.details,
            organizationId = context.organization.id!!,
            groupId = context.groupId!!,
            targetUserId = member.user.id!!
        )

        assertThat(groupUserRepository.existsByGroup_IdAndUsers_Id(context.groupId!!, member.user.id!!)).isFalse()
        assertThat(groupUserRepository.existsByGroup_IdAndUsers_Id(context.groupId!!, context.leader.user.id!!)).isTrue()
    }

    @Test
    fun `non owner cannot kick member`() {
        val context = createOrganizationWithLeader("kick-owner-only")
        val member = createJoinedMember(context, "kick-owner-only-member")
        val target = createJoinedMember(context, "kick-owner-only-target")

        val exception = assertThrows<ApiException> {
            groupService.kickGroupMember(
                userDetail = member.details,
                organizationId = context.organization.id!!,
                groupId = context.groupId!!,
                targetUserId = target.user.id!!
            )
        }

        assertThat(exception.responseCode).isEqualTo(ResponseCode.FORBIDDEN)
        assertThat(exception.messages).isEqualTo("ROOM_OWNER_ONLY")
    }

    private fun createRoom(context: GroupTestContext): Long {
        val groupId = groupService.createGroup(
            userDetail = context.leader.details,
            organizationId = context.organization.id!!,
            reqDto = GroupCreateReqDto(
                name = "권한 테스트 방",
                maxMemberCount = 4,
                password = null
            )
        )
        context.groupId = groupId
        return groupId
    }

    private fun createJoinedMember(context: GroupTestContext, prefix: String): TestUser {
        val groupId = context.groupId ?: createRoom(context)
        val member = createOrganizationMember(context.organization, prefix)
        groupService.joinGroup(
            userDetail = member.details,
            organizationId = context.organization.id!!,
            reqDto = GroupJoinReqDto(groupId = groupId)
        )
        return member
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
        val leader: TestUser,
        var groupId: Long? = null
    )

    private data class TestUser(
        val user: User,
        val details: UserDetailsImpl
    )
}
