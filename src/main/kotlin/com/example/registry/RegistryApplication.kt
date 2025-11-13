package com.example.registry

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableCaching
@EnableJpaAuditing
@EnableAsync
@ConfigurationPropertiesScan
@RegisterReflectionForBinding(
    com.example.registry.web.dto.ProblemDetail::class,
    com.example.registry.web.dto.ErrorCode::class
)
class RegistryApplication

fun main(args: Array<String>) {
    runApplication<RegistryApplication>(*args)
}

