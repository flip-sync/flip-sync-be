package com.zeki.flipsyncserver.domain.service

import com.zeki.common.exception.ApiException
import com.zeki.common.exception.ResponseCode
import com.zeki.flipsyncdb.entity.AppVersionPolicy
import com.zeki.flipsyncdb.repository.AppVersionPolicyRepository
import com.zeki.flipsyncserver.domain.dto.request.AppVersionPolicyUpdateReqDto
import com.zeki.flipsyncserver.domain.dto.response.AppVersionPolicyResDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AppVersionPolicyService(
    private val appVersionPolicyRepository: AppVersionPolicyRepository,
    @Value("\${flipsync.version-policy.android.latest-version:1.0.0}")
    private val androidLatestVersion: String,
    @Value("\${flipsync.version-policy.android.latest-build-version:11}")
    private val androidLatestBuildVersion: Int,
    @Value("\${flipsync.version-policy.android.minimum-build-version:1}")
    private val androidMinimumBuildVersion: Int,
    @Value("\${flipsync.version-policy.android.store-url:https://play.google.com/store/apps/details?id=com.fliplyze.flipsync}")
    private val androidStoreUrl: String,
    @Value("\${flipsync.version-policy.android.force-update-message:A new app version is required.}")
    private val androidForceUpdateMessage: String,
    @Value("\${flipsync.version-policy.android.optional-update-message:A new app version is available.}")
    private val androidOptionalUpdateMessage: String,
    @Value("\${flipsync.version-policy.ios.latest-version:1.0.0}")
    private val iosLatestVersion: String,
    @Value("\${flipsync.version-policy.ios.latest-build-version:1}")
    private val iosLatestBuildVersion: Int,
    @Value("\${flipsync.version-policy.ios.minimum-build-version:1}")
    private val iosMinimumBuildVersion: Int,
    @Value("\${flipsync.version-policy.ios.store-url:https://fliplyze.com/mob}")
    private val iosStoreUrl: String,
    @Value("\${flipsync.version-policy.ios.force-update-message:A new app version is required.}")
    private val iosForceUpdateMessage: String,
    @Value("\${flipsync.version-policy.ios.optional-update-message:A new app version is available.}")
    private val iosOptionalUpdateMessage: String
) {
    @Transactional(readOnly = true)
    fun getPolicy(platform: String): AppVersionPolicyResDto {
        val normalizedPlatform = normalizePlatform(platform)
        val dbPolicy = runCatching {
            appVersionPolicyRepository.findFirstByPlatformIgnoreCase(normalizedPlatform)
        }.getOrElse { error ->
            if (error is DataAccessException) {
                null
            } else {
                throw error
            }
        }

        return dbPolicy?.toResDto() ?: defaultPolicy(normalizedPlatform)
    }

    @Transactional
    fun upsertPolicy(reqDto: AppVersionPolicyUpdateReqDto): AppVersionPolicyResDto {
        if (reqDto.minimumBuildVersion > reqDto.latestBuildVersion) {
            throw ApiException(
                ResponseCode.BAD_REQUEST,
                "minimumBuildVersion cannot be greater than latestBuildVersion."
            )
        }

        val normalizedPlatform = normalizePlatform(reqDto.platform)
        val fallback = defaultPolicy(normalizedPlatform)
        val forceUpdateMessage = reqDto.forceUpdateMessage?.trim()?.ifBlank { null } ?: fallback.forceUpdateMessage
        val optionalUpdateMessage =
            reqDto.optionalUpdateMessage?.trim()?.ifBlank { null } ?: fallback.optionalUpdateMessage
        val existingPolicy = appVersionPolicyRepository.findFirstByPlatformIgnoreCase(normalizedPlatform)

        val policy = if (existingPolicy == null) {
            AppVersionPolicy.create(
                platform = normalizedPlatform,
                latestVersion = reqDto.latestVersion.trim(),
                latestBuildVersion = reqDto.latestBuildVersion,
                minimumBuildVersion = reqDto.minimumBuildVersion,
                storeUrl = reqDto.storeUrl.trim(),
                forceUpdateMessage = forceUpdateMessage,
                optionalUpdateMessage = optionalUpdateMessage
            )
        } else {
            existingPolicy.apply {
                update(
                    latestVersion = reqDto.latestVersion.trim(),
                    latestBuildVersion = reqDto.latestBuildVersion,
                    minimumBuildVersion = reqDto.minimumBuildVersion,
                    storeUrl = reqDto.storeUrl.trim(),
                    forceUpdateMessage = forceUpdateMessage,
                    optionalUpdateMessage = optionalUpdateMessage
                )
            }
        }

        return appVersionPolicyRepository.save(policy).toResDto()
    }

    private fun normalizePlatform(platform: String): String {
        return when (platform.trim().lowercase()) {
            "ios" -> "ios"
            else -> "android"
        }
    }

    private fun defaultPolicy(platform: String): AppVersionPolicyResDto {
        if (platform == "ios") {
            return AppVersionPolicyResDto(
                platform = platform,
                latestVersion = iosLatestVersion,
                latestBuildVersion = iosLatestBuildVersion,
                minimumBuildVersion = iosMinimumBuildVersion,
                storeUrl = iosStoreUrl,
                forceUpdateMessage = iosForceUpdateMessage,
                optionalUpdateMessage = iosOptionalUpdateMessage
            )
        }

        return AppVersionPolicyResDto(
            platform = platform,
            latestVersion = androidLatestVersion,
            latestBuildVersion = androidLatestBuildVersion,
            minimumBuildVersion = androidMinimumBuildVersion,
            storeUrl = androidStoreUrl,
            forceUpdateMessage = androidForceUpdateMessage,
            optionalUpdateMessage = androidOptionalUpdateMessage
        )
    }

    private fun AppVersionPolicy.toResDto(): AppVersionPolicyResDto {
        return AppVersionPolicyResDto(
            platform = platform.lowercase(),
            latestVersion = latestVersion,
            latestBuildVersion = latestBuildVersion,
            minimumBuildVersion = minimumBuildVersion,
            storeUrl = storeUrl,
            forceUpdateMessage = forceUpdateMessage,
            optionalUpdateMessage = optionalUpdateMessage
        )
    }
}
