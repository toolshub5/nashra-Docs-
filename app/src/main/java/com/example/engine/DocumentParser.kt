package com.example.engine

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

sealed class ParsedContent {
    data class TextContent(val text: String, val lineCount: Int, val wordCount: Int) : ParsedContent()
    data class DocxContent(val title: String, val paragraphs: List<String>, val tables: List<List<List<String>>>) : ParsedContent()
    data class TableContent(val headers: List<String>, val rows: List<List<String>>, val totalRows: Int, val totalCols: Int) : ParsedContent()
    data class SlidesContent(val slides: List<String>, val totalSlides: Int) : ParsedContent()
    data class ImageContent(val filePath: String, val width: Int = 0, val height: Int = 0) : ParsedContent()
    data class PdfInfo(val pageCount: Int, val filePath: String) : ParsedContent()
    data class ErrorContent(val message: String) : ParsedContent()
}

object DocumentParser {

    fun parseFile(file: File, fileType: String): ParsedContent {
        if (!file.exists()) {
            return ParsedContent.ErrorContent("الملف غير موجود / File not found")
        }
        return try {
            when (fileType.uppercase()) {
                "TXT" -> parseTxt(file)
                "CSV" -> parseCsv(file)
                "DOCX" -> parseDocx(file)
                "XLSX" -> parseXlsx(file)
                "PPTX" -> parsePptx(file)
                "IMAGE", "JPG", "JPEG", "PNG", "WEBP" -> ParsedContent.ImageContent(file.absolutePath)
                "PDF" -> {
                    val pageCount = getPdfPageCount(file)
                    ParsedContent.PdfInfo(pageCount, file.absolutePath)
                }
                else -> parseTxt(file)
            }
        } catch (e: Exception) {
            ParsedContent.ErrorContent("حدث خطأ أثناء قراءة الملف: ${e.localizedMessage}")
        }
    }

    private fun parseTxt(file: File): ParsedContent.TextContent {
        val text = file.readText(Charsets.UTF_8)
        val lines = text.lines()
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        return ParsedContent.TextContent(text, lines.size, words.size)
    }

    fun parseCsv(file: File): ParsedContent.TableContent {
        val rows = mutableListOf<List<String>>()
        file.useLines(Charsets.UTF_8) { lines ->
            lines.forEach { line ->
                val tokens = parseCsvLine(line)
                if (tokens.isNotEmpty()) {
                    rows.add(tokens)
                }
            }
        }
        val headers = if (rows.isNotEmpty()) rows.first() else emptyList()
        val dataRows = if (rows.size > 1) rows.drop(1) else emptyList()
        val maxCols = rows.maxOfOrNull { it.size } ?: 0
        return ParsedContent.TableContent(
            headers = headers,
            rows = dataRows,
            totalRows = dataRows.size,
            totalCols = maxCols
        )
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (c in line) {
            when {
                c == '\"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                }
                c == ';' && !inQuotes && !line.contains(',') -> {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                }
                else -> sb.append(c)
            }
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    fun parseDocx(file: File): ParsedContent.DocxContent {
        val paragraphs = mutableListOf<String>()
        val tables = mutableListOf<List<List<String>>>()

        ZipInputStream(file.inputStream()).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val xmlContent = zip.bufferedReader(Charsets.UTF_8).readText()
                    // Extract paragraphs: <w:p>...</w:p>
                    val pRegex = Regex("<w:p[ >](.*?)</w:p>", RegexOption.DOT_MATCHES_ALL)
                    val tRegex = Regex("<w:t[^>]*>(.*?)</w:t>", RegexOption.DOT_MATCHES_ALL)

                    pRegex.findAll(xmlContent).forEach { pMatch ->
                        val pText = StringBuilder()
                        tRegex.findAll(pMatch.value).forEach { tMatch ->
                            pText.append(tMatch.groupValues[1])
                        }
                        val text = pText.toString().trim()
                        if (text.isNotBlank()) {
                            paragraphs.add(text)
                        }
                    }

                    // Extract tables: <w:tbl>...</w:tbl>
                    val tblRegex = Regex("<w:tbl[ >](.*?)</w:tbl>", RegexOption.DOT_MATCHES_ALL)
                    val trRegex = Regex("<w:tr[ >](.*?)</w:tr>", RegexOption.DOT_MATCHES_ALL)
                    val tcRegex = Regex("<w:tc[ >](.*?)</w:tc>", RegexOption.DOT_MATCHES_ALL)

                    tblRegex.findAll(xmlContent).forEach { tblMatch ->
                        val table = mutableListOf<List<String>>()
                        trRegex.findAll(tblMatch.value).forEach { trMatch ->
                            val row = mutableListOf<String>()
                            tcRegex.findAll(trMatch.value).forEach { tcMatch ->
                                val cellText = StringBuilder()
                                tRegex.findAll(tcMatch.value).forEach { tMatch ->
                                    cellText.append(tMatch.groupValues[1])
                                }
                                row.add(cellText.toString().trim())
                            }
                            if (row.isNotEmpty()) {
                                table.add(row)
                            }
                        }
                        if (table.isNotEmpty()) {
                            tables.add(table)
                        }
                    }
                    break
                }
                entry = zip.nextEntry
            }
        }

        val title = paragraphs.firstOrNull() ?: file.nameWithoutExtension
        return ParsedContent.DocxContent(
            title = title,
            paragraphs = paragraphs,
            tables = tables
        )
    }

    fun parseXlsx(file: File): ParsedContent.TableContent {
        val sharedStrings = mutableListOf<String>()
        var sheet1Xml: String? = null

        // First pass: Read sharedStrings.xml and sheet1.xml
        ZipInputStream(file.inputStream()).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                if (entry.name == "xl/sharedStrings.xml") {
                    val xml = zip.bufferedReader(Charsets.UTF_8).readText()
                    val tRegex = Regex("<t[^>]*>(.*?)</t>", RegexOption.DOT_MATCHES_ALL)
                    tRegex.findAll(xml).forEach { m ->
                        sharedStrings.add(m.groupValues[1])
                    }
                } else if (entry.name.startsWith("xl/worksheets/sheet1") && entry.name.endsWith(".xml")) {
                    sheet1Xml = zip.bufferedReader(Charsets.UTF_8).readText()
                }
                entry = zip.nextEntry
            }
        }

        if (sheet1Xml.isNullOrBlank()) {
            return ParsedContent.TableContent(
                headers = listOf("بيانات فارغة / Empty Sheet"),
                rows = emptyList(),
                totalRows = 0,
                totalCols = 0
            )
        }

        val rows = mutableListOf<List<String>>()
        val rowRegex = Regex("<row[^>]*>(.*?)</row>", RegexOption.DOT_MATCHES_ALL)
        val cellRegex = Regex("<c r=\"([A-Z]+[0-9]+)\"(?: t=\"([^\"]+)\")?[^>]*>(?:<v>([^<]*)</v>)?", RegexOption.DOT_MATCHES_ALL)

        rowRegex.findAll(sheet1Xml!!).forEach { rowMatch ->
            val rowValues = mutableListOf<String>()
            cellRegex.findAll(rowMatch.value).forEach { cellMatch ->
                val type = cellMatch.groupValues[2]
                val rawVal = cellMatch.groupValues[3]
                val value = when (type) {
                    "s" -> {
                        val index = rawVal.toIntOrNull()
                        if (index != null && index < sharedStrings.size) sharedStrings[index] else rawVal
                    }
                    "b" -> if (rawVal == "1") "TRUE" else "FALSE"
                    else -> rawVal
                }
                rowValues.add(value)
            }
            if (rowValues.any { it.isNotBlank() }) {
                rows.add(rowValues)
            }
        }

        val headers = if (rows.isNotEmpty()) rows.first() else emptyList()
        val dataRows = if (rows.size > 1) rows.drop(1) else emptyList()
        val maxCols = rows.maxOfOrNull { it.size } ?: 0

        return ParsedContent.TableContent(
            headers = headers,
            rows = dataRows,
            totalRows = dataRows.size,
            totalCols = maxCols
        )
    }

    fun parsePptx(file: File): ParsedContent.SlidesContent {
        val slidesMap = mutableMapOf<Int, String>()

        ZipInputStream(file.inputStream()).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                if (name.startsWith("ppt/slides/slide") && name.endsWith(".xml")) {
                    val numberStr = name.removePrefix("ppt/slides/slide").removeSuffix(".xml")
                    val slideNum = numberStr.toIntOrNull() ?: 0
                    val xml = zip.bufferedReader(Charsets.UTF_8).readText()
                    val tRegex = Regex("<a:t[^>]*>(.*?)</a:t>", RegexOption.DOT_MATCHES_ALL)
                    val slideText = StringBuilder()
                    tRegex.findAll(xml).forEach { m ->
                        val t = m.groupValues[1].trim()
                        if (t.isNotBlank()) {
                            slideText.append(t).append("\n")
                        }
                    }
                    slidesMap[slideNum] = slideText.toString().trim()
                }
                entry = zip.nextEntry
            }
        }

        val sortedSlides = slidesMap.keys.sorted().map { num ->
            slidesMap[num] ?: ""
        }.filter { it.isNotBlank() }

        val finalSlides = if (sortedSlides.isEmpty()) {
            listOf("الشريحة لا تحتوي على نصوص / Empty Slide")
        } else sortedSlides

        return ParsedContent.SlidesContent(
            slides = finalSlides,
            totalSlides = finalSlides.size
        )
    }

    private fun getPdfPageCount(file: File): Int {
        return try {
            val pfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = android.graphics.pdf.PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            count
        } catch (_: Exception) {
            1
        }
    }
}
