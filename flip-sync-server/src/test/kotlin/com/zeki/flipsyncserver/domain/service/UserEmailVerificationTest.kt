package com.zeki.flipsyncserver.domain.service

import com.zeki.common.em.Status
import com.zeki.common.exception.ApiException
import com.zeki.common.exception.ResponseCode
import com.zeki.flipsyncdb.entity.EmailVerify
import com.zeki.flipsyncdb.repository.EmailVerifyRepository
import com.zeki.flipsyncdb.repository.UserRepository
import com.zeki.flipsyncserver.domain.dto.request.UserSignupReqDto
import com.zeki.flipsyncserver.domain.dto.request.UserVerifyEmailReqDto
import com.zeki.flipsyncserver.support.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import java.time.LocalDateTime

class UserEmailVerificationTest : IntegrationTest() {

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var emailVerifyRepository: EmailVerifyRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @MockBean
    private lateinit var emailService: EmailService

    @BeforeEach
    fun setUp() {
        userRepository.deleteAll()
        emailVerifyRepository.deleteAll()
    }

    @Test
    fun `createVerifyEmail creates six digit pending code`() {
        val email = testEmail("create")

        userService.createVerifyEmail(email)

        val emailVerify = emailVerifyRepository.findByEmail(email)
        assertThat(emailVerify).isNotNull
        assertThat(emailVerify!!.status).isEqualTo(Status.N)
        assertThat(emailVerify.code).matches("\\d{6}")
        assertThat(emailVerify.expiredAt).isAfter(LocalDateTime.now())
        assertThat(emailVerify.tryCount).isZero()
    }

    @Test
    fun `createVerifyEmail enforces resend cooldown`() {
        val email = testEmail("cooldown")

        userService.createVerifyEmail(email)

        assertApiException(ResponseCode.TOO_MANY_REQUESTS) {
            userService.createVerifyEmail(email)
        }
    }

    @Test
    fun `checkVerifyEmail marks email as verified when code matches`() {
        val email = testEmail("verify")
        userService.createVerifyEmail(email)
        val code = emailVerifyRepository.findByEmail(email)!!.code

        userService.checkVerifyEmail(UserVerifyEmailReqDto(email = email, code = code))

        val emailVerify = emailVerifyRepository.findByEmail(email)
        assertThat(emailVerify!!.status).isEqualTo(Status.Y)
        assertThat(emailVerify.isVerified()).isTrue()
    }

    @Test
    fun `checkVerifyEmail rejects expired code`() {
        val email = testEmail("expired")
        emailVerifyRepository.save(
            EmailVerify.create(
                email = email,
                code = "123456",
                expiredAt = LocalDateTime.now().minusSeconds(1)
            )
        )

        assertApiException(ResponseCode.EMAIL_VERIFY_EXPIRED) {
            userService.checkVerifyEmail(UserVerifyEmailReqDto(email = email, code = "123456"))
        }
    }

    @Test
    fun `checkVerifyEmail limits wrong code attempts after three failures`() {
        val email = testEmail("try-limit")
        userService.createVerifyEmail(email)

        repeat(3) {
            assertApiException(ResponseCode.EMAIL_VERIFY_UNAUTHORIZED) {
                userService.checkVerifyEmail(UserVerifyEmailReqDto(email = email, code = "000000"))
            }
        }

        assertApiException(ResponseCode.EMAIL_VERIFY_TRY_LIMIT) {
            userService.checkVerifyEmail(UserVerifyEmailReqDto(email = email, code = "000000"))
        }
    }

    @Test
    fun `signup requires verified email`() {
        val email = testEmail("signup-required")

        assertApiException(ResponseCode.EMAIL_VERIFY_REQUIRED) {
            userService.signup(signupRequest(email))
        }
    }

    @Test
    fun `signup consumes verified email and creates user`() {
        val email = testEmail("signup")
        userService.createVerifyEmail(email)
        val code = emailVerifyRepository.findByEmail(email)!!.code
        userService.checkVerifyEmail(UserVerifyEmailReqDto(email = email, code = code))

        val userId = userService.signup(signupRequest(email))

        assertThat(userId).isPositive()
        assertThat(userRepository.findByUsername(email)).isNotNull
        assertThat(emailVerifyRepository.findByEmail(email)).isNull()
    }

    private fun assertApiException(responseCode: ResponseCode, block: () -> Unit) {
        val exception = assertThrows<ApiException> { block() }
        assertThat(exception.responseCode).isEqualTo(responseCode)
    }

    private fun signupRequest(email: String): UserSignupReqDto {
        return UserSignupReqDto(
            email = email,
            password = "Password1234!",
            passwordConfirm = "Password1234!",
            name = "테스트 사용자"
        )
    }

    private fun testEmail(prefix: String): String {
        return "$prefix-${System.nanoTime()}@flipsync.test"
    }
}
