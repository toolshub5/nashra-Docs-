package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.model.DocumentEntity
import com.example.engine.ParsedContent
import com.example.engine.PdfRendererHelper
import com.example.ui.components.HighlightDialog
import com.example.ui.components.PendingSignature
import com.example.ui.components.SignatureDialog
import com.example.ui.components.SignatureOverlayPlacer
import com.example.ui.locale.AppStrings
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: MainViewModel,
    strings: AppStrings,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val activeDoc by viewModel.activeDocument.collectAsState()
    val activeContent by viewModel.activeContent.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val highlights by viewModel.activeHighlights.collectAsState()
    val signatures by viewModel.activeSignatures.collectAsState()
    val ocrState by viewModel.ocrState.collectAsState()
    val extractedOcrText by viewModel.extractedOcrText.collectAsState()

    var currentPageIndex by remember { mutableIntStateOf(activeDoc?.lastReadPage ?: 0) }
    var showHighlightDialog by remember { mutableStateOf(false) }
    var showSignatureDialog by remember { mutableStateOf(false) }
    var pendingSignature by remember { mutableStateOf<PendingSignature?>(null) }
    var showOcrSheet by remember { mutableStateOf(false) }

    if (activeDoc == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "اختر مستنداً من المكتبة للبدء في القراءة",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val doc = activeDoc!!

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("reader_screen")
    ) {
        // Top Toolbar
        ReaderToolbar(
            document = doc,
            strings = strings,
            onToggleFavorite = { viewModel.toggleFavorite(doc) },
            onHighlightClick = { showHighlightDialog = true },
            onSignatureClick = { showSignatureDialog = true },
            onOcrClick = {
                val file = File(doc.filePath)
                viewModel.runOcrOnImage(file)
                showOcrSheet = true
            },
            onShare = { shareDocument(context, doc) },
            onExportPdf = {
                viewModel.exportActivePdfWithAnnotations(doc.title) { exportedFile ->
                    if (exportedFile != null) {
                        shareFile(context, exportedFile, "application/pdf")
                    }
                }
            }
        )

        // Main Viewer Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    when (userSettings.readerTheme) {
                        "dark" -> Color(0xFF121212)
                        "sepia" -> Color(0xFFFBF0D9)
                        else -> Color(0xFFF8FAFC)
                    }
                )
        ) {
            when (val content = activeContent) {
                is ParsedContent.PdfInfo -> {
                    PdfViewerView(
                        filePath = content.filePath,
                        pageCount = content.pageCount,
                        currentPageIndex = currentPageIndex,
                        onPageChanged = { newPage ->
                            currentPageIndex = newPage
                            viewModel.updateReadingPage(newPage, content.pageCount)
                        },
                        highlights = highlights.filter { it.pageIndex == currentPageIndex },
                        signatures = signatures.filter { it.pageIndex == currentPageIndex }
                    )
                }
                is ParsedContent.DocxContent -> {
                    DocxViewerView(
                        content = content,
                        fontSize = userSettings.readerFontSize,
                        theme = userSettings.readerTheme
                    )
                }
                is ParsedContent.TableContent -> {
                    TableViewerView(
                        content = content,
                        theme = userSettings.readerTheme
                    )
                }
                is ParsedContent.SlidesContent -> {
                    SlidesViewerView(
                        content = content,
                        currentPageIndex = currentPageIndex,
                        onPageChanged = { newPage ->
                            currentPageIndex = newPage
                            viewModel.updateReadingPage(newPage, content.totalSlides)
                        },
                        theme = userSettings.readerTheme
                    )
                }
                is ParsedContent.ImageContent -> {
                    ImageViewerView(filePath = content.filePath)
                }
                is ParsedContent.TextContent -> {
                    TextViewerView(
                        text = content.text,
                        fontSize = userSettings.readerFontSize,
                        theme = userSettings.readerTheme
                    )
                }
                is ParsedContent.ErrorContent -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = content.message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
                null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Interactive Signature Placement Overlay
            if (pendingSignature != null) {
                SignatureOverlayPlacer(
                    pendingSignature = pendingSignature!!,
                    onConfirmPlacement = { normX, normY ->
                        val sig = pendingSignature!!
                        viewModel.addSignature(
                            signerName = sig.signerName,
                            type = sig.type,
                            data = sig.data,
                            colorHex = sig.colorHex,
                            normX = normX,
                            normY = normY,
                            pageIndex = currentPageIndex
                        )
                        pendingSignature = null
                    },
                    onCancel = {
                        pendingSignature = null
                    }
                )
            }
        }
    }

    // Highlight Dialog
    if (showHighlightDialog) {
        HighlightDialog(
            strings = strings,
            onDismiss = { showHighlightDialog = false },
            onConfirm = { colorHex, text, note ->
                viewModel.addHighlight(colorHex, text, note)
                showHighlightDialog = false
            }
        )
    }

    // Signature Dialog
    if (showSignatureDialog) {
        SignatureDialog(
            strings = strings,
            onDismiss = { showSignatureDialog = false },
            onConfirm = { signerName, type, data, colorHex ->
                pendingSignature = PendingSignature(
                    signerName = signerName,
                    type = type,
                    data = data,
                    colorHex = colorHex,
                    pageIndex = currentPageIndex
                )
                showSignatureDialog = false
            }
        )
    }

    // OCR Bottom Sheet
    if (showOcrSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showOcrSheet = false
                viewModel.clearOcrResult()
            },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.toolOcr,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { showOcrSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (ocrState == "ANALYZING") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(strings.ocrAnalyzing, style = MaterialTheme.typography.bodyMedium)
                    }
                } else if (extractedOcrText != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = extractedOcrText!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(extractedOcrText!!))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.ocrCopyText)
                        }

                        Button(
                            onClick = {
                                viewModel.createNewTextDocument("OCR_${doc.title}", extractedOcrText!!)
                                showOcrSheet = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text(strings.ocrSaveDoc)
                        }
                    }
                } else {
                    Text(
                        text = strings.ocrError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ReaderToolbar(
    document: DocumentEntity,
    strings: AppStrings,
    onToggleFavorite: () -> Unit,
    onHighlightClick: () -> Unit,
    onSignatureClick: () -> Unit,
    onOcrClick: () -> Unit,
    onShare: () -> Unit,
    onExportPdf: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reader_toolbar"),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "${document.fileType} • ${(document.readingProgress * 100).toInt()}% مقروء",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Favorite
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (document.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (document.isFavorite) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Highlight
                IconButton(onClick = onHighlightClick, modifier = Modifier.testTag("btn_highlight")) {
                    Icon(
                        imageVector = Icons.Default.Highlight,
                        contentDescription = strings.highlightTool,
                        tint = Color(0xFFEAB308)
                    )
                }

                // Signature
                IconButton(onClick = onSignatureClick, modifier = Modifier.testTag("btn_signature")) {
                    Icon(
                        imageVector = Icons.Default.Draw,
                        contentDescription = strings.signatureTool,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // OCR
                IconButton(onClick = onOcrClick, modifier = Modifier.testTag("btn_ocr")) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = strings.toolOcr,
                        tint = Color(0xFF0284C7)
                    )
                }

                // Export / Share
                if (document.fileType == "PDF") {
                    IconButton(onClick = onExportPdf, modifier = Modifier.testTag("btn_export_pdf")) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = strings.exportPdf,
                            tint = Color(0xFFE11D48)
                        )
                    }
                }

                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = strings.share,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PdfViewerView(
    filePath: String,
    pageCount: Int,
    currentPageIndex: Int,
    onPageChanged: (Int) -> Unit,
    highlights: List<com.example.data.model.HighlightEntity>,
    signatures: List<com.example.data.model.SignatureEntity>
) {
    val file = remember(filePath) { File(filePath) }
    val helper = remember(filePath) { PdfRendererHelper(file) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpPageInput by remember { mutableStateOf("") }

    DisposableEffect(helper) {
        onDispose {
            helper.close()
        }
    }

    LaunchedEffect(currentPageIndex, scale) {
        isLoading = true
        val targetWidth = (1200 * scale.coerceAtLeast(1f)).toInt().coerceIn(1080, 2400)
        currentBitmap = helper.renderPage(currentPageIndex, targetWidth)
        isLoading = false
    }

    // Reset zoom when navigating between pages
    LaunchedEffect(currentPageIndex) {
        scale = 1f
        offset = Offset.Zero
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pdf_viewer")
    ) {
        // Page view with overlay annotations and interactive zoom/pan
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF1F5F9))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 4f)
                        if (scale == 1f) {
                            offset = Offset.Zero
                        } else {
                            val maxOffsetX = (size.width * (scale - 1f)) / 2f
                            val maxOffsetY = (size.height * (scale - 1f)) / 2f
                            offset = Offset(
                                x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            )
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.2f
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading && currentBitmap == null) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else if (currentBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = currentBitmap!!.asImageBitmap(),
                        contentDescription = "PDF Page ${currentPageIndex + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    // Highlights overlay banner
                    if (highlights.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        ) {
                            highlights.forEach { hl ->
                                val col = try {
                                    Color(android.graphics.Color.parseColor(hl.colorHex))
                                } catch (_: Exception) { Color.Yellow }
                                Box(
                                    modifier = Modifier
                                        .padding(bottom = 4.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(col.copy(alpha = 0.85f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "ملاحظة: ${hl.selectedText}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }

                    // Signatures overlay positioned by normalized coordinates
                    if (signatures.isNotEmpty()) {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val containerWidth = maxWidth
                            val containerHeight = maxHeight

                            signatures.forEach { sig ->
                                val targetX = (containerWidth * sig.normX.coerceIn(0.05f, 0.95f)) - 75.dp
                                val targetY = (containerHeight * sig.normY.coerceIn(0.05f, 0.95f)) - 35.dp
                                val sigColor = try {
                                    Color(android.graphics.Color.parseColor(sig.strokeColorHex))
                                } catch (_: Exception) {
                                    Color(0xFF0F766E)
                                }

                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    modifier = Modifier
                                        .offset(
                                            x = targetX.coerceAtLeast(8.dp),
                                            y = targetY.coerceAtLeast(8.dp)
                                        )
                                        .width(150.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        if (sig.signatureType == "TYPED") {
                                            Text(
                                                text = sig.signatureData,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = sigColor
                                            )
                                        } else {
                                            MiniatureSignatureCanvas(
                                                data = sig.signatureData,
                                                color = sigColor
                                            )
                                        }
                                        Text(
                                            text = "موقّع رقمياً: ${sig.signerName}",
                                            fontSize = 9.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating Zoom & Reset Quick Controls
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { scale = (scale + 0.3f).coerceAtMost(4f) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(18.dp))
                }

                if (scale > 1f) {
                    IconButton(
                        onClick = {
                            scale = 1f
                            offset = Offset.Zero
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset Zoom", tint = Color.Yellow, modifier = Modifier.size(18.dp))
                    }
                }

                IconButton(
                    onClick = {
                        scale = (scale - 0.3f).coerceAtLeast(1f)
                        if (scale == 1f) offset = Offset.Zero
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Bottom Page Scrubber & Navigation Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                if (pageCount > 1) {
                    Slider(
                        value = currentPageIndex.toFloat(),
                        onValueChange = { onPageChanged(it.toInt().coerceIn(0, pageCount - 1)) },
                        valueRange = 0f..(pageCount - 1).toFloat(),
                        steps = (pageCount - 2).coerceAtLeast(0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (currentPageIndex > 0) {
                                onPageChanged(currentPageIndex - 1)
                            }
                        },
                        enabled = currentPageIndex > 0
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Prev")
                    }

                    // Clickable page indicator to open Jump to Page dialog
                    Card(
                        onClick = {
                            jumpPageInput = (currentPageIndex + 1).toString()
                            showJumpDialog = true
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.FindInPage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "صفحة ${currentPageIndex + 1} من $pageCount",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (currentPageIndex < pageCount - 1) {
                                onPageChanged(currentPageIndex + 1)
                            }
                        },
                        enabled = currentPageIndex < pageCount - 1
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                    }
                }
            }
        }
    }

    // Jump to Page Dialog
    if (showJumpDialog) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("الذهاب إلى صفحة", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("أدخل رقم الصفحة من 1 إلى $pageCount:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jumpPageInput,
                        onValueChange = { jumpPageInput = it.filter { char -> char.isDigit() } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = jumpPageInput.toIntOrNull()
                        if (target != null && target in 1..pageCount) {
                            onPageChanged(target - 1)
                        }
                        showJumpDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("انتقال")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showJumpDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun DocxViewerView(
    content: ParsedContent.DocxContent,
    fontSize: Float,
    theme: String
) {
    val textColor = when (theme) {
        "dark" -> Color(0xFFF1F5F9)
        "sepia" -> Color(0xFF5F4B32)
        else -> Color(0xFF0F172A)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = content.title,
                fontSize = (fontSize + 6).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        }

        items(content.paragraphs) { paragraph ->
            Text(
                text = paragraph,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * 1.5).sp,
                color = textColor
            )
        }

        // Tables
        items(content.tables) { table ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    table.forEachIndexed { rowIndex, row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            row.forEach { cell ->
                                Text(
                                    text = cell,
                                    fontSize = (fontSize - 2).sp,
                                    fontWeight = if (rowIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                                    color = textColor
                                )
                            }
                        }
                        if (rowIndex < table.size - 1) {
                            Divider(color = Color.LightGray.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TableViewerView(
    content: ParsedContent.TableContent,
    theme: String
) {
    val textColor = when (theme) {
        "dark" -> Color(0xFFF1F5F9)
        "sepia" -> Color(0xFF5F4B32)
        else -> Color(0xFF0F172A)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(
            text = "عرض الجداول والبيانات (${content.totalRows} صفوف)",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .horizontalScroll(rememberScrollState())
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxHeight(),
                contentPadding = PaddingValues(8.dp)
            ) {
                // Header row
                if (content.headers.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                        ) {
                            content.headers.forEach { header ->
                                Text(
                                    text = header,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.width(130.dp).padding(horizontal = 6.dp)
                                )
                            }
                        }
                        Divider()
                    }
                }

                itemsIndexed(content.rows) { index, row ->
                    Row(
                        modifier = Modifier
                            .background(if (index % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        row.forEach { cell ->
                            Text(
                                text = cell,
                                fontSize = 12.sp,
                                color = textColor,
                                modifier = Modifier.width(130.dp).padding(horizontal = 6.dp)
                            )
                        }
                    }
                    Divider(color = Color.LightGray.copy(alpha = 0.25f))
                }
            }
        }
    }
}

@Composable
fun SlidesViewerView(
    content: ParsedContent.SlidesContent,
    currentPageIndex: Int,
    onPageChanged: (Int) -> Unit,
    theme: String
) {
    val slideText = content.slides.getOrElse(currentPageIndex) { "" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "شريحة ${currentPageIndex + 1} من ${content.totalSlides}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(14.dp))
                Divider()
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = slideText,
                    fontSize = 18.sp,
                    lineHeight = 28.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { if (currentPageIndex > 0) onPageChanged(currentPageIndex - 1) },
                enabled = currentPageIndex > 0,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("الشريحة السابقة")
            }

            Text(
                text = "${currentPageIndex + 1} / ${content.totalSlides}",
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = { if (currentPageIndex < content.totalSlides - 1) onPageChanged(currentPageIndex + 1) },
                enabled = currentPageIndex < content.totalSlides - 1,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("الشريحة التالية")
            }
        }
    }
}

@Composable
fun ImageViewerView(filePath: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = File(filePath),
            contentDescription = "Image Document",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun TextViewerView(
    text: String,
    fontSize: Float,
    theme: String
) {
    val textColor = when (theme) {
        "dark" -> Color(0xFFF1F5F9)
        "sepia" -> Color(0xFF5F4B32)
        else -> Color(0xFF0F172A)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = text,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.6).sp,
            color = textColor
        )
    }
}

private fun shareDocument(context: Context, doc: DocumentEntity) {
    try {
        val file = File(doc.filePath)
        if (!file.exists()) return
        shareFile(context, file, doc.mimeType.ifBlank { "*/*" })
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun shareFile(context: Context, file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, file.name))
}

@Composable
private fun MiniatureSignatureCanvas(data: String, color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(Color(0xFFF8FAFC), RoundedCornerShape(4.dp))
            .padding(4.dp)
    ) {
        if (data.startsWith("POINTS:")) {
            val coords = data.removePrefix("POINTS:").split(";")
            val path = Path()
            var first = true
            for (coord in coords) {
                val parts = coord.split(",")
                if (parts.size == 2) {
                    val px = (parts[0].toFloatOrNull() ?: continue) / 300f * size.width
                    val py = (parts[1].toFloatOrNull() ?: continue) / 150f * size.height
                    if (first) {
                        path.moveTo(px, py)
                        first = false
                    } else {
                        path.lineTo(px, py)
                    }
                }
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        } else {
            drawCircle(color = color, radius = 6f)
        }
    }
}
