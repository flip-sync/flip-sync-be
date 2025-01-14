package com.zeki.flipsyncserver.config.log

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import ch.qos.logback.core.LayoutBase
import com.fasterxml.jackson.databind.ObjectMapper
import com.zeki.flipsyncserver.config.log.GoogleChatWebhookDto
import io.netty.util.internal.logging.MessageFormatter
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class GoogleChatAppender() : AppenderBase<ILoggingEvent>() {
    private val webhookUri: String? = null
    private val layout: LayoutBase<ILoggingEvent?> = defaultLayout
    private val timeout: Int = 30000

    override fun append(evt: ILoggingEvent) {
        try {
            if (!webhookUri.isNullOrBlank()) {
                sendMessageWithWebhookUri(webhookUri, evt)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            addError("Error sending message to GoogleChat: $evt", e)
        }
    }

    @Throws(IOException::class)
    private fun sendMessageWithWebhookUri(webhookUri: String, evt: ILoggingEvent) {
        val text: String = layout.doLayout(evt)

        val webhookDto: GoogleChatWebhookDto = GoogleChatWebhookDto(text)
        val bytes: ByteArray = ObjectMapper().writeValueAsBytes(webhookDto)

        postMessage(webhookUri, "application/json", bytes)
    }

    @Throws(IOException::class)
    private fun postMessage(uri: String, contentType: String, bytes: ByteArray) {
        val conn: HttpURLConnection = URL(uri).openConnection() as HttpURLConnection
        conn.connectTimeout = timeout
        conn.readTimeout = timeout
        conn.doOutput = true
        conn.requestMethod = "POST"
        conn.setFixedLengthStreamingMode(bytes.size)
        conn.setRequestProperty("Content-Type", contentType)

        val os: OutputStream = conn.outputStream
        os.write(bytes)
        os.flush()
        os.close()
    }

    companion object {
        private val defaultLayout: LayoutBase<ILoggingEvent?>
            get() = object : LayoutBase<ILoggingEvent?>() {

                override fun doLayout(event: ILoggingEvent?): String {
                    return ("-- [" + event?.level + "]" +
                            event?.loggerName + " - " +
                            MessageFormatter.arrayFormat(event?.formattedMessage, event?.argumentArray).message
                                .replace("\n", "\n\t"))
                }
            }
    }
}