package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "creations")
data class CreationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val prompt: String,
    val negativePrompt: String,
    val styleName: String,
    val aspectRatioLabel: String,
    val modelId: String,
    val imagePath: String,
    val isRemix: Boolean,
    val isFavorite: Boolean = false,
    val seed: Int? = null,
    val tags: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
