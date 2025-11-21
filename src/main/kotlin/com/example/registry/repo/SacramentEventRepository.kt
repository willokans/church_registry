package com.example.registry.repo

import com.example.registry.domain.SacramentType
import com.example.registry.domain.Status
import com.example.registry.domain.entity.SacramentEvent
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface SacramentEventRepository : JpaRepository<SacramentEvent, Long> {
    fun findAllByTenantId(tenantId: Long, pageable: Pageable): Page<SacramentEvent>
    
    @Query("""
        SELECT e FROM SacramentEvent e 
        WHERE e.tenantId = :tenantId 
        AND (:type IS NULL OR e.type = :type)
        AND (:status IS NULL OR e.status = :status)
        AND (:fromDate IS NULL OR e.date >= :fromDate)
        AND (:toDate IS NULL OR e.date <= :toDate)
        AND (:cursor IS NULL OR e.id > :cursor)
        ORDER BY e.id ASC
    """)
    fun findAllByFilters(
        @Param("tenantId") tenantId: Long,
        @Param("type") type: SacramentType?,
        @Param("status") status: Status?,
        @Param("fromDate") fromDate: LocalDate?,
        @Param("toDate") toDate: LocalDate?,
        @Param("cursor") cursor: Long?,
        pageable: Pageable
    ): Page<SacramentEvent>
    
    fun findByTenantIdAndTypeAndBookNoAndPageNoAndEntryNoAndStatus(
        tenantId: Long,
        type: SacramentType,
        bookNo: Int,
        pageNo: Int,
        entryNo: Int,
        status: Status
    ): SacramentEvent?
}

