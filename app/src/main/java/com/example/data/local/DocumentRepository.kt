package com.example.data.local

import com.example.data.model.DocumentEntity
import com.example.data.model.HighlightEntity
import com.example.data.model.OcrEntity
import com.example.data.model.SignatureEntity
import kotlinx.coroutines.flow.Flow
import java.io.File

class DocumentRepository(private val dao: DocumentDao) {

    val allDocuments: Flow<List<DocumentEntity>> = dao.getAllDocuments()
    val recentDocuments: Flow<List<DocumentEntity>> = dao.getRecentDocuments()
    val favoriteDocuments: Flow<List<DocumentEntity>> = dao.getFavoriteDocuments()
    val allOcrRecords: Flow<List<OcrEntity>> = dao.getAllOcrRecords()

    suspend fun getDocumentById(id: Long): DocumentEntity? = dao.getDocumentById(id)

    fun getDocumentFlow(id: Long): Flow<DocumentEntity?> = dao.getDocumentByIdFlow(id)

    suspend fun insertDocument(doc: DocumentEntity): Long = dao.insertDocument(doc)

    suspend fun updateDocument(doc: DocumentEntity) = dao.updateDocument(doc)

    suspend fun deleteDocument(doc: DocumentEntity) {
        try {
            val file = File(doc.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
        dao.deleteHighlightsByDocId(doc.id)
        dao.deleteSignaturesByDocId(doc.id)
        dao.deleteDocument(doc)
    }

    suspend fun deleteDocumentById(id: Long) {
        val doc = dao.getDocumentById(id)
        if (doc != null) {
            deleteDocument(doc)
        } else {
            dao.deleteDocumentById(id)
        }
    }

    suspend fun updateReadingProgress(id: Long, page: Int, progress: Float) {
        dao.updateReadingProgress(id, page, progress)
    }

    suspend fun updateProgress(id: Long, page: Int, progress: Float) {
        dao.updateReadingProgress(id, page, progress)
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        dao.updateFavorite(id, isFavorite)
    }

    suspend fun renameDocument(id: Long, newTitle: String) {
        dao.renameDocument(id, newTitle)
    }

    // --- Highlights ---
    fun getHighlights(docId: Long): Flow<List<HighlightEntity>> = dao.getHighlightsForDoc(docId)

    suspend fun addHighlight(highlight: HighlightEntity): Long = dao.insertHighlight(highlight)

    suspend fun deleteHighlight(id: Long) = dao.deleteHighlight(id)

    // --- Signatures ---
    fun getSignatures(docId: Long): Flow<List<SignatureEntity>> = dao.getSignaturesForDoc(docId)

    suspend fun addSignature(signature: SignatureEntity): Long = dao.insertSignature(signature)

    suspend fun deleteSignature(id: Long) = dao.deleteSignature(id)

    // --- OCR ---
    suspend fun addOcr(ocr: OcrEntity): Long = dao.insertOcr(ocr)

    suspend fun deleteOcr(id: Long) = dao.deleteOcr(id)

    // --- Tags (Many-to-Many) ---
    val allTags: Flow<List<com.example.data.model.TagEntity>> = dao.getAllTags()
    val allDocumentsWithTags: Flow<List<com.example.data.model.DocumentWithTags>> = dao.getAllDocumentsWithTags()

    suspend fun createTag(name: String, colorHex: String = "#0F766E"): Long {
        return dao.insertTag(com.example.data.model.TagEntity(name = name, colorHex = colorHex))
    }

    suspend fun deleteTag(tagId: Long) = dao.deleteTag(tagId)

    suspend fun addTagToDocument(documentId: Long, tagId: Long) {
        dao.insertDocumentTagCrossRef(com.example.data.model.DocumentTagCrossRef(documentId = documentId, tagId = tagId))
    }

    suspend fun removeTagFromDocument(documentId: Long, tagId: Long) {
        dao.removeTagFromDocument(documentId, tagId)
    }

    fun getTagsForDocument(documentId: Long): Flow<List<com.example.data.model.TagEntity>> {
        return dao.getTagsForDocument(documentId)
    }

    fun getDocumentWithTags(docId: Long): Flow<com.example.data.model.DocumentWithTags?> {
        return dao.getDocumentWithTags(docId)
    }

    fun searchDocuments(query: String, fileType: String, tagId: Long): Flow<List<com.example.data.model.DocumentWithTags>> {
        return dao.searchDocumentsWithTags(query, fileType, tagId)
    }
}
