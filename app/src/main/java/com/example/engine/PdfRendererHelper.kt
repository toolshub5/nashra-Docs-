package com.example.engine

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android Native PDF Renderer Engine.
 * Provides high-performance, hardware-accelerated PDF page rendering,
 * LRU bitmap caching, multi-resolution zoom support, and thumbnail generation.
 */
class PdfRendererHelper(val file: File) {

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private val lock = Any()

    val pageCount: Int
        get() = synchronized(lock) {
            pdfRenderer?.pageCount ?: 0
        }

    val isValid: Boolean
        get() = synchronized(lock) {
            pdfRenderer != null && pageCount > 0
        }

    // Cache sized for high-res pages (up to 40MB of bitmap memory)
    private val bitmapCache = object : LruCache<String, Bitmap>(40 * 1024) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    init {
        open()
    }

    private fun open() {
        synchronized(lock) {
            try {
                if (file.exists() && file.length() > 0) {
                    fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    fileDescriptor?.let {
                        pdfRenderer = PdfRenderer(it)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open PDF file: ${file.absolutePath}", e)
            }
        }
    }

    /**
     * Gets page dimensions (width, height in PDF points) for layout aspect ratios.
     */
    fun getPageDimensions(pageIndex: Int): Pair<Int, Int>? = synchronized(lock) {
        try {
            val renderer = pdfRenderer ?: return null
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
            val page = renderer.openPage(pageIndex)
            val dims = Pair(page.width, page.height)
            page.close()
            dims
        } catch (e: Exception) {
            Log.e(TAG, "Error reading page dimensions: $pageIndex", e)
            null
        }
    }

    /**
     * Renders a PDF page to a Bitmap at the requested target width.
     * Scale factor can be increased for crystal-clear pinch-to-zoom.
     */
    suspend fun renderPage(pageIndex: Int, targetWidth: Int = 1200): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "${pageIndex}_${targetWidth}"
        val cached = bitmapCache.get(cacheKey)
        if (cached != null && !cached.isRecycled) {
            return@withContext cached
        }

        synchronized(lock) {
            try {
                val renderer = pdfRenderer ?: return@withContext null
                if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null

                val page = renderer.openPage(pageIndex)
                val width = targetWidth.coerceIn(400, 2400)
                val height = ((width.toFloat() / page.width) * page.height).toInt().coerceAtLeast(100)

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                bitmapCache.put(cacheKey, bitmap)
                bitmap
            } catch (e: Exception) {
                Log.e(TAG, "Error rendering page $pageIndex", e)
                null
            }
        }
    }

    /**
     * Generates a lightweight thumbnail for bottom carousel navigation.
     */
    suspend fun renderThumbnail(pageIndex: Int, thumbWidth: Int = 240): Bitmap? = withContext(Dispatchers.IO) {
        renderPage(pageIndex, thumbWidth)
    }

    /**
     * Closes the renderer and releases ParcelFileDescriptor.
     */
    fun close() {
        synchronized(lock) {
            try {
                pdfRenderer?.close()
                pdfRenderer = null
                fileDescriptor?.close()
                fileDescriptor = null
                bitmapCache.evictAll()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing PdfRenderer", e)
            }
        }
    }

    companion object {
        private const val TAG = "PdfRendererHelper"
    }
}
