package com.zeki.flipsyncserver.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Path

@Configuration
class LocalUploadResourceConfig : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val uploadRoot = Path.of(System.getProperty("java.io.tmpdir"), "flipsync-local-upload")
        registry.addResourceHandler("/local-upload/**")
            .addResourceLocations(uploadRoot.toUri().toString())
    }
}
