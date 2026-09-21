package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val tagId: Long = 0L,
    val name: String,
    val colorHex: String = "#0F766E",
    val createdAt: Long = System.currentTimeMillis()
)
