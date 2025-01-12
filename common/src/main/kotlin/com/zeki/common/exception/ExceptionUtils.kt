package com.zeki.common.exception

import mu.KotlinLogging

object ExceptionUtils {
    val log = KotlinLogging.logger {}
    private val prefixPackageName = this.javaClass.packageName.substring(0, this.javaClass.packageName.lastIndexOf("."))

    /**
     * StackTrace 에서 패키지 내 에러 첫번째 위치를 출력 하는 함수
     */
    private fun loggingStackTraceLocation(e: Exception): StackTraceElement {
        var projectStackTraceElement: StackTraceElement? = null
        for (element in e.stackTrace) {
            if (element.className.startsWith(prefixPackageName)) {
                projectStackTraceElement = element
                break
            }
        }
        return projectStackTraceElement ?: e.stackTrace[0]
    }

    fun logWarn(e: Exception) {
        log.warn(e.message + " | EndPoint : " + loggingStackTraceLocation(e))
    }

    fun logError(e: Exception) {
        log.error(e.message + " | EndPoint : " + loggingStackTraceLocation(e))
    }

}