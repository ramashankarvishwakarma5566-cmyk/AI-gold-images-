package com.example.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.StudioCatalog
import com.example.model.TagUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BatchTagEditorSheet(
    selectedCount: Int,
    existingGalleryTags: List<String>,
    onDismiss: () -> Unit,
    onApplyTags: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tagInput by remember { mutableStateOf("") }
    var queuedTags by remember { mutableStateOf<List<String>>(emptyList()) }

    val quickSuggestions = remember(existingGalleryTags) {
        (existingGalleryTags + StudioCatalog.suggestedGalleryTags).distinct().take(14)
    }

    val previewCombined = remember(queuedTags, tagInput) {
        TagUtils.serializeTags(queuedTags + TagUtils.parseTags(tagInput))
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
                            imageVector = Icons.AutoMirrored.Rounded.Label,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Add Custom Tags",
                            style = MaterialTheme.typography.titleLarge
                        )
                        val artworkWord = if (selectedCount == 1) "artwork" else "artworks"
                        Text(
                            text = "Apply searchable tags to $selectedCount selected $artworkWord",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close tag editor")
                }
            }

            OutlinedTextField(
                value = tagInput,
                onValueChange = { tagInput = it },
                label = { Text("Custom Tags (comma-separated)") },
                placeholder = { Text("e.g. editorial, golden-hour, client-v1") },
                singleLine = true,
                trailingIcon = {
                    if (tagInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val parsed = TagUtils.parseTags(tagInput)
                                queuedTags = (queuedTags + parsed).distinct()
                                tagInput = ""
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("queue_custom_tag_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Queue tags")
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("batch_tag_input")
            )

            if (queuedTags.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Tags to Apply",
                        style = MaterialTheme.typography.titleSmall
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        queuedTags.forEach { tag ->
                            AssistChip(
                                onClick = { queuedTags = queuedTags - tag },
                                label = { Text("#$tag") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Remove $tag",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Quick Tag Suggestions",
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickSuggestions.forEach { suggestion ->
                        val selected = suggestion in queuedTags
                        FilterChip(
                            selected = selected,
                            onClick = {
                                queuedTags = if (selected) {
                                    queuedTags - suggestion
                                } else {
                                    (queuedTags + suggestion).distinct()
                                }
                            },
                            label = { Text("#$suggestion") }
                        )
                    }
                }
            }

            Button(
                onClick = { onApplyTags(previewCombined) },
                enabled = previewCombined.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("apply_batch_tags_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Label,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apply Tags to $selectedCount Selected")
            }
        }
    }
}
