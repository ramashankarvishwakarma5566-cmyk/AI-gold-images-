package com.example.ui.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import java.io.File

@Composable
fun RecentGenerationsBar(
    items: List<RecentGenerationItem>,
    activeKey: String?,
    isGenerating: Boolean,
    onSelect: (RecentGenerationItem) -> Unit,
    onRegenerate: (RecentGenerationItem) -> Unit,
    onSave: (RecentGenerationItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recent_generations_bar"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.recent_generations_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${items.size}/5",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Re-run or save",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(items, key = { _, item -> item.key }) { index, item ->
                    RecentGenerationThumbnailCard(
                        item = item,
                        index = index,
                        isSelected = item.key == activeKey,
                        isGenerating = isGenerating,
                        onSelect = { onSelect(item) },
                        onRegenerate = { onRegenerate(item) },
                        onSave = { onSave(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentGenerationThumbnailCard(
    item: RecentGenerationItem,
    index: Int,
    isSelected: Boolean,
    isGenerating: Boolean,
    onSelect: () -> Unit,
    onRegenerate: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .width(128.dp)
            .testTag("recent_generation_card_$index"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clickable(onClick = onSelect)
            ) {
                val memoryBitmap = item.bitmap
                if (memoryBitmap != null) {
                    Image(
                        bitmap = memoryBitmap.asImageBitmap(),
                        contentDescription = item.prompt,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (item.savedCreation != null) {
                    val imageRequest = remember(item.savedCreation.imagePath) {
                        ImageRequest.Builder(context)
                            .data(File(item.savedCreation.imagePath))
                            .crossfade(true)
                            .build()
                    }
                    SubcomposeAsyncImage(
                        model = imageRequest,
                        contentDescription = item.prompt,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BrokenImage,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    )
                }

                Surface(
                    color = if (item.isSavedInGallery) {
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.94f)
                    } else {
                        Color.Black.copy(alpha = 0.65f)
                    },
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (item.isSavedInGallery) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                        Text(
                            text = if (item.isSavedInGallery) "Saved" else "Draft",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.isSavedInGallery) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                Color.White
                            }
                        )
                    }
                }
            }

            Text(
                text = item.prompt.ifBlank { item.styleName },
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSelect)
                    .padding(start = 8.dp, end = 8.dp, top = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onRegenerate,
                    enabled = !isGenerating,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("recent_regen_$index")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.cd_recent_regenerate),
                        tint = if (isGenerating) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                IconButton(
                    onClick = onSave,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("recent_save_$index")
                ) {
                    Icon(
                        imageVector = if (item.isSavedInGallery) {
                            Icons.Rounded.Download
                        } else {
                            Icons.Rounded.BookmarkAdd
                        },
                        contentDescription = if (item.isSavedInGallery) {
                            stringResource(R.string.cd_recent_save_pictures)
                        } else {
                            stringResource(R.string.cd_recent_save_gallery)
                        },
                        tint = if (item.isSavedInGallery) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
