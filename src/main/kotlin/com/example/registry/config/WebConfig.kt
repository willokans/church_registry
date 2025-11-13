package com.example.registry.config

import com.example.registry.tenancy.TenantFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WebConfig {
    
    @Bean
    fun tenantFilterRegistration(tenantFilter: TenantFilter): FilterRegistrationBean<TenantFilter> {
        val registration = FilterRegistrationBean(tenantFilter)
        registration.order = 1
        return registration
    }
}

