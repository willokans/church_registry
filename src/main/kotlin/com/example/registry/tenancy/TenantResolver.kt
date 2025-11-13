package com.example.registry.tenancy

import com.example.registry.repo.TenantRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component
import java.util.*

@Component
class TenantResolver(
    private val tenantRepository: TenantRepository,
    private val tenantProperties: TenantProperties
) {
    fun resolve(request: HttpServletRequest): UUID? {
        val slug = when (tenantProperties.resolutionMode) {
            "hostname" -> extractSlugFromHost(request.getHeader("Host") ?: "")
            "header" -> request.getHeader(tenantProperties.headerName)
            else -> null
        }
        
        return slug?.let { tenantRepository.findBySlug(it)?.id }
    }
    
    private fun extractSlugFromHost(host: String): String? {
        // Extract subdomain or first part before domain
        val parts = host.split(".")
        return if (parts.size > 2) parts[0] else null
    }
}

