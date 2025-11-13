package com.example.registry.pdf

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

@Service
@ConditionalOnMissingBean(PDFService::class)
class StubPDFService : PDFService {
    override fun generateFromHtml(html: String): ByteArray {
        // Stub implementation - returns empty PDF
        // In production, replace with actual PDF generation (Playwright, OpenPDF, etc.)
        return "PDF_STUB".toByteArray()
    }
    
    override fun generateFromTemplate(templateName: String, data: Map<String, Any>): ByteArray {
        return generateFromHtml("<html><body>Template: $templateName</body></html>")
    }
}

