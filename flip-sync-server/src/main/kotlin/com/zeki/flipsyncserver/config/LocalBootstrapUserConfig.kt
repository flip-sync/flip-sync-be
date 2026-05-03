package com.zeki.flipsyncserver.config

import com.zeki.flipsyncdb.entity.Organization
import com.zeki.flipsyncdb.entity.OrganizationMember
import com.zeki.flipsyncdb.entity.User
import com.zeki.flipsyncdb.repository.OrganizationMemberRepository
import com.zeki.flipsyncdb.repository.OrganizationRepository
import com.zeki.flipsyncdb.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
@Profile("test", "dev")
class LocalBootstrapUserConfig(
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository,
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${flipsync.bootstrap-user.enabled:false}")
    private val enabled: Boolean,
    @Value("\${flipsync.bootstrap-user.email:local@flipsync.dev}")
    private val email: String,
    @Value("\${flipsync.bootstrap-user.password:Password123!}")
    private val password: String,
    @Value("\${flipsync.bootstrap-user.name:\uBC29\uC7A5 \uACC4\uC815}")
    private val name: String,
    @Value("\${flipsync.bootstrap-user.organization:FlipSync}")
    private val organization: String,
    @Value("\${flipsync.bootstrap-user.secondary-enabled:false}")
    private val secondaryEnabled: Boolean,
    @Value("\${flipsync.bootstrap-user.secondary-email:guest@flipsync.dev}")
    private val secondaryEmail: String,
    @Value("\${flipsync.bootstrap-user.secondary-password:Password1234}")
    private val secondaryPassword: String,
    @Value("\${flipsync.bootstrap-user.secondary-name:\uAC8C\uC2A4\uD2B8 \uACC4\uC815}")
    private val secondaryName: String,
    @Value("\${flipsync.bootstrap-user.secondary-organization:FlipSync Guest}")
    private val secondaryOrganization: String,
    @Value("\${flipsync.bootstrap-user.shared-organization:FlipSync Demo}")
    private val sharedOrganizationName: String,
    @Value("\${flipsync.bootstrap-user.shared-organization-invite-code:DEMO2026}")
    private val sharedOrganizationInviteCode: String,
    @Value("\${flipsync.bootstrap-user.extra-user-count:0}")
    private val extraUserCount: Int,
    @Value("\${flipsync.bootstrap-user.extra-user-email-prefix:member}")
    private val extraUserEmailPrefix: String,
    @Value("\${flipsync.bootstrap-user.extra-user-email-domain:flipsync.dev}")
    private val extraUserEmailDomain: String,
    @Value("\${flipsync.bootstrap-user.extra-user-password-prefix:Member}")
    private val extraUserPasswordPrefix: String,
    @Value("\${flipsync.bootstrap-user.extra-user-name-prefix:테스터}")
    private val extraUserNamePrefix: String,
    @Value("\${flipsync.bootstrap-user.extra-user-organization:FlipSync Demo Member}")
    private val extraUserOrganization: String
) {

    @Bean
    fun bootstrapUserRunner() = ApplicationRunner {
        if (!enabled) {
            return@ApplicationRunner
        }

        val primaryUser = upsertBootstrapUser(
            email = email,
            password = password,
            name = name,
            organization = organization
        )

        val memberUsers = mutableListOf<User>()

        if (secondaryEnabled) {
            val secondaryUser = upsertBootstrapUser(
                email = secondaryEmail,
                password = secondaryPassword,
                name = secondaryName,
                organization = secondaryOrganization
            )
            memberUsers += secondaryUser
        }

        memberUsers += upsertExtraUsers()
        upsertSharedOrganization(primaryUser, memberUsers)
    }

    private fun upsertBootstrapUser(email: String, password: String, name: String, organization: String?): User {
        val existingUser = userRepository.findByUsername(email)
        if (existingUser != null) {
            existingUser.updatePassword(passwordEncoder.encode(password))
            existingUser.updateProfile(name = name, bio = organization)
            return existingUser
        }

        return userRepository.save(
            User.create(
                username = email,
                password = passwordEncoder.encode(password),
                name = name
            ).apply {
                updateProfile(name = name, bio = organization)
            }
        )
    }

    private fun upsertExtraUsers(): List<User> {
        if (extraUserCount <= 0) {
            return emptyList()
        }

        return (1..extraUserCount).map { index ->
            val paddedIndex = index.toString().padStart(2, '0')
            upsertBootstrapUser(
                email = "${extraUserEmailPrefix}${paddedIndex}@${extraUserEmailDomain}",
                password = "${extraUserPasswordPrefix}${paddedIndex}1234",
                name = "${extraUserNamePrefix} ${paddedIndex}",
                organization = extraUserOrganization
            )
        }
    }

    private fun upsertSharedOrganization(primaryUser: User, memberUsers: List<User>) {
        val organization = organizationRepository.findByNameAndCreator_Id(sharedOrganizationName, primaryUser.id!!)
            ?: organizationRepository.save(
                Organization.create(
                    name = sharedOrganizationName,
                    inviteCode = sharedOrganizationInviteCode,
                    creator = primaryUser
                )
            )

        ensureOrganizationMember(organization, primaryUser, OrganizationMember.OrganizationRole.LEADER)
        memberUsers.forEach {
            ensureOrganizationMember(organization, it, OrganizationMember.OrganizationRole.MEMBER)
        }
    }

    private fun ensureOrganizationMember(
        organization: Organization,
        user: User,
        role: OrganizationMember.OrganizationRole
    ) {
        val existingMember = organizationMemberRepository.findByUsersAndOrganization(user, organization)
        if (existingMember != null) {
            existingMember.updateRole(role)
            return
        }

        organizationMemberRepository.save(
            OrganizationMember.create(
                organization = organization,
                user = user,
                role = role
            )
        )
    }
}

