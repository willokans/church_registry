package com.example.registry

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class BaseIntegrationTest {
    
    companion object {
        // Check if Docker is available
        private val dockerAvailable: Boolean = try {
            val process = ProcessBuilder("docker", "ps").start()
            process.waitFor()
            process.exitValue() == 0
        } catch (e: Exception) {
            false
        }
        
        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            if (dockerAvailable) {
                // Try to use Testcontainers PostgreSQL if Docker is available
                try {
                    val postgres = org.testcontainers.containers.PostgreSQLContainer("postgres:15-alpine")
                        .withDatabaseName("registry_test")
                        .withUsername("test")
                        .withPassword("test")
                    postgres.start()
                    registry.add("spring.datasource.url") { postgres.jdbcUrl }
                    registry.add("spring.datasource.username") { postgres.username }
                    registry.add("spring.datasource.password") { postgres.password }
                } catch (e: Exception) {
                    // Fallback to H2 if Testcontainers fails
                    configureH2(registry)
                }
            } else {
                // Use H2 when Docker is not available
                configureH2(registry)
            }
        }
        
        private fun configureH2(registry: DynamicPropertyRegistry) {
            // Use a custom H2 URL that supports PostgreSQL compatibility mode
            // This helps with TIMESTAMP(0) -> TIMESTAMP conversion
            registry.add("spring.datasource.url") { "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE" }
            registry.add("spring.datasource.username") { "sa" }
            registry.add("spring.datasource.password") { "" }
            registry.add("spring.datasource.driver-class-name") { "org.h2.Driver" }
            registry.add("spring.jpa.database-platform") { "com.example.registry.config.TestH2Dialect" }
            registry.add("spring.jpa.properties.hibernate.dialect") { "com.example.registry.config.TestH2Dialect" }
            registry.add("spring.jpa.properties.hibernate.globally_quoted_identifiers") { "true" }
            registry.add("spring.jpa.properties.hibernate.jdbc.use_get_generated_keys") { "true" }
            // Disable Hibernate DDL - we'll create tables manually via H2SchemaCreator
            registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
            registry.add("spring.liquibase.enabled") { "false" }
        }
    }
    
}
