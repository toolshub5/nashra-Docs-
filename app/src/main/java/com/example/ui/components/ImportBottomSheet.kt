package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.locale.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportBottomSheet(
    sheetState: SheetState,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onPickDocument: () -> Unit,
    onPickImage: () -> Unit,
    onCaptureCamera: () -> Unit,
    onCreateText: () -> Unit,
    onShareInfo: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("import_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = strings.importFile,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            ImportOptionItem(
                title = strings.chooseFromStorage,
                subtitle = "PDF, Word, Excel, PowerPoint, TXT, CSV",
                icon = Icons.Default.FolderOpen,
                iconColor = Color(0xFF0F766E),
                onClick = {
                    onDismiss()
                    onPickDocument()
                },
                testTag = "import_option_storage"
            )

            ImportOptionItem(
                title = strings.chooseFromGallery,
                subtitle = "JPG, PNG, WEBP",
                icon = Icons.Default.Image,
                iconColor = Color(0xFF0284C7),
                onClick = {
                    onDismiss()
                    onPickImage()
                },
                testTag = "import_option_gallery"
            )

            ImportOptionItem(
                title = strings.captureCamera,
                subtitle = "التقاط سريع ومسح ضوئي OCR",
                icon = Icons.Default.CameraAlt,
                iconColor = Color(0xFFE11D48),
                onClick = {
                    onDismiss()
                    onCaptureCamera()
                },
                testTag = "import_option_camera"
            )

            ImportOptionItem(
                title = strings.createTextDoc,
                subtitle = "محرر نصي محلي وحفظ فوري",
                icon = Icons.Default.EditNote,
                iconColor = Color(0xFFD97706),
                onClick = {
                    onDismiss()
                    onCreateText()
                },
                testTag = "import_option_text"
            )

            ImportOptionItem(
                title = strings.importFromShare,
                subtitle = "استقبال مباشر من واتساب، البريد والتطبيقات",
                icon = Icons.Default.Share,
                iconColor = Color(0xFF7C3AED),
                onClick = {
                    onDismiss()
                    onShareInfo()
                },
                testTag = "import_option_share"
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ImportOptionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
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
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
