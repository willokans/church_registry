package com.example.registry.web.controller

import com.example.registry.domain.RevocationStatus
import com.example.registry.service.CertificateService
import com.example.registry.web.dto.CertificateVerificationDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/certificates")
class CertificateController(
    private val certificateService: CertificateService
) {
    
    @GetMapping("/{serial}/verify")
    fun verifyCertificate(@PathVariable serial: String): ResponseEntity<CertificateVerificationDto> {
        val certificate = certificateService.findBySerialNo(serial)
        
        if (certificate == null) {
            return ResponseEntity.ok(
                CertificateVerificationDto(
                    serialNo = serial,
                    valid = false,
                    eventId = null,
                    issuedAt = null,
                    revocationStatus = null
                )
            )
        }
        
        val valid = certificate.revocationStatus == RevocationStatus.ACTIVE
        
        val dto = CertificateVerificationDto(
            serialNo = certificate.serialNo,
            valid = valid,
            eventId = certificate.eventId,
            issuedAt = certificate.issuedAt,
            revocationStatus = certificate.revocationStatus.name
        )
        
        return ResponseEntity.ok(dto)
    }
}

