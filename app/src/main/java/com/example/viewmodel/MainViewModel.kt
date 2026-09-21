package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.UserManager
import com.example.data.auth.UserProfile
import com.example.data.local.AppDatabase
import com.example.data.local.DocumentRepository
import com.example.data.local.FileStorageManager
import com.example.data.local.PreferencesManager
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentWithTags
import com.example.data.model.HighlightEntity
import com.example.data.model.OcrEntity
import com.example.data.model.SignatureEntity
import com.example.data.model.TagEntity
import com.example.data.model.UserSettings
import com.example.engine.DocumentParser
import com.example.engine.GeminiOcrResult
import com.example.engine.GeminiOcrService
import com.example.engine.OcrEngine
import com.example.engine.OcrResult
import com.example.engine.ParsedContent
import com.example.engine.PdfToolsEngine
import com.example.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = DocumentRepository(db.documentDao())
    val preferencesManager = PreferencesManager(application)
    val fileStorageManager = FileStorageManager(application)
    val userManager = UserManager(application)
    val geminiOcrService = GeminiOcrService(application)

    // User profile & sync state
    val currentUser: StateFlow<UserProfile> = userManager.currentUser

    // Storage Statistics
    private val _storageUsage = MutableStateFlow("0 B")
    val storageUsage: StateFlow<String> = _storageUsage.asStateFlow()

    private val _storageBreakdown = MutableStateFlow<Map<String, String>>(emptyMap())
    val storageBreakdown: StateFlow<Map<String, String>> = _storageBreakdown.asStateFlow()

    // User preferences & settings
    val userSettings: StateFlow<UserSettings> = preferencesManager.settings

    // Navigation state
    private val _currentScreen = MutableStateFlow(Screen.HOME)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Search and filter state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("ALL") // ALL, PDF, WORD, EXCEL, PPTX, IMAGE, TXT
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedTagFilterId = MutableStateFlow(0L) // 0 = ALL
    val selectedTagFilterId: StateFlow<Long> = _selectedTagFilterId.asStateFlow()

    private val _sortOption = MutableStateFlow("DATE") // DATE, NAME, SIZE, LAST_OPENED
    val sortOption: StateFlow<String> = _sortOption.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    // Status message for snackbars
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Active document in Reader
    private val _activeDocument = MutableStateFlow<DocumentEntity?>(null)
    val activeDocument: StateFlow<DocumentEntity?> = _activeDocument.asStateFlow()

    private val _activeContent = MutableStateFlow<ParsedContent?>(null)
    val activeContent: StateFlow<ParsedContent?> = _activeContent.asStateFlow()

    private val _activeHighlights = MutableStateFlow<List<HighlightEntity>>(emptyList())
    val activeHighlights: StateFlow<List<HighlightEntity>> = _activeHighlights.asStateFlow()

    private val _activeSignatures = MutableStateFlow<List<SignatureEntity>>(emptyList())
    val activeSignatures: StateFlow<List<SignatureEntity>> = _activeSignatures.asStateFlow()

    // Active OCR State
    private val _ocrState = MutableStateFlow<String?>(null)
    val ocrState: StateFlow<String?> = _ocrState.asStateFlow()

    private val _extractedOcrText = MutableStateFlow<String?>(null)
    val extractedOcrText: StateFlow<String?> = _extractedOcrText.asStateFlow()

    // Documents & Tags flows
    val allTags: StateFlow<List<TagEntity>> = repository.allTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDocumentsWithTags: StateFlow<List<DocumentWithTags>> = repository.allDocumentsWithTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDocuments: StateFlow<List<DocumentEntity>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentDocuments: StateFlow<List<DocumentEntity>> = repository.recentDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteDocuments: StateFlow<List<DocumentEntity>> = repository.favoriteDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOcrRecords: StateFlow<List<OcrEntity>> = repository.allOcrRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered documents with tags for Home Search & Library
    val filteredDocumentsWithTags: StateFlow<List<DocumentWithTags>> = combine(
        allDocumentsWithTags,
        searchQuery,
        selectedCategory,
        selectedTagFilterId,
        sortOption
    ) { docsWithTags, query, category, tagId, sort ->
        var list = docsWithTags
        if (query.isNotBlank()) {
            list = list.filter { item ->
                item.document.title.contains(query, ignoreCase = true) ||
                        item.document.originalFileName.contains(query, ignoreCase = true) ||
                        item.tags.any { it.name.contains(query, ignoreCase = true) }
            }
        }
        if (category != "ALL") {
            list = list.filter { it.document.fileType.equals(category, ignoreCase = true) }
        }
        if (tagId != 0L) {
            list = list.filter { item -> item.tags.any { it.tagId == tagId } }
        }
        when (sort) {
            "NAME" -> list.sortedBy { it.document.title.lowercase() }
            "SIZE" -> list.sortedByDescending { it.document.fileSizeBytes }
            "LAST_OPENED" -> list.sortedByDescending { it.document.lastOpenedAt }
            else -> list.sortedByDescending { it.document.createdAt }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered documents for legacy callers
    val filteredDocuments: StateFlow<List<DocumentEntity>> = combine(
        filteredDocumentsWithTags
    ) { arrayOfLists ->
        arrayOfLists[0].map { it.document }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshStorageStats()
    }

    fun refreshStorageStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val total = fileStorageManager.getTotalStorageUsageBytes()
            _storageUsage.value = FileStorageManager.formatFileSize(total)
            val breakdown = fileStorageManager.getStorageBreakdownBytes()
            _storageBreakdown.value = breakdown.mapValues { FileStorageManager.formatFileSize(it.value) }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSortOption(sort: String) {
        _sortOption.value = sort
    }

    fun toggleGridView() {
        _isGridView.value = !_isGridView.value
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // --- Import Functions ---
    fun importFileFromUri(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val context = getApplication<Application>()
                val doc = copyUriToInternalStorage(context, uri)
                if (doc != null) {
                    val id = repository.insertDocument(doc)
                    val insertedDoc = doc.copy(id = id)
                    _statusMessage.value = "تم استيراد الملف بنجاح / File imported successfully"
                    openDocument(insertedDoc)
                } else {
                    _statusMessage.value = "تعذر استيراد الملف / Could not import file"
                }
            } catch (e: Exception) {
                _statusMessage.value = "خطأ في الاستيراد: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createNewTextDocument(title: String, content: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val safeTitle = title.trim().ifBlank { "مستند_جديد_${System.currentTimeMillis()}" }
                val fileName = if (safeTitle.endsWith(".txt")) safeTitle else "$safeTitle.txt"
                val targetFile = fileStorageManager.saveDocumentBytes(
                    fileName = fileName,
                    fileType = "TXT",
                    bytes = content.toByteArray(Charsets.UTF_8)
                )

                val doc = DocumentEntity(
                    title = safeTitle.removeSuffix(".txt"),
                    originalFileName = fileName,
                    filePath = targetFile.absolutePath,
                    fileType = "TXT",
                    mimeType = "text/plain",
                    fileSizeBytes = targetFile.length(),
                    pageCount = 1
                )
                val id = repository.insertDocument(doc)
                _statusMessage.value = "تم إنشاء المستند النصي بنجاح"
                refreshStorageStats()
                openDocument(doc.copy(id = id))
            } catch (e: Exception) {
                _statusMessage.value = "تعذر إنشاء المستند: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun copyUriToInternalStorage(context: Context, uri: Uri): DocumentEntity? = withContext(Dispatchers.IO) {
        try {
            var fileName = "document_${System.currentTimeMillis()}"
            var fileSize = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            val ext = fileName.substringAfterLast('.', "").lowercase()
            val fileType = when (ext) {
                "pdf" -> "PDF"
                "docx", "doc" -> "WORD"
                "xlsx", "xls" -> "EXCEL"
                "pptx", "ppt" -> "PPTX"
                "csv" -> "CSV"
                "jpg", "jpeg", "png", "webp" -> "IMAGE"
                "txt" -> "TXT"
                else -> {
                    val mime = context.contentResolver.getType(uri) ?: ""
                    when {
                        mime.contains("pdf") -> "PDF"
                        mime.contains("word") -> "WORD"
                        mime.contains("sheet") || mime.contains("excel") -> "EXCEL"
                        mime.contains("presentation") || mime.contains("powerpoint") -> "PPTX"
                        mime.contains("image") -> "IMAGE"
                        mime.contains("csv") -> "CSV"
                        else -> "TXT"
                    }
                }
            }

            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val targetFile = inputStream.use { input ->
                fileStorageManager.saveDocument(fileName, fileType, input)
            }

            if (fileSize == 0L) fileSize = targetFile.length()
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

            val pageCount = if (fileType == "PDF") {
                try {
                    val pfd = android.os.ParcelFileDescriptor.open(targetFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = android.graphics.pdf.PdfRenderer(pfd)
                    val count = renderer.pageCount
                    renderer.close()
                    pfd.close()
                    count
                } catch (_: Exception) { 1 }
            } else 1

            refreshStorageStats()

            DocumentEntity(
                title = fileName.substringBeforeLast('.'),
                originalFileName = fileName,
                filePath = targetFile.absolutePath,
                fileType = fileType,
                mimeType = mimeType,
                fileSizeBytes = fileSize,
                pageCount = pageCount
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- Document Viewer and Actions ---
    fun openDocument(doc: DocumentEntity) {
        viewModelScope.launch {
            _activeDocument.value = doc
            _currentScreen.value = Screen.READER

            // Update last opened time in Room
            repository.updateProgress(doc.id, doc.lastReadPage, doc.readingProgress)

            // Parse document content
            val file = File(doc.filePath)
            val parsed = withContext(Dispatchers.IO) {
                DocumentParser.parseFile(file, doc.fileType)
            }
            _activeContent.value = parsed

            // Observe highlights and signatures
            launch {
                repository.getHighlights(doc.id).collect {
                    _activeHighlights.value = it
                }
            }
            launch {
                repository.getSignatures(doc.id).collect {
                    _activeSignatures.value = it
                }
            }
        }
    }

    fun updateReadingPage(pageIndex: Int, totalPages: Int) {
        val doc = _activeDocument.value ?: return
        val progress = if (totalPages > 0) (pageIndex + 1).toFloat() / totalPages.toFloat() else 0f
        _activeDocument.value = doc.copy(lastReadPage = pageIndex, readingProgress = progress)
        viewModelScope.launch {
            repository.updateProgress(doc.id, pageIndex, progress)
        }
    }

    fun toggleFavorite(doc: DocumentEntity) {
        viewModelScope.launch {
            val newFav = !doc.isFavorite
            repository.toggleFavorite(doc.id, newFav)
            if (_activeDocument.value?.id == doc.id) {
                _activeDocument.value = _activeDocument.value?.copy(isFavorite = newFav)
            }
        }
    }

    fun renameDocument(doc: DocumentEntity, newTitle: String) {
        viewModelScope.launch {
            if (newTitle.isNotBlank()) {
                repository.renameDocument(doc.id, newTitle)
                if (_activeDocument.value?.id == doc.id) {
                    _activeDocument.value = _activeDocument.value?.copy(title = newTitle)
                }
                _statusMessage.value = "تمت إعادة تسمية الملف"
            }
        }
    }

    fun deleteDocument(doc: DocumentEntity) {
        viewModelScope.launch {
            fileStorageManager.deleteDocumentFile(doc.filePath)
            repository.deleteDocument(doc)
            if (_activeDocument.value?.id == doc.id) {
                _activeDocument.value = null
                _activeContent.value = null
                _currentScreen.value = Screen.LIBRARY
            }
            _statusMessage.value = "تم حذف المستند من المكتبة"
            refreshStorageStats()
        }
    }

    // --- Highlights ---
    fun addHighlight(colorHex: String, text: String, note: String = "") {
        val doc = _activeDocument.value ?: return
        viewModelScope.launch {
            val highlight = HighlightEntity(
                docId = doc.id,
                pageIndex = doc.lastReadPage,
                selectedText = text,
                colorHex = colorHex,
                note = note
            )
            repository.addHighlight(highlight)
            _statusMessage.value = "تم حفظ التظليل"
        }
    }

    fun deleteHighlight(id: Long) {
        viewModelScope.launch {
            repository.deleteHighlight(id)
            _statusMessage.value = "تم حذف التظليل"
        }
    }

    // --- Signatures ---
    fun addSignature(
        signerName: String,
        type: String, // DRAWN, TYPED
        data: String,
        colorHex: String = "#0F766E",
        normX: Float = 0.5f,
        normY: Float = 0.8f,
        pageIndex: Int = _activeDocument.value?.lastReadPage ?: 0
    ) {
        val doc = _activeDocument.value ?: return
        viewModelScope.launch {
            val signature = SignatureEntity(
                docId = doc.id,
                pageIndex = pageIndex,
                signerName = signerName,
                signatureType = type,
                signatureData = data,
                strokeColorHex = colorHex,
                normX = normX,
                normY = normY
            )
            repository.addSignature(signature)
            _statusMessage.value = "تم تثبيت التوقيع وإحداثياته في المستند بنجاح"
        }
    }

    fun deleteSignature(id: Long) {
        viewModelScope.launch {
            repository.deleteSignature(id)
            _statusMessage.value = "تم حذف التوقيع"
        }
    }

    // --- Tags Operations (Many-to-Many) ---
    fun setSelectedTagFilter(tagId: Long) {
        _selectedTagFilterId.value = tagId
    }

    fun createTag(name: String, colorHex: String = "#0F766E") {
        viewModelScope.launch {
            repository.createTag(name, colorHex)
            _statusMessage.value = "تم إنشاء الوسم: $name"
        }
    }

    fun deleteTag(tagId: Long) {
        viewModelScope.launch {
            repository.deleteTag(tagId)
            _statusMessage.value = "تم حذف الوسم"
        }
    }

    fun addTagToDocument(documentId: Long, tagId: Long) {
        viewModelScope.launch {
            repository.addTagToDocument(documentId, tagId)
            _statusMessage.value = "تم ربط الوسم بالمستند"
        }
    }

    fun removeTagFromDocument(documentId: Long, tagId: Long) {
        viewModelScope.launch {
            repository.removeTagFromDocument(documentId, tagId)
            _statusMessage.value = "تم فك ارتباط الوسم"
        }
    }

    fun getTagsForDocument(documentId: Long): Flow<List<TagEntity>> {
        return repository.getTagsForDocument(documentId)
    }

    fun getDocumentWithTags(docId: Long): Flow<DocumentWithTags?> {
        return repository.getDocumentWithTags(docId)
    }

    // --- Google Auth & Sync Operations ---
    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = userManager.signInWithGoogle(activityContext)
            _isLoading.value = false
            result.onSuccess { profile ->
                _statusMessage.value = "تم تسجيل الدخول بنجاح بحساب: ${profile.email}"
                userManager.updateSyncStats(allDocuments.value.size)
            }.onFailure { err ->
                _statusMessage.value = "تعذر تسجيل الدخول: ${err.localizedMessage}"
            }
        }
    }

    fun signInDirectly(email: String, name: String = "Google User") {
        userManager.signInDirectly(email, name)
        _statusMessage.value = "تم ربط الحساب: $email"
        userManager.updateSyncStats(allDocuments.value.size)
    }

    fun signOutGoogle() {
        viewModelScope.launch {
            userManager.signOut()
            _statusMessage.value = "تم تسجيل الخروج بنجاح"
        }
    }

    // --- OCR Functions (Gemini API Integration) ---
    fun runOcrOnImage(imageFile: File, lang: String = "ar_en") {
        viewModelScope.launch {
            _ocrState.value = "ANALYZING"
            // First call the dedicated Gemini OCR Service (gemini-3.5-flash)
            val geminiResult = withContext(Dispatchers.IO) {
                geminiOcrService.extractTextFromFile(imageFile, lang)
            }
            when (geminiResult) {
                is GeminiOcrResult.Success -> {
                    _ocrState.value = "SUCCESS"
                    _extractedOcrText.value = geminiResult.text
                    // Save to Room OCR database
                    val ocrRecord = OcrEntity(
                        docId = _activeDocument.value?.id ?: 0L,
                        title = "OCR_${imageFile.nameWithoutExtension}",
                        extractedText = geminiResult.text,
                        language = geminiResult.detectedLanguage,
                        imagePath = imageFile.absolutePath
                    )
                    repository.addOcr(ocrRecord)
                    _statusMessage.value = "تم استخراج النص بنجاح (${geminiResult.wordCount} كلمة)"
                }
                is GeminiOcrResult.Error -> {
                    // Fallback to secondary OCR engine if needed
                    val fallbackResult = withContext(Dispatchers.IO) {
                        OcrEngine.recognizeText(imageFile, lang)
                    }
                    when (fallbackResult) {
                        is OcrResult.Success -> {
                            _ocrState.value = "SUCCESS"
                            _extractedOcrText.value = fallbackResult.text
                            val ocrRecord = OcrEntity(
                                docId = _activeDocument.value?.id ?: 0L,
                                title = "OCR_${imageFile.nameWithoutExtension}",
                                extractedText = fallbackResult.text,
                                language = lang,
                                imagePath = imageFile.absolutePath
                            )
                            repository.addOcr(ocrRecord)
                        }
                        is OcrResult.Error -> {
                            _ocrState.value = "ERROR"
                            _statusMessage.value = geminiResult.message
                        }
                    }
                }
            }
        }
    }

    fun clearOcrResult() {
        _ocrState.value = null
        _extractedOcrText.value = null
    }

    // --- Tools Operations ---
    fun mergePdfs(files: List<File>, outputName: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val context = getApplication<Application>()
            val dir = File(context.filesDir, "documents")
            val targetFile = File(dir, "$outputName.pdf")
            val success = PdfToolsEngine.mergePdfs(files, targetFile)
            if (success) {
                val doc = DocumentEntity(
                    title = outputName,
                    originalFileName = "$outputName.pdf",
                    filePath = targetFile.absolutePath,
                    fileType = "PDF",
                    mimeType = "application/pdf",
                    fileSizeBytes = targetFile.length(),
                    pageCount = files.size
                )
                repository.insertDocument(doc)
                _statusMessage.value = "تم دمج ملفات PDF بنجاح"
            } else {
                _statusMessage.value = "تعذر دمج الملفات"
            }
            _isLoading.value = false
            onDone(success)
        }
    }

    fun imagesToPdf(imageFiles: List<File>, outputName: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val context = getApplication<Application>()
            val dir = File(context.filesDir, "documents")
            val targetFile = File(dir, "$outputName.pdf")
            val success = PdfToolsEngine.imagesToPdf(imageFiles, targetFile)
            if (success) {
                val doc = DocumentEntity(
                    title = outputName,
                    originalFileName = "$outputName.pdf",
                    filePath = targetFile.absolutePath,
                    fileType = "PDF",
                    mimeType = "application/pdf",
                    fileSizeBytes = targetFile.length(),
                    pageCount = imageFiles.size
                )
                repository.insertDocument(doc)
                _statusMessage.value = "تم إنشاء ملف PDF من الصور بنجاح"
            } else {
                _statusMessage.value = "تعذر تحويل الصور إلى PDF"
            }
            _isLoading.value = false
            onDone(success)
        }
    }

    fun exportActivePdfWithAnnotations(outputTitle: String, onDone: (File?) -> Unit) {
        val doc = _activeDocument.value ?: return
        if (doc.fileType != "PDF") return
        viewModelScope.launch {
            _isLoading.value = true
            val sourceFile = File(doc.filePath)
            val outputFile = fileStorageManager.createExportFile(outputTitle, "pdf")

            val success = PdfToolsEngine.exportPdfWithAnnotations(
                sourceFile,
                _activeHighlights.value,
                _activeSignatures.value,
                outputFile
            )
            _isLoading.value = false
            if (success) {
                _statusMessage.value = "تم تصدير نسخة PDF الموقعة بنجاح"
                refreshStorageStats()
                onDone(outputFile)
            } else {
                _statusMessage.value = "تعذر تصدير الملف"
                onDone(null)
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val freed = fileStorageManager.clearAllCache() + fileStorageManager.cleanupTemporaryFiles(0L)
            _statusMessage.value = "تم مسح الملفات المؤقتة (${FileStorageManager.formatFileSize(freed)})"
            refreshStorageStats()
        }
    }
}
