package com.zeki.flipsyncserver.domain.service

import com.zeki.common.exception.ApiException
import com.zeki.common.exception.ResponseCode
import com.zeki.flipsyncdb.entity.User
import com.zeki.flipsyncdb.repository.UserRepository
import com.zeki.flipsyncserver.config.security.jwt.JwtTokenProvider
import com.zeki.flipsyncserver.support.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder

class UserLoginTokenTest : IntegrationTest() {

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @BeforeEach
    fun setUp() {
        userRepository.deleteAll()
    }

    @Test
    fun `login returns access token and persists refresh token`() {
        val email = testEmail("login")
        createUser(email = email, password = "Password1234!")

        val token = userService.login(email = email, password = "Password1234!")

        assertThat(token.accessToken).isNotBlank()
        assertThat(token.refreshToken).isNotBlank()
        assertThat(token.accessTokenExpiresIn).isEqualTo(JwtTokenProvider.ACCESS_TOKEN_EXPIRE_TIME)
        assertThat(jwtTokenProvider.getUsername(token.accessToken)).isEqualTo(email)
        assertThat(userRepository.findByUsername(email)!!.refreshToken).isEqualTo(token.refreshToken)
    }

    @Test
    fun `login rejects wrong password`() {
        val email = testEmail("wrong-password")
        createUser(email = email, password = "Password1234!")

        val exception = assertThrows<ApiException> {
            userService.login(email = email, password = "WrongPassword1234!")
        }

        assertThat(exception.responseCode).isEqualTo(ResponseCode.UNAUTHORIZED)
    }

    @Test
    fun `loginRefresh reissues access token and keeps refresh token`() {
        val email = testEmail("refresh")
        createUser(email = email, password = "Password1234!")
        val loginToken = userService.login(email = email, password = "Password1234!")

        val refreshedToken = userService.loginRefresh(loginToken.refreshToken)

        assertThat(refreshedToken.accessToken).isNotBlank()
        assertThat(refreshedToken.refreshToken).isEqualTo(loginToken.refreshToken)
        assertThat(refreshedToken.accessTokenExpiresIn).isEqualTo(JwtTokenProvider.ACCESS_TOKEN_EXPIRE_TIME)
        assertThat(jwtTokenProvider.getUsername(refreshedToken.accessToken)).isEqualTo(email)
    }

    private fun createUser(email: String, password: String): User {
        return userRepository.save(
            User.create(
                username = email,
                password = passwordEncoder.encode(password),
                name = "로그인 사용자"
            )
        )
    }

    private fun testEmail(prefix: String): String {
        return "$prefix-${System.nanoTime()}@flipsync.test"
    }
}
