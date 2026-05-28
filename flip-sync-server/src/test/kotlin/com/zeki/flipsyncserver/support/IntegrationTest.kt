package com.zeki.flipsyncserver.support

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    properties = [
        "spring.custom.datasource.driver-class-name=org.h2.Driver",
        "spring.custom.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;INIT=CREATE SCHEMA IF NOT EXISTS flip_sync",
        "spring.custom.datasource.username=sa",
        "spring.custom.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "flipsync.local-upload.enabled=true",
        "flipsync.local-upload.base-url=http://localhost:8080"
    ]
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegrationTest
