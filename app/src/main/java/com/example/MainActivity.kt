package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.CameraScanDialog
import com.example.ui.components.ImportBottomSheet
import com.example.ui.components.NashraBottomBar
import com.example.ui.components.NashraTopBar
import com.example.ui.locale.LocalizedStrings
import com.example.ui.navigation.Screen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingIntent(intent)

        setContent {
            val userSettings by viewModel.userSettings.collectAsState()
            val isDark = when (userSettings.themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            val layoutDirection = if (userSettings.language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
            val strings = LocalizedStrings.get(userSettings.language)

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MyApplicationTheme(darkTheme = isDark) {
                    NashraDocsApp(viewModel = viewModel, strings = strings)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action

        when (action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    viewModel.importFileFromUri(uri)
                }
            }
            Intent.ACTION_SEND -> {
                (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: intent.data)?.let { uri ->
                    viewModel.importFileFromUri(uri)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NashraDocsApp(
    viewModel: MainViewModel,
    strings: com.example.ui.locale.AppStrings
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.HOME.route

    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showImportSheet by remember { mutableStateOf(false) }
    var showCameraScanDialog by remember { mutableStateOf(false) }
    var showCameraPermissionRationale by remember { mutableStateOf(false) }

    // Text Doc Creation Dialog
    var showCreateTextDialog by remember { mutableStateOf(false) }
    var textDocTitle by remember { mutableStateOf("") }
    var textDocContent by remember { mutableStateOf("") }

    // Share Info Dialog
    var showShareInfoDialog by remember { mutableStateOf(false) }

    // Document Picker
    val docPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importFileFromUri(uri)
        }
    }

    // Image Picker
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.importFileFromUri(uri)
        }
    }

    // Camera Permission Launcher using Activity Result API
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCameraScanDialog = true
        } else {
            showCameraPermissionRationale = true
        }
    }

    // Function to launch CameraX scan with permission check
    val requestCameraAndScan = {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            showCameraScanDialog = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Keep NavController synchronized with ViewModel screen state
    LaunchedEffect(currentScreen) {
        val targetRoute = currentScreen.route
        if (currentRoute != targetRoute) {
            navController.navigate(targetRoute) {
                popUpTo(Screen.HOME.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // Status Message Snackbar
    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            NashraTopBar(
                currentScreen = currentScreen,
                strings = strings,
                searchQuery = searchQuery,
                onSearchChange = { viewModel.setSearchQuery(it) },
                onImportClick = { showImportSheet = true },
                onBackClick = {
                    if (currentScreen != Screen.HOME) {
                        viewModel.navigateTo(Screen.HOME)
                    }
                }
            )
        },
        bottomBar = {
            NashraBottomBar(
                currentScreen = currentScreen,
                strings = strings,
                onScreenSelected = { targetScreen ->
                    viewModel.navigateTo(targetScreen)
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.HOME.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.HOME.route) {
                HomeScreen(
                    viewModel = viewModel,
                    strings = strings,
                    onImportClick = { showImportSheet = true }
                )
            }
            composable(Screen.LIBRARY.route) {
                LibraryScreen(
                    viewModel = viewModel,
                    strings = strings,
                    onImportClick = { showImportSheet = true }
                )
            }
            composable(Screen.READER.route) {
                ReaderScreen(
                    viewModel = viewModel,
                    strings = strings,
                    onBack = { viewModel.navigateTo(Screen.HOME) }
                )
            }
            composable(Screen.TOOLS.route) {
                ToolsScreen(
                    viewModel = viewModel,
                    strings = strings
                )
            }
            composable(Screen.SETTINGS.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    strings = strings
                )
            }
        }
    }

    // CameraX Live Document Scanner Dialog
    if (showCameraScanDialog) {
        CameraScanDialog(
            strings = strings,
            onDismiss = { showCameraScanDialog = false },
            onSaveToLibrary = { file ->
                viewModel.importFileFromUri(Uri.fromFile(file))
            },
            onPerformOcr = { file ->
                viewModel.runOcrOnImage(file)
                viewModel.importFileFromUri(Uri.fromFile(file))
            }
        )
    }

    // Camera Permission Rationale Dialog
    if (showCameraPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showCameraPermissionRationale = false },
            shape = RoundedCornerShape(18.dp),
            title = { Text("إذن الكاميرا مطلوب", fontWeight = FontWeight.Bold) },
            text = {
                Text("يتطلب مسح المستندات ضوئياً واستخراج النصوص (OCR) السماح لتطبيق Nashra Docs بالوصول إلى الكاميرا. يرجى منح الإذن للمتابعة.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCameraPermissionRationale = false
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("منح الإذن")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCameraPermissionRationale = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Import Modal Bottom Sheet
    if (showImportSheet) {
        ImportBottomSheet(
            sheetState = sheetState,
            strings = strings,
            onDismiss = { showImportSheet = false },
            onPickDocument = {
                docPicker.launch(
                    arrayOf(
                        "application/pdf",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                        "application/vnd.ms-powerpoint",
                        "text/plain",
                        "text/csv",
                        "image/*",
                        "*/*"
                    )
                )
            },
            onPickImage = {
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onCaptureCamera = {
                showImportSheet = false
                requestCameraAndScan()
            },
            onCreateText = {
                textDocTitle = ""
                textDocContent = ""
                showCreateTextDialog = true
            },
            onShareInfo = {
                showShareInfoDialog = true
            }
        )
    }

    // Create Text Document Dialog
    if (showCreateTextDialog) {
        AlertDialog(
            onDismissRequest = { showCreateTextDialog = false },
            title = { Text(strings.createTextDoc, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = textDocTitle,
                        onValueChange = { textDocTitle = it },
                        label = { Text("عنوان المستند") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = textDocContent,
                        onValueChange = { textDocContent = it },
                        label = { Text("المحتوى النصي") },
                        minLines = 6,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createNewTextDocument(textDocTitle, textDocContent)
                        showCreateTextDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCreateTextDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Share Info Dialog
    if (showShareInfoDialog) {
        AlertDialog(
            onDismissRequest = { showShareInfoDialog = false },
            title = { Text(strings.importFromShare, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "تطبيق Nashra Docs جاهز لاستقبال الملفات والمستندات مباشرةً:\n\n" +
                            "1. افتح أي ملف PDF أو مستند في واتساب، تيليجرام أو البريد.\n" +
                            "2. اضغط على زر 'مشاركة' أو 'فتح باستخدام'.\n" +
                            "3. اختر تطبيق Nashra Docs وسيتم فتحه واستيراده تلقائياً في مكتبتك."
                )
            },
            confirmButton = {
                Button(
                    onClick = { showShareInfoDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("فهمت")
                }
            }
        )
    }
}
