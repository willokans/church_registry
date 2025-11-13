package com.example.registry.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["app.openapi.enabled"], havingValue = "true", matchIfMissing = true)
class OpenAPIConfig {
    
    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Church Registry API")
                    .version("1.0.0")
                    .description("Multi-tenant Catholic sacramental registry backend API")
            )
            .addSecurityItem(SecurityRequirement().addList("bearer-jwt"))
            .components(
                io.swagger.v3.oas.models.Components()
                    .addSecuritySchemes(
                        "bearer-jwt",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .`in`(SecurityScheme.In.HEADER)
                            .name("Authorization")
                    )
            )
    }
}

