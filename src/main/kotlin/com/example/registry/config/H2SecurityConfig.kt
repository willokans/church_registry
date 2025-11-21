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
                // For H2/local dev, accept any token and create a mock JWT
                // In production, this should never be used
                return Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("sub", "dev-user-id")
                    .claim("aud", "registry-api")
                    .claim("iss", "dev-issuer")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build()
            }
        }
    }
}

