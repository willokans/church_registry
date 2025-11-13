package com.example.registry.config

import com.example.registry.security.JwtAuthenticationConverter
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
    private val corsProperties: CorsProperties
) {
    
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    .requestMatchers("/public/**").permitAll()
                    .requestMatchers("/certificates/**/verify").permitAll()
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

