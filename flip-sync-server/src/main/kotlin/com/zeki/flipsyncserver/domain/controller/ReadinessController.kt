package com.zeki.flipsyncserver.domain.controller

import com.zeki.common.dto.CommonResDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import javax.sql.DataSource

@RestController
class ReadinessController(
    private val dataSource: DataSource
) {

    @GetMapping("/health", "/mob/health")
    fun health(): CommonResDto<ReadinessResponse> {
        return CommonResDto.success(
            ReadinessResponse(
                status = "UP",
                checkedAt = OffsetDateTime.now().toString(),
                database = "NOT_CHECKED"
            )
        )
    }

    @GetMapping("/ready", "/mob/ready")
    fun ready(): ResponseEntity<CommonResDto<ReadinessResponse>> {
        return try {
            dataSource.connection.use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("SELECT 1")
                }
            }
            ResponseEntity.ok(
                CommonResDto.success(
                    ReadinessResponse(
                        status = "UP",
                        checkedAt = OffsetDateTime.now().toString(),
                        database = "UP"
                    )
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                CommonResDto.error(
                    code = "503_0",
                    message = "Service is not ready",
                    data = ReadinessResponse(
                        status = "DOWN",
                        checkedAt = OffsetDateTime.now().toString(),
                        database = "DOWN"
                    )
                )
            )
        }
    }
}

data class ReadinessResponse(
    val status: String,
    val checkedAt: String,
    val database: String
)
