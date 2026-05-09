package com.zeki.flipsyncserver.config

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
class PublicPageController {

    @GetMapping(
        value = [
            "/legal/privacy-policy",
            "/legal/privacy-policy/",
            "/mob/legal/privacy-policy",
            "/mob/legal/privacy-policy/"
        ],
        produces = [MediaType.TEXT_HTML_VALUE]
    )
    @ResponseBody
    fun privacyPolicy(): ResponseEntity<String> = htmlResponse(
        """
        <!DOCTYPE html>
        <html lang="ko">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>FlipSync 개인정보처리방침</title>
            <style>
                :root {
                    color-scheme: light;
                    --bg: #f4f7fb;
                    --card: #ffffff;
                    --text: #172033;
                    --muted: #5a6782;
                    --line: #d9e2f1;
                    --accent: #2e9cd0;
                    --accent-soft: #e8f6fc;
                }
                * { box-sizing: border-box; }
                body {
                    margin: 0;
                    font-family: "Pretendard", "Noto Sans KR", "Apple SD Gothic Neo", sans-serif;
                    background:
                        radial-gradient(circle at top left, #ecf9ff 0, transparent 34%),
                        linear-gradient(180deg, #f7fbff 0%, var(--bg) 100%);
                    color: var(--text);
                    line-height: 1.7;
                }
                .page {
                    max-width: 920px;
                    margin: 0 auto;
                    padding: 32px 20px 72px;
                }
                .hero, .section, .footer-card {
                    background: var(--card);
                    border: 1px solid var(--line);
                    border-radius: 24px;
                    box-shadow: 0 18px 50px rgba(34, 52, 93, 0.08);
                }
                .hero {
                    padding: 32px 28px;
                    margin-bottom: 20px;
                }
                .eyebrow {
                    display: inline-flex;
                    align-items: center;
                    padding: 6px 12px;
                    border-radius: 999px;
                    background: var(--accent-soft);
                    color: var(--accent);
                    font-size: 14px;
                    font-weight: 700;
                }
                h1 {
                    margin: 16px 0 10px;
                    font-size: clamp(30px, 5vw, 42px);
                    line-height: 1.18;
                }
                h2 {
                    margin: 0 0 12px;
                    font-size: 21px;
                }
                .hero p, .meta {
                    margin: 0;
                    color: var(--muted);
                }
                .meta {
                    margin-top: 16px;
                    font-size: 14px;
                }
                .section {
                    padding: 24px 24px 8px;
                    margin-bottom: 16px;
                }
                p, li { font-size: 16px; }
                ul {
                    margin: 0 0 16px;
                    padding-left: 20px;
                }
                .nav {
                    display: flex;
                    flex-wrap: wrap;
                    gap: 10px;
                    margin-top: 18px;
                }
                .nav a, a {
                    color: var(--accent);
                    text-decoration: none;
                    font-weight: 700;
                }
                .footer-card {
                    padding: 24px;
                    margin-top: 20px;
                }
            </style>
        </head>
        <body>
        <main class="page">
            <section class="hero">
                <div class="eyebrow">FlipSync Legal</div>
                <h1>개인정보처리방침</h1>
                <p>FlipSync는 합주 팀과 음악 그룹이 악보를 공유하고 정리하는 과정에서 필요한 최소한의 개인정보를 수집하고, 안전하게 처리하기 위해 노력합니다.</p>
                <p class="meta">시행일: 2026-05-04 · 마지막 업데이트: 2026-05-05</p>
                <div class="nav">
                    <a href="./account-deletion">계정삭제 안내</a>
                    <a href="../support">고객 지원</a>
                </div>
            </section>

            <section class="section">
                <h2>1. 수집하는 개인정보</h2>
                <p>FlipSync는 서비스 제공을 위해 다음 정보를 수집하거나 처리할 수 있습니다.</p>
                <ul>
                    <li>회원가입 및 로그인 정보: 이메일 주소, 이름 또는 닉네임, 비밀번호</li>
                    <li>프로필 정보: 프로필 이미지</li>
                    <li>서비스 이용 중 생성되는 정보: 소속 정보, 공유방 참여 정보, 사용자가 업로드한 악보 이미지</li>
                    <li>서비스 운영 과정에서 자동으로 생성될 수 있는 정보: 접속 로그, 오류 로그, 기기 및 앱 버전 정보</li>
                </ul>
            </section>

            <section class="section">
                <h2>2. 개인정보 이용 목적</h2>
                <ul>
                    <li>회원 식별 및 로그인 처리</li>
                    <li>소속 선택, 공유방 참여, 악보 업로드 및 조회 기능 제공</li>
                    <li>사용자 프로필 관리</li>
                    <li>서비스 안정성 확보 및 오류 대응</li>
                    <li>고객 문의 대응 및 운영 지원</li>
                </ul>
            </section>

            <section class="section">
                <h2>3. 개인정보 보관 기간</h2>
                <p>FlipSync는 원칙적으로 회원 탈퇴 또는 수집 목적 달성 시 개인정보를 지체 없이 삭제합니다.</p>
                <p>다만 관련 법령상 보관 의무가 있거나, 보안 및 장애 대응을 위해 필요한 최소 범위의 기록은 법령 또는 내부 운영 기준에 따라 일정 기간 보관될 수 있습니다.</p>
            </section>

            <section class="section">
                <h2>4. 개인정보 제공 및 처리 위탁</h2>
                <p>FlipSync는 원칙적으로 이용자의 개인정보를 판매하거나 무단으로 제3자에게 제공하지 않습니다.</p>
                <p>다만 서비스 운영을 위해 클라우드 인프라, 파일 저장, 이메일 발송 등 외부 서비스를 이용할 수 있으며, 이 경우 필요한 범위 내에서만 개인정보를 처리합니다.</p>
            </section>

            <section class="section">
                <h2>5. 이용자의 권리</h2>
                <ul>
                    <li>본인 개인정보 조회 및 수정</li>
                    <li>프로필 정보 변경</li>
                    <li>계정 삭제 요청</li>
                    <li>개인정보 처리 관련 문의</li>
                </ul>
                <p>계정 삭제는 앱 내 안내 화면 또는 별도 계정삭제 안내 페이지를 통해 진행할 수 있습니다.</p>
            </section>

            <section class="section">
                <h2>6. 계정 삭제 및 데이터 삭제</h2>
                <p>이용자가 계정 삭제를 요청하면, 계정과 관련된 개인정보 및 업로드 콘텐츠는 운영 정책에 따라 삭제됩니다.</p>
                <p>다만 법령상 보관 의무가 있는 기록이나, 보안 및 분쟁 대응, 장애 분석을 위해 필요한 최소 범위의 로그는 예외적으로 일정 기간 보관될 수 있습니다.</p>
            </section>

            <section class="section">
                <h2>7. 문의처</h2>
                <p>개인정보 처리와 관련한 문의는 아래 이메일로 접수할 수 있습니다.</p>
                <p><strong>이메일:</strong> <a href="mailto:flipsync.score@gmail.com">flipsync.score@gmail.com</a></p>
            </section>

            <section class="footer-card">
                <strong>고지 의무</strong>
                <p>이 개인정보처리방침은 관련 법령, 서비스 기능, 운영 정책의 변경에 따라 수정될 수 있으며, 중요한 변경이 있는 경우 앱 또는 공개 페이지를 통해 안내합니다.</p>
            </section>
        </main>
        </body>
        </html>
        """.trimIndent()
    )

    @GetMapping(
        value = [
            "/legal/account-deletion",
            "/legal/account-deletion/",
            "/mob/legal/account-deletion",
            "/mob/legal/account-deletion/"
        ],
        produces = [MediaType.TEXT_HTML_VALUE]
    )
    @ResponseBody
    fun accountDeletion(): ResponseEntity<String> = htmlResponse(
        """
        <!DOCTYPE html>
        <html lang="ko">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>FlipSync 계정삭제 안내</title>
            <style>
                :root {
                    color-scheme: light;
                    --bg: #f8fafc;
                    --card: #ffffff;
                    --text: #172033;
                    --muted: #5a6782;
                    --line: #d9e2f1;
                    --accent: #e05757;
                    --accent-soft: #fff1f1;
                    --link: #2e9cd0;
                }
                * { box-sizing: border-box; }
                body {
                    margin: 0;
                    font-family: "Pretendard", "Noto Sans KR", "Apple SD Gothic Neo", sans-serif;
                    background:
                        radial-gradient(circle at top right, #fff1f1 0, transparent 28%),
                        linear-gradient(180deg, #ffffff 0%, var(--bg) 100%);
                    color: var(--text);
                    line-height: 1.7;
                }
                .page {
                    max-width: 920px;
                    margin: 0 auto;
                    padding: 32px 20px 72px;
                }
                .hero, .section, .footer-card {
                    background: var(--card);
                    border: 1px solid var(--line);
                    border-radius: 24px;
                    box-shadow: 0 18px 50px rgba(34, 52, 93, 0.08);
                }
                .hero {
                    padding: 32px 28px;
                    margin-bottom: 20px;
                }
                .eyebrow {
                    display: inline-flex;
                    align-items: center;
                    padding: 6px 12px;
                    border-radius: 999px;
                    background: var(--accent-soft);
                    color: var(--accent);
                    font-size: 14px;
                    font-weight: 700;
                }
                h1 {
                    margin: 16px 0 10px;
                    font-size: clamp(30px, 5vw, 42px);
                    line-height: 1.18;
                }
                h2 {
                    margin: 0 0 12px;
                    font-size: 21px;
                }
                .hero p, .meta {
                    margin: 0;
                    color: var(--muted);
                }
                .meta {
                    margin-top: 16px;
                    font-size: 14px;
                }
                .nav {
                    display: flex;
                    flex-wrap: wrap;
                    gap: 10px;
                    margin-top: 18px;
                }
                .nav a, a {
                    color: var(--link);
                    text-decoration: none;
                    font-weight: 700;
                }
                .section {
                    padding: 24px 24px 8px;
                    margin-bottom: 16px;
                }
                p, li { font-size: 16px; }
                ul, ol {
                    margin: 0 0 16px;
                    padding-left: 20px;
                }
                .footer-card {
                    padding: 24px;
                    margin-top: 20px;
                }
            </style>
        </head>
        <body>
        <main class="page">
            <section class="hero">
                <div class="eyebrow">FlipSync Legal</div>
                <h1>계정삭제 안내</h1>
                <p>FlipSync 사용자는 앱 내 안내 절차를 통해 계정을 삭제할 수 있습니다. 계정 삭제 시 개인정보와 업로드 콘텐츠는 운영 정책에 따라 삭제됩니다.</p>
                <p class="meta">마지막 업데이트: 2026-05-05</p>
                <div class="nav">
                    <a href="./privacy-policy">개인정보처리방침</a>
                    <a href="../support">고객 지원</a>
                </div>
            </section>

            <section class="section">
                <h2>1. 계정 삭제 방법</h2>
                <p>앱 내에서 아래 순서로 계정 삭제를 진행할 수 있습니다.</p>
                <ol>
                    <li>로그인</li>
                    <li>내 정보 또는 프로필 화면 진입</li>
                    <li><strong>회원탈퇴</strong> 또는 <strong>계정 삭제 안내</strong> 선택</li>
                    <li>현재 비밀번호 입력</li>
                    <li>계정 삭제 확인</li>
                </ol>
            </section>

            <section class="section">
                <h2>2. 계정 삭제 시 삭제되는 항목</h2>
                <ul>
                    <li>계정 프로필 정보</li>
                    <li>로그인에 사용되는 계정 정보</li>
                    <li>프로필 이미지</li>
                    <li>사용자가 업로드한 악보 이미지</li>
                    <li>서비스 이용을 위해 생성된 사용자 식별 정보</li>
                </ul>
            </section>

            <section class="section">
                <h2>3. 예외적으로 보관될 수 있는 정보</h2>
                <p>다음 정보는 법령 또는 서비스 운영상 필요에 따라 일정 기간 보관될 수 있습니다.</p>
                <ul>
                    <li>보안 및 장애 분석용 로그</li>
                    <li>법령상 보관 의무가 있는 정보</li>
                </ul>
            </section>

            <section class="section">
                <h2>4. 조직 및 공유방 관련 처리</h2>
                <p>계정 삭제 시 사용자가 소속 또는 공유방 운영자 역할을 가지고 있는 경우, 다른 구성원이 있는지 여부에 따라 운영 정책에 맞게 처리됩니다.</p>
                <p>필요한 경우 소유권 이전, 구성원 기준 정리 또는 관련 데이터 삭제가 함께 이루어질 수 있습니다.</p>
            </section>

            <section class="section">
                <h2>5. 처리 기간</h2>
                <p>앱 내 즉시 삭제가 가능한 항목은 계정 삭제 요청 완료와 함께 바로 처리될 수 있습니다.</p>
                <p>다만 운영 검토가 필요한 항목이나 법령상 보관 대상 정보는 별도 기준에 따라 후속 처리될 수 있습니다.</p>
            </section>

            <section class="section">
                <h2>6. 문의 방법</h2>
                <p>계정 삭제와 관련한 문의는 아래 이메일로 접수할 수 있습니다.</p>
                <p><strong>이메일:</strong> <a href="mailto:flipsync.score@gmail.com">flipsync.score@gmail.com</a></p>
            </section>

            <section class="footer-card">
                <strong>안내</strong>
                <p>계정 삭제 후에는 일부 기능 복구가 제한될 수 있습니다. 삭제 전 필요한 정보가 있는지 반드시 확인해 주세요.</p>
            </section>
        </main>
        </body>
        </html>
        """.trimIndent()
    )

    @GetMapping(
        value = [
            "/support",
            "/support/",
            "/mob/support",
            "/mob/support/"
        ],
        produces = [MediaType.TEXT_HTML_VALUE]
    )
    @ResponseBody
    fun support(): ResponseEntity<String> = htmlResponse(
        """
        <!DOCTYPE html>
        <html lang="ko">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>FlipSync 고객 지원</title>
            <style>
                :root {
                    color-scheme: light;
                    --bg: #f6f8fc;
                    --card: #ffffff;
                    --text: #172033;
                    --muted: #5a6782;
                    --line: #d9e2f1;
                    --accent: #2e9cd0;
                    --accent-soft: #e8f6fc;
                }
                * { box-sizing: border-box; }
                body {
                    margin: 0;
                    font-family: "Pretendard", "Noto Sans KR", "Apple SD Gothic Neo", sans-serif;
                    background:
                        radial-gradient(circle at top center, #ecf9ff 0, transparent 26%),
                        linear-gradient(180deg, #ffffff 0%, var(--bg) 100%);
                    color: var(--text);
                    line-height: 1.7;
                }
                .page {
                    max-width: 920px;
                    margin: 0 auto;
                    padding: 32px 20px 72px;
                }
                .hero, .section {
                    background: var(--card);
                    border: 1px solid var(--line);
                    border-radius: 24px;
                    box-shadow: 0 18px 50px rgba(34, 52, 93, 0.08);
                }
                .hero {
                    padding: 32px 28px;
                    margin-bottom: 20px;
                }
                .eyebrow {
                    display: inline-flex;
                    align-items: center;
                    padding: 6px 12px;
                    border-radius: 999px;
                    background: var(--accent-soft);
                    color: var(--accent);
                    font-size: 14px;
                    font-weight: 700;
                }
                h1 {
                    margin: 16px 0 10px;
                    font-size: clamp(30px, 5vw, 42px);
                    line-height: 1.18;
                }
                h2 {
                    margin: 0 0 12px;
                    font-size: 21px;
                }
                .hero p {
                    margin: 0;
                    color: var(--muted);
                }
                .nav {
                    display: flex;
                    flex-wrap: wrap;
                    gap: 10px;
                    margin-top: 18px;
                }
                .nav a, a {
                    color: var(--accent);
                    text-decoration: none;
                    font-weight: 700;
                }
                .section {
                    padding: 24px;
                    margin-bottom: 16px;
                }
                p, li { font-size: 16px; }
                ul {
                    margin: 0;
                    padding-left: 20px;
                }
            </style>
        </head>
        <body>
        <main class="page">
            <section class="hero">
                <div class="eyebrow">FlipSync Support</div>
                <h1>고객 지원</h1>
                <p>FlipSync 사용 중 문의, 계정 문제, 개인정보 처리 관련 요청은 아래 채널로 접수해 주세요.</p>
                <div class="nav">
                    <a href="./legal/privacy-policy">개인정보처리방침</a>
                    <a href="./legal/account-deletion">계정삭제 안내</a>
                    <a href="mailto:flipsync.score@gmail.com">이메일 보내기</a>
                </div>
            </section>

            <section class="section">
                <h2>문의 채널</h2>
                <p><strong>이메일:</strong> <a href="mailto:flipsync.score@gmail.com">flipsync.score@gmail.com</a></p>
            </section>

            <section class="section">
                <h2>문의 시 함께 보내주시면 좋은 정보</h2>
                <ul>
                    <li>사용 중인 플랫폼: Android 또는 iPhone</li>
                    <li>앱 버전</li>
                    <li>문제가 발생한 화면 또는 기능</li>
                    <li>오류 메시지 또는 재현 방법</li>
                    <li>계정 삭제 또는 개인정보 처리 요청 여부</li>
                </ul>
            </section>

            <section class="section">
                <h2>지원 페이지에서 확인할 수 있는 내용</h2>
                <ul>
                    <li>개인정보처리방침</li>
                    <li>계정삭제 안내</li>
                    <li>고객 문의 접수 이메일</li>
                </ul>
            </section>
        </main>
        </body>
        </html>
        """.trimIndent()
    )

    private fun htmlResponse(document: String): ResponseEntity<String> = ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, "${MediaType.TEXT_HTML_VALUE};charset=UTF-8")
        .body(document)
}
