package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ocr_records")
data class OcrEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val docId: Long = 0L,
    val title: String,
    val extractedText: String,
    val language: String = "ar_en",
    val imagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
