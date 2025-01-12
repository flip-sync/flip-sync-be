package com.zeki.flipsyncserver.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import javax.sql.DataSource

@Configuration
class CustomDataSource(
    private val env: Environment
) {

    @Bean
    fun dataSource(): DataSource {
        val config = HikariConfig()
        config.jdbcUrl = env.getProperty("spring.custom.datasource.url")
        config.username = env.getProperty("spring.custom.datasource.username")
        config.password = env.getProperty("spring.custom.datasource.password")
        config.driverClassName = env.getProperty("spring.custom.datasource.driver-class-name")

        return HikariDataSource(config)
    }

}