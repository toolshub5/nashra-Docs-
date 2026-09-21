package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val originalFileName: String,
    val filePath: String,
    val fileType: String, // PDF, DOCX, XLSX, PPTX, TXT, IMAGE, CSV
    val mimeType: String,
    val fileSizeBytes: Long,
    val pageCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long = System.currentTimeMillis(),
    val lastReadPage: Int = 0,
    val readingProgress: Float = 0f,
    val isFavorite: Boolean = false,
    val isShared: Boolean = false,
    val isEncrypted: Boolean = false,
    val passwordHash: String? = null
)
