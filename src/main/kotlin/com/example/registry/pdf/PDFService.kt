package com.example.registry.pdf

interface PDFService {
    fun generateFromHtml(html: String): ByteArray
    fun generateFromTemplate(templateName: String, data: Map<String, Any>): ByteArray
}

