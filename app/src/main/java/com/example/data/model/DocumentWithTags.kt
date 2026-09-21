package com.example.data.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class DocumentWithTags(
    @Embedded
    val document: DocumentEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "tagId",
        associateBy = Junction(
            value = DocumentTagCrossRef::class,
            parentColumn = "documentId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity> = emptyList()
)
