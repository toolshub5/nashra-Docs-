package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "highlights")
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val docId: Long,
    val pageIndex: Int,
    val selectedText: String,
    val colorHex: String = "#FDE047", // Yellow, Green (#86EFAC), Blue (#93C5FD), Pink (#F9A8D4)
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
