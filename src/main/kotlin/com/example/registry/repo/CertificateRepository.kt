package com.example.registry.repo

import com.example.registry.domain.entity.Certificate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CertificateRepository : JpaRepository<Certificate, UUID> {
    fun findBySerialNo(serialNo: String): Certificate?
    fun findAllByEventId(eventId: UUID): List<Certificate>
}

