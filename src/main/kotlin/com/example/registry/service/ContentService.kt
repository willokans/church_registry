package com.example.registry.service

import com.example.registry.domain.entity.ContentBlock
import com.example.registry.repo.ContentBlockRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class ContentService(
    private val contentBlockRepository: ContentBlockRepository,
    private val auditService: AuditService
) {
    
    fun findByTenantAndKey(tenantId: UUID, key: String): ContentBlock? {
        return contentBlockRepository.findByTenantIdAndKey(tenantId, key)
    }
    
    fun findAllByTenant(tenantId: UUID): List<ContentBlock> {
        return contentBlockRepository.findAllByTenantId(tenantId)
    }
    
    @Transactional
    fun publish(tenantId: UUID, key: String, content: Map<String, Any>, updatedBy: UUID?): ContentBlock {
        val existing = contentBlockRepository.findByTenantIdAndKey(tenantId, key)
        
        val block = if (existing != null) {
            existing.copy(
                published = content,
                updatedAt = Instant.now()
            )
        } else {
            ContentBlock(
                tenantId = tenantId,
                key = key,
                published = content,
                draft = null
            )
        }
        
        val saved = contentBlockRepository.save(block)
        auditService.log(tenantId, updatedBy, "PUBLISH", "ContentBlock", saved.id, existing, saved)
        return saved
    }
    
    @Transactional
    fun saveDraft(tenantId: UUID, key: String, content: Map<String, Any>, updatedBy: UUID?): ContentBlock {
        val existing = contentBlockRepository.findByTenantIdAndKey(tenantId, key)
        
        val block = if (existing != null) {
            existing.copy(
                draft = content,
                updatedAt = Instant.now()
            )
        } else {
            ContentBlock(
                tenantId = tenantId,
                key = key,
                draft = content,
                published = null
            )
        }
        
        val saved = contentBlockRepository.save(block)
        auditService.log(tenantId, updatedBy, "SAVE_DRAFT", "ContentBlock", saved.id, existing, saved)
        return saved
    }
}

