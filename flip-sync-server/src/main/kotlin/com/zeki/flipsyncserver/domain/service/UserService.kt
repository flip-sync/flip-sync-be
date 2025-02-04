package com.zeki.flipsyncserver.domain.service

import com.zeki.common.exception.ApiException
import com.zeki.common.exception.ResponseCode
import com.zeki.flipsyncdb.entity.EmailVerify
import com.zeki.flipsyncdb.entity.User
import com.zeki.flipsyncdb.repository.EmailVerifyRepository
import com.zeki.flipsyncdb.repository.UserRepository
import com.zeki.flipsyncserver.config.security.jwt.JwtTokenProvider
import com.zeki.flipsyncserver.domain.dto.request.UserResetPasswordReqDto
import com.zeki.flipsyncserver.domain.dto.request.UserSignupReqDto
import com.zeki.flipsyncserver.domain.dto.request.UserVerifyEmailReqDto
import com.zeki.flipsyncserver.domain.dto.response.TokenResDto
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class UserService(
    private val userRepository: UserRepository,
    private val getUserEntityService: GetUserEntityService,
    private val emailVerifyRepository: EmailVerifyRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,

    private val emailService: EmailService
) {

    @Transactional
    fun signup(reqDto: UserSignupReqDto): Long {
        val user = getUserEntityService.getUserNullable(reqDto.email)
        if (user != null) throw ApiException(ResponseCode.CONFLICT_DATA)
        val userEntity = User.create(reqDto.email, passwordEncoder.encode(reqDto.password), reqDto.name)

        return userRepository.save(userEntity).id!!
    }

    @Transactional
    fun createVerifyEmail(email: String) {
        // 기존에 생성된 인증코드가 있다면 이메일 인증정보 삭제
        emailVerifyRepository.findByEmail(email)?.let {
            emailVerifyRepository.delete(it)
        }

        val emailVerify = EmailVerify.create(email, this.createCode(), LocalDateTime.now().plusMinutes(5))

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
        if (emailVerify.code != reqDto.code) throw ApiException(ResponseCode.EMAIL_VERIFY_UNAUTHORIZED)
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

    @Transactional(readOnly = true)
    fun resetPassword(reqDto: UserResetPasswordReqDto) {
        val user = userRepository.findByUsername(reqDto.email) ?: throw ApiException(ResponseCode.RESOURCE_NOT_FOUND)
        user.updatePassword(passwordEncoder.encode(reqDto.password))
    }
}
