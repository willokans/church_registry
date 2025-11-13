package com.example.registry.tenancy

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(1)
class TenantFilter(
    private val tenantResolver: TenantResolver
) : OncePerRequestFilter() {
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val tenantId = tenantResolver.resolve(request)
            TenantContext.set(tenantId)
            filterChain.doFilter(request, response)
        } finally {
            TenantContext.clear()
        }
    }
}

