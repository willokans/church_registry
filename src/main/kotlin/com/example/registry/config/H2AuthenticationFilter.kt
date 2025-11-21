package com.example.registry.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Instant

/**
 * Custom authentication filter for H2/local dev that accepts email-based tokens
 * This bypasses Spring Security's JWT format validation
 */
@Component
@Profile("h2", "test")
class H2AuthenticationFilter : OncePerRequestFilter() {
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        logger.info("H2AuthenticationFilter: Processing request to ${request.requestURI}")
        val authHeader = request.getHeader("Authorization")
        logger.info("H2AuthenticationFilter: Authorization header = ${if (authHeader != null) "present" else "null"}")
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7) // Remove "Bearer " prefix
            logger.info("H2AuthenticationFilter: Token = $token")
            
            // Only process if token looks like an email (contains @)
            // Otherwise, let OAuth2ResourceServer handle it
            if (token.contains("@")) {
                logger.info("H2AuthenticationFilter: Token contains @, processing...")
                try {
                    // Extract email from token
                    val emailPattern = Regex("""([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})""")
                    val emailMatch = emailPattern.find(token)
                    val email = emailMatch?.value ?: "dev@example.com"
                    
                    // Extract role from token
                    val role = when {
                        token.contains("super-admin", ignoreCase = true) || 
                        token.contains("SUPER_ADMIN", ignoreCase = true) -> "SUPER_ADMIN"
                        token.contains("parish-admin", ignoreCase = true) -> "PARISH_ADMIN"
                        token.contains("registrar", ignoreCase = true) -> "REGISTRAR"
                        token.contains("priest", ignoreCase = true) -> "PRIEST"
                        token.contains("viewer", ignoreCase = true) -> "VIEWER"
                        token.contains("admin", ignoreCase = true) -> "PARISH_ADMIN"
                        else -> "SUPER_ADMIN"
                    }
                    
                    // Create a mock JWT
                    val jwt = Jwt.withTokenValue(token)
                        .header("alg", "none")
                        .claim("sub", email)
                        .claim("aud", "registry-api")
                        .claim("iss", "dev-issuer")
                        .claim("email", email)
                        .claim("roles", listOf(role))
                        .claim("scope", "openid profile email")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .build()
                    
                    // Create authorities
                    val authorities = mutableListOf<org.springframework.security.core.GrantedAuthority>()
                    val roleName = role.uppercase().replace("-", "_")
                    authorities.add(SimpleGrantedAuthority("ROLE_$roleName"))
                    
                    // Create authentication token
                    val authentication = JwtAuthenticationToken(jwt, authorities)
                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                    
                    // Set authentication in security context
                    // Use SecurityContextHolder.setContext() to ensure it persists
                    val context = SecurityContextHolder.createEmptyContext()
                    context.authentication = authentication
                    SecurityContextHolder.setContext(context)
                    
                    logger.info("H2AuthenticationFilter: Set authentication for email=$email, role=$role, authorities=${authorities.map { it.authority }}")
                    
                    // Wrap request to remove Authorization header so BearerTokenAuthenticationFilter doesn't process it
                    val wrappedRequest = object : HttpServletRequestWrapper(request) {
                        override fun getHeader(name: String): String? {
                            return if (name.equals("Authorization", ignoreCase = true)) {
                                null // Remove Authorization header to prevent BearerTokenAuthenticationFilter from processing
                            } else {
                                super.getHeader(name)
                            }
                        }
                        
                        override fun getHeaders(name: String): java.util.Enumeration<String> {
                            return if (name.equals("Authorization", ignoreCase = true)) {
                                java.util.Collections.emptyEnumeration()
                            } else {
                                super.getHeaders(name)
                            }
                        }
                    }
                    
                    filterChain.doFilter(wrappedRequest, response)
                    return
                } catch (e: Exception) {
                    // If anything goes wrong, continue without authentication
                    logger.debug("Failed to process H2 token: ${e.message}", e)
                }
            }
        }
        
        filterChain.doFilter(request, response)
    }
}

