package com.example.registry.service

import com.example.registry.domain.entity.IdempotencyKey
import com.example.registry.repo.IdempotencyKeyRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import java.util.*

@Service
class IdempotencyService(
    private val idempotencyKeyRepository: IdempotencyKeyRepository,
    @Value("\${app.idempotency.ttl-hours:24}") private val ttlHours: Long
) {
    
    fun checkAndStore(
        tenantId: UUID,
        key: String,
        requestBody: String?
    ): IdempotencyResult {
        val existing = idempotencyKeyRepository.findByTenantIdAndKey(tenantId, key)
        
        if (existing != null) {
            val age = java.time.Duration.between(existing.createdAt, Instant.now())
            if (age.toHours() < ttlHours) {
                return IdempotencyResult(
                    isDuplicate = true,
                    responseCode = existing.responseCode
                )
            } else {
                // Expired, delete and allow new request
                idempotencyKeyRepository.delete(existing)
            }
        }
        
        val requestHash = requestBody?.let { hash(it) }
        val newKey = IdempotencyKey(
            tenantId = tenantId,
            key = key,
            requestHash = requestHash
        )
        idempotencyKeyRepository.save(newKey)
        
        return IdempotencyResult(isDuplicate = false, responseCode = null)
    }
    
    @Transactional
    fun recordResponse(tenantId: UUID, key: String, responseCode: Int) {
        val existing = idempotencyKeyRepository.findByTenantIdAndKey(tenantId, key)
        if (existing != null) {
            val updated = existing.copy(responseCode = responseCode)
            idempotencyKeyRepository.save(updated)
        }
    }
    
    private fun hash(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(content.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    data class IdempotencyResult(
        val isDuplicate: Boolean,
        val responseCode: Int?
    )
}

