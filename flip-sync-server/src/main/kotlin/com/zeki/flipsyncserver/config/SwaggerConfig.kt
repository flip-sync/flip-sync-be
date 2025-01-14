package com.zeki.flipsyncserver.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {
    private val header: String = "Authorization"

    @Bean
    fun openAPI(): OpenAPI {
        val info = Info()
            .title("Flip Sync API")
            .description(
                """
                  Flip Sync API
                """.trimIndent()
            )
            .version("1.0")

        // SecuritySecheme명
        val jwtSchemeName = "Authorization"
        // API 요청헤더에 인증정보 포함
//        val securityRequirement = SecurityRequirement().addList(jwtSchemeName)
        // SecuritySchemes 등록
        val components = Components()
            .addSecuritySchemes(
                jwtSchemeName, SecurityScheme()
                    .type(SecurityScheme.Type.HTTP) // HTTP 방식
                    .scheme("bearer")
                    .bearerFormat("JWT")
            ) // 토큰 형식을 지정하는 임의의 문자(Optional)

        return OpenAPI()
            .info(info)
            .components(components)
    }
}