package com.example.registry.service

import com.example.registry.domain.entity.AuditLog
import com.example.registry.repo.AuditLogRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.util.*

@Service
class AuditService(
    private val auditLogRepository: AuditLogRepository,
    private val objectMapper: ObjectMapper,
    @Value("\${app.audit.hash-chain-enabled:true}") private val hashChainEnabled: Boolean
) {
    
    @Transactional
    fun log(
        tenantId: UUID?,
        actorId: UUID?,
        action: String,
        entity: String,
        entityId: UUID?,
        before: Any?,
        after: Any?
    ) {
        val beforeJson = before?.let { objectMapper.convertValue(it, Map::class.java) as? Map<String, Any> }
        val afterJson = after?.let { objectMapper.convertValue(it, Map::class.java) as? Map<String, Any> }
        
        val prevHash = if (hashChainEnabled) {
            getLastHash(tenantId)
        } else null
        
        val content = buildString {
            append(action)
            append(entity)
            entityId?.let { append(it) }
            beforeJson?.let { append(objectMapper.writeValueAsString(it)) }
            afterJson?.let { append(objectMapper.writeValueAsString(it)) }
            prevHash?.let { append(it) }
        }
        
        val hash = if (hashChainEnabled) {
            hash(content)
        } else null
        
        val auditLog = AuditLog(
            tenantId = tenantId,
            actorId = actorId,
            action = action,
            entity = entity,
            entityId = entityId,
            before = beforeJson,
            after = afterJson,
            hash = hash,
            prevHash = prevHash
        )
        
        auditLogRepository.save(auditLog)
    }
    
    private fun getLastHash(tenantId: UUID?): String? {
        val lastLog = if (tenantId != null) {
            auditLogRepository.findAllByFilters(
                tenantId = tenantId,
                actorId = null,
                entity = null,
                entityId = null,
                fromTs = null,
                toTs = null,
                cursor = null,
                org.springframework.data.domain.PageRequest.of(0, 1, org.springframework.data.domain.Sort.by("id").descending())
            ).content.firstOrNull()
        } else {
            null
        }
        return lastLog?.hash
    }
    
    private fun hash(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

