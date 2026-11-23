package com.example.ui.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.local.CreationEntity
import com.example.data.repository.PdfLayoutMode
import com.example.model.ImageFilterPreset
import com.example.model.ImageFilterUtils
import com.example.model.TagUtils
import java.io.File
import java.text.DateFormat
import java.util.Date

private enum class GalleryFilter(val label: String) {
    ALL("All"),
    FAVORITES("Favorites"),
    CREATED("Created"),
    REMIXES("Remixes")
}

private enum class GalleryLayoutMode {
    GRID,
    LIST
}

@Composable
fun GalleryScreen(
    creations: List<CreationEntity>,
    isExportingPdf: Boolean,
    onGoToStudio: () -> Unit,
    onSaveToPhotos: (CreationEntity, ImageFilterPreset) -> Unit,
    onSaveAsPng: (CreationEntity, ImageFilterPreset) -> Unit,
    onSaveFilteredCopy: (CreationEntity, ImageFilterPreset) -> Unit,
    onShare: (CreationEntity, ImageFilterPreset) -> Unit,
    onToggleFavorite: (CreationEntity) -> Unit,
    onAddTagsToSelection: (List<CreationEntity>, String) -> Unit,
    onRemoveTag: (CreationEntity, String) -> Unit,
    onExportPdfShare: (List<CreationEntity>, String, PdfLayoutMode) -> Unit,
    onExportPdfSave: (List<CreationEntity>, String, PdfLayoutMode) -> Unit,
    onRemix: (CreationEntity) -> Unit,
    onReusePrompts: (CreationEntity) -> Unit,
    onDelete: (CreationEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var filter by remember { mutableStateOf(GalleryFilter.ALL) }
    var layoutMode by remember { mutableStateOf(GalleryLayoutMode.GRID) }
    var searchQuery by remember { mutableStateOf("") }
    var activeTagFilter by remember { mutableStateOf<String?>(null) }
    var selectedCreationId by remember { mutableStateOf<Long?>(null) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var showPdfExportSheet by remember { mutableStateOf(false) }
    var showBatchTagSheet by remember { mutableStateOf(false) }

    val selectedCreation = remember(creations, selectedCreationId) {
        selectedCreationId?.let { id -> creations.find { it.id == id } }
    }

    val allGalleryTags = remember(creations) {
        creations.flatMap { TagUtils.parseTags(it.tags) }.distinct().sorted()
    }

    val filtered = remember(creations, filter, searchQuery, activeTagFilter) {
        val base = when (filter) {
            GalleryFilter.ALL -> creations
            GalleryFilter.FAVORITES -> creations.filter { it.isFavorite }
            GalleryFilter.CREATED -> creations.filter { !it.isRemix }
            GalleryFilter.REMIXES -> creations.filter { it.isRemix }
        }
        val tagFiltered = if (activeTagFilter != null) {
            base.filter { activeTagFilter in TagUtils.parseTags(it.tags) }
        } else {
            base
        }
        val trimmedQuery = searchQuery.trim().removePrefix("#")
        if (trimmedQuery.isEmpty()) {
            tagFiltered
        } else {
            tagFiltered.filter { creation ->
                creation.prompt.contains(trimmedQuery, ignoreCase = true) ||
                    creation.styleName.contains(trimmedQuery, ignoreCase = true) ||
                    creation.aspectRatioLabel.contains(trimmedQuery, ignoreCase = true) ||
                    creation.tags.contains(trimmedQuery, ignoreCase = true) ||
                    (creation.seed?.toString()?.contains(trimmedQuery) == true)
            }
        }
    }

    LaunchedEffect(creations, allGalleryTags) {
        val validIds = creations.map { it.id }.toSet()
        selectedIds = selectedIds.filter { it in validIds }
        if (activeTagFilter != null && activeTagFilter !in allGalleryTags) {
            activeTagFilter = null
        }
        if (creations.isEmpty()) {
            isSelectionMode = false
            showPdfExportSheet = false
            showBatchTagSheet = false
        }
    }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedIds = emptyList()
    }

    if (creations.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Collections,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(22.dp)
                            .size(40.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.gallery_empty_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.gallery_empty_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onGoToStudio,
                    modifier = Modifier.testTag("empty_gallery_studio_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.go_to_studio))
                }
            }
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            if (isSelectionMode) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                isSelectionMode = false
                                selectedIds = emptyList()
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("gallery_cancel_selection_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Exit multi-select",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "${selectedIds.size} selected",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                val filteredIds = filtered.map { it.id }
                                selectedIds = if (filteredIds.isNotEmpty() && filteredIds.all { it in selectedIds }) {
                                    emptyList()
                                } else {
                                    filteredIds
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.testTag("gallery_select_all_button")
                        ) {
                            val allSelected = filtered.isNotEmpty() && filtered.all { it.id in selectedIds }
                            Text(if (allSelected) "Clear" else "All")
                        }
                        FilledTonalButton(
                            onClick = { showBatchTagSheet = true },
                            enabled = selectedIds.isNotEmpty(),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("gallery_batch_tag_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Label,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.gallery_batch_tags_cta))
                        }
                        Button(
                            onClick = { showPdfExportSheet = true },
                            enabled = selectedIds.isNotEmpty() && !isExportingPdf,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("gallery_export_pdf_button")
                        ) {
                            if (isExportingPdf) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.PictureAsPdf,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF")
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GalleryFilter.entries.forEach { entry ->
                            FilterChip(
                                selected = filter == entry,
                                onClick = { filter = entry },
                                label = { Text(entry.label) },
                                leadingIcon = if (entry == GalleryFilter.FAVORITES) {
                                    {
                                        Icon(
                                            imageVector = if (filter == entry) {
                                                Icons.Rounded.Favorite
                                            } else {
                                                Icons.Rounded.FavoriteBorder
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else {
                                    null
                                },
                                modifier = Modifier.testTag("gallery_filter_${entry.name.lowercase()}")
                            )
                        }
                    }

                    FilledTonalIconButton(
                        onClick = {
                            layoutMode = if (layoutMode == GalleryLayoutMode.GRID) {
                                GalleryLayoutMode.LIST
                            } else {
                                GalleryLayoutMode.GRID
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("gallery_layout_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (layoutMode == GalleryLayoutMode.GRID) {
                                Icons.Rounded.ViewAgenda
                            } else {
                                Icons.Rounded.GridView
                            },
                            contentDescription = stringResource(R.string.cd_toggle_gallery_layout)
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            isSelectionMode = true
                            selectedIds = emptyList()
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("gallery_select_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Checklist,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.gallery_select_pdf),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // Search bar for prompts, styles, seeds, and custom tags
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.gallery_search_tags_hint),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("gallery_search_input")
                )

                if (allGalleryTags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        allGalleryTags.forEach { tag ->
                            val isTagSelected = activeTagFilter == tag
                            FilterChip(
                                selected = isTagSelected,
                                onClick = {
                                    activeTagFilter = if (isTagSelected) null else tag
                                },
                                label = { Text("#$tag") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Label,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                modifier = Modifier.testTag("gallery_tag_chip_$tag")
                            )
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (filter == GalleryFilter.FAVORITES) {
                                Icons.Rounded.FavoriteBorder
                            } else {
                                Icons.Rounded.Collections
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotBlank() || activeTagFilter != null) {
                                "No artworks match your current search or tag filter."
                            } else if (filter == GalleryFilter.FAVORITES) {
                                stringResource(R.string.gallery_favorites_empty)
                            } else {
                                "No artworks match the ${filter.label.lowercase()} filter yet."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (layoutMode == GalleryLayoutMode.GRID) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 164.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("gallery_grid_view"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.id }) { creation ->
                        val selectionOrder = selectedIds.indexOf(creation.id)
                        val isSelected = selectionOrder >= 0
                        SwipeableGalleryCreationItem(
                            creation = creation,
                            isListView = false,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            selectionNumber = if (isSelected) selectionOrder + 1 else null,
                            onClick = {
                                if (isSelectionMode) {
                                    selectedIds = if (isSelected) {
                                        selectedIds - creation.id
                                    } else {
                                        selectedIds + creation.id
                                    }
                                } else {
                                    selectedCreationId = creation.id
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                    selectedIds = listOf(creation.id)
                                }
                            },
                            onShareClick = { onShare(creation, ImageFilterPreset.ORIGINAL) },
                            onDelete = {
                                if (selectedCreationId == creation.id) {
                                    selectedCreationId = null
                                }
                                onDelete(creation)
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("gallery_list_view"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.id }) { creation ->
                        val selectionOrder = selectedIds.indexOf(creation.id)
                        val isSelected = selectionOrder >= 0
                        SwipeableGalleryCreationItem(
                            creation = creation,
                            isListView = true,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            selectionNumber = if (isSelected) selectionOrder + 1 else null,
                            onClick = {
                                if (isSelectionMode) {
                                    selectedIds = if (isSelected) {
                                        selectedIds - creation.id
                                    } else {
                                        selectedIds + creation.id
                                    }
                                } else {
                                    selectedCreationId = creation.id
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                    selectedIds = listOf(creation.id)
                                }
                            },
                            onShareClick = { onShare(creation, ImageFilterPreset.ORIGINAL) },
                            onDelete = {
                                if (selectedCreationId == creation.id) {
                                    selectedCreationId = null
                                }
                                onDelete(creation)
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }

    selectedCreation?.let { item ->
        CreationDetailSheet(
            creation = item,
            onDismiss = { selectedCreationId = null },
            onSaveToPhotos = { filterPreset -> onSaveToPhotos(item, filterPreset) },
            onSaveAsPng = { filterPreset -> onSaveAsPng(item, filterPreset) },
            onSaveFilteredCopy = { filterPreset -> onSaveFilteredCopy(item, filterPreset) },
            onShare = { filterPreset -> onShare(item, filterPreset) },
            onToggleFavorite = { onToggleFavorite(item) },
            onRemoveTag = { tag -> onRemoveTag(item, tag) },
            onRemix = {
                selectedCreationId = null
                onRemix(item)
            },
            onReusePrompts = {
                selectedCreationId = null
                onReusePrompts(item)
            },
            onDelete = {
                selectedCreationId = null
                onDelete(item)
            }
        )
    }

    if (showBatchTagSheet) {
        val selectedCreations = remember(creations, selectedIds) {
            val map = creations.associateBy { it.id }
            selectedIds.mapNotNull { map[it] }
        }
        BatchTagEditorSheet(
            selectedCount = selectedCreations.size,
            existingGalleryTags = allGalleryTags,
            onDismiss = { showBatchTagSheet = false },
            onApplyTags = { rawTags ->
                showBatchTagSheet = false
                isSelectionMode = false
                selectedIds = emptyList()
                onAddTagsToSelection(selectedCreations, rawTags)
            }
        )
    }

    if (showPdfExportSheet) {
        val selectedCreations = remember(creations, selectedIds) {
            val map = creations.associateBy { it.id }
            selectedIds.mapNotNull { map[it] }
        }
        PdfPortfolioExportSheet(
            selectedCount = selectedCreations.size,
            isExporting = isExportingPdf,
            onDismiss = { showPdfExportSheet = false },
            onSharePdf = { title, mode ->
                showPdfExportSheet = false
                isSelectionMode = false
                selectedIds = emptyList()
                onExportPdfShare(selectedCreations, title, mode)
            },
            onSavePdf = { title, mode ->
                showPdfExportSheet = false
                isSelectionMode = false
                selectedIds = emptyList()
                onExportPdfSave(selectedCreations, title, mode)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableGalleryCreationItem(
    creation: CreationEntity,
    isListView: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    selectionNumber: Int?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onShareClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnDelete by rememberUpdatedState(onDelete)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            !isSelectionMode && value == SwipeToDismissBoxValue.EndToStart
        }
    )

    LaunchedEffect(dismissState.currentValue) {
        if (!isSelectionMode && dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            currentOnDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        gesturesEnabled = !isSelectionMode,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !isSelectionMode,
        modifier = modifier
            .fillMaxWidth()
            .testTag("gallery_swipe_item_${creation.id}"),
        backgroundContent = {
            val isTriggered = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
            val containerColor by animateColorAsState(
                targetValue = if (isTriggered) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                label = "swipe_delete_bg"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isTriggered) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                label = "swipe_delete_fg"
            )
            val iconScale by animateFloatAsState(
                targetValue = if (isTriggered) 1.15f else 0.9f,
                label = "swipe_delete_icon_scale"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(containerColor)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.cd_swipe_delete_artwork),
                        tint = contentColor,
                        modifier = Modifier
                            .size(26.dp)
                            .scale(iconScale)
                    )
                    Text(
                        text = stringResource(R.string.delete_artwork),
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor
                    )
                }
            }
        }
    ) {
        if (isListView) {
            GalleryListCreationCard(
                creation = creation,
                isSelectionMode = isSelectionMode,
                isSelected = isSelected,
                selectionNumber = selectionNumber,
                onClick = onClick,
                onLongClick = onLongClick,
                onShareClick = onShareClick
            )
        } else {
            GalleryCreationCard(
                creation = creation,
                isSelectionMode = isSelectionMode,
                isSelected = isSelected,
                selectionNumber = selectionNumber,
                onClick = onClick,
                onLongClick = onLongClick,
                onShareClick = onShareClick
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun GalleryListCreationCard(
    creation: CreationEntity,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    selectionNumber: Int?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val context = LocalContext.current
    val imageRequest = remember(creation.imagePath) {
        ImageRequest.Builder(context)
            .data(File(creation.imagePath))
            .crossfade(true)
            .build()
    }
    val parsedTags = remember(creation.tags) { TagUtils.parseTags(creation.tags) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("gallery_list_card_${creation.id}"),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (isSelected) 2.5.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                SubcomposeAsyncImage(
                    model = imageRequest,
                    contentDescription = creation.prompt,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (creation.isFavorite) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.94f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = "Favorite artwork",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(12.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = creation.styleName.ifBlank { "No Style" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = creation.aspectRatioLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (creation.seed != null) {
                            Text(
                                text = "#${creation.seed}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    if (isSelectionMode) {
                        Surface(
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            shape = CircleShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (selectionNumber != null) {
                                    Text(
                                        text = selectionNumber.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    } else {
                        IconButton(
                            onClick = onShareClick,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = stringResource(R.string.share_artwork),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Text(
                    text = creation.prompt,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                if (parsedTags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        parsedTags.take(4).forEach { tag ->
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "#$tag",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryCreationCard(
    creation: CreationEntity,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    selectionNumber: Int?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val context = LocalContext.current
    val imageRequest = remember(creation.imagePath) {
        ImageRequest.Builder(context)
            .data(File(creation.imagePath))
            .crossfade(true)
            .build()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("gallery_card_${creation.id}"),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (isSelected) 2.5.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = creation.prompt,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BrokenImage,
                            contentDescription = "Missing image file",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.78f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.62f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = creation.aspectRatioLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
                if (creation.isFavorite) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.94f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = "Favorite artwork",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier
                                .padding(horizontal = 5.dp, vertical = 4.dp)
                                .size(12.dp)
                        )
                    }
                }
                if (creation.negativePrompt.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Block,
                                contentDescription = "Has negative prompt",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "NEG",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            if (isSelectionMode) {
                Surface(
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Black.copy(alpha = 0.55f)
                    },
                    shape = CircleShape,
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.White
                        }
                    ),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .testTag("gallery_select_badge_${creation.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (selectionNumber != null) {
                            Text(
                                text = selectionNumber.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            } else {
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .testTag("gallery_quick_share_${creation.id}")
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.62f),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = stringResource(R.string.share_artwork),
                            tint = Color.White,
                            modifier = Modifier
                                .padding(7.dp)
                                .size(16.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = creation.prompt,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CreationDetailSheet(
    creation: CreationEntity,
    onDismiss: () -> Unit,
    onSaveToPhotos: (ImageFilterPreset) -> Unit,
    onSaveAsPng: (ImageFilterPreset) -> Unit,
    onSaveFilteredCopy: (ImageFilterPreset) -> Unit,
    onShare: (ImageFilterPreset) -> Unit,
    onToggleFavorite: () -> Unit,
    onRemoveTag: (String) -> Unit,
    onRemix: () -> Unit,
    onReusePrompts: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateLabel = remember(creation.createdAt) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(creation.createdAt))
    }
    var activeFilter by remember(creation.id) { mutableStateOf(ImageFilterPreset.ORIGINAL) }
    val activeColorFilter = remember(activeFilter) {
        ImageFilterUtils.colorMatrixValues(activeFilter)?.let { ColorFilter.colorMatrix(ColorMatrix(it)) }
    }
    val parsedTags = remember(creation.tags) { TagUtils.parseTags(creation.tags) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (creation.isRemix) "Image Remix" else "Generated Artwork",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "${creation.modelId} • ${creation.aspectRatioLabel} • $dateLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconToggleButton(
                        checked = creation.isFavorite,
                        onCheckedChange = { onToggleFavorite() },
                        colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
                            checkedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            checkedContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("detail_favorite_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (creation.isFavorite) {
                                Icons.Rounded.Favorite
                            } else {
                                Icons.Rounded.FavoriteBorder
                            },
                            contentDescription = if (creation.isFavorite) {
                                stringResource(R.string.cd_unfavorite_artwork)
                            } else {
                                stringResource(R.string.cd_favorite_artwork)
                            }
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close artwork details")
                    }
                }
            }

            val detailContext = LocalContext.current
            val detailImageRequest = remember(creation.imagePath) {
                ImageRequest.Builder(detailContext)
                    .data(File(creation.imagePath))
                    .crossfade(true)
                    .build()
            }
            var showMetadataOverlay by remember(creation.id) { mutableStateOf(true) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .clickable { showMetadataOverlay = !showMetadataOverlay }
                    .testTag("detail_image_container")
            ) {
                AsyncImage(
                    model = detailImageRequest,
                    contentDescription = creation.prompt,
                    contentScale = ContentScale.Fit,
                    colorFilter = activeColorFilter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = 380.dp)
                )

                // Top-end toggle button to show/hide metadata overlay
                IconButton(
                    onClick = { showMetadataOverlay = !showMetadataOverlay },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(48.dp)
                        .testTag("detail_metadata_overlay_toggle")
                ) {
                    Surface(
                        color = if (showMetadataOverlay) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f)
                        } else {
                            Color.Black.copy(alpha = 0.62f)
                        },
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = stringResource(R.string.cd_toggle_metadata_overlay),
                            tint = if (showMetadataOverlay) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                Color.White
                            },
                            modifier = Modifier
                                .padding(8.dp)
                                .size(18.dp)
                        )
                    }
                }

                // Bottom frosted gradient metadata overlay showing style, aspect ratio & original prompt
                androidx.compose.animation.AnimatedVisibility(
                    visible = showMetadataOverlay,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.74f),
                                        Color.Black.copy(alpha = 0.90f)
                                    )
                                )
                            )
                            .padding(start = 14.dp, end = 14.dp, top = 28.dp, bottom = 14.dp)
                            .testTag("detail_metadata_overlay")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("detail_overlay_style")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Palette,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = creation.styleName.ifBlank { "No Style" },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                Surface(
                                    color = Color.White.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                    modifier = Modifier.testTag("detail_overlay_aspect_ratio")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AspectRatio,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = creation.aspectRatioLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                }

                                if (creation.seed != null) {
                                    Surface(
                                        color = Color.White.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                        modifier = Modifier.testTag("detail_overlay_seed")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Pin,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "Seed ${creation.seed}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "ORIGINAL PROMPT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                                Text(
                                    text = creation.prompt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("detail_overlay_prompt")
                                )
                            }
                        }
                    }
                }
            }

            // Post-processing Visual Effects Filter Layer
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.detail_filters_title),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        if (activeFilter != ImageFilterPreset.ORIGINAL) {
                            TextButton(
                                onClick = { onSaveFilteredCopy(activeFilter) },
                                modifier = Modifier.testTag("detail_save_filtered_copy_button")
                            ) {
                                Text(stringResource(R.string.save_filtered_copy))
                            }
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ImageFilterPreset.entries) { preset ->
                            FilterChip(
                                selected = activeFilter == preset,
                                onClick = { activeFilter = preset },
                                label = { Text(preset.label) },
                                modifier = Modifier.testTag("detail_filter_${preset.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onSaveToPhotos(activeFilter) },
                    modifier = Modifier
                        .weight(1.25f)
                        .minimumInteractiveComponentSize()
                        .testTag("detail_save_to_pictures_button")
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.save_to_photos),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                FilledTonalButton(
                    onClick = { onSaveAsPng(activeFilter) },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("detail_save_png_button")
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.save_as_png),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onShare(activeFilter) },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("detail_share_button")
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.share_artwork))
                }
                OutlinedButton(
                    onClick = onRemix,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.remix_this))
                }
                OutlinedButton(
                    onClick = onReusePrompts,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reuse",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Style: ${creation.styleName.ifBlank { "No Style" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Ratio: ${creation.aspectRatioLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        if (creation.seed != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Seed: ${creation.seed}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "POSITIVE PROMPT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = creation.prompt,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (creation.negativePrompt.isNotBlank()) {
                        Text(
                            text = "NEGATIVE PROMPT (EXCLUDED)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = creation.negativePrompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (parsedTags.isNotEmpty()) {
                        Text(
                            text = "CUSTOM TAGS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            parsedTags.forEach { tag ->
                                AssistChip(
                                    onClick = { onRemoveTag(tag) },
                                    label = { Text("#$tag") },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Remove tag $tag",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            TextButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.delete_artwork),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PdfPortfolioExportSheet(
    selectedCount: Int,
    isExporting: Boolean,
    onDismiss: () -> Unit,
    onSharePdf: (title: String, layoutMode: PdfLayoutMode) -> Unit,
    onSavePdf: (title: String, layoutMode: PdfLayoutMode) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf("Lumina Studio Portfolio") }
    var layoutMode by remember { mutableStateOf(PdfLayoutMode.PORTFOLIO) }

    val pageCount = remember(selectedCount, layoutMode) {
        when (layoutMode) {
            PdfLayoutMode.PORTFOLIO -> selectedCount.coerceAtLeast(1)
            PdfLayoutMode.CONTACT_SHEET -> ((selectedCount + 3) / 4).coerceAtLeast(1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Export PDF Portfolio",
                            style = MaterialTheme.typography.titleLarge
                        )
                        val artworkWord = if (selectedCount == 1) "artwork" else "artworks"
                        val pageWord = if (pageCount == 1) "page" else "pages"
                        Text(
                            text = "$selectedCount $artworkWord • $pageCount A4 $pageWord",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close PDF export options"
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Portfolio Title") },
                placeholder = { Text("Lumina Studio Portfolio") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pdf_title_input")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Page Layout",
                    style = MaterialTheme.typography.titleSmall
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    PdfLayoutMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = layoutMode == mode,
                            onClick = { layoutMode = mode },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = PdfLayoutMode.entries.size
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("pdf_layout_${mode.name.lowercase()}")
                        ) {
                            Text(mode.label)
                        }
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = layoutMode.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onSavePdf(title, layoutMode) },
                    enabled = selectedCount > 0 && !isExporting,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("pdf_save_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.save_pdf_cta))
                }
                Button(
                    onClick = { onSharePdf(title, layoutMode) },
                    enabled = selectedCount > 0 && !isExporting,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("pdf_share_button")
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.share_pdf_cta))
                }
            }
        }
    }
}
