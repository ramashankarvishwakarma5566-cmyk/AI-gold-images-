package com.example.ui.studio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Polyline
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.StylePreset

@Composable
fun ArtStyleSelectorCard(
    selectedStyle: StylePreset,
    onSelectStyle: (StylePreset) -> Unit,
    onInsertIntoPrompt: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("art_style_selector_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(
            width = 1.dp,
            color = if (selectedStyle != StylePreset.NONE) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
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
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Art Style Preset",
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Surface(
                    color = if (selectedStyle != StylePreset.NONE) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = selectedStyle.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selectedStyle != StylePreset.NONE) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 4.dp)
            ) {
                items(StylePreset.entries, key = { it.name }) { style ->
                    ArtStyleTile(
                        style = style,
                        selected = style == selectedStyle,
                        onClick = { onSelectStyle(style) }
                    )
                }
            }

            AnimatedVisibility(visible = selectedStyle != StylePreset.NONE) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "APPENDED TO PROMPT",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = selectedStyle.promptSuffix,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(
                            onClick = onInsertIntoPrompt,
                            modifier = Modifier.testTag("insert_style_into_prompt_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Insert")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtStyleTile(
    style: StylePreset,
    selected: Boolean,
    onClick: () -> Unit
) {
    val visual = style.visualSpec()
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = Modifier
            .width(106.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("style_option_${style.name.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (selected) 1.75.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(visual.swatchColors)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (selected) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selected style",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = style.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class StyleVisualSpec(
    val icon: ImageVector,
    val swatchColors: List<Color>
)

@Composable
private fun StylePreset.visualSpec(): StyleVisualSpec {
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        StylePreset.NONE -> StyleVisualSpec(
            icon = Icons.Rounded.Tune,
            swatchColors = listOf(scheme.outline, scheme.surfaceVariant)
        )
        StylePreset.CINEMATIC -> StyleVisualSpec(
            icon = Icons.Rounded.Movie,
            swatchColors = listOf(scheme.primary, scheme.secondary)
        )
        StylePreset.OIL_PAINTING -> StyleVisualSpec(
            icon = Icons.Rounded.Brush,
            swatchColors = listOf(scheme.primary, scheme.tertiary)
        )
        StylePreset.CYBERPUNK -> StyleVisualSpec(
            icon = Icons.Rounded.ElectricBolt,
            swatchColors = listOf(scheme.secondary, scheme.tertiary)
        )
        StylePreset.PHOTOREALISTIC -> StyleVisualSpec(
            icon = Icons.Rounded.CameraAlt,
            swatchColors = listOf(scheme.tertiary, scheme.primary)
        )
        StylePreset.ANIME -> StyleVisualSpec(
            icon = Icons.Rounded.AutoAwesome,
            swatchColors = listOf(scheme.secondary, scheme.primary)
        )
        StylePreset.CLAY_3D -> StyleVisualSpec(
            icon = Icons.Rounded.ViewInAr,
            swatchColors = listOf(scheme.primaryContainer, scheme.secondary)
        )
        StylePreset.WATERCOLOR -> StyleVisualSpec(
            icon = Icons.Rounded.WaterDrop,
            swatchColors = listOf(scheme.tertiary, scheme.tertiaryContainer)
        )
        StylePreset.MINIMAL_VECTOR -> StyleVisualSpec(
            icon = Icons.Rounded.Polyline,
            swatchColors = listOf(scheme.secondaryContainer, scheme.tertiary)
        )
    }
}
