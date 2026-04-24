package com.healthmine.testforge.config

import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.AbstractRequestLoggingFilter

@Configuration
class RequestLoggingConfig {

    @Bean
    fun logFilter(): CustomRequestLoggingFilter {
        val filter = CustomRequestLoggingFilter()
        filter.setIncludeQueryString(true)
        filter.setIncludePayload(true)
        filter.setMaxPayloadLength(10000)
        return filter
    }
}

class CustomRequestLoggingFilter : AbstractRequestLoggingFilter() {

    override fun beforeRequest(request: HttpServletRequest, message: String) {}

    override fun afterRequest(request: HttpServletRequest, message: String) {
        logger.info(maskSensitiveData(message))
    }

    private fun maskSensitiveData(payload: String): String {
        var masked = payload
        listOf("password", "token").forEach { field ->
            masked = masked.replace(Regex("(?<=\"$field\":\").*?(?=\")"), "***")
        }
        return masked
    }
}
