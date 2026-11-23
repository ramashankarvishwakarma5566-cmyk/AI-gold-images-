package com.example.ui.studio

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.AspectRatioOption

@Composable
fun BatchSynthesisLoadingCard(
    state: GenerationUiState.BatchLoading,
    aspectRatio: AspectRatioOption,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "batch_shimmer")
    val shimmerOffset by transition.animateFloat(
        initialValue = -300f,
        targetValue = 950f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "batch_shimmer_offset"
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("batch_loading_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GridView,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.batch_generating_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${state.completedCount}/${state.totalCount} • ${state.elapsedSeconds}s",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            val tileAspect = aspectRatio.ratioFloat.coerceIn(0.8f, 1.35f)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (row in 0 until 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (col in 0 until 2) {
                            val idx = row * 2 + col
                            val isReady = state.slotReady.getOrElse(idx) { false }
                            val isFailed = state.slotFailed.getOrElse(idx) { false }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(tileAspect)
                                    .clip(RoundedCornerShape(16.dp))
                                    .then(
                                        when {
                                            isReady -> Modifier.background(
                                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
                                            )
                                            isFailed -> Modifier.background(
                                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                            )
                                            else -> Modifier.background(shimmerBrush)
                                        }
                                    )
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    when {
                                        isReady -> {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Text(
                                                text = "Variant #${idx + 1} ready",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                textAlign = TextAlign.Center
                                            )
                                        }

                                        isFailed -> {
                                            Icon(
                                                imageVector = Icons.Rounded.ErrorOutline,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(26.dp)
                                            )
                                            Text(
                                                text = "Variant #${idx + 1} failed",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                textAlign = TextAlign.Center
                                            )
                                        }

                                        else -> {
                                            CircularProgressIndicator(
                                                strokeWidth = 2.5.dp,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = "Variant #${idx + 1}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (state.completedCount > 0) {
                LinearProgressIndicator(
                    progress = { (state.completedCount.toFloat() / state.totalCount.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Synthesizing ${state.totalCount} variants simultaneously…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("cancel_batch_generation_button")
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
fun BatchResultGridCard(
    state: GenerationUiState.BatchSuccess,
    onInspectVariant: (Int) -> Unit,
    onOpenInCanvas: (Int) -> Unit,
    onCropVariant: (Int) -> Unit,
    onSaveVariant: (Int) -> Unit,
    onRetryVariant: (Int) -> Unit,
    onSaveAllToGallery: () -> Unit,
    onRegenerateBatch: () -> Unit,
    onResetCanvas: () -> Unit,
    modifier: Modifier = Modifier
) {
    val readyVariants = state.variants.filter { it.bitmap != null }
    val allSaved = readyVariants.isNotEmpty() && readyVariants.all { it.savedCreation != null }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("batch_result_grid"),
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
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GridView,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.batch_grid_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    val seedNote = state.baseSeed?.let { " • Seed $it" } ?: ""
                    Text(
                        text = "${state.styleName} • ${state.aspectRatioLabel}$seedNote • Tap a tile to inspect",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = onSaveAllToGallery,
                    enabled = !allSaved && !state.isSavingAll && readyVariants.isNotEmpty(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        disabledContainerColor = if (allSaved) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        disabledContentColor = if (allSaved) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("batch_save_all_button")
                ) {
                    if (state.isSavingAll) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(15.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.batch_saving_all))
                    } else {
                        Icon(
                            imageVector = if (allSaved) Icons.Rounded.Check else Icons.Rounded.BookmarkAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(if (allSaved) "All Saved" else stringResource(R.string.batch_save_all))
                    }
                }
            }

            // 2x2 grid of generated variants
            val rows = state.variants.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { variant ->
                            BatchVariantGridCell(
                                variant = variant,
                                onInspect = { onInspectVariant(variant.index) },
                                onOpenInCanvas = { onOpenInCanvas(variant.index) },
                                onCrop = { onCropVariant(variant.index) },
                                onSave = { onSaveVariant(variant.index) },
                                onRetry = { onRetryVariant(variant.index) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onRegenerateBatch,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("batch_regenerate_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Regenerate 4x")
                }

                TextButton(
                    onClick = onResetCanvas,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("batch_reset_canvas_button")
                ) {
                    Text("Reset Canvas")
                }
            }
        }
    }
}

@Composable
private fun BatchVariantGridCell(
    variant: BatchVariantItem,
    onInspect: () -> Unit,
    onOpenInCanvas: () -> Unit,
    onCrop: () -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSaved = variant.savedCreation != null

    Card(
        modifier = modifier.testTag("batch_variant_card_${variant.index}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSaved) {
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(enabled = variant.bitmap != null, onClick = onInspect)
            ) {
                val bmp = variant.bitmap
                when {
                    variant.isRetrying -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(26.dp)
                                )
                                Text(
                                    text = "Retrying #${variant.index + 1}…",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    bmp != null -> {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = stringResource(
                                R.string.cd_batch_variant,
                                variant.index + 1
                            ),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(7.dp)
                        ) {
                            Text(
                                text = if (variant.isCropped) {
                                    "#${variant.index + 1} • Trim"
                                } else {
                                    "#${variant.index + 1}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }

                        if (isSaved) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.94f),
                                shape = CircleShape,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(7.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Saved to Gallery",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier
                                        .padding(5.dp)
                                        .size(14.dp)
                                )
                            }
                        } else {
                            Surface(
                                color = Color.Black.copy(alpha = 0.55f),
                                shape = CircleShape,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(7.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ZoomIn,
                                    contentDescription = "Inspect variant",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .padding(5.dp)
                                        .size(14.dp)
                                )
                            }
                        }
                    }

                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = variant.errorMessage ?: "Variant failed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FilledTonalButton(
                                onClick = onRetry,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("batch_variant_retry_${variant.index}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = stringResource(
                                        R.string.cd_retry_variant,
                                        variant.index + 1
                                    ),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retry", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            if (variant.bitmap != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCrop,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("batch_variant_crop_${variant.index}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Crop,
                            contentDescription = stringResource(
                                R.string.cd_crop_variant,
                                variant.index + 1
                            ),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onSave,
                        enabled = !isSaved && !variant.isSavingToGallery,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("batch_variant_save_${variant.index}")
                    ) {
                        if (variant.isSavingToGallery) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (isSaved) Icons.Rounded.Check else Icons.Rounded.BookmarkAdd,
                                contentDescription = stringResource(
                                    R.string.cd_save_variant,
                                    variant.index + 1
                                ),
                                tint = if (isSaved) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenInCanvas,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("batch_variant_open_${variant.index}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInFull,
                            contentDescription = "Focus variant #${variant.index + 1} in canvas",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BatchVariantInspectorDialog(
    variant: BatchVariantItem,
    totalCount: Int,
    onDismiss: () -> Unit,
    onCrop: () -> Unit,
    onSaveToGallery: () -> Unit,
    onSaveToPictures: () -> Unit,
    onShare: () -> Unit,
    onRemix: () -> Unit,
    onFocusInCanvas: () -> Unit
) {
    val bmp = variant.bitmap ?: return
    val isSaved = variant.savedCreation != null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Variant #${variant.index + 1} of $totalCount",
                            style = MaterialTheme.typography.titleMedium
                        )
                        val seedNote = variant.seed?.let { " • Seed $it" } ?: ""
                        Text(
                            text = "${bmp.width}×${bmp.height} px" +
                                (if (variant.isCropped) " • Trimmed" else "") +
                                seedNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close variant preview")
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = stringResource(
                            R.string.cd_batch_variant,
                            variant.index + 1
                        ),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCrop,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("inspector_crop_button")
                    ) {
                        Icon(Icons.Rounded.Crop, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Crop")
                    }
                    Button(
                        onClick = onSaveToGallery,
                        enabled = !isSaved && !variant.isSavingToGallery,
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
                            .weight(1.3f)
                            .minimumInteractiveComponentSize()
                            .testTag("inspector_save_gallery_button")
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Rounded.Check else Icons.Rounded.BookmarkAdd,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onSaveToPictures,
                        enabled = !variant.isSavingToPictures,
                        modifier = Modifier
                            .weight(1.3f)
                            .minimumInteractiveComponentSize()
                            .testTag("inspector_save_pictures_button")
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (variant.isSavingToPictures) {
                                stringResource(R.string.saving_to_pictures)
                            } else {
                                stringResource(R.string.save_to_photos)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("inspector_share_button")
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.share_artwork))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRemix,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remix")
                    }
                    TextButton(
                        onClick = onFocusInCanvas,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.OpenInFull, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Focus Canvas")
                    }
                }
            }
        }
    }
}
