package com.zeki.flipsyncdb.entity

import com.zeki.common.em.Status
import com.zeki.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "email_verify", schema = "flip_sync")
class EmailVerify private constructor(
    status: Status = Status.N,
    email: String,
    code: String,
    expiredAt: LocalDateTime
) : BaseEntity() {
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: Status = status

    @Column(name = "email", nullable = false, length = 50)
    var email: String = email

    @Column(name = "code", nullable = false, length = 7)
    var code: String = code

    @Column(name = "expired_at", nullable = false)
    var expiredAt: LocalDateTime = expiredAt

    companion object {
        fun create(email: String, code: String, expiredAt: LocalDateTime): EmailVerify {
            return EmailVerify(email = email, code = code, expiredAt = expiredAt)
        }
    }
}