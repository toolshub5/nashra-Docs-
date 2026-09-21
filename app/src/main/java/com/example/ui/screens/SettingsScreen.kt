package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.ui.components.GoogleSignInCard
import com.example.ui.locale.AppStrings
import com.example.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.userSettings.collectAsState()
    val storageUsage by viewModel.storageUsage.collectAsState()
    val storageBreakdown by viewModel.storageBreakdown.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val allDocs by viewModel.allDocuments.collectAsState()

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = strings.settingsTitle,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "تخصيص الواجهة، اللغات، وأنماط القراءة والخصوصية",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 0. Google Account & Cloud Sync
        item {
            GoogleSignInCard(
                userProfile = currentUser,
                isLoading = isLoading,
                totalDocumentsCount = allDocs.size,
                onSignInClick = { viewModel.signInWithGoogle(context) },
                onDirectSignIn = { email, name -> viewModel.signInDirectly(email, name) },
                onSignOutClick = { viewModel.signOutGoogle() }
            )
        }

        // 1. Language Setting
        item {
            SettingsCard(title = strings.languageLabel, icon = Icons.Default.Language) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = settings.language == "ar",
                        onClick = { viewModel.preferencesManager.setLanguage("ar") },
                        label = { Text("العربية (افتراضي)", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f).testTag("lang_ar_chip")
                    )
                    FilterChip(
                        selected = settings.language == "en",
                        onClick = { viewModel.preferencesManager.setLanguage("en") },
                        label = { Text("English", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f).testTag("lang_en_chip")
                    )
                }
            }
        }

        // 2. Theme Setting
        item {
            SettingsCard(title = strings.themeLabel, icon = Icons.Default.DarkMode) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.preferencesManager.setThemeMode("light") }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.themeMode == "light",
                            onClick = { viewModel.preferencesManager.setThemeMode("light") }
                        )
                        Text(strings.themeLight, style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.preferencesManager.setThemeMode("dark") }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.themeMode == "dark",
                            onClick = { viewModel.preferencesManager.setThemeMode("dark") }
                        )
                        Text(strings.themeDark, style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.preferencesManager.setThemeMode("system") }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.themeMode == "system",
                            onClick = { viewModel.preferencesManager.setThemeMode("system") }
                        )
                        Text(strings.themeSystem, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // 3. Reader Typography & Themes
        item {
            SettingsCard(title = "تخصيص القارئ", icon = Icons.Default.MenuBook) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.fontSizeLabel, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${settings.readerFontSize.toInt()} sp",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = settings.readerFontSize,
                        onValueChange = { viewModel.preferencesManager.setReaderFontSize(it) },
                        valueRange = 12f..28f,
                        steps = 8
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(strings.readerColorLabel, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = settings.readerTheme == "light",
                            onClick = { viewModel.preferencesManager.setReaderTheme("light") },
                            label = { Text(strings.themeLight) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = settings.readerTheme == "sepia",
                            onClick = { viewModel.preferencesManager.setReaderTheme("sepia") },
                            label = { Text("سيبيا / Sepia") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = settings.readerTheme == "dark",
                            onClick = { viewModel.preferencesManager.setReaderTheme("dark") },
                            label = { Text(strings.themeDark) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 4. Reading Preferences & Toggles
        item {
            SettingsCard(title = "سلوك التطبيق", icon = Icons.Default.FormatSize) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.keepLastPageLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = settings.keepLastPage,
                            onCheckedChange = { viewModel.preferencesManager.setKeepLastPage(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.confirmDeleteToggleLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = settings.confirmDelete,
                            onCheckedChange = { viewModel.preferencesManager.setConfirmDelete(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }

        // 5. Storage & Cache
        item {
            SettingsCard(title = strings.storageManagerLabel, icon = Icons.Default.CleaningServices) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("المساحة المستخدمة", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("إجمالي حجم المستندات والكاش المحفوظ محلياً", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = storageUsage,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (storageBreakdown.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("PDF", "WORD", "EXCEL", "CACHE").forEach { key ->
                                val size = storageBreakdown[key] ?: "0 B"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$key: $size",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.clearCacheLabel, style = MaterialTheme.typography.bodyMedium)
                            Text("مسح الملفات المؤقتة لتوفير المساحة", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { viewModel.clearCache() },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("مسح الكاش")
                        }
                    }
                }
            }
        }

        // 6. Privacy & Legal Links
        item {
            SettingsCard(title = "الخصوصية والدعم", icon = Icons.Default.PrivacyTip) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPrivacyDialog = true }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(strings.privacyPolicyLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }

                    Divider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTermsDialog = true }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(strings.termsOfServiceLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }

                    Divider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAboutDialog = true }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(strings.appInfoLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Version badge
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${strings.appTitle} • v1.0.0 (Build 1)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text(strings.privacyPolicyLabel, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "تطبيق Nashra Docs يلتزم بالخصوصية التامة للمستخدمين:\n\n" +
                            "• جميع ملفاتك ومستنداتك تُخزن محلياً داخل جهازك فقط.\n" +
                            "• التطبيق لا يقوم برفع أي ملف تلقائياً إلى خوادم خارجية.\n" +
                            "• التوقيع الرقمي والتظليلات تحفظ محلياً ولا يتم تداولها.\n" +
                            "• ميزة التعرف الضوئي OCR تعمل بنظام معالجة آمن دون الاحتفاظ ببياناتك."
                )
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }, shape = RoundedCornerShape(10.dp)) {
                    Text("حسناً")
                }
            }
        )
    }

    // Terms of Service Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text(strings.termsOfServiceLabel, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "شروط الاستخدام لتطبيق Nashra Docs:\n\n" +
                            "• التطبيق مخصص لإدارة وقراءة المستندات الشخصية والمهنية.\n" +
                            "• التوقيع الرقمي داخل التطبيق هو توقيع يدوي/رقمي محلي لتسهيل المعاملات، ولا يدعي أنه شهادة رسمية مشفرة من جهة حكومية.\n" +
                            "• يحق للمستخدم تصدير ومشاركة ملفاته بحرية كاملة."
                )
            },
            confirmButton = {
                Button(onClick = { showTermsDialog = false }, shape = RoundedCornerShape(10.dp)) {
                    Text("موافق")
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text(strings.appInfoLabel, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Nashra Docs | نشرة\n\n" +
                            "تطبيق قارئ وإدارة مستندات احترافي وعصري يدعم جميع صيغ الملفات (PDF, Word, Excel, PowerPoint, Text, Images).\n" +
                            "تم تصميمه بأعلى معايير الأناقة والهدوء البصري مع دعم كامل للغتين العربية والإنجليزية.\n\n" +
                            strings.appVersionLabel
                )
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }, shape = RoundedCornerShape(10.dp)) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
