package com.zeki.flipsyncserver.domain.service

import jakarta.mail.internet.MimeMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val emailSender: JavaMailSender
) {
    fun sendEmail(email: String, title: String, html: String) {
        val message: MimeMessage = emailSender.createMimeMessage()

        // MimeMessageHelper를 사용해 HTML 이메일 구성
        val helper = MimeMessageHelper(message, false, "UTF-8")
        helper.setTo(email)
        helper.setSubject(title)
        helper.setText(html, true) // HTML 내용을 true로 설정

        helper.setFrom("flipsync.score@gmail.com") // 발신자 이메일 설정

        emailSender.send(message) // 이메일 발송
    }

}