package com.zeki.flipsyncdb.entity

import com.zeki.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "app_version_policies",
    schema = "flip_sync",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_app_version_policies_platform", columnNames = ["platform"])
    ]
)
class AppVersionPolicy private constructor(
    platform: String,
    latestVersion: String,
    latestBuildVersion: Int,
    minimumBuildVersion: Int,
    storeUrl: String,
    forceUpdateMessage: String,
    optionalUpdateMessage: String
) : BaseEntity() {
    @Column(name = "platform", length = 20, nullable = false)
    var platform: String = platform
        protected set

    @Column(name = "latest_version", length = 30, nullable = false)
    var latestVersion: String = latestVersion
        protected set

    @Column(name = "latest_build_version", nullable = false)
    var latestBuildVersion: Int = latestBuildVersion
        protected set

    @Column(name = "minimum_build_version", nullable = false)
    var minimumBuildVersion: Int = minimumBuildVersion
        protected set

    @Column(name = "store_url", length = 500, nullable = false)
    var storeUrl: String = storeUrl
        protected set

    @Column(name = "force_update_message", length = 500, nullable = false)
    var forceUpdateMessage: String = forceUpdateMessage
        protected set

    @Column(name = "optional_update_message", length = 500, nullable = false)
    var optionalUpdateMessage: String = optionalUpdateMessage
        protected set

    companion object {
        fun create(
            platform: String,
            latestVersion: String,
            latestBuildVersion: Int,
            minimumBuildVersion: Int,
            storeUrl: String,
            forceUpdateMessage: String,
            optionalUpdateMessage: String
        ): AppVersionPolicy {
            return AppVersionPolicy(
                platform = platform,
                latestVersion = latestVersion,
                latestBuildVersion = latestBuildVersion,
                minimumBuildVersion = minimumBuildVersion,
                storeUrl = storeUrl,
                forceUpdateMessage = forceUpdateMessage,
                optionalUpdateMessage = optionalUpdateMessage
            )
        }
    }

    fun update(
        latestVersion: String,
        latestBuildVersion: Int,
        minimumBuildVersion: Int,
        storeUrl: String,
        forceUpdateMessage: String,
        optionalUpdateMessage: String
    ) {
        this.latestVersion = latestVersion
        this.latestBuildVersion = latestBuildVersion
        this.minimumBuildVersion = minimumBuildVersion
        this.storeUrl = storeUrl
        this.forceUpdateMessage = forceUpdateMessage
        this.optionalUpdateMessage = optionalUpdateMessage
    }
}
