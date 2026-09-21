package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentWithTags
import com.example.data.model.TagEntity
import com.example.ui.components.DocumentCard
import com.example.ui.components.DocumentDetailsDialog
import com.example.ui.components.GoogleSignInCard
import com.example.ui.components.SearchableFilterBar
import com.example.ui.components.StatsCard
import com.example.ui.locale.AppStrings
import com.example.ui.navigation.Screen
import com.example.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    strings: AppStrings,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allDocs by viewModel.allDocuments.collectAsState()
    val recentDocs by viewModel.recentDocuments.collectAsState()
    val filteredDocsWithTags by viewModel.filteredDocumentsWithTags.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val selectedTagId by viewModel.selectedTagFilterId.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var docToRename by remember { mutableStateOf<DocumentEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var docToDelete by remember { mutableStateOf<DocumentEntity?>(null) }
    var docForTagDetails by remember { mutableStateOf<DocumentWithTags?>(null) }

    val categories = listOf(
        Pair("ALL", strings.catAll),
        Pair("PDF", strings.catPdf),
        Pair("WORD", strings.catWord),
        Pair("EXCEL", strings.catExcel),
        Pair("PPTX", strings.catPptx),
        Pair("IMAGE", strings.catImages),
        Pair("TXT", strings.catTexts)
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Google Sign-In & Sync Card
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

        // 2. Searchable Filter Bar (Search by name, type, or tags)
        item {
            SearchableFilterBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                selectedCategory = selectedCategory,
                onCategorySelected = { viewModel.setCategory(it) },
                availableTags = allTags,
                selectedTagId = selectedTagId,
                onTagSelected = { viewModel.setSelectedTagFilter(it) },
                categories = categories
            )
        }

        // 3. Library Overview Stats Card
        item {
            StatsCard(documents = allDocs, strings = strings)
        }

        // 4. Recently Opened Section (if any)
        if (recentDocs.isNotEmpty() && searchQuery.isBlank() && selectedCategory == "ALL" && selectedTagId == 0L) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = strings.recentFiles,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TextButton(onClick = { viewModel.navigateTo(Screen.LIBRARY) }) {
                        Text(
                            text = strings.navLibrary,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            items(recentDocs.take(3), key = { "recent_${it.id}" }) { doc ->
                DocumentCard(
                    document = doc,
                    strings = strings,
                    onOpen = { viewModel.openDocument(doc) },
                    onToggleFavorite = { viewModel.toggleFavorite(doc) },
                    onRename = {
                        docToRename = doc
                        renameText = doc.title
                    },
                    onDelete = { docToDelete = doc }
                )
            }
        }

        // 5. Filtered Documents Section with Tags
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderSpecial,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (searchQuery.isNotBlank() || selectedCategory != "ALL" || selectedTagId != 0L)
                            "نتائج البحث والفلترة"
                        else
                            strings.allDocuments,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${filteredDocsWithTags.size} ملف",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (filteredDocsWithTags.isEmpty()) {
            item {
                EmptyLibraryView(strings = strings, onImportClick = onImportClick)
            }
        } else {
            items(filteredDocsWithTags, key = { "doc_${it.document.id}" }) { item ->
                DocumentCard(
                    document = item.document,
                    strings = strings,
                    tags = item.tags,
                    onOpen = { viewModel.openDocument(item.document) },
                    onToggleFavorite = { viewModel.toggleFavorite(item.document) },
                    onRename = {
                        docToRename = item.document
                        renameText = item.document.title
                    },
                    onDelete = { docToDelete = item.document },
                    onShowDetails = { docForTagDetails = item }
                )
            }
        }
    }

    // Document Details & Tags Management Dialog
    docForTagDetails?.let { item ->
        DocumentDetailsDialog(
            document = item.document,
            attachedTags = item.tags,
            allAvailableTags = allTags,
            onAddTagToDocument = { docId, tagId ->
                viewModel.addTagToDocument(docId, tagId)
            },
            onRemoveTagFromDocument = { docId, tagId ->
                viewModel.removeTagFromDocument(docId, tagId)
            },
            onCreateAndAttachTag = { docId, name, color ->
                viewModel.createTag(name, color)
            },
            onOpenDocument = {
                viewModel.openDocument(item.document)
                docForTagDetails = null
            },
            onDismiss = { docForTagDetails = null }
        )
    }

    // Rename Dialog
    docToRename?.let { doc ->
        AlertDialog(
            onDismissRequest = { docToRename = null },
            shape = RoundedCornerShape(18.dp),
            title = { Text(strings.rename, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("اسم الملف الجديد") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameDocument(doc, renameText)
                        docToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { docToRename = null }, shape = RoundedCornerShape(10.dp)) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    docToDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { docToDelete = null },
            shape = RoundedCornerShape(18.dp),
            title = { Text(strings.confirmDeleteTitle, fontWeight = FontWeight.Bold) },
            text = { Text("${strings.confirmDeleteMsg}\n(${doc.title})") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDocument(doc)
                        docToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(strings.confirm, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { docToDelete = null }, shape = RoundedCornerShape(10.dp)) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
fun EmptyLibraryView(
    strings: AppStrings,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = strings.emptyDocumentsMsg,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onImportClick,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(strings.importFile, fontWeight = FontWeight.Bold)
        }
    }
}
