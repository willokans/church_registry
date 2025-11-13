package com.example.registry.storage

import java.net.URL
import java.util.*

interface StorageService {
    fun upload(key: String, content: ByteArray, contentType: String): String
    fun getSignedUrl(key: String, expirationMinutes: Int = 60): URL
    fun delete(key: String)
    fun exists(key: String): Boolean
}

