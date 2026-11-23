package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.ImageFilterPreset
import com.example.model.ImageFilterUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ImageStorageHelper(private val context: Context) {

    private val creationsDir: File
        get() = File(context.filesDir, "creations").apply {
            if (!exists()) mkdirs()
        }

    suspend fun saveCreationBitmap(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val file = File(creationsDir, "lumina_${UUID.randomUUID()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        file.absolutePath
    }

    suspend fun deleteCreationFile(imagePath: String) = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(imagePath)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    suspend fun loadBitmapFromPath(imagePath: String): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(imagePath)
        if (!file.exists()) return@withContext null
        BitmapFactory.decodeFile(file.absolutePath)
    }

    suspend fun decodeUriForRemix(uri: Uri, maxDimension: Int = 1536): Bitmap? =
        withContext(Dispatchers.IO) {
            val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } ?: return@withContext null

            scaleDownIfNeeded(rawBitmap, maxDimension)
        }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val longest = maxOf(width, height)
        if (longest <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / longest.toFloat()
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    data class MediaStoreSaveResult(
        val uri: Uri,
        val displayLocation: String
    )

    suspend fun saveBitmapToPublicPictures(
        bitmap: Bitmap,
        fileNamePrefix: String = "Lumina"
    ): Result<MediaStoreSaveResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                insertIntoPublicPictures(fileNamePrefix = fileNamePrefix) { out ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                        "Failed to encode PNG bitmap."
                    }
                }
            }
        }

    suspend fun stageBitmapForSafExport(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "lumina_export_${UUID.randomUUID()}.png")
        FileOutputStream(tempFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        tempFile.absolutePath
    }

    suspend fun saveToDeviceGallery(imagePath: String): Result<MediaStoreSaveResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val sourceFile = File(imagePath)
                require(sourceFile.exists()) { "Image file no longer exists." }

                insertIntoPublicPictures { out ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
            }
        }

    private fun insertIntoPublicPictures(
        fileNamePrefix: String = "Lumina",
        writeBlock: (java.io.OutputStream) -> Unit
    ): MediaStoreSaveResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw UnsupportedApiLevelException("Use document saver on Android 9 and below.")
        }

        val nowMs = System.currentTimeMillis()
        val displayName = "${fileNamePrefix}_$nowMs.png"
        val relativePath = "${Environment.DIRECTORY_PICTURES}/Lumina Studio"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.DATE_ADDED, nowMs / 1000L)
            put(MediaStore.Images.Media.DATE_TAKEN, nowMs)
            put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val itemUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Could not create MediaStore entry.")

        try {
            resolver.openOutputStream(itemUri)?.use { out ->
                writeBlock(out)
                out.flush()
            } ?: throw IllegalStateException("Could not open MediaStore output stream.")

            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)
            return MediaStoreSaveResult(
                uri = itemUri,
                displayLocation = relativePath
            )
        } catch (t: Throwable) {
            runCatching { resolver.delete(itemUri, null, null) }
            throw t
        }
    }

    suspend fun writeToUri(sourcePath: String, destinationUri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val sourceFile = File(sourcePath)
                require(sourceFile.exists()) { "Image file not found." }
                context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                } ?: throw IllegalStateException("Could not open destination URI.")
                Unit
            }
        }

    fun getShareUri(imagePath: String): Uri? {
        val file = File(imagePath)
        if (!file.exists()) return null
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    suspend fun getShareUriForBitmap(bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        val shareDir = File(context.cacheDir, "shared_exports").apply {
            if (!exists()) mkdirs()
        }
        val file = File(shareDir, "lumina_share_${UUID.randomUUID()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    suspend fun applyColorFilter(
        source: Bitmap,
        preset: ImageFilterPreset
    ): Bitmap = withContext(Dispatchers.Default) {
        val matrixValues = ImageFilterUtils.colorMatrixValues(preset) ?: return@withContext source
        val output = Bitmap.createBitmap(
            source.width.coerceAtLeast(1),
            source.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix(matrixValues))
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        output
    }

    suspend fun cropBitmap(
        source: Bitmap,
        leftFraction: Float,
        topFraction: Float,
        rightFraction: Float,
        bottomFraction: Float
    ): Bitmap = withContext(Dispatchers.Default) {
        val bounds = computeCropPixelRect(
            imageWidth = source.width,
            imageHeight = source.height,
            leftFraction = leftFraction,
            topFraction = topFraction,
            rightFraction = rightFraction,
            bottomFraction = bottomFraction
        )
        Bitmap.createBitmap(source, bounds.x, bounds.y, bounds.width, bounds.height)
    }

    companion object {
        data class PixelCropRect(
            val x: Int,
            val y: Int,
            val width: Int,
            val height: Int
        )

        fun computeCropPixelRect(
            imageWidth: Int,
            imageHeight: Int,
            leftFraction: Float,
            topFraction: Float,
            rightFraction: Float,
            bottomFraction: Float
        ): PixelCropRect {
            val safeLeft = leftFraction.coerceIn(0f, 0.98f)
            val safeTop = topFraction.coerceIn(0f, 0.98f)
            val safeRight = rightFraction.coerceIn(safeLeft + 0.02f, 1f)
            val safeBottom = bottomFraction.coerceIn(safeTop + 0.02f, 1f)

            val x = (safeLeft * imageWidth).toInt().coerceIn(0, (imageWidth - 1).coerceAtLeast(0))
            val y = (safeTop * imageHeight).toInt().coerceIn(0, (imageHeight - 1).coerceAtLeast(0))
            val rightPx = (safeRight * imageWidth).toInt().coerceIn(x + 1, imageWidth.coerceAtLeast(1))
            val bottomPx = (safeBottom * imageHeight).toInt().coerceIn(y + 1, imageHeight.coerceAtLeast(1))

            return PixelCropRect(
                x = x,
                y = y,
                width = (rightPx - x).coerceAtLeast(1),
                height = (bottomPx - y).coerceAtLeast(1)
            )
        }
    }
}

class UnsupportedApiLevelException(message: String) : Exception(message)
