package com.example.registry.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import java.time.Instant

/**
 * Mock JWT decoder for H2/local development and tests when OAuth2 issuer is not available
 */
@Configuration
@Profile("h2", "test")
class H2SecurityConfig {
    
    @Bean
    @ConditionalOnMissingBean
    fun jwtDecoder(): JwtDecoder {
        return object : JwtDecoder {
            override fun decode(token: String): Jwt {
                // For H2/local dev, accept ANY token string and create a mock JWT
                // This includes email addresses, which Spring Security would normally reject
                // In production, this should never be used
                
                // Extract email from token if present (format: "email@example.com" or "user-email@example.com")
                val emailPattern = Regex("""([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})""")
                val emailMatch = emailPattern.find(token)
                val email = emailMatch?.value ?: "dev@example.com"
                
                // Extract role from token if present
                val role = when {
                    token.contains("super-admin", ignoreCase = true) || 
                    token.contains("SUPER_ADMIN", ignoreCase = true) -> "SUPER_ADMIN"
                    token.contains("parish-admin", ignoreCase = true) -> "PARISH_ADMIN"
                    token.contains("registrar", ignoreCase = true) -> "REGISTRAR"
                    token.contains("priest", ignoreCase = true) -> "PRIEST"
                    token.contains("viewer", ignoreCase = true) -> "VIEWER"
                    token.contains("admin", ignoreCase = true) -> "PARISH_ADMIN"
                    else -> "SUPER_ADMIN" // Default to SUPER_ADMIN for H2 dev
                }
                
                // Always return a valid JWT, regardless of input token format
                return Jwt.withTokenValue(token)
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
            }
        }
    }
}

