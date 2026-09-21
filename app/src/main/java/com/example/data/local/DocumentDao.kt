package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentTagCrossRef
import com.example.data.model.DocumentWithTags
import com.example.data.model.HighlightEntity
import com.example.data.model.OcrEntity
import com.example.data.model.SignatureEntity
import com.example.data.model.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    // --- Documents ---
    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents ORDER BY lastOpenedAt DESC LIMIT :limit")
    fun getRecentDocuments(limit: Int = 10): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): DocumentEntity?

    @Query("SELECT * FROM documents WHERE id = :id")
    fun getDocumentByIdFlow(id: Long): Flow<DocumentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)

    @Query("UPDATE documents SET lastReadPage = :page, readingProgress = :progress, lastOpenedAt = :timestamp WHERE id = :id")
    suspend fun updateReadingProgress(id: Long, page: Int, progress: Float, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFav: Boolean)

    @Query("UPDATE documents SET title = :newTitle WHERE id = :id")
    suspend fun renameDocument(id: Long, newTitle: String)

    // --- Tags (Many-to-Many) ---
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("DELETE FROM tags WHERE tagId = :tagId")
    suspend fun deleteTag(tagId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDocumentTagCrossRef(crossRef: DocumentTagCrossRef)

    @Delete
    suspend fun deleteDocumentTagCrossRef(crossRef: DocumentTagCrossRef)

    @Query("DELETE FROM document_tag_cross_ref WHERE documentId = :documentId AND tagId = :tagId")
    suspend fun removeTagFromDocument(documentId: Long, tagId: Long)

    @Query("DELETE FROM document_tag_cross_ref WHERE documentId = :documentId")
    suspend fun clearTagsForDocument(documentId: Long)

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN document_tag_cross_ref xref ON t.tagId = xref.tagId
        WHERE xref.documentId = :documentId
        ORDER BY t.name ASC
    """)
    fun getTagsForDocument(documentId: Long): Flow<List<TagEntity>>

    @Transaction
    @Query("SELECT * FROM documents WHERE id = :id")
    fun getDocumentWithTags(id: Long): Flow<DocumentWithTags?>

    @Transaction
    @Query("SELECT * FROM documents ORDER BY lastOpenedAt DESC")
    fun getAllDocumentsWithTags(): Flow<List<DocumentWithTags>>

    @Transaction
    @Query("""
        SELECT DISTINCT d.* FROM documents d
        LEFT JOIN document_tag_cross_ref xref ON d.id = xref.documentId
        LEFT JOIN tags t ON xref.tagId = t.tagId
        WHERE (:query = '' OR d.title LIKE '%' || :query || '%' OR d.originalFileName LIKE '%' || :query || '%' OR t.name LIKE '%' || :query || '%')
        AND (:fileType = 'ALL' OR d.fileType = :fileType)
        AND (:tagId = 0 OR t.tagId = :tagId)
        ORDER BY d.lastOpenedAt DESC
    """)
    fun searchDocumentsWithTags(query: String, fileType: String, tagId: Long): Flow<List<DocumentWithTags>>

    // --- Highlights ---
    @Query("SELECT * FROM highlights WHERE docId = :docId ORDER BY pageIndex ASC, createdAt ASC")
    fun getHighlightsForDoc(docId: Long): Flow<List<HighlightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: HighlightEntity): Long

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteHighlight(id: Long)

    @Query("DELETE FROM highlights WHERE docId = :docId")
    suspend fun deleteHighlightsByDocId(docId: Long)

    // --- Signatures ---
    @Query("SELECT * FROM signatures WHERE docId = :docId ORDER BY pageIndex ASC, createdAt ASC")
    fun getSignaturesForDoc(docId: Long): Flow<List<SignatureEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignature(signature: SignatureEntity): Long

    @Query("DELETE FROM signatures WHERE id = :id")
    suspend fun deleteSignature(id: Long)

    @Query("DELETE FROM signatures WHERE docId = :docId")
    suspend fun deleteSignaturesByDocId(docId: Long)

    // --- OCR Records ---
    @Query("SELECT * FROM ocr_records ORDER BY createdAt DESC")
    fun getAllOcrRecords(): Flow<List<OcrEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOcr(ocr: OcrEntity): Long

    @Query("DELETE FROM ocr_records WHERE id = :id")
    suspend fun deleteOcr(id: Long)
}
