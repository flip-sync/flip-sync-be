package com.zeki.common.exception

import org.springframework.http.HttpStatus

/**
 * 처리결과 응답 코드
 */
enum class ResponseCode(
    val status: HttpStatus,
    val code: String,
    val defaultMessage: String
) {
    /* 200 */
    OK(HttpStatus.OK, "200_0", "정상 처리 되었습니다."),

    /* 400 */
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "400_0", "잘못된 요청입니다."),
    BINDING_FAILED(HttpStatus.BAD_REQUEST, "400_1", "유효성 검사 실패"),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "400_2", "Request body가 잘못되었습니다."),

    /* 401 */
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "401_0", "인증 정보가 없습니다."),

    /* 403 */
    UNMODIFIABLE_INFORMATION(HttpStatus.FORBIDDEN, "403_0", "변경할 수 없는 정보입니다."),

    /* 404 */
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "404_0", "데이터가 존재하지 않습니다."),

    /* 409 Conflict - 클라이언트의 요청이 서버에서 충돌을 일으킨 경우 사용 */
    CONFLICT_DATA(HttpStatus.CONFLICT, "409_0", "데이터가 충돌되었습니다."),

    /* 500 */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500_0", "서버측 에러"),
    INTERNAL_SERVER_WEBCLIENT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500_1", "WebClient 통신 에러"),
    INVALID_PROFILE(HttpStatus.INTERNAL_SERVER_ERROR, "500_2", "유효하지 않은 TradeMode 입니다."),
    GOOGLE_CHAT_CONNECT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500_3", "Google Chat 연동 에러"),
}
