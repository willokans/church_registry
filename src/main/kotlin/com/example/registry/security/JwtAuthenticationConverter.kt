package com.example.registry.security

import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter

class JwtAuthenticationConverter(
    private val authoritiesConverter: JwtGrantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter()
) : Converter<Jwt, JwtAuthenticationToken> {
    
    override fun convert(jwt: Jwt): JwtAuthenticationToken {
        val authorities = authoritiesConverter.convert(jwt)?.toMutableList() ?: mutableListOf()
        
        // Extract roles from JWT claims and add as ROLE_* authorities
        // Check for "roles" claim (list or single value)
        val roles = when (val rolesClaim = jwt.claims["roles"]) {
            is List<*> -> rolesClaim.mapNotNull { it?.toString() }
            is String -> listOf(rolesClaim)
            else -> emptyList()
        }
        
        // Add ROLE_* authorities for Spring Security hasRole() checks
        roles.forEach { role ->
            val roleName = role.uppercase().replace("-", "_")
            authorities.add(SimpleGrantedAuthority("ROLE_$roleName"))
        }
        
        return JwtAuthenticationToken(jwt, authorities)
    }
}

