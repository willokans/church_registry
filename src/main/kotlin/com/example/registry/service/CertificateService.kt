package com.example.registry.service

import com.example.registry.domain.RevocationStatus
import com.example.registry.domain.entity.Certificate
import com.example.registry.repo.CertificateRepository
import com.github.f4b6a3.ulid.UlidCreator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class CertificateService(
    private val certificateRepository: CertificateRepository
) {
    
    fun findBySerialNo(serialNo: String): Certificate? {
        return certificateRepository.findBySerialNo(serialNo)
    }
    
    fun findAllByEventId(eventId: Long): List<Certificate> {
        return certificateRepository.findAllByEventId(eventId)
    }
    
    @Transactional
    fun issue(eventId: Long, issuerId: Long): Certificate {
        val serialNo = UlidCreator.getUlid().toString()
        
        val certificate = Certificate(
            eventId = eventId,
            serialNo = serialNo,
            issuerId = issuerId
        )
        
        return certificateRepository.save(certificate)
    }
    
    @Transactional
    fun revoke(serialNo: String) {
        val certificate = certificateRepository.findBySerialNo(serialNo)
            ?: throw IllegalArgumentException("Certificate not found")
        
        val updated = certificate.copy(revocationStatus = RevocationStatus.REVOKED)
        certificateRepository.save(updated)
    }
}

