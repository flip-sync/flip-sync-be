package com.zeki.flipsyncdb.entity

import com.zeki.common.em.Status
import com.zeki.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "email_verify",
    schema = "flip_sync",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_email_verify_email", columnNames = ["email"])
    ]
)
class EmailVerify private constructor(
    status: Status = Status.N,
    email: String,
    code: String,
    expiredAt: LocalDateTime
) : BaseEntity() {
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: Status = status
        protected set

    @Column(name = "email", nullable = false, length = 255)
    var email: String = email
        protected set

    @Column(name = "code", nullable = false, length = 7)
    var code: String = code
        protected set

    @Column(name = "expired_at", nullable = false)
    var expiredAt: LocalDateTime = expiredAt
        protected set

    @Column(name = "try_count", nullable = false)
    var tryCount: Int = 0
        protected set

    companion object {
        fun create(email: String, code: String, expiredAt: LocalDateTime): EmailVerify {
            return EmailVerify(email = email, code = code, expiredAt = expiredAt)
        }
    }
    
    fun updateTryCount() {
        tryCount++
    }

    fun reissue(code: String, expiredAt: LocalDateTime) {
        this.status = Status.N
        this.code = code
        this.expiredAt = expiredAt
        this.tryCount = 0
    }

    fun markVerified() {
        status = Status.Y
    }

    fun isVerified(): Boolean {
        return status == Status.Y
    }
}
