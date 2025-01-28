package com.zeki.flipsyncserver.domain.service

import jakarta.mail.internet.MimeMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val emailSender: JavaMailSender
) {
    fun sendEmail(email: String, code: String) {
        val title = "[플립싱크] 회원가입 인증 코드"
        val html = """
            <h1>플립싱크 회원가입 인증 코드</h1>
            <p>아래의 인증 코드를 입력해주세요.</p>
            <p>인증 코드 : ${code}</p>
            <p>감사합니다.</p>
            """
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