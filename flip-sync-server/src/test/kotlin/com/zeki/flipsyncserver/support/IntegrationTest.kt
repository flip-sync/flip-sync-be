package com.zeki.flipsyncserver.support

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    properties = [
        "flipsync.local-upload.enabled=true",
        "flipsync.local-upload.base-url=http://localhost:8080"
    ]
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegrationTest
