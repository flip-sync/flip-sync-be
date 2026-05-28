package com.zeki.flipsyncserver.domain.service

import com.zeki.common.exception.ApiException
import com.zeki.common.exception.ResponseCode
import com.zeki.flipsyncdb.repository.AppVersionPolicyRepository
import com.zeki.flipsyncserver.domain.dto.request.AppVersionPolicyUpdateReqDto
import com.zeki.flipsyncserver.support.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class AppVersionPolicyServiceTest : IntegrationTest() {

    @Autowired
    private lateinit var appVersionPolicyService: AppVersionPolicyService

    @Autowired
    private lateinit var appVersionPolicyRepository: AppVersionPolicyRepository

    @BeforeEach
    fun setUp() {
        appVersionPolicyRepository.deleteAll()
    }

    @Test
    fun `getPolicy returns android defaults from configuration`() {
        val policy = appVersionPolicyService.getPolicy("android")

        assertThat(policy.platform).isEqualTo("android")
        assertThat(policy.latestVersion).isNotBlank()
        assertThat(policy.latestBuildVersion).isPositive()
        assertThat(policy.minimumBuildVersion).isPositive()
        assertThat(policy.minimumBuildVersion).isLessThanOrEqualTo(policy.latestBuildVersion)
        assertThat(policy.storeUrl).isNotBlank()
    }

    @Test
    fun `getPolicy returns ios defaults from configuration`() {
        val policy = appVersionPolicyService.getPolicy("ios")

        assertThat(policy.platform).isEqualTo("ios")
        assertThat(policy.latestVersion).isNotBlank()
        assertThat(policy.latestBuildVersion).isPositive()
        assertThat(policy.minimumBuildVersion).isPositive()
        assertThat(policy.minimumBuildVersion).isLessThanOrEqualTo(policy.latestBuildVersion)
        assertThat(policy.storeUrl).isNotBlank()
    }

    @Test
    fun `getPolicy rejects unsupported platform`() {
        val exception = assertThrows<ApiException> {
            appVersionPolicyService.getPolicy("web")
        }

        assertThat(exception.responseCode).isEqualTo(ResponseCode.BAD_REQUEST)
        assertThat(exception.messages).isEqualTo("Unsupported platform.")
    }

    @Test
    fun `upsertPolicy creates and updates policy by normalized platform`() {
        val fallback = appVersionPolicyService.getPolicy("android")
        val created = appVersionPolicyService.upsertPolicy(
            AppVersionPolicyUpdateReqDto(
                platform = "ANDROID",
                latestVersion = "1.2.0",
                latestBuildVersion = 12,
                minimumBuildVersion = 10,
                storeUrl = "https://play.google.com/store/apps/details?id=com.fliplyze.flipsync",
                forceUpdateMessage = "필수 업데이트가 필요합니다.",
                optionalUpdateMessage = "새 업데이트가 있어요."
            )
        )
        val updated = appVersionPolicyService.upsertPolicy(
            AppVersionPolicyUpdateReqDto(
                platform = "android",
                latestVersion = "1.3.0",
                latestBuildVersion = 13,
                minimumBuildVersion = 11,
                storeUrl = "https://play.google.com/store/apps/details?id=com.fliplyze.flipsync",
                forceUpdateMessage = null,
                optionalUpdateMessage = null
            )
        )

        assertThat(created.platform).isEqualTo("android")
        assertThat(updated.latestVersion).isEqualTo("1.3.0")
        assertThat(updated.latestBuildVersion).isEqualTo(13)
        assertThat(updated.minimumBuildVersion).isEqualTo(11)
        assertThat(updated.forceUpdateMessage).isEqualTo(fallback.forceUpdateMessage)
        assertThat(updated.optionalUpdateMessage).isEqualTo(fallback.optionalUpdateMessage)
        assertThat(appVersionPolicyRepository.count()).isEqualTo(1)
    }

    @Test
    fun `upsertPolicy rejects minimum build greater than latest build`() {
        val exception = assertThrows<ApiException> {
            appVersionPolicyService.upsertPolicy(
                AppVersionPolicyUpdateReqDto(
                    platform = "android",
                    latestVersion = "1.0.0",
                    latestBuildVersion = 9,
                    minimumBuildVersion = 10,
                    storeUrl = "https://play.google.com/store/apps/details?id=com.fliplyze.flipsync"
                )
            )
        }

        assertThat(exception.responseCode).isEqualTo(ResponseCode.BAD_REQUEST)
        assertThat(appVersionPolicyRepository.count()).isZero()
    }
}
