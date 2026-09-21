package com.example.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.DocumentEntity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * ExportHelper handles exporting and sharing documents between internal app storage
 * and the user's public device storage using standard Android Intent APIs:
 * - Intent.ACTION_CREATE_DOCUMENT (SAF) to save to Downloads / Documents / SD Card
 * - Intent.ACTION_SEND / ACTION_SEND_MULTIPLE with FileProvider to share externally
 */
object ExportHelper {

    private const val TAG = "ExportHelper"

    /**
     * Builds an Intent for Intent.ACTION_CREATE_DOCUMENT so the user can choose
     * a destination in their public directory (Downloads, Documents, Drive, etc.).
     */
    fun createSaveToPublicDirectoryIntent(document: DocumentEntity): Intent {
        val extension = when (document.fileType) {
            "PDF" -> "pdf"
            "WORD" -> "docx"
            "EXCEL" -> "xlsx"
            "PPTX" -> "pptx"
            "IMAGE" -> "jpg"
            "CSV" -> "csv"
            else -> "txt"
        }
        val safeFileName = if (document.title.endsWith(".$extension", ignoreCase = true)) {
            document.title
        } else {
            "${document.title}.$extension"
        }

        val resolvedMimeType = when (document.fileType) {
            "PDF" -> "application/pdf"
            "WORD" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "EXCEL" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "PPTX" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "IMAGE" -> "image/jpeg"
            "CSV" -> "text/csv"
            else -> "text/plain"
        }

        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = resolvedMimeType
            putExtra(Intent.EXTRA_TITLE, safeFileName)
        }
    }

    /**
     * Copies the content of an internal storage file to a user-selected public URI.
     */
    fun writeDocumentToPublicUri(context: Context, sourceFilePath: String, targetUri: Uri): Boolean {
        return try {
            val sourceFile = File(sourceFilePath)
            if (!sourceFile.exists() || !sourceFile.canRead()) {
                Log.e(TAG, "Source file does not exist or cannot be read: $sourceFilePath")
                return false
            }

            context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                FileInputStream(sourceFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error writing file to public URI: ${e.message}", e)
            false
        }
    }

    /**
     * Builds an Intent for Intent.ACTION_SEND using FileProvider to share a document
     * to external apps, file managers, messaging, or cloud storage.
     */
    fun createShareIntent(context: Context, document: DocumentEntity): Intent? {
        val file = File(document.filePath)
        if (!file.exists()) return null

        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)

        val mimeType = document.mimeType.ifBlank {
            when (document.fileType) {
                "PDF" -> "application/pdf"
                "IMAGE" -> "image/jpeg"
                "WORD" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "EXCEL" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                "PPTX" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                "CSV" -> "text/csv"
                else -> "text/plain"
            }
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, document.title)
            putExtra(Intent.EXTRA_TEXT, "مستند مشارك من تطبيق نشرة: ${document.title}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return Intent.createChooser(shareIntent, "مشاركة المستند / Share Document")
    }

    /**
     * Builds an Intent for sharing multiple documents simultaneously.
     */
    fun createBatchShareIntent(context: Context, documents: List<DocumentEntity>): Intent? {
        val validFiles = documents.map { File(it.filePath) }.filter { it.exists() }
        if (validFiles.isEmpty()) return null

        val authority = "${context.packageName}.fileprovider"
        val uris = ArrayList<Uri>()
        for (file in validFiles) {
            uris.add(FileProvider.getUriForFile(context, authority, file))
        }

        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return Intent.createChooser(shareIntent, "مشاركة المستندات المحددة / Share Selected Documents")
    }
}
