package com.example.data.repository

import android.graphics.Bitmap
import com.example.data.local.CreationDao
import com.example.data.local.CreationEntity
import kotlinx.coroutines.flow.Flow

class CreationRepository(
    private val creationDao: CreationDao,
    private val storageHelper: ImageStorageHelper
) {
    val allCreations: Flow<List<CreationEntity>> = creationDao.observeAll()

    suspend fun getCreationById(id: Long): CreationEntity? = creationDao.getById(id)

    suspend fun saveCreation(
        bitmap: Bitmap,
        prompt: String,
        negativePrompt: String,
        styleName: String,
        aspectRatioLabel: String,
        modelId: String,
        isRemix: Boolean,
        seed: Int? = null,
        tags: String = ""
    ): CreationEntity {
        val savedPath = storageHelper.saveCreationBitmap(bitmap)
        val entity = CreationEntity(
            prompt = prompt.trim(),
            negativePrompt = negativePrompt.trim(),
            styleName = styleName,
            aspectRatioLabel = aspectRatioLabel,
            modelId = modelId,
            imagePath = savedPath,
            isRemix = isRemix,
            seed = seed,
            tags = tags.trim()
        )
        val newId = creationDao.insert(entity)
        return entity.copy(id = newId)
    }

    suspend fun setFavorite(id: Long, isFavorite: Boolean) {
        creationDao.updateFavoriteStatus(id, isFavorite)
    }

    suspend fun updateTags(id: Long, tags: String) {
        creationDao.updateTags(id, tags)
    }

    suspend fun deleteCreation(creation: CreationEntity) {
        creationDao.deleteById(creation.id)
        storageHelper.deleteCreationFile(creation.imagePath)
    }
}
