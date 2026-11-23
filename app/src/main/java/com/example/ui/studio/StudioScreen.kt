package com.example.ui.studio

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.repository.PromptHistoryEntry
import com.example.model.AspectRatioOption
import com.example.model.ImageModelOption
import com.example.model.InspirationCategory
import com.example.model.InspirationTemplate
import com.example.model.NegativePromptPreset
import com.example.model.PromptStarter
import com.example.model.SeedParseResult
import com.example.model.SeedUtils
import com.example.model.StudioCatalog
import com.example.model.StudioMode
import com.example.model.StylePreset
import com.example.model.UpscaleFactor

@Composable
fun StudioScreen(
    composer: StudioComposerState,
    generationState: GenerationUiState,
    enhanceState: EnhanceUiState,
    inspiratorState: PromptInspiratorState,
    recentGenerations: List<RecentGenerationItem>,
    promptHistory: List<PromptHistoryEntry>,
    onSelectRecent: (RecentGenerationItem) -> Unit,
    onRegenerateRecent: (RecentGenerationItem) -> Unit,
    onSaveRecent: (RecentGenerationItem) -> Unit,
    onRecallPromptHistory: (PromptHistoryEntry) -> Unit,
    onDeletePromptHistory: (PromptHistoryEntry) -> Unit,
    onClearPromptHistory: () -> Unit,
    onSelectInspirationCategory: (InspirationCategory) -> Unit,
    onShuffleInspiration: () -> Unit,
    onBrainstormInspiration: () -> Unit,
    onApplyPromptStarter: (PromptStarter) -> Unit,
    onModeChange: (StudioMode) -> Unit,
    onPromptChange: (String) -> Unit,
    onNegativePromptChange: (String) -> Unit,
    onClearNegativePrompt: () -> Unit,
    onToggleNegativePreset: (NegativePromptPreset) -> Unit,
    onOpenNegativeGuide: () -> Unit,
    onSelectStyle: (StylePreset) -> Unit,
    onInsertStyleIntoPrompt: () -> Unit,
    onSelectAspectRatio: (AspectRatioOption) -> Unit,
    onSelectModel: (ImageModelOption) -> Unit,
    onSeedInputChange: (String) -> Unit,
    onRandomizeSeed: () -> Unit,
    onClearSeed: () -> Unit,
    onPickReferencePhoto: () -> Unit,
    onClearReferencePhoto: () -> Unit,
    onEnhancePrompt: () -> Unit,
    onGenerate: () -> Unit,
    onGenerateBatch: () -> Unit,
    onRetryGeneration: () -> Unit,
    onCancelGeneration: () -> Unit,
    onApplyInspiration: (InspirationTemplate) -> Unit,
    onUpscaleResult: (UpscaleFactor) -> Unit,
    onSaveToLocalGallery: () -> Unit,
    onApplyCrop: (Float, Float, Float, Float) -> Unit,
    onResetCrop: () -> Unit,
    onSaveResult: () -> Unit,
    onShareResult: () -> Unit,
    onRemixResult: (Bitmap) -> Unit,
    onOpenBatchVariantInCanvas: (Int) -> Unit,
    onApplyBatchCrop: (Int, Float, Float, Float, Float) -> Unit,
    onSaveBatchVariantToGallery: (Int) -> Unit,
    onSaveAllBatchVariantsToGallery: () -> Unit,
    onSaveBatchVariantToPhotos: (Int) -> Unit,
    onShareBatchVariant: (Int) -> Unit,
    onRetryBatchVariant: (Int) -> Unit,
    onResetCanvas: () -> Unit,
    modifier: Modifier = Modifier
) {
    var zoomBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cropTarget by remember { mutableStateOf<Pair<Int?, Bitmap>?>(null) }
    var inspectedBatchVariantIndex by remember { mutableStateOf<Int?>(null) }
    var showInspiratorSheet by remember { mutableStateOf(false) }

    val activeRecentKey = (generationState as? GenerationUiState.Success)?.let { success ->
        success.draftKey ?: success.savedCreation?.id?.let { "db_$it" }
    }
    val isGenerating = generationState is GenerationUiState.Loading ||
        generationState is GenerationUiState.BatchLoading
    val isBatchGenerating = generationState is GenerationUiState.BatchLoading

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideLayout = maxWidth >= 840.dp

        if (isWideLayout) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AnimatedVisibility(visible = recentGenerations.isNotEmpty()) {
                    RecentGenerationsBar(
                        items = recentGenerations,
                        activeKey = activeRecentKey,
                        isGenerating = isGenerating,
                        onSelect = onSelectRecent,
                        onRegenerate = onRegenerateRecent,
                        onSave = onSaveRecent
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.05f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CanvasViewportSection(
                            composer = composer,
                            generationState = generationState,
                            onPickReferencePhoto = onPickReferencePhoto,
                            onClearReferencePhoto = onClearReferencePhoto,
                            onApplyInspiration = onApplyInspiration,
                            onUpscaleResult = onUpscaleResult,
                            onSaveToLocalGallery = onSaveToLocalGallery,
                            onOpenCrop = { cropTarget = null to it },
                            onResetCrop = onResetCrop,
                            onSaveResult = onSaveResult,
                            onShareResult = onShareResult,
                            onRemixResult = onRemixResult,
                            onZoomBitmap = { zoomBitmap = it },
                            onInspectBatchVariant = { inspectedBatchVariantIndex = it },
                            onOpenBatchVariantInCanvas = onOpenBatchVariantInCanvas,
                            onOpenBatchCrop = { idx, bmp -> cropTarget = idx to bmp },
                            onSaveBatchVariantToGallery = onSaveBatchVariantToGallery,
                            onSaveAllBatchVariantsToGallery = onSaveAllBatchVariantsToGallery,
                            onRetryBatchVariant = onRetryBatchVariant,
                            onRegenerateBatch = onGenerateBatch,
                            onResetCanvas = onResetCanvas,
                            onCancelGeneration = onCancelGeneration,
                            onRetry = onRetryGeneration
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(0.95f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ComposerControlsSection(
                            composer = composer,
                            promptHistory = promptHistory,
                            isGenerating = isGenerating,
                            isBatchGenerating = isBatchGenerating,
                            isEnhancing = enhanceState is EnhanceUiState.Loading,
                            onRecallPromptHistory = onRecallPromptHistory,
                            onDeletePromptHistory = onDeletePromptHistory,
                            onClearPromptHistory = onClearPromptHistory,
                            onOpenInspirator = { showInspiratorSheet = true },
                            onModeChange = onModeChange,
                            onPromptChange = onPromptChange,
                            onNegativePromptChange = onNegativePromptChange,
                            onClearNegativePrompt = onClearNegativePrompt,
                            onToggleNegativePreset = onToggleNegativePreset,
                            onOpenNegativeGuide = onOpenNegativeGuide,
                            onSelectStyle = onSelectStyle,
                            onInsertStyleIntoPrompt = onInsertStyleIntoPrompt,
                            onSelectAspectRatio = onSelectAspectRatio,
                            onSelectModel = onSelectModel,
                            onSeedInputChange = onSeedInputChange,
                            onRandomizeSeed = onRandomizeSeed,
                            onClearSeed = onClearSeed,
                            onPickReferencePhoto = onPickReferencePhoto,
                            onEnhancePrompt = onEnhancePrompt,
                            onGenerate = onGenerate,
                            onGenerateBatch = onGenerateBatch
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnimatedVisibility(visible = recentGenerations.isNotEmpty()) {
                        RecentGenerationsBar(
                            items = recentGenerations,
                            activeKey = activeRecentKey,
                            isGenerating = isGenerating,
                            onSelect = onSelectRecent,
                            onRegenerate = onRegenerateRecent,
                            onSave = onSaveRecent
                        )
                    }

                    CanvasViewportSection(
                        composer = composer,
                        generationState = generationState,
                        onPickReferencePhoto = onPickReferencePhoto,
                        onClearReferencePhoto = onClearReferencePhoto,
                        onApplyInspiration = onApplyInspiration,
                        onUpscaleResult = onUpscaleResult,
                        onSaveToLocalGallery = onSaveToLocalGallery,
                        onOpenCrop = { cropTarget = null to it },
                        onResetCrop = onResetCrop,
                        onSaveResult = onSaveResult,
                        onShareResult = onShareResult,
                        onRemixResult = onRemixResult,
                        onZoomBitmap = { zoomBitmap = it },
                        onInspectBatchVariant = { inspectedBatchVariantIndex = it },
                        onOpenBatchVariantInCanvas = onOpenBatchVariantInCanvas,
                        onOpenBatchCrop = { idx, bmp -> cropTarget = idx to bmp },
                        onSaveBatchVariantToGallery = onSaveBatchVariantToGallery,
                        onSaveAllBatchVariantsToGallery = onSaveAllBatchVariantsToGallery,
                        onRetryBatchVariant = onRetryBatchVariant,
                        onRegenerateBatch = onGenerateBatch,
                        onResetCanvas = onResetCanvas,
                        onCancelGeneration = onCancelGeneration,
                        onRetry = onRetryGeneration
                    )

                    ComposerControlsSection(
                        composer = composer,
                        promptHistory = promptHistory,
                        isGenerating = isGenerating,
                        isBatchGenerating = isBatchGenerating,
                        isEnhancing = enhanceState is EnhanceUiState.Loading,
                        onRecallPromptHistory = onRecallPromptHistory,
                        onDeletePromptHistory = onDeletePromptHistory,
                        onClearPromptHistory = onClearPromptHistory,
                        onOpenInspirator = { showInspiratorSheet = true },
                        onModeChange = onModeChange,
                        onPromptChange = onPromptChange,
                        onNegativePromptChange = onNegativePromptChange,
                        onClearNegativePrompt = onClearNegativePrompt,
                        onToggleNegativePreset = onToggleNegativePreset,
                        onOpenNegativeGuide = onOpenNegativeGuide,
                        onSelectStyle = onSelectStyle,
                        onInsertStyleIntoPrompt = onInsertStyleIntoPrompt,
                        onSelectAspectRatio = onSelectAspectRatio,
                        onSelectModel = onSelectModel,
                        onSeedInputChange = onSeedInputChange,
                        onRandomizeSeed = onRandomizeSeed,
                        onClearSeed = onClearSeed,
                        onPickReferencePhoto = onPickReferencePhoto,
                        onEnhancePrompt = onEnhancePrompt,
                        onGenerate = onGenerate,
                        onGenerateBatch = onGenerateBatch
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    val batchState = generationState as? GenerationUiState.BatchSuccess
    val inspectedVariant = inspectedBatchVariantIndex?.let { idx ->
        batchState?.variants?.find { it.index == idx }
    }
    if (batchState != null && inspectedVariant != null) {
        BatchVariantInspectorDialog(
            variant = inspectedVariant,
            totalCount = batchState.variants.size,
            onDismiss = { inspectedBatchVariantIndex = null },
            onCrop = {
                val bmp = inspectedVariant.bitmap
                inspectedBatchVariantIndex = null
                if (bmp != null) {
                    cropTarget = inspectedVariant.index to bmp
                }
            },
            onSaveToGallery = { onSaveBatchVariantToGallery(inspectedVariant.index) },
            onSaveToPictures = { onSaveBatchVariantToPhotos(inspectedVariant.index) },
            onShare = { onShareBatchVariant(inspectedVariant.index) },
            onRemix = {
                val bmp = inspectedVariant.bitmap
                inspectedBatchVariantIndex = null
                if (bmp != null) {
                    onRemixResult(bmp)
                }
            },
            onFocusInCanvas = {
                val idx = inspectedVariant.index
                inspectedBatchVariantIndex = null
                onOpenBatchVariantInCanvas(idx)
            }
        )
    }

    cropTarget?.let { (batchIndex, targetBitmap) ->
        ImageCropDialog(
            bitmap = targetBitmap,
            onDismiss = { cropTarget = null },
            onApplyCrop = { l, t, r, b ->
                cropTarget = null
                if (batchIndex != null) {
                    onApplyBatchCrop(batchIndex, l, t, r, b)
                } else {
                    onApplyCrop(l, t, r, b)
                }
            }
        )
    }

    zoomBitmap?.let { bitmap ->
        Dialog(
            onDismissRequest = { zoomBitmap = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .clickable { zoomBitmap = null }
                    .padding(16.dp)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.cd_generated_artwork),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = { zoomBitmap = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 24.dp, end = 8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close preview",
                        tint = Color.White
                    )
                }
            }
        }
    }

    if (showInspiratorSheet) {
        PromptInspiratorSheet(
            state = inspiratorState,
            onDismiss = { showInspiratorSheet = false },
            onSelectCategory = onSelectInspirationCategory,
            onShuffle = onShuffleInspiration,
            onBrainstormAi = onBrainstormInspiration,
            onApplyStarter = { starter ->
                onApplyPromptStarter(starter)
                showInspiratorSheet = false
            }
        )
    }
}

@Composable
private fun CanvasViewportSection(
    composer: StudioComposerState,
    generationState: GenerationUiState,
    onPickReferencePhoto: () -> Unit,
    onClearReferencePhoto: () -> Unit,
    onApplyInspiration: (InspirationTemplate) -> Unit,
    onUpscaleResult: (UpscaleFactor) -> Unit,
    onSaveToLocalGallery: () -> Unit,
    onOpenCrop: (Bitmap) -> Unit,
    onResetCrop: () -> Unit,
    onSaveResult: () -> Unit,
    onShareResult: () -> Unit,
    onRemixResult: (Bitmap) -> Unit,
    onZoomBitmap: (Bitmap) -> Unit,
    onInspectBatchVariant: (Int) -> Unit,
    onOpenBatchVariantInCanvas: (Int) -> Unit,
    onOpenBatchCrop: (Int, Bitmap) -> Unit,
    onSaveBatchVariantToGallery: (Int) -> Unit,
    onSaveAllBatchVariantsToGallery: () -> Unit,
    onRetryBatchVariant: (Int) -> Unit,
    onRegenerateBatch: () -> Unit,
    onResetCanvas: () -> Unit,
    onCancelGeneration: () -> Unit,
    onRetry: () -> Unit
) {
    when (generationState) {
        is GenerationUiState.Idle -> {
            if (composer.mode == StudioMode.IMAGE_REMIX && composer.referenceBitmap != null) {
                ReferencePreviewCard(
                    bitmap = composer.referenceBitmap,
                    onPickAnother = onPickReferencePhoto,
                    onClear = onClearReferencePhoto
                )
            } else {
                AtelierIdleShowcase(
                    aspectRatio = composer.selectedAspectRatio,
                    isRemixMode = composer.mode == StudioMode.IMAGE_REMIX,
                    onPickReferencePhoto = onPickReferencePhoto,
                    onApplyInspiration = onApplyInspiration
                )
            }
        }

        is GenerationUiState.Loading -> {
            SynthesisLoadingCard(
                aspectRatio = composer.selectedAspectRatio,
                elapsedSeconds = generationState.elapsedSeconds,
                hasNegativePrompt = composer.negativePrompt.isNotBlank(),
                isRemix = composer.mode == StudioMode.IMAGE_REMIX,
                onCancel = onCancelGeneration
            )
        }

        is GenerationUiState.BatchLoading -> {
            BatchSynthesisLoadingCard(
                state = generationState,
                aspectRatio = composer.selectedAspectRatio,
                onCancel = onCancelGeneration
            )
        }

        is GenerationUiState.Success -> {
            GeneratedResultCard(
                state = generationState,
                onCrop = { onOpenCrop(generationState.bitmap) },
                onResetCrop = onResetCrop,
                onUpscale = onUpscaleResult,
                onSaveToLocalGallery = onSaveToLocalGallery,
                onSaveToPhotos = onSaveResult,
                onShare = onShareResult,
                onRemix = { onRemixResult(generationState.bitmap) },
                onZoom = { onZoomBitmap(generationState.bitmap) },
                onNewCanvas = onResetCanvas
            )
        }

        is GenerationUiState.BatchSuccess -> {
            BatchResultGridCard(
                state = generationState,
                onInspectVariant = onInspectBatchVariant,
                onOpenInCanvas = onOpenBatchVariantInCanvas,
                onCropVariant = { idx ->
                    generationState.variants.find { it.index == idx }?.bitmap?.let { bmp ->
                        onOpenBatchCrop(idx, bmp)
                    }
                },
                onSaveVariant = onSaveBatchVariantToGallery,
                onRetryVariant = onRetryBatchVariant,
                onSaveAllToGallery = onSaveAllBatchVariantsToGallery,
                onRegenerateBatch = onRegenerateBatch,
                onResetCanvas = onResetCanvas
            )
        }

        is GenerationUiState.Error -> {
            GenerationErrorCard(
                message = generationState.message,
                onRetry = onRetry,
                onDismiss = onResetCanvas
            )
        }
    }
}

@Composable
private fun AtelierIdleShowcase(
    aspectRatio: AspectRatioOption,
    isRemixMode: Boolean,
    onPickReferencePhoto: () -> Unit,
    onApplyInspiration: (InspirationTemplate) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 240.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceContainerHighest,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable(enabled = isRemixMode, onClick = onPickReferencePhoto)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = if (isRemixMode) Icons.Rounded.AddPhotoAlternate else Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(14.dp)
                                .size(28.dp)
                        )
                    }
                    Text(
                        text = if (isRemixMode) {
                            "Select a Photo to Remix"
                        } else {
                            "Canvas Ready • ${aspectRatio.label} (${aspectRatio.subtitle})"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isRemixMode) {
                            "Pick any image from your device and describe edits or exclusions below."
                        } else {
                            "Write a prompt below, refine with Negative Prompts, or tap an inspiration template."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isRemixMode) {
                        Button(
                            onClick = onPickReferencePhoto,
                            modifier = Modifier.testTag("pick_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.pick_reference_photo))
                        }
                    }
                }
            }

            if (!isRemixMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Starter Templates (with Negative Prompts)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Tap to fill",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 4.dp)
                ) {
                    items(StudioCatalog.inspirationTemplates, key = { it.id }) { template ->
                        InspirationTemplateCard(
                            template = template,
                            onClick = { onApplyInspiration(template) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InspirationTemplateCard(
    template: InspirationTemplate,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onClick)
            .testTag("template_${template.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Image(
                painter = painterResource(id = template.imageRes),
                contentDescription = template.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
            )
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Excludes: ${template.negativePrompt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ReferencePreviewCard(
    bitmap: Bitmap,
    onPickAnother: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reference Photo",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = onPickAnother) {
                        Text(stringResource(R.string.change_reference_photo))
                    }
                    IconButton(onClick = onClear) {
                        Icon(Icons.Rounded.Close, contentDescription = "Remove reference photo")
                    }
                }
            }
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.cd_reference_photo),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}

@Composable
private fun SynthesisLoadingCard(
    aspectRatio: AspectRatioOption,
    elapsedSeconds: Int,
    hasNegativePrompt: Boolean,
    isRemix: Boolean,
    onCancel: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by transition.animateFloat(
        initialValue = -300f,
        targetValue = 950f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
            MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        start = Offset(shimmerOffset, 0f),
        end = Offset(shimmerOffset + 360f, 360f)
    )

    val stageLabel = when {
        elapsedSeconds < 5 -> if (isRemix) "Analyzing reference photo..." else "Interpreting prompt & style..."
        elapsedSeconds < 18 -> if (hasNegativePrompt) "Synthesizing with negative exclusions..." else "Synthesizing composition..."
        else -> "Refining high-resolution details..."
    }

    Card(
        modifier = Modifier.testTag("generation_loading_indicator"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Shimmering skeleton artwork frame matching the selected aspect ratio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = 330.dp)
                    .aspectRatio(
                        aspectRatio.ratioFloat.coerceIn(0.7f, 1.8f),
                        matchHeightConstraintsFirst = true
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(shimmerBrush)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                        shape = CircleShape
                    ) {
                        Box(
                            modifier = Modifier.padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Text(
                        text = stageLabel,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${aspectRatio.label} • ${elapsedSeconds}s elapsed",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )

            // Skeleton action pills + Cancel control
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(shimmerBrush)
                    )
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.testTag("cancel_generation_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun GeneratedResultCard(
    state: GenerationUiState.Success,
    onCrop: () -> Unit,
    onResetCrop: () -> Unit,
    onUpscale: (UpscaleFactor) -> Unit,
    onSaveToLocalGallery: () -> Unit,
    onSaveToPhotos: () -> Unit,
    onShare: () -> Unit,
    onRemix: () -> Unit,
    onZoom: () -> Unit,
    onNewCanvas: () -> Unit
) {
    val isSaved = state.savedCreation != null
    val isSaving = state.isSavingToGallery

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onZoom)
            ) {
                Image(
                    bitmap = state.bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.cd_generated_artwork),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                )

                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    val upscaleTag = state.upscaleFactor?.let { " • ${it.label} AI" } ?: ""
                    val cropTag = if (state.isCropped) " • Trimmed" else ""
                    Text(
                        text = "${state.bitmap.width}×${state.bitmap.height} px$cropTag$upscaleTag",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.62f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ZoomIn,
                        contentDescription = "Expand artwork",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(18.dp)
                    )
                }
            }

            // AI Resolution Upscaler row (2x / 4x before saving to gallery)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UpscaleFactor.entries.forEach { factor ->
                    val isThisUpscaling = state.isUpscaling && state.upscalingTarget == factor
                    val isApplied = state.upscaleFactor == factor
                    FilledTonalButton(
                        onClick = { onUpscale(factor) },
                        enabled = !state.isUpscaling && !isSaving,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isApplied) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                            contentColor = if (isApplied) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("upscale_${factor.multiplier}x_button")
                    ) {
                        if (isThisUpscaling) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(15.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.upscaling_progress, factor.label),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Icon(
                                imageVector = if (isApplied) Icons.Rounded.Check else Icons.Rounded.Hd,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (factor == UpscaleFactor.X2) {
                                    stringResource(R.string.upscale_2x_cta)
                                } else {
                                    stringResource(R.string.upscale_4x_cta)
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Primary workflow row: Trim/Crop before saving to local gallery
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCrop,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("crop_image_button")
                ) {
                    Icon(Icons.Rounded.Crop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (state.isCropped) "Crop Again" else "Crop / Trim")
                }

                Button(
                    onClick = onSaveToLocalGallery,
                    enabled = !isSaved && !isSaving,
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = if (isSaved) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        disabledContentColor = if (isSaved) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    ),
                    modifier = Modifier
                        .weight(1.25f)
                        .minimumInteractiveComponentSize()
                        .testTag("save_to_local_gallery_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.saving_to_gallery))
                    } else {
                        Icon(
                            imageVector = if (isSaved) Icons.Rounded.Check else Icons.Rounded.BookmarkAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (isSaved) {
                                stringResource(R.string.saved_to_gallery)
                            } else {
                                stringResource(R.string.save_to_gallery)
                            }
                        )
                    }
                }
            }

            // Public MediaStore export & share row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onSaveToPhotos,
                    enabled = !state.isSavingToPictures,
                    modifier = Modifier
                        .weight(1.35f)
                        .minimumInteractiveComponentSize()
                        .testTag("save_to_pictures_button")
                ) {
                    if (state.isSavingToPictures) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.saving_to_pictures))
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.save_to_photos),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("share_image_button")
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(stringResource(R.string.share_artwork))
                }
            }

            OutlinedButton(
                onClick = onRemix,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("remix_result_button")
            ) {
                Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Remix This Artwork")
            }

            if (state.negativePrompt.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Block,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Excluded: ${state.negativePrompt}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (state.seed != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.testTag("result_seed_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Pin,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Seed: ${state.seed}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSaved) {
                        "Saved in Gallery • ${state.aspectRatioLabel}"
                    } else {
                        "Draft Preview • Trim before saving"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSaved) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
                Row {
                    if (state.isCropped) {
                        TextButton(
                            onClick = onResetCrop,
                            modifier = Modifier.testTag("reset_crop_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Crop")
                        }
                    }
                    TextButton(onClick = onNewCanvas) {
                        Text("Reset Canvas")
                    }
                }
            }
        }
    }
}

@Composable
private fun GenerationErrorCard(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Generation Failed",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun ComposerControlsSection(
    composer: StudioComposerState,
    promptHistory: List<PromptHistoryEntry>,
    isGenerating: Boolean,
    isBatchGenerating: Boolean,
    isEnhancing: Boolean,
    onRecallPromptHistory: (PromptHistoryEntry) -> Unit,
    onDeletePromptHistory: (PromptHistoryEntry) -> Unit,
    onClearPromptHistory: () -> Unit,
    onOpenInspirator: () -> Unit,
    onModeChange: (StudioMode) -> Unit,
    onPromptChange: (String) -> Unit,
    onNegativePromptChange: (String) -> Unit,
    onClearNegativePrompt: () -> Unit,
    onToggleNegativePreset: (NegativePromptPreset) -> Unit,
    onOpenNegativeGuide: () -> Unit,
    onSelectStyle: (StylePreset) -> Unit,
    onInsertStyleIntoPrompt: () -> Unit,
    onSelectAspectRatio: (AspectRatioOption) -> Unit,
    onSelectModel: (ImageModelOption) -> Unit,
    onSeedInputChange: (String) -> Unit,
    onRandomizeSeed: () -> Unit,
    onClearSeed: () -> Unit,
    onPickReferencePhoto: () -> Unit,
    onEnhancePrompt: () -> Unit,
    onGenerate: () -> Unit,
    onGenerateBatch: () -> Unit
) {
    var historyExpanded by remember { mutableStateOf(false) }

    // 1. Mode Switcher (Text-to-Image vs Image Remix)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = composer.mode == StudioMode.TEXT_TO_IMAGE,
            onClick = { onModeChange(StudioMode.TEXT_TO_IMAGE) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            modifier = Modifier.testTag("mode_text_to_image")
        ) {
            Text(stringResource(R.string.mode_create))
        }
        SegmentedButton(
            selected = composer.mode == StudioMode.IMAGE_REMIX,
            onClick = { onModeChange(StudioMode.IMAGE_REMIX) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            modifier = Modifier.testTag("mode_image_remix")
        ) {
            Text(stringResource(R.string.mode_remix))
        }
    }

    AnimatedVisibility(visible = composer.mode == StudioMode.IMAGE_REMIX) {
        OutlinedButton(
            onClick = onPickReferencePhoto,
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("remix_pick_photo_button")
        ) {
            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (composer.referenceBitmap == null) {
                    stringResource(R.string.pick_reference_photo)
                } else {
                    stringResource(R.string.change_reference_photo)
                }
            )
        }
    }

    // 2. Positive Prompt Card + Prompt Inspirator + Searchable Query History + AI Enhance
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (composer.mode == StudioMode.IMAGE_REMIX) {
                    "Edit Instruction"
                } else {
                    stringResource(R.string.prompt_label)
                },
                style = MaterialTheme.typography.titleSmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onOpenInspirator,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("open_prompt_inspirator_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.prompt_inspirator_cta),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }

                TextButton(
                    onClick = { historyExpanded = !historyExpanded },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("toggle_prompt_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (promptHistory.isNotEmpty()) {
                            "${stringResource(R.string.prompt_history_cta)} (${promptHistory.size})"
                        } else {
                            stringResource(R.string.prompt_history_cta)
                        },
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = onEnhancePrompt,
                    enabled = !isEnhancing && !isGenerating,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("enhance_prompt_button")
                ) {
                    if (isEnhancing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Refining...")
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.enhance_cta), maxLines = 1)
                    }
                }
            }

            OutlinedTextField(
                value = composer.prompt,
                onValueChange = onPromptChange,
                placeholder = {
                    Text(
                        text = if (composer.mode == StudioMode.IMAGE_REMIX) {
                            "e.g. Transform into golden hour twilight with glowing warm lanterns"
                        } else {
                            stringResource(R.string.prompt_hint)
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("prompt_input")
            )

            AnimatedVisibility(visible = historyExpanded) {
                PromptHistoryPanel(
                    entries = promptHistory,
                    onRecall = { entry ->
                        onRecallPromptHistory(entry)
                        historyExpanded = false
                    },
                    onDeleteEntry = onDeletePromptHistory,
                    onClearAll = onClearPromptHistory
                )
            }
        }
    }

    // 2b. Predefined Art Style Selector Card
    ArtStyleSelectorCard(
        selectedStyle = composer.selectedStyle,
        onSelectStyle = onSelectStyle,
        onInsertIntoPrompt = onInsertStyleIntoPrompt
    )

    // 3. Dedicated Negative Prompt Studio Card
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(
            width = 1.dp,
            color = if (composer.negativePrompt.isNotBlank()) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Block,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.negative_prompt_label),
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                TextButton(
                    onClick = onOpenNegativeGuide,
                    modifier = Modifier.testTag("negative_guide_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                        contentDescription = stringResource(R.string.cd_negative_guide),
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.negative_guide_cta),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            OutlinedTextField(
                value = composer.negativePrompt,
                onValueChange = onNegativePromptChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.negative_prompt_hint),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                trailingIcon = {
                    if (composer.negativePrompt.isNotEmpty()) {
                        IconButton(onClick = onClearNegativePrompt) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear negative prompt"
                            )
                        }
                    }
                },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("negative_prompt_input")
            )

            Text(
                text = "Quick Exclusion Presets",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(StudioCatalog.negativePresets, key = { it.id }) { preset ->
                    val isSelected = remember(composer.negativePrompt, preset) {
                        val currentTokens = composer.negativePrompt
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        val presetTokens = preset.keywords
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        presetTokens.all { target ->
                            currentTokens.any { it.equals(target, ignoreCase = true) }
                        }
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggleNegativePreset(preset) },
                        label = { Text(preset.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = if (isSelected) Icons.Rounded.Check else Icons.Rounded.Block,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        modifier = Modifier.testTag("neg_chip_${preset.id}")
                    )
                }
            }
        }
    }

    // 4. Aspect Ratio Toggle Group Card
    AspectRatioSelectorCard(
        selectedAspectRatio = composer.selectedAspectRatio,
        onSelectAspectRatio = onSelectAspectRatio
    )

    // 4b. Model Engine Selector Card
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Model Engine",
                style = MaterialTheme.typography.titleSmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ImageModelOption.entries.forEach { model ->
                    val selected = composer.selectedModel == model
                    Surface(
                        onClick = { onSelectModel(model) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = model.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = model.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 4c. Optional Seed Control Card
    SeedSelectorCard(
        seedInput = composer.seedInput,
        onSeedInputChange = onSeedInputChange,
        onRandomizeSeed = onRandomizeSeed,
        onClearSeed = onClearSeed
    )

    // 5. Primary Generate & 4-Variant Batch CTAs
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onGenerate,
            enabled = !isGenerating,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .weight(1.2f)
                .height(54.dp)
                .testTag("generate_button")
        ) {
            if (isGenerating && !isBatchGenerating) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generating…",
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Icon(
                    imageVector = if (composer.mode == StudioMode.IMAGE_REMIX) {
                        Icons.Rounded.AutoFixHigh
                    } else {
                        Icons.Rounded.AutoAwesome
                    },
                    contentDescription = null,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (composer.mode == StudioMode.IMAGE_REMIX) {
                        stringResource(R.string.remix_cta)
                    } else {
                        "Generate"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        FilledTonalButton(
            onClick = onGenerateBatch,
            enabled = !isGenerating,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
                .testTag("generate_batch_button")
        ) {
            if (isBatchGenerating) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Batch…",
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.GridView,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.generate_batch_cta),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SeedSelectorCard(
    seedInput: String,
    onSeedInputChange: (String) -> Unit,
    onRandomizeSeed: () -> Unit,
    onClearSeed: () -> Unit
) {
    val parseResult = remember(seedInput) { SeedUtils.parseSeed(seedInput) }
    val isError = parseResult is SeedParseResult.Invalid

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
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
                        imageVector = Icons.Rounded.Pin,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.seed_label),
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (parseResult) {
                        is SeedParseResult.Empty -> MaterialTheme.colorScheme.surfaceContainerHigh
                        is SeedParseResult.Valid -> MaterialTheme.colorScheme.primaryContainer
                        is SeedParseResult.Invalid -> MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Text(
                        text = when (parseResult) {
                            is SeedParseResult.Empty -> "Random"
                            is SeedParseResult.Valid -> "Locked: ${parseResult.seed}"
                            is SeedParseResult.Invalid -> "Invalid"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when (parseResult) {
                            is SeedParseResult.Empty -> MaterialTheme.colorScheme.onSurfaceVariant
                            is SeedParseResult.Valid -> MaterialTheme.colorScheme.onPrimaryContainer
                            is SeedParseResult.Invalid -> MaterialTheme.colorScheme.onErrorContainer
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            OutlinedTextField(
                value = seedInput,
                onValueChange = onSeedInputChange,
                placeholder = { Text(stringResource(R.string.seed_hint)) },
                singleLine = true,
                isError = isError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = {
                    Text(
                        text = if (parseResult is SeedParseResult.Invalid) {
                            parseResult.error
                        } else {
                            stringResource(R.string.seed_helper)
                        }
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (seedInput.isNotEmpty()) {
                            IconButton(
                                onClick = onClearSeed,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("clear_seed_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.cd_clear_seed)
                                )
                            }
                        }
                        IconButton(
                            onClick = onRandomizeSeed,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("randomize_seed_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Casino,
                                contentDescription = stringResource(R.string.cd_randomize_seed),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("seed_input")
            )
        }
    }
}

