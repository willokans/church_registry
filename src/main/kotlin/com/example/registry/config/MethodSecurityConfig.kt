package com.example.registry.config

import com.example.registry.security.AuthorizationService
import com.example.registry.tenancy.TenantContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

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
                    val perm = permission.toString()
                    
                    // For tenant management permissions, check if user has permission in ANY tenant
                    // if tenant context is not available
                    if ((perm == "tenants.manage" || perm == "tenants.view") && targetDomainObject == null) {
                        val tenantIdFromContext = TenantContext.get()
                        if (tenantIdFromContext != null) {
                            return authorizationService.can(tenantIdFromContext, perm, authentication)
                        }
                        // If no tenant context, check if user has permission in any of their tenants
                        return authorizationService.hasPermissionInAnyTenant(perm, authentication)
                    }
                    
                    val tenantId = when (targetDomainObject) {
                        is Long -> targetDomainObject
                        is String -> try { targetDomainObject.toLong() } catch (e: Exception) { return false }
                        else -> TenantContext.get() ?: return false
                    }
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

