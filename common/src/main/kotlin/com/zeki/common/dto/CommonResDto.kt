package com.zeki.common.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.zeki.common.exception.ResponseCode
import java.io.Serializable

data class CommonResDto<T>(
    val code: String,
    val message: String,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    var data: T? = null
) : Serializable {

    companion object {
        // 리턴할 데이터가 있을 때 성공 응답
        fun <T> success(data: T? = null): CommonResDto<T> =
            CommonResDto(ResponseCode.OK.code, ResponseCode.OK.defaultMessage, data)

        // 리턴할 데이터가 없을 때 성공 응답
        fun <T> success(): CommonResDto<T> = success(null)

        // 리턴할 데이터가 있을 때 에러 응답
        fun <T> error(code: String, message: String, data: T? = null): CommonResDto<T> =
            CommonResDto(code, message, data)

        // 리턴할 데이터가 없을 때 에러 응답
        fun <T> error(code: String, message: String): CommonResDto<T> = error(code, message, null)
    }
}
