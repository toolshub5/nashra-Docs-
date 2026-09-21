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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentWithTags
import com.example.ui.components.DocumentCard
import com.example.ui.components.DocumentDetailsDialog
import com.example.ui.locale.AppStrings
import com.example.viewmodel.MainViewModel

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    strings: AppStrings,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredDocsWithTags by viewModel.filteredDocumentsWithTags.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("library_screen")
            .padding(horizontal = 16.dp)
    ) {
        // Controls: Category Filter Chips & Sort / Grid toggles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { (catKey, catLabel) ->
                    val isSelected = selectedCategory == catKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setCategory(catKey) },
                        label = { Text(catLabel, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Sort & View toggles
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.sortByDate) },
                            onClick = {
                                viewModel.setSortOption("DATE")
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.sortByName) },
                            onClick = {
                                viewModel.setSortOption("NAME")
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.sortBySize) },
                            onClick = {
                                viewModel.setSortOption("SIZE")
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.sortByLastOpened) },
                            onClick = {
                                viewModel.setSortOption("LAST_OPENED")
                                showSortMenu = false
                            }
                        )
                    }
                }

                IconButton(onClick = { viewModel.toggleGridView() }) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle View",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Summary banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${strings.allDocuments} (${filteredDocsWithTags.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            val currentSortName = when (sortOption) {
                "NAME" -> strings.sortByName
                "SIZE" -> strings.sortBySize
                "LAST_OPENED" -> strings.sortByLastOpened
                else -> strings.sortByDate
            }
            Text(
                text = "الترتيب: $currentSortName",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredDocsWithTags.isEmpty()) {
            EmptyLibraryView(
                strings = strings,
                onImportClick = onImportClick,
                modifier = Modifier.padding(top = 40.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredDocsWithTags, key = { "lib_${it.document.id}" }) { item ->
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
    }

    // Document Details Dialog
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
