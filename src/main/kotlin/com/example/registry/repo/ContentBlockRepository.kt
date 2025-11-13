package com.example.registry.repo

import com.example.registry.domain.entity.ContentBlock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ContentBlockRepository : JpaRepository<ContentBlock, UUID> {
    fun findByTenantIdAndKey(tenantId: UUID, key: String): ContentBlock?
    fun findAllByTenantId(tenantId: UUID): List<ContentBlock>
}

