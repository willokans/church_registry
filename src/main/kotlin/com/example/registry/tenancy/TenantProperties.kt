package com.example.registry.tenancy

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.tenant")
data class TenantProperties(
    val resolutionMode: String = "header",
    val headerName: String = "X-Tenant"
)

