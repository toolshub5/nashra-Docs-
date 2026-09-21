package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "signatures")
data class SignatureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val docId: Long,
    val pageIndex: Int,
    val signerName: String,
    val signatureType: String, // DRAWN, TYPED
    val signatureData: String, // Path points or typed signature text
    val strokeColorHex: String = "#0F766E",
    val normX: Float = 0.5f,
    val normY: Float = 0.8f,
    val scale: Float = 1.0f,
    val createdAt: Long = System.currentTimeMillis()
)
