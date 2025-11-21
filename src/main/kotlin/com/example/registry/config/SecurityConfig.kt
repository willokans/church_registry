package com.example.registry.config

import com.example.registry.security.JwtAuthenticationConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val corsProperties: CorsProperties,
    private val applicationContext: ApplicationContext
) {
    
    @Bean
    @org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
    fun securityFilterChain(
        http: HttpSecurity
    ): SecurityFilterChain {
        // Add H2 authentication filter FIRST, before any other configuration
        // This ensures it runs before BearerTokenAuthenticationFilter and CSRF
        try {
            val h2Filter = applicationContext.getBeanProvider(H2AuthenticationFilter::class.java).getIfAvailable()
            h2Filter?.let { filter ->
                val logger = org.slf4j.LoggerFactory.getLogger(SecurityConfig::class.java)
                logger.info("H2AuthenticationFilter found, adding to security filter chain")
                // Add filter before BearerTokenAuthenticationFilter to intercept email-based tokens
                // This ensures it runs before OAuth2ResourceServer tries to decode the token
                http.addFilterBefore(
                    filter,
                    org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter::class.java
                )
            } ?: run {
                org.slf4j.LoggerFactory.getLogger(SecurityConfig::class.java).debug("H2AuthenticationFilter not found - this is normal if not using 'h2' or 'test' profile")
            }
        } catch (e: Exception) {
            org.slf4j.LoggerFactory.getLogger(SecurityConfig::class.java).debug("H2AuthenticationFilter not available: ${e.message}")
        }
        
        // Disable CSRF completely
        // This removes CSRF filter from the chain for all HTTP methods (POST, PUT, DELETE, etc.)
        // Note: CSRF only applies to state-changing methods (POST, PUT, DELETE, PATCH), not GET
        // That's why GET works but POST doesn't - CSRF filter blocks POST requests
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    .requestMatchers("/api/public/**").permitAll()
                    .requestMatchers("/api/certificates/*/verify").permitAll()
                    .requestMatchers("/h2-console/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").authenticated()
                    .anyRequest().authenticated()
            }
            .headers { headers ->
                headers.frameOptions { it.disable() } // Allow H2 console frames
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                    // In H2 mode, allow any token format (handled by H2SecurityConfig mock decoder)
                }
            }
            .cors { it.configurationSource(corsConfigurationSource()) }
        
        return http.build()
    }
    
    @Bean
    fun jwtAuthenticationConverter(): Converter<Jwt, JwtAuthenticationToken> {
        return JwtAuthenticationConverter()
    }
    
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = corsProperties.allowedOrigins.split(",")
        configuration.allowedMethods = corsProperties.allowedMethods.split(",")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = corsProperties.allowCredentials
        configuration.maxAge = corsProperties.maxAge
        
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}

