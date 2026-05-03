package com.zeki.flipsyncserver.domain.service

import com.zeki.common.em.Status
import com.zeki.common.exception.ApiException
import com.zeki.common.exception.ResponseCode
import com.zeki.flipsyncdb.entity.EmailVerify
import com.zeki.flipsyncdb.entity.Group
import com.zeki.flipsyncdb.entity.Organization
import com.zeki.flipsyncdb.entity.OrganizationMember
import com.zeki.flipsyncdb.entity.User
import com.zeki.flipsyncdb.repository.EmailVerifyRepository
import com.zeki.flipsyncdb.repository.GroupRepository
import com.zeki.flipsyncdb.repository.GroupUserRepository
import com.zeki.flipsyncdb.repository.OrganizationMemberRepository
import com.zeki.flipsyncdb.repository.OrganizationRepository
import com.zeki.flipsyncdb.repository.OrganizationScoreImageRepository
import com.zeki.flipsyncdb.repository.OrganizationScoreRepository
import com.zeki.flipsyncdb.repository.ScoreRepository
import com.zeki.flipsyncdb.repository.SocreImageRepository
import com.zeki.flipsyncdb.repository.UserRepository
import com.zeki.flipsyncserver.config.security.jwt.JwtTokenProvider
import com.zeki.flipsyncserver.domain.dto.request.UserDeleteAccountReqDto
import com.zeki.flipsyncserver.domain.dto.request.UserResetPasswordReqDto
import com.zeki.flipsyncserver.domain.dto.request.UserSignupReqDto
import com.zeki.flipsyncserver.domain.dto.request.UserUpdateEmailReqDto
import com.zeki.flipsyncserver.domain.dto.request.UserUpdateProfileReqDto
import com.zeki.flipsyncserver.domain.dto.request.UserVerifyEmailReqDto
import com.zeki.flipsyncserver.domain.dto.response.TokenResDto
import com.zeki.flipsyncserver.domain.dto.response.UserProfileResDto
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime

@Service
class UserService(
    private val userRepository: UserRepository,
    private val getUserEntityService: GetUserEntityService,
    private val emailVerifyRepository: EmailVerifyRepository,
    private val groupRepository: GroupRepository,
    private val groupUserRepository: GroupUserRepository,
    private val organizationRepository: OrganizationRepository,
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val organizationScoreRepository: OrganizationScoreRepository,
    private val organizationScoreImageRepository: OrganizationScoreImageRepository,
    private val scoreRepository: ScoreRepository,
    private val scoreImageRepository: SocreImageRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val emailService: EmailService,
    private val s3Service: S3Service
) {

    @Transactional
    fun signup(reqDto: UserSignupReqDto): Long {
        val user = getUserEntityService.getUserByUsernameNullable(reqDto.email)
        if (user != null) throw ApiException(ResponseCode.CONFLICT_DATA)
        consumeVerifiedEmail(reqDto.email)
        val userEntity = User.create(reqDto.email, passwordEncoder.encode(reqDto.password), reqDto.name)

        return userRepository.save(userEntity).id!!
    }

    @Transactional
    fun createVerifyEmail(email: String) {
        val code = createCode()
        val expiredAt = LocalDateTime.now().plusMinutes(5)
        val emailVerify = emailVerifyRepository.findByEmail(email)?.apply {
            reissue(code, expiredAt)
        } ?: EmailVerify.create(email, code, expiredAt)

        emailVerifyRepository.save(emailVerify)
        emailService.sendEmail(email, emailVerify.code)
    }

    private fun createCode(): String {
        return (100000..999999).random().toString()
    }

    @Transactional
    fun checkVerifyEmail(reqDto: UserVerifyEmailReqDto) {
        val emailVerify = emailVerifyRepository.findByEmail(reqDto.email)
        if (emailVerify == null) throw ApiException(ResponseCode.EMAIL_VERIFY_NOT_FOUND)
        if (emailVerify.expiredAt.isBefore(LocalDateTime.now())) throw ApiException(ResponseCode.EMAIL_VERIFY_EXPIRED)
        if (emailVerify.status == Status.Y) return
        if (emailVerify.tryCount >= 3) throw ApiException(ResponseCode.EMAIL_VERIFY_TRY_LIMIT)
        if (emailVerify.code != reqDto.code) {
            emailVerify.updateTryCount()
            throw ApiException(ResponseCode.EMAIL_VERIFY_UNAUTHORIZED)
        }
        emailVerify.markVerified()
    }

    @Transactional
    fun login(email: String, password: String): TokenResDto {
        val user = userRepository.findByUsername(email) ?: throw ApiException(ResponseCode.RESOURCE_NOT_FOUND)
        if (!passwordEncoder.matches(password, user.password)) throw ApiException(ResponseCode.UNAUTHORIZED)

        return jwtTokenProvider.createToken(user)
    }

    @Transactional(readOnly = true)
    fun loginRefresh(refreshToken: String): TokenResDto {
        return jwtTokenProvider.regenerateAccessToken(refreshToken) ?: throw ApiException(ResponseCode.UNAUTHORIZED)
    }

    @Transactional
    fun resetPassword(reqDto: UserResetPasswordReqDto) {
        val user = userRepository.findByUsername(reqDto.email) ?: throw ApiException(ResponseCode.RESOURCE_NOT_FOUND)
        consumeVerifiedEmail(reqDto.email)
        user.updatePassword(passwordEncoder.encode(reqDto.password))
    }

    @Transactional(readOnly = true)
    fun getProfile(email: String): UserProfileResDto {
        val user = getUserEntityService.getUserByUsername(email)
        return user.toProfileResponse()
    }

    @Transactional
    fun updateProfile(email: String, reqDto: UserUpdateProfileReqDto): UserProfileResDto {
        val user = getUserEntityService.getUserByUsername(email)
        user.updateProfile(reqDto.name.trim(), reqDto.organization)
        return user.toProfileResponse()
    }

    @Transactional
    fun updateProfileImage(email: String, file: MultipartFile): UserProfileResDto {
        val user = getUserEntityService.getUserByUsername(email)
        user.profileImageUrl?.let { existingUrl ->
            s3Service.deleteFile(existingUrl)
        }
        val profileImageUrl = s3Service.createUrl(file, "profile/")
        user.updateProfileImage(profileImageUrl)
        return user.toProfileResponse()
    }

    @Transactional
    fun updateEmail(email: String, reqDto: UserUpdateEmailReqDto): TokenResDto {
        val user = getUserEntityService.getUserByUsername(email)
        val nextEmail = reqDto.email.trim()

        if (user.username == nextEmail) {
            throw ApiException(ResponseCode.CONFLICT_DATA)
        }

        getUserEntityService.getUserByUsernameNullable(nextEmail)?.let {
            throw ApiException(ResponseCode.CONFLICT_DATA)
        }

        consumeVerifiedEmail(nextEmail)
        user.updateUsername(nextEmail)

        return jwtTokenProvider.createToken(user)
    }

    @Transactional
    fun deleteAccount(email: String, reqDto: UserDeleteAccountReqDto) {
        val user = getUserEntityService.getUserByUsername(email)
        if (!passwordEncoder.matches(reqDto.password, user.password)) {
            throw ApiException(ResponseCode.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.")
        }

        val userId = user.id ?: throw ApiException(ResponseCode.RESOURCE_NOT_FOUND)
        user.profileImageUrl?.let { existingUrl ->
            s3Service.deleteFile(existingUrl)
        }

        organizationScoreImageRepository.deleteByOrganizationScore_UploadedUserId(userId)
        organizationScoreRepository.deleteByUploadedUserId(userId)
        scoreImageRepository.deleteByScore_UploadedUserId(userId)
        scoreRepository.deleteByUploadedUserId(userId)

        transferOrDeleteLeadingOrganizations(user)
        transferOrDeleteCreatedGroups(user)

        groupUserRepository.deleteByUsers(user)
        organizationMemberRepository.deleteByUsers(user)
        emailVerifyRepository.findByEmail(user.username)?.let { emailVerifyRepository.delete(it) }
        userRepository.delete(user)
    }

    private fun consumeVerifiedEmail(email: String) {
        val emailVerify = emailVerifyRepository.findByEmail(email)
            ?: throw ApiException(ResponseCode.EMAIL_VERIFY_REQUIRED)

        if (emailVerify.expiredAt.isBefore(LocalDateTime.now())) {
            throw ApiException(ResponseCode.EMAIL_VERIFY_EXPIRED)
        }

        if (!emailVerify.isVerified()) {
            throw ApiException(ResponseCode.EMAIL_VERIFY_REQUIRED)
        }

        emailVerifyRepository.delete(emailVerify)
    }

    private fun transferOrDeleteLeadingOrganizations(user: User) {
        val memberships = organizationMemberRepository.findByUsersOrderByCreatedAtAsc(user)
        memberships.forEach { membership ->
            if (membership.role != OrganizationMember.OrganizationRole.LEADER) {
                return@forEach
            }

            val organization = membership.organization
            val otherMembers = organizationMemberRepository.findByOrganization_IdOrderByCreatedAtAsc(organization.id!!)
                .filter { it.users.id != user.id }

            if (otherMembers.isEmpty()) {
                deleteOrganizationData(organization)
            } else {
                val nextLeader = otherMembers.first()
                nextLeader.updateRole(OrganizationMember.OrganizationRole.LEADER)
                organization.updateCreator(nextLeader.users)
            }
        }
    }

    private fun transferOrDeleteCreatedGroups(user: User) {
        val userId = user.id ?: throw ApiException(ResponseCode.RESOURCE_NOT_FOUND)
        val createdGroups = groupRepository.findByCreator_Id(userId)
        createdGroups.forEach { group ->
            val otherMembers = groupUserRepository.findByGroup_IdOrderByCreatedAtAsc(group.id!!)
                .filter { it.users.id != userId }

            if (otherMembers.isEmpty()) {
                deleteGroupData(group)
            } else {
                group.updateCreator(otherMembers.first().users)
            }
        }
    }

    private fun deleteOrganizationData(organization: Organization) {
        organizationScoreImageRepository.deleteByOrganizationScore_Organization(organization)
        organizationScoreRepository.deleteByOrganization(organization)
        scoreImageRepository.deleteByScore_Group_Organization(organization)
        scoreRepository.deleteByGroup_Organization(organization)
        groupUserRepository.deleteByGroup_Organization(organization)
        groupRepository.deleteByOrganization(organization)
        organizationMemberRepository.deleteByOrganization(organization)
        organizationRepository.delete(organization)
    }

    private fun deleteGroupData(group: Group) {
        scoreImageRepository.deleteByScore_Group(group)
        scoreRepository.deleteByGroup(group)
        groupUserRepository.deleteAll(groupUserRepository.findByGroup_Id(group.id!!))
        groupRepository.delete(group)
    }

    private fun User.toProfileResponse(): UserProfileResDto {
        return UserProfileResDto(
            id = id ?: throw ApiException(ResponseCode.RESOURCE_NOT_FOUND),
            email = username,
            name = name,
            organization = bio,
            profileImageUrl = profileImageUrl
        )
    }
}
