package com.example.ui.studio

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.repository.ImageStorageHelper
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

private enum class CropAspectLock(val label: String, val ratio: Float?) {
    FREE("Free", null),
    SQUARE("1:1", 1f),
    WIDE("16:9", 16f / 9f),
    TALL("9:16", 9f / 16f),
    CLASSIC("4:3", 4f / 3f),
    PORTRAIT("3:4", 3f / 4f)
}

private enum class DragTarget {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    MOVE_INSIDE,
    NONE
}

@Composable
fun ImageCropDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onApplyCrop: (left: Float, top: Float, right: Float, bottom: Float) -> Unit
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val imageAspect = remember(bitmap) {
        bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1).toFloat()
    }

    var left by remember { mutableFloatStateOf(0.06f) }
    var top by remember { mutableFloatStateOf(0.06f) }
    var right by remember { mutableFloatStateOf(0.94f) }
    var bottom by remember { mutableFloatStateOf(0.94f) }
    var activeLock by remember { mutableStateOf(CropAspectLock.FREE) }

    val previewPixels = remember(bitmap, left, top, right, bottom) {
        ImageStorageHelper.computeCropPixelRect(
            imageWidth = bitmap.width,
            imageHeight = bitmap.height,
            leftFraction = left,
            topFraction = top,
            rightFraction = right,
            bottomFraction = bottom
        )
    }

    val applyAspectLock: (CropAspectLock) -> Unit = { lock ->
        activeLock = lock
        val targetRatio = lock.ratio
        if (targetRatio != null) {
            // Normalized ratio: (normW * imgW) / (normH * imgH) = targetRatio => normW / normH = targetRatio / imageAspect
            val normRatio = targetRatio / imageAspect
            val maxW: Float
            val maxH: Float
            if (normRatio >= 1f) {
                maxW = 0.88f
                maxH = (maxW / normRatio).coerceAtMost(0.88f)
            } else {
                maxH = 0.88f
                maxW = (maxH * normRatio).coerceAtMost(0.88f)
            }
            val finalW = min(maxW, maxH * normRatio)
            val finalH = min(maxH, maxW / normRatio)
            left = (1f - finalW) / 2f
            top = (1f - finalH) / 2f
            right = left + finalW
            bottom = top + finalH
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.background

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(surfaceColor)
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("image_crop_dialog")
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cancel crop"
                        )
                    }
                    Column {
                        Text(
                            text = "Trim & Crop Image",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${previewPixels.width} × ${previewPixels.height} px",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                TextButton(
                    onClick = {
                        activeLock = CropAspectLock.FREE
                        left = 0f
                        top = 0f
                        right = 1f
                        bottom = 1f
                    },
                    modifier = Modifier.testTag("crop_reset_bounds_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Full")
                }
            }

            // Interactive crop canvas
            var imageBounds by remember { mutableStateOf(Rect.Zero) }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(bitmap, activeLock) {
                            var currentTarget = DragTarget.NONE
                            val minSize = 0.14f
                            val cornerHitRadiusPx = 42.dp.toPx()

                            detectDragGestures(
                                onDragStart = { offset ->
                                    if (imageBounds.width <= 0f || imageBounds.height <= 0f) {
                                        currentTarget = DragTarget.NONE
                                        return@detectDragGestures
                                    }
                                    val cLeft = imageBounds.left + left * imageBounds.width
                                    val cTop = imageBounds.top + top * imageBounds.height
                                    val cRight = imageBounds.left + right * imageBounds.width
                                    val cBottom = imageBounds.top + bottom * imageBounds.height

                                    val dTL = hypot(offset.x - cLeft, offset.y - cTop)
                                    val dTR = hypot(offset.x - cRight, offset.y - cTop)
                                    val dBL = hypot(offset.x - cLeft, offset.y - cBottom)
                                    val dBR = hypot(offset.x - cRight, offset.y - cBottom)

                                    val minDist = minOf(dTL, dTR, dBL, dBR)
                                    currentTarget = when {
                                        minDist <= cornerHitRadiusPx && minDist == dTL -> DragTarget.TOP_LEFT
                                        minDist <= cornerHitRadiusPx && minDist == dTR -> DragTarget.TOP_RIGHT
                                        minDist <= cornerHitRadiusPx && minDist == dBL -> DragTarget.BOTTOM_LEFT
                                        minDist <= cornerHitRadiusPx && minDist == dBR -> DragTarget.BOTTOM_RIGHT
                                        offset.x in cLeft..cRight && offset.y in cTop..cBottom -> DragTarget.MOVE_INSIDE
                                        else -> DragTarget.NONE
                                    }
                                },
                                onDragEnd = { currentTarget = DragTarget.NONE },
                                onDragCancel = { currentTarget = DragTarget.NONE },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (imageBounds.width <= 0f || imageBounds.height <= 0f) return@detectDragGestures
                                    val dx = dragAmount.x / imageBounds.width
                                    val dy = dragAmount.y / imageBounds.height
                                    val lockedNormRatio = activeLock.ratio?.let { it / imageAspect }

                                    when (currentTarget) {
                                        DragTarget.MOVE_INSIDE -> {
                                            val w = right - left
                                            val h = bottom - top
                                            val newLeft = (left + dx).coerceIn(0f, 1f - w)
                                            val newTop = (top + dy).coerceIn(0f, 1f - h)
                                            left = newLeft
                                            top = newTop
                                            right = newLeft + w
                                            bottom = newTop + h
                                        }

                                        DragTarget.TOP_LEFT -> {
                                            if (lockedNormRatio == null) {
                                                left = (left + dx).coerceIn(0f, right - minSize)
                                                top = (top + dy).coerceIn(0f, bottom - minSize)
                                            } else {
                                                val newW = (right - (left + dx)).coerceIn(minSize, right)
                                                val newH = newW / lockedNormRatio
                                                if (newH >= minSize && bottom - newH >= 0f) {
                                                    left = right - newW
                                                    top = bottom - newH
                                                }
                                            }
                                        }

                                        DragTarget.TOP_RIGHT -> {
                                            if (lockedNormRatio == null) {
                                                right = (right + dx).coerceIn(left + minSize, 1f)
                                                top = (top + dy).coerceIn(0f, bottom - minSize)
                                            } else {
                                                val newW = ((right + dx) - left).coerceIn(minSize, 1f - left)
                                                val newH = newW / lockedNormRatio
                                                if (newH >= minSize && bottom - newH >= 0f) {
                                                    right = left + newW
                                                    top = bottom - newH
                                                }
                                            }
                                        }

                                        DragTarget.BOTTOM_LEFT -> {
                                            if (lockedNormRatio == null) {
                                                left = (left + dx).coerceIn(0f, right - minSize)
                                                bottom = (bottom + dy).coerceIn(top + minSize, 1f)
                                            } else {
                                                val newW = (right - (left + dx)).coerceIn(minSize, right)
                                                val newH = newW / lockedNormRatio
                                                if (newH >= minSize && top + newH <= 1f) {
                                                    left = right - newW
                                                    bottom = top + newH
                                                }
                                            }
                                        }

                                        DragTarget.BOTTOM_RIGHT -> {
                                            if (lockedNormRatio == null) {
                                                right = (right + dx).coerceIn(left + minSize, 1f)
                                                bottom = (bottom + dy).coerceIn(top + minSize, 1f)
                                            } else {
                                                val newW = ((right + dx) - left).coerceIn(minSize, 1f - left)
                                                val newH = newW / lockedNormRatio
                                                if (newH >= minSize && top + newH <= 1f) {
                                                    right = left + newW
                                                    bottom = top + newH
                                                }
                                            }
                                        }

                                        DragTarget.NONE -> Unit
                                    }
                                }
                            )
                        }
                ) {
                    val canvasW = size.width
                    val canvasH = size.height
                    val canvasAspect = canvasW / canvasH.coerceAtLeast(1f)

                    val drawW: Float
                    val drawH: Float
                    if (imageAspect > canvasAspect) {
                        drawW = canvasW
                        drawH = canvasW / imageAspect
                    } else {
                        drawH = canvasH
                        drawW = canvasH * imageAspect
                    }
                    val offsetX = (canvasW - drawW) / 2f
                    val offsetY = (canvasH - drawH) / 2f
                    imageBounds = Rect(offsetX, offsetY, offsetX + drawW, offsetY + drawH)

                    drawImage(
                        image = imageBitmap,
                        dstOffset = IntOffset(offsetX.roundToInt(), offsetY.roundToInt()),
                        dstSize = IntSize(drawW.roundToInt(), drawH.roundToInt())
                    )

                    val cropRect = Rect(
                        left = offsetX + left * drawW,
                        top = offsetY + top * drawH,
                        right = offsetX + right * drawW,
                        bottom = offsetY + bottom * drawH
                    )

                    // Dimmed mask outside crop box
                    val fullImagePath = Path().apply { addRect(imageBounds) }
                    val cropPath = Path().apply { addRect(cropRect) }
                    val scrimPath = Path.combine(PathOperation.Difference, fullImagePath, cropPath)
                    drawPath(path = scrimPath, color = Color.Black.copy(alpha = 0.62f))

                    // Rule-of-thirds interior lines
                    val thirdW = cropRect.width / 3f
                    val thirdH = cropRect.height / 3f
                    for (i in 1..2) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.35f),
                            start = Offset(cropRect.left + thirdW * i, cropRect.top),
                            end = Offset(cropRect.left + thirdW * i, cropRect.bottom),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.35f),
                            start = Offset(cropRect.left, cropRect.top + thirdH * i),
                            end = Offset(cropRect.right, cropRect.top + thirdH * i),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Crop outline
                    drawRect(
                        color = Color.White,
                        topLeft = cropRect.topLeft,
                        size = Size(cropRect.width, cropRect.height),
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Corner L-handles
                    val handleLen = 20.dp.toPx()
                    val handleStroke = 3.5.dp.toPx()
                    // Top-Left
                    drawLine(primaryColor, cropRect.topLeft, Offset(cropRect.left + handleLen, cropRect.top), handleStroke)
                    drawLine(primaryColor, cropRect.topLeft, Offset(cropRect.left, cropRect.top + handleLen), handleStroke)
                    // Top-Right
                    drawLine(primaryColor, cropRect.topRight, Offset(cropRect.right - handleLen, cropRect.top), handleStroke)
                    drawLine(primaryColor, cropRect.topRight, Offset(cropRect.right, cropRect.top + handleLen), handleStroke)
                    // Bottom-Left
                    drawLine(primaryColor, cropRect.bottomLeft, Offset(cropRect.left + handleLen, cropRect.bottom), handleStroke)
                    drawLine(primaryColor, cropRect.bottomLeft, Offset(cropRect.left, cropRect.bottom - handleLen), handleStroke)
                    // Bottom-Right
                    drawLine(primaryColor, cropRect.bottomRight, Offset(cropRect.right - handleLen, cropRect.bottom), handleStroke)
                    drawLine(primaryColor, cropRect.bottomRight, Offset(cropRect.right, cropRect.bottom - handleLen), handleStroke)
                }
            }

            // Bottom crop controls
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 4.dp)
                    ) {
                        items(CropAspectLock.entries) { lock ->
                            val selected = lock == activeLock
                            FilterChip(
                                selected = selected,
                                onClick = { applyAspectLock(lock) },
                                label = { Text(lock.label) },
                                leadingIcon = if (selected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = { onApplyCrop(left, top, right, bottom) },
                            modifier = Modifier
                                .weight(1.4f)
                                .testTag("apply_crop_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Crop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Apply Crop")
                        }
                    }
                }
            }
        }
    }
}
