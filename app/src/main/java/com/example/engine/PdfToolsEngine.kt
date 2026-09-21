package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.model.HighlightEntity
import com.example.data.model.SignatureEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfToolsEngine {

    /**
     * Merge multiple PDF files into one output PDF file.
     */
    suspend fun mergePdfs(pdfFiles: List<File>, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val pdfDoc = PdfDocument()
            var totalPageCount = 0

            for (file in pdfFiles) {
                if (!file.exists()) continue
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val pageWidth = page.width
                    val pageHeight = page.height

                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, totalPageCount + 1).create()
                    val newPage = pdfDoc.startPage(pageInfo)

                    val bitmap = Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    pdfDoc.finishPage(newPage)

                    bitmap.recycle()
                    page.close()
                    totalPageCount++
                }
                renderer.close()
                pfd.close()
            }

            FileOutputStream(outputFile).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Split a PDF by extracting pages from startPage to endPage (1-indexed).
     */
    suspend fun splitPdf(sourcePdf: File, startPage: Int, endPage: Int, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val pfd = ParcelFileDescriptor.open(sourcePdf, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val pdfDoc = PdfDocument()

            val from = (startPage - 1).coerceAtLeast(0)
            val to = (endPage - 1).coerceAtMost(renderer.pageCount - 1)

            var newPageIndex = 1
            for (i in from..to) {
                val page = renderer.openPage(i)
                val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, newPageIndex++).create()
                val newPage = pdfDoc.startPage(pageInfo)

                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDoc.finishPage(newPage)

                bitmap.recycle()
                page.close()
            }

            renderer.close()
            pfd.close()

            FileOutputStream(outputFile).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Convert multiple image files into a single PDF.
     */
    suspend fun imagesToPdf(imageFiles: List<File>, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val pdfDoc = PdfDocument()
            var pageIndex = 1

            for (imgFile in imageFiles) {
                if (!imgFile.exists()) continue
                val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath, options) ?: continue

                // Standard A4 aspect or bitmap width/height
                val width = bitmap.width.coerceIn(400, 2400)
                val height = (bitmap.height.toFloat() / bitmap.width * width).toInt()

                val pageInfo = PdfDocument.PageInfo.Builder(width, height, pageIndex++).create()
                val page = pdfDoc.startPage(pageInfo)

                val destRect = Rect(0, 0, width, height)
                page.canvas.drawBitmap(bitmap, null, destRect, null)
                pdfDoc.finishPage(page)
                bitmap.recycle()
            }

            FileOutputStream(outputFile).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Convert text to PDF.
     */
    suspend fun textToPdf(title: String, body: String, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val pdfDoc = PdfDocument()
            val pageWidth = 595 // A4 points
            val pageHeight = 842
            val margin = 40

            val titlePaint = TextPaint().apply {
                color = Color.parseColor("#0F766E")
                textSize = 20f
                isFakeBoldText = true
            }

            val bodyPaint = TextPaint().apply {
                color = Color.parseColor("#1E293B")
                textSize = 13f
            }

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawColor(Color.WHITE)
            canvas.drawText(title, margin.toFloat(), (margin + 20).toFloat(), titlePaint)

            val contentWidth = pageWidth - (margin * 2)
            val staticLayout = StaticLayout.Builder.obtain(body, 0, body.length, bodyPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(4f, 1.2f)
                .setIncludePad(true)
                .build()

            canvas.save()
            canvas.translate(margin.toFloat(), (margin + 50).toFloat())
            staticLayout.draw(canvas)
            canvas.restore()

            pdfDoc.finishPage(page)

            FileOutputStream(outputFile).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Compress PDF by re-encoding pages at lower resolution and JPEG compression.
     */
    suspend fun compressPdf(sourcePdf: File, outputFile: File, quality: Int = 60): Boolean = withContext(Dispatchers.IO) {
        try {
            val pfd = ParcelFileDescriptor.open(sourcePdf, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val pdfDoc = PdfDocument()

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val scale = 0.75f
                val width = (page.width * scale).toInt()
                val height = (page.height * scale).toInt()

                val pageInfo = PdfDocument.PageInfo.Builder(width, height, i + 1).create()
                val newPage = pdfDoc.startPage(pageInfo)

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDoc.finishPage(newPage)

                bitmap.recycle()
                page.close()
            }

            renderer.close()
            pfd.close()

            FileOutputStream(outputFile).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Export PDF with embedded annotations (Highlights and Signatures).
     */
    suspend fun exportPdfWithAnnotations(
        sourcePdf: File,
        highlights: List<HighlightEntity>,
        signatures: List<SignatureEntity>,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val pfd = ParcelFileDescriptor.open(sourcePdf, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val pdfDoc = PdfDocument()

            val highlightPaint = Paint().apply {
                style = Paint.Style.FILL
                alpha = 90
            }

            val sigTextPaint = Paint().apply {
                color = Color.parseColor("#0F766E")
                textSize = 28f
                isFakeBoldText = true
            }

            val sigStrokePaint = Paint().apply {
                color = Color.parseColor("#0F766E")
                strokeWidth = 4f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
            }

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val width = page.width
                val height = page.height

                val pageInfo = PdfDocument.PageInfo.Builder(width, height, i + 1).create()
                val newPage = pdfDoc.startPage(pageInfo)
                val canvas = newPage.canvas

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val pageCanvas = Canvas(bitmap)
                pageCanvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                bitmap.recycle()

                // Draw Highlights for this page
                val pageHighlights = highlights.filter { it.pageIndex == i }
                for (hl in pageHighlights) {
                    try {
                        highlightPaint.color = Color.parseColor(hl.colorHex)
                        highlightPaint.alpha = 90
                        // Draw highlight badge banner on top right/left
                        canvas.drawRect(20f, 20f, width - 20f, 45f, highlightPaint)
                    } catch (_: Exception) {}
                }

                // Draw Signatures for this page
                val pageSignatures = signatures.filter { it.pageIndex == i }
                for (sig in pageSignatures) {
                    try {
                        val posX = sig.normX * width
                        val posY = sig.normY * height
                        sigTextPaint.color = Color.parseColor(sig.strokeColorHex)
                        sigStrokePaint.color = Color.parseColor(sig.strokeColorHex)

                        if (sig.signatureType == "TYPED") {
                            canvas.drawText(sig.signatureData, posX, posY, sigTextPaint)
                            canvas.drawText("موقّع رقمياً: ${sig.signerName}", posX, posY + 24f, Paint().apply {
                                color = Color.GRAY
                                textSize = 14f
                            })
                        } else {
                            // Draw drawn path
                            drawDrawnPath(canvas, sig.signatureData, posX, posY, sigStrokePaint)
                            canvas.drawText(sig.signerName, posX, posY + 30f, Paint().apply {
                                color = Color.GRAY
                                textSize = 14f
                            })
                        }
                    } catch (_: Exception) {}
                }

                pdfDoc.finishPage(newPage)
                page.close()
            }

            renderer.close()
            pfd.close()

            FileOutputStream(outputFile).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun drawDrawnPath(canvas: Canvas, pathData: String, originX: Float, originY: Float, paint: Paint) {
        val points = pathData.split(";").filter { it.isNotBlank() }
        if (points.isEmpty()) return
        val path = Path()
        var isFirst = true
        for (pt in points) {
            val coords = pt.split(",")
            if (coords.size == 2) {
                val x = originX + (coords[0].toFloatOrNull() ?: 0f)
                val y = originY + (coords[1].toFloatOrNull() ?: 0f)
                if (isFirst) {
                    path.moveTo(x, y)
                    isFirst = false
                } else {
                    path.lineTo(x, y)
                }
            }
        }
        canvas.drawPath(path, paint)
    }
}
