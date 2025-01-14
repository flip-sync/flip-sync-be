package com.zeki.flipsyncserver.domain.service

import com.zeki.common.exception.ApiException
import com.zeki.common.exception.ResponseCode
import com.zeki.flipsyncdb.entity.EmailVerify
import com.zeki.flipsyncdb.entity.User
import com.zeki.flipsyncdb.repository.EmailVerifyRepository
import com.zeki.flipsyncdb.repository.UserRepository
import com.zeki.flipsyncserver.domain.dto.request.UserSignupReqDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class UserService(
    private val userRepository: UserRepository,
    private val emailVerifyRepository: EmailVerifyRepository
) {

    @Transactional
    fun signup(reqDto: UserSignupReqDto): Long {
        val userEntity = User.create(reqDto.email, reqDto.name)

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

        // TODO : 메일러 연동 및 메일 전~
    }

    private fun createCode(): String {
        return (100000..999999).random().toString()
    }

    @Transactional
    fun checkVerifyEmail(email: String, code: String) {
        val emailVerify = emailVerifyRepository.findByEmail(email)
        if (emailVerify == null) {
            throw ApiException(ResponseCode.EMAIL_VERIFY_NOT_FOUND)
        }

        if (emailVerify.code != code) {
            throw ApiException(ResponseCode.EMAIL_VERIFY_UNAUTHORIZED)
        }

    }

}
