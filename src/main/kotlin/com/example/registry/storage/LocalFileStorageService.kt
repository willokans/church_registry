package com.example.registry.storage

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.concurrent.TimeUnit

@Service
@ConditionalOnProperty(name = ["app.storage.type"], havingValue = "local", matchIfMissing = false)
class LocalFileStorageService(
    @Value("\${app.storage.local.base-path:./storage}") private val basePath: String,
    @Value("\${app.storage.local.base-url:http://localhost:8080/storage}") private val baseUrl: String
) : StorageService {
    
    private val storageDir: Path = Paths.get(basePath).toAbsolutePath().normalize()
    
    init {
        Files.createDirectories(storageDir)
    }
    
    override fun upload(key: String, content: ByteArray, contentType: String): String {
        val filePath = storageDir.resolve(key)
        Files.createDirectories(filePath.parent)
        Files.write(filePath, content)
        return key
    }
    
    override fun getSignedUrl(key: String, expirationMinutes: Int): URL {
        // For local storage, return a simple URL (no signing needed in dev)
        // In production, you might want to generate time-limited tokens
        return URL("$baseUrl/$key")
    }
    
    override fun delete(key: String) {
        val filePath = storageDir.resolve(key)
        Files.deleteIfExists(filePath)
    }
    
    override fun exists(key: String): Boolean {
        val filePath = storageDir.resolve(key)
        return Files.exists(filePath)
    }
}

