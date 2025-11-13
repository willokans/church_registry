package com.example.registry.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.cors")
data class CorsProperties(
    val allowedOrigins: String = "http://localhost:3000",
    val allowedMethods: String = "GET,POST,PUT,DELETE,OPTIONS,PATCH",
    val allowedHeaders: String = "*",
    val allowCredentials: Boolean = true,
    val maxAge: Long = 3600
)

