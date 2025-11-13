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
        // Add any custom authorities from JWT claims if needed
        return JwtAuthenticationToken(jwt, authorities)
    }
}

