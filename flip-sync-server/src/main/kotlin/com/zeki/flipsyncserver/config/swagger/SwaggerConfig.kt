package com.zeki.flipsyncserver.config.swagger

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.security.SecurityRequirement
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
        val securityRequirement = SecurityRequirement().addList(jwtSchemeName)
        // SecuritySchemes 등록
        val components = Components()


        return OpenAPI()
            .info(info)
            .components(
                Components()
                    .addSecuritySchemes(
                        jwtSchemeName, SecurityScheme()
                            .type(SecurityScheme.Type.HTTP) // HTTP 방식
                            .scheme("bearer")
                            .bearerFormat("JWT")
                    ) // 토큰 형식을 지정하는 임의의 문자(Optional)

                    // 401
                    .addResponses(
                        "UNAUTHORIZED",
                        ApiResponse().description("인증 정보가 없거나 유효하지 않음").content(
                            Content().addMediaType(
                                "application/json",
                                MediaType().example(mapOf("code" to "401_0", "message" to "인증 정보가 없거나 유효하지 않습니다."))
                            )
                        )
                    )

                    .addResponses(
                        "EMAIL_VERIFY_UNAUTHORIZED",
                        ApiResponse().description("인증코드가 유효하지 않음").content(
                            Content().addMediaType(
                                "application/json",
                                MediaType().example(mapOf("code" to "401_1", "message" to "인증 코드가 유효하지 않습니다."))
                            )
                        )
                    )


                    // 403
                    .addResponses(
                        "FORBIDDEN",
                        ApiResponse().description("접근 권한이 없음").content(
                            Content().addMediaType(
                                "application/json",
                                MediaType().example(mapOf("code" to "403_0", "message" to "접근 권한이 없습니다."))
                            )
                        )
                    )


                    // 404
                    .addResponses(
                        "RESOURCE_NOT_FOUND",
                        ApiResponse().description("데이터가 존재하지 않음").content(
                            Content().addMediaType(
                                "application/json",
                                MediaType().example(mapOf("code" to "404_0", "message" to "데이터가 존재하지 않습니다."))
                            )
                        )
                    )

                    .addResponses(
                        "EMAIL_VERIFY_NOT_FOUND",
                        ApiResponse().description("이메일 인증 정보가 DB에 없음").content(
                            Content().addMediaType(
                                "application/json",
                                MediaType().example(mapOf("code" to "404_1", "message" to "이메일 인증 정보가 존재하지 않습니다."))
                            )
                        )
                    )


                    //409
                    .addResponses(
                        "CONFLICT_DATA",
                        ApiResponse().description("데이터가 충돌 됨").content(
                            Content().addMediaType(
                                "application/json",
                                MediaType().example(mapOf("code" to "409_0", "message" to "데이터가 충돌되었습니다."))
                            )
                        )
                    )


            )
    }
}