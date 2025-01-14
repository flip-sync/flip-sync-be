package com.zeki.flipsyncdb.repository

import com.zeki.flipsyncdb.entity.EmailVerify
import org.springframework.data.jpa.repository.JpaRepository

interface EmailVerifyRepository : JpaRepository<EmailVerify, Long> {
    fun findByEmail(email: String): EmailVerify?
}