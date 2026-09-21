package com.example.data.local

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.DecimalFormat

/**
 * FileStorageManager handles document persistence, structured directory organization
 * by file type (PDF, Word, Excel, PPTX, Images, Texts), and cleanup of temporary files and caches.
 */
class FileStorageManager(private val context: Context) {

    private val baseDocumentsDir: File get() = File(context.filesDir, "documents").apply { if (!exists()) mkdirs() }
    private val pdfDir: File get() = File(baseDocumentsDir, "pdf").apply { if (!exists()) mkdirs() }
    private val wordDir: File get() = File(baseDocumentsDir, "word").apply { if (!exists()) mkdirs() }
    private val excelDir: File get() = File(baseDocumentsDir, "excel").apply { if (!exists()) mkdirs() }
    private val pptxDir: File get() = File(baseDocumentsDir, "pptx").apply { if (!exists()) mkdirs() }
    private val textDir: File get() = File(baseDocumentsDir, "txt").apply { if (!exists()) mkdirs() }
    private val imageDir: File get() = File(baseDocumentsDir, "images").apply { if (!exists()) mkdirs() }
    private val scansDir: File get() = File(context.filesDir, "scans").apply { if (!exists()) mkdirs() }
    private val exportsDir: File get() = File(context.filesDir, "exports").apply { if (!exists()) mkdirs() }
    private val tempDir: File get() = File(context.cacheDir, "temp_docs").apply { if (!exists()) mkdirs() }

    init {
        // Ensure all folders exist
        baseDocumentsDir
        pdfDir
        wordDir
        excelDir
        pptxDir
        textDir
        imageDir
        scansDir
        exportsDir
        tempDir
    }

    /**
     * Determines the appropriate directory for a given file type.
     */
    fun getDirectoryForType(fileType: String): File {
        return when (fileType.uppercase()) {
            "PDF" -> pdfDir
            "DOCX", "DOC", "WORD" -> wordDir
            "XLSX", "XLS", "CSV", "EXCEL" -> excelDir
            "PPTX", "PPT" -> pptxDir
            "TXT", "TEXT", "MD" -> textDir
            "IMAGE", "JPG", "JPEG", "PNG", "WEBP" -> imageDir
            else -> baseDocumentsDir
        }
    }

    /**
     * Saves a document from an InputStream into the organized directory structure.
     */
    fun saveDocument(originalName: String, fileType: String, inputStream: InputStream): File {
        val targetDir = getDirectoryForType(fileType)
        val sanitizedName = sanitizeFileName(originalName)
        var targetFile = File(targetDir, sanitizedName)

        // Prevent overwriting existing files with identical names
        if (targetFile.exists()) {
            val nameWithoutExt = sanitizedName.substringBeforeLast(".", sanitizedName)
            val ext = sanitizedName.substringAfterLast(".", "")
            val extSuffix = if (ext.isNotBlank()) ".$ext" else ""
            targetFile = File(targetDir, "${nameWithoutExt}_${System.currentTimeMillis()}$extSuffix")
        }

        targetFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }

        return targetFile
    }

    /**
     * Saves a document from byte array.
     */
    fun saveDocumentBytes(fileName: String, fileType: String, bytes: ByteArray): File {
        val targetDir = getDirectoryForType(fileType)
        val sanitizedName = sanitizeFileName(fileName)
        val targetFile = File(targetDir, sanitizedName)
        targetFile.writeBytes(bytes)
        return targetFile
    }

    /**
     * Creates a temporary file in the cache directory.
     */
    fun createTempFile(prefix: String, suffix: String): File {
        return File.createTempFile(prefix, suffix, tempDir)
    }

    /**
     * Creates a new export file in the exports directory.
     */
    fun createExportFile(title: String, extension: String): File {
        val sanitizedTitle = sanitizeFileName(title)
        val ext = if (extension.startsWith(".")) extension else ".$extension"
        return File(exportsDir, "${sanitizedTitle}_exported_${System.currentTimeMillis()}$ext")
    }

    /**
     * Deletes a document file safely from disk.
     */
    fun deleteDocumentFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete file: $filePath", e)
            false
        }
    }

    /**
     * Cleans up temporary and cache files older than a specified duration (default: 24 hours).
     * Returns the count of deleted files.
     */
    fun cleanupTemporaryFiles(maxAgeMs: Long = 24 * 60 * 60 * 1000L): Int {
        var deletedCount = 0
        val cutoffTime = System.currentTimeMillis() - maxAgeMs

        try {
            tempDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    if (file.delete()) deletedCount++
                }
            }

            File(context.cacheDir, "camera").listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    if (file.delete()) deletedCount++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up temporary files", e)
        }

        return deletedCount
    }

    /**
     * Clears all cache files and returns the total bytes freed.
     */
    fun clearAllCache(): Long {
        var freedBytes = 0L

        fun deleteRecursive(file: File) {
            if (file.isDirectory) {
                file.listFiles()?.forEach { deleteRecursive(it) }
            } else {
                freedBytes += file.length()
                file.delete()
            }
        }

        try {
            context.cacheDir.listFiles()?.forEach { deleteRecursive(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
        }

        return freedBytes
    }

    /**
     * Calculates total storage used by all documents and files in the app.
     */
    fun getTotalStorageUsageBytes(): Long {
        return calculateDirSize(baseDocumentsDir) +
                calculateDirSize(scansDir) +
                calculateDirSize(exportsDir) +
                calculateDirSize(context.cacheDir)
    }

    /**
     * Returns a breakdown of storage usage per category in bytes.
     */
    fun getStorageBreakdownBytes(): Map<String, Long> {
        return mapOf(
            "PDF" to calculateDirSize(pdfDir),
            "WORD" to calculateDirSize(wordDir),
            "EXCEL" to calculateDirSize(excelDir),
            "PPTX" to calculateDirSize(pptxDir),
            "TXT" to calculateDirSize(textDir),
            "IMAGES" to calculateDirSize(imageDir),
            "SCANS" to calculateDirSize(scansDir),
            "EXPORTS" to calculateDirSize(exportsDir),
            "CACHE" to calculateDirSize(context.cacheDir)
        )
    }

    private fun calculateDirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        var size = 0L
        dir.walkTopDown().forEach { file ->
            if (file.isFile) {
                size += file.length()
            }
        }
        return size
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim()
            .ifBlank { "document_${System.currentTimeMillis()}" }
    }

    companion object {
        private const val TAG = "FileStorageManager"

        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            val safeIndex = digitGroups.coerceIn(0, units.size - 1)
            val value = bytes / Math.pow(1024.0, safeIndex.toDouble())
            return "${DecimalFormat("#,##0.#").format(value)} ${units[safeIndex]}"
        }
    }
}
