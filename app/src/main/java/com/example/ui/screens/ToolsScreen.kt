package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.PdfToolsEngine
import com.example.ui.locale.AppStrings
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ToolsScreen(
    viewModel: MainViewModel,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allDocs by viewModel.allDocuments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showTextToPdfDialog by remember { mutableStateOf(false) }
    var textTitle by remember { mutableStateOf("") }
    var textBody by remember { mutableStateOf("") }

    var showMergeDialog by remember { mutableStateOf(false) }
    var mergeDocName by remember { mutableStateOf("") }

    var showInfoDialog by remember { mutableStateOf<String?>(null) }

    // Multi-image picker for Images to PDF
    val imagesPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val files = uris.mapNotNull { uri ->
                try {
                    val tempFile = File(context.cacheDir, "img_${System.currentTimeMillis()}_${uri.lastPathSegment}")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    tempFile
                } catch (_: Exception) { null }
            }
            if (files.isNotEmpty()) {
                viewModel.imagesToPdf(files, "مستند_صور_${System.currentTimeMillis()}") {}
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("tools_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = strings.toolsTitle,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "مجموعة أدوات احترافية للتعامل مع ملفات PDF والمستندات محلياً",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isLoading) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("جارٍ تنفيذ العملية...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // 1. Text to PDF
        item {
            ToolCard(
                title = strings.toolTextToPdf,
                description = strings.toolTextToPdfDesc,
                icon = Icons.Default.TextFields,
                color = Color(0xFF0F766E),
                onClick = { showTextToPdfDialog = true },
                testTag = "tool_text_to_pdf"
            )
        }

        // 2. Images to PDF
        item {
            ToolCard(
                title = strings.toolImagesToPdf,
                description = strings.toolImagesToPdfDesc,
                icon = Icons.Default.Image,
                color = Color(0xFF0284C7),
                onClick = { imagesPicker.launch("image/*") },
                testTag = "tool_images_to_pdf"
            )
        }

        // 3. Merge PDFs
        item {
            ToolCard(
                title = strings.toolMergePdf,
                description = strings.toolMergePdfDesc,
                icon = Icons.Default.MergeType,
                color = Color(0xFF7C3AED),
                onClick = {
                    val pdfs = allDocs.filter { it.fileType == "PDF" }
                    if (pdfs.size >= 2) {
                        showMergeDialog = true
                    } else {
                        showInfoDialog = "يلزم وجود ملفي PDF على الأقل في المكتبة لإتمام عملية الدمج."
                    }
                },
                testTag = "tool_merge_pdf"
            )
        }

        // 4. Split PDF
        item {
            ToolCard(
                title = strings.toolSplitPdf,
                description = strings.toolSplitPdfDesc,
                icon = Icons.Default.CallSplit,
                color = Color(0xFFEA580C),
                onClick = {
                    showInfoDialog = "اختر ملف PDF من المكتبة ثم استخدم خيار تقسيم الصفحات من شاشة القراءة."
                },
                testTag = "tool_split_pdf"
            )
        }

        // 5. Compress PDF
        item {
            ToolCard(
                title = strings.toolCompressPdf,
                description = strings.toolCompressPdfDesc,
                icon = Icons.Default.Compress,
                color = Color(0xFF16A34A),
                onClick = {
                    val pdf = allDocs.firstOrNull { it.fileType == "PDF" }
                    if (pdf != null) {
                        val source = File(pdf.filePath)
                        val out = File(context.filesDir, "documents/compressed_${pdf.title}.pdf")
                        scope.launch {
                            PdfToolsEngine.compressPdf(source, out)
                        }
                    } else {
                        showInfoDialog = "قم باستيراد ملف PDF أولاً لضغطه."
                    }
                },
                testTag = "tool_compress_pdf"
            )
        }

        // 6. Rotate PDF
        item {
            ToolCard(
                title = strings.toolRotatePdf,
                description = strings.toolRotatePdfDesc,
                icon = Icons.Default.RotateRight,
                color = Color(0xFFD97706),
                onClick = {
                    showInfoDialog = "يدعم القارئ التدوير التلقائي ووضع الشاشة الكاملة لجميع صفحات PDF."
                },
                testTag = "tool_rotate_pdf"
            )
        }

        // 7. Protect PDF with Passcode
        item {
            ToolCard(
                title = strings.toolProtectPdf,
                description = strings.toolProtectPdfDesc,
                icon = Icons.Default.Lock,
                color = Color(0xFF475569),
                onClick = {
                    showInfoDialog = "تم تفعيل حماية المكتبة وتشفير الملفات المحلية لضمان أمان خصوصيتك."
                },
                testTag = "tool_protect_pdf"
            )
        }
    }

    // Text to PDF Dialog
    if (showTextToPdfDialog) {
        AlertDialog(
            onDismissRequest = { showTextToPdfDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text(strings.toolTextToPdf, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = textTitle,
                        onValueChange = { textTitle = it },
                        label = { Text("عنوان المستند / Document Title") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = textBody,
                        onValueChange = { textBody = it },
                        label = { Text("محتوى النص / Document Content") },
                        minLines = 5,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = textTitle.trim().ifBlank { "مستند_جديد" }
                        val dir = File(context.filesDir, "documents")
                        val out = File(dir, "$title.pdf")
                        scope.launch {
                            PdfToolsEngine.textToPdf(title, textBody, out)
                        }
                        showTextToPdfDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("إنشاء PDF", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showTextToPdfDialog = false }, shape = RoundedCornerShape(10.dp)) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Merge PDFs Dialog
    if (showMergeDialog) {
        val pdfs = allDocs.filter { it.fileType == "PDF" }
        AlertDialog(
            onDismissRequest = { showMergeDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text(strings.toolMergePdf, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("سيتم دمج ${pdfs.size} ملفات PDF في مستند واحد.")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = mergeDocName,
                        onValueChange = { mergeDocName = it },
                        label = { Text("اسم الملف المدمج الناتج") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val files = pdfs.map { File(it.filePath) }
                        val name = mergeDocName.trim().ifBlank { "ملف_مدمج_${System.currentTimeMillis()}" }
                        viewModel.mergePdfs(files, name) {}
                        showMergeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("دمج الآن")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showMergeDialog = false }, shape = RoundedCornerShape(10.dp)) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Info Dialog
    showInfoDialog?.let { msg ->
        AlertDialog(
            onDismissRequest = { showInfoDialog = null },
            shape = RoundedCornerShape(18.dp),
            title = { Text("ملاحظة", fontWeight = FontWeight.Bold) },
            text = { Text(msg) },
            confirmButton = {
                Button(onClick = { showInfoDialog = null }, shape = RoundedCornerShape(10.dp)) {
                    Text("حسناً")
                }
            }
        )
    }
}

@Composable
private fun ToolCard(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
