package com.example.registry.config

import com.example.registry.security.AuthorizationService
import com.example.registry.tenancy.TenantContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

@Configuration
@EnableMethodSecurity
class MethodSecurityConfig(
    private val authorizationService: AuthorizationService
) {
    
    @Bean
    fun tenantContext(): TenantContext {
        return TenantContext
    }
    
    @Bean
    fun methodSecurityExpressionHandler(): MethodSecurityExpressionHandler {
        val handler = DefaultMethodSecurityExpressionHandler()
        handler.setPermissionEvaluator(
            object : org.springframework.security.access.PermissionEvaluator {
                override fun hasPermission(
                    authentication: org.springframework.security.core.Authentication?,
                    targetDomainObject: Any?,
                    permission: Any
                ): Boolean {
                    val tenantId = when (targetDomainObject) {
                        is java.util.UUID -> targetDomainObject
                        is String -> try { java.util.UUID.fromString(targetDomainObject) } catch (e: Exception) { return false }
                        else -> TenantContext.get() ?: return false
                    }
                    val perm = permission.toString()
                    return authorizationService.can(tenantId, perm, authentication)
                }
                
                override fun hasPermission(
                    authentication: org.springframework.security.core.Authentication?,
                    targetId: java.io.Serializable?,
                    targetType: String?,
                    permission: Any?
                ): Boolean = false
            }
        )
        return handler
    }
}

