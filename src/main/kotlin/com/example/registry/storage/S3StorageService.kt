package com.example.registry.storage

import io.minio.*
import io.minio.http.Method
import io.minio.errors.ErrorResponseException
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.net.URL
import java.util.concurrent.TimeUnit

@Service
@ConditionalOnProperty(name = ["app.storage.type"], havingValue = "s3", matchIfMissing = true)
class S3StorageService(
    @Value("\${app.storage.s3.endpoint}") private val endpoint: String,
    @Value("\${app.storage.s3.bucket}") private val bucket: String,
    @Value("\${app.storage.s3.access-key}") private val accessKey: String,
    @Value("\${app.storage.s3.secret-key}") private val secretKey: String,
    @Value("\${app.storage.s3.region:us-east-1}") private val region: String
) : StorageService {
    
    private val minioClient: MinioClient = MinioClient.builder()
        .endpoint(endpoint)
        .credentials(accessKey, secretKey)
        .region(region)
        .build()
    
    init {
        ensureBucketExists()
    }
    
    override fun upload(key: String, content: ByteArray, contentType: String): String {
        val args = PutObjectArgs.builder()
            .bucket(bucket)
            .`object`(key)
            .stream(ByteArrayInputStream(content), content.size.toLong(), -1)
            .contentType(contentType)
            .build()
        
        minioClient.putObject(args)
        
        return key
    }
    
    override fun getSignedUrl(key: String, expirationMinutes: Int): URL {
        val args = GetPresignedObjectUrlArgs.builder()
            .method(Method.GET)
            .bucket(bucket)
            .`object`(key)
            .expiry(expirationMinutes, TimeUnit.MINUTES)
            .build()
        
        return URL(minioClient.getPresignedObjectUrl(args))
    }
    
    override fun delete(key: String) {
        try {
            val args = RemoveObjectArgs.builder()
                .bucket(bucket)
                .`object`(key)
                .build()
            minioClient.removeObject(args)
        } catch (e: ErrorResponseException) {
            if (e.errorResponse().code() != "NoSuchKey") {
                throw e
            }
        }
    }
    
    override fun exists(key: String): Boolean {
        return try {
            val args = StatObjectArgs.builder()
                .bucket(bucket)
                .`object`(key)
                .build()
            minioClient.statObject(args)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun ensureBucketExists() {
        try {
            val bucketExistsArgs = BucketExistsArgs.builder()
                .bucket(bucket)
                .build()
            if (!minioClient.bucketExists(bucketExistsArgs)) {
                val makeBucketArgs = MakeBucketArgs.builder()
                    .bucket(bucket)
                    .build()
                minioClient.makeBucket(makeBucketArgs)
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to ensure bucket exists: ${e.message}", e)
        }
    }
}

