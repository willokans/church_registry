package com.example.registry.util

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*

@Component
@Order(0)
class CorrelationIdFilter : OncePerRequestFilter() {
    
    companion object {
        private const val CORRELATION_ID_HEADER = "X-Request-Id"
        private const val CORRELATION_ID_MDC_KEY = "correlationId"
        private const val TRACE_ID_MDC_KEY = "traceId"
        private const val SPAN_ID_MDC_KEY = "spanId"
    }
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            // Get or generate correlation ID
            var correlationId = request.getHeader(CORRELATION_ID_HEADER)
            if (correlationId.isNullOrBlank()) {
                correlationId = UUID.randomUUID().toString()
            }
            
            // Set in response header
            response.setHeader(CORRELATION_ID_HEADER, correlationId)
            
            // Set in MDC for logging
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId)
            
            // Extract trace/span from traceparent header (W3C Trace Context)
            val traceparent = request.getHeader("traceparent")
            if (!traceparent.isNullOrBlank()) {
                val parts = traceparent.split("-")
                if (parts.size >= 2) {
                    MDC.put(TRACE_ID_MDC_KEY, parts[1])
                    if (parts.size >= 3) {
                        MDC.put(SPAN_ID_MDC_KEY, parts[2])
                    }
                }
            }
            
            filterChain.doFilter(request, response)
        } finally {
            MDC.clear()
        }
    }
}

