package com.example.data.repository

import android.graphics.Bitmap
import com.example.model.AspectRatioOption
import com.example.model.ImageModelOption
import com.example.model.InspirationCategory
import com.example.model.PromptStarter
import com.example.model.SeedUtils
import com.example.model.StylePreset
import com.example.model.UpscaleFactor
import com.example.model.UpscaleMath
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class EnhancedPromptResult(
    val enhancedPrompt: String,
    val suggestedNegativePrompt: String
)

sealed interface BatchVariantOutcome {
    val index: Int

    data class Success(
        override val index: Int,
        val bitmap: Bitmap
    ) : BatchVariantOutcome

    data class Failure(
        override val index: Int,
        val error: Throwable
    ) : BatchVariantOutcome
}

class AiImageRepository {

    companion object {
        const val DEFAULT_BATCH_SIZE = 4

        private val VARIANT_DIRECTIONS = listOf(
            "balanced primary composition with clear focal framing",
            "alternative camera angle with dynamic depth and lighting",
            "distinct atmospheric mood with subtle tonal variation",
            "expressive close-to-mid framing highlighting rich surface detail"
        )
    }

    private val promptEnhancerModel: GenerativeModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "enhancedPrompt" to Schema.string(
                    description = "Richly detailed visual image generation prompt with lighting, composition, texture, and atmosphere."
                ),
                "suggestedNegativePrompt" to Schema.string(
                    description = "Comma-separated list of 5 to 8 concise negative prompt terms to exclude unwanted artifacts, styles, or clutter for this subject."
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val promptInspiratorModel: GenerativeModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "title" to Schema.string(description = "Short 2 to 4 word poetic title for the artwork idea."),
                "prompt" to Schema.string(description = "Rich, detailed image generation prompt describing subject, lighting, mood, camera/medium, and textures."),
                "negativePrompt" to Schema.string(description = "Comma-separated list of 4 to 7 elements or artifacts to exclude."),
                "styleLabel" to Schema.enumeration(
                    values = StylePreset.entries.filter { it != StylePreset.NONE }.map { it.label },
                    description = "Best matching visual style preset label."
                ),
                "aspectRatioLabel" to Schema.enumeration(
                    values = AspectRatioOption.entries.map { it.label },
                    description = "Best matching aspect ratio label."
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private fun buildImageModel(
        modelOption: ImageModelOption,
        aspectRatio: AspectRatioOption,
        imageSize: ImageSize = ImageSize.SIZE_1K
    ): GenerativeModel {
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
            imageConfig = ImageConfig.builder()
                .setAspectRatio(aspectRatio.toSdkAspectRatio())
                .setImageSize(imageSize)
                .build()
        }
        return Firebase.ai.generativeModel(
            modelName = modelOption.modelId,
            generationConfig = config
        )
    }

    suspend fun generateOrRemixImage(
        prompt: String,
        negativePrompt: String,
        style: StylePreset,
        aspectRatio: AspectRatioOption,
        modelOption: ImageModelOption,
        referenceBitmap: Bitmap? = null,
        seed: Int? = null,
        variantIndex: Int? = null,
        batchSize: Int = DEFAULT_BATCH_SIZE
    ): Bitmap {
        val composedPrompt = composeFinalPrompt(
            prompt = prompt,
            negativePrompt = negativePrompt,
            style = style,
            isRemix = referenceBitmap != null,
            seed = seed,
            variantIndex = variantIndex,
            batchSize = batchSize
        )

        val model = buildImageModel(
            modelOption = modelOption,
            aspectRatio = aspectRatio
        )

        val response = withTimeout(60_000L) {
            if (referenceBitmap != null) {
                model.generateContent(
                    content {
                        image(referenceBitmap)
                        text(composedPrompt)
                    }
                )
            } else {
                model.generateContent(composedPrompt)
            }
        }

        val imagePart = response.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()

        return imagePart?.image
            ?: throw IllegalStateException("No image was returned by the model. Try adjusting your prompt or negative prompt.")
    }

    suspend fun generateBatchVariants(
        prompt: String,
        negativePrompt: String,
        style: StylePreset,
        aspectRatio: AspectRatioOption,
        modelOption: ImageModelOption,
        referenceBitmap: Bitmap? = null,
        seed: Int? = null,
        count: Int = DEFAULT_BATCH_SIZE,
        onVariantCompleted: (BatchVariantOutcome) -> Unit = {}
    ): List<BatchVariantOutcome> = coroutineScope {
        val safeCount = count.coerceIn(1, DEFAULT_BATCH_SIZE)
        (0 until safeCount).map { index ->
            async {
                val variantSeed = SeedUtils.deriveVariantSeed(seed, index)
                val outcome = try {
                    val bitmap = generateOrRemixImage(
                        prompt = prompt,
                        negativePrompt = negativePrompt,
                        style = style,
                        aspectRatio = aspectRatio,
                        modelOption = modelOption,
                        referenceBitmap = referenceBitmap,
                        seed = variantSeed,
                        variantIndex = index,
                        batchSize = safeCount
                    )
                    BatchVariantOutcome.Success(index = index, bitmap = bitmap)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    BatchVariantOutcome.Failure(index = index, error = e)
                }
                onVariantCompleted(outcome)
                outcome
            }
        }.awaitAll()
    }

    suspend fun enhancePrompt(
        rawPrompt: String,
        currentNegativePrompt: String,
        style: StylePreset
    ): EnhancedPromptResult {
        val instruction = buildString {
            append("You are an expert visual prompt engineer for AI image generation.\n")
            append("Expand and refine the user's draft prompt into a vivid, cohesive image description ")
            if (style != StylePreset.NONE) {
                append("suited for the '${style.label}' (${style.promptSuffix}) aesthetic, ")
            }
            append("and suggest a tailored negative prompt (things to strictly exclude to keep the result clean and artifact-free).\n")
            append("User draft prompt: \"$rawPrompt\"\n")
            if (currentNegativePrompt.isNotBlank()) {
                append("Existing negative prompt terms to preserve/refine: \"$currentNegativePrompt\"")
            }
        }

        val response = withTimeout(30_000L) {
            promptEnhancerModel.generateContent(instruction)
        }
        val jsonText = response.text
            ?: throw IllegalStateException("Prompt assistant returned an empty response.")
        val json = JSONObject(jsonText)
        return EnhancedPromptResult(
            enhancedPrompt = json.getString("enhancedPrompt").trim(),
            suggestedNegativePrompt = json.getString("suggestedNegativePrompt").trim()
        )
    }

    suspend fun upscaleImage(
        sourceBitmap: Bitmap,
        factor: UpscaleFactor,
        promptHint: String,
        style: StylePreset,
        aspectRatio: AspectRatioOption,
        modelOption: ImageModelOption
    ): Bitmap {
        val sdkSize = when (factor) {
            UpscaleFactor.X2 -> ImageSize.SIZE_2K
            UpscaleFactor.X4 -> ImageSize.SIZE_4K
        }
        val model = buildImageModel(
            modelOption = modelOption,
            aspectRatio = aspectRatio,
            imageSize = sdkSize
        )
        val instruction = buildString {
            append("Perform high-fidelity ${factor.multiplier}x AI super-resolution upscaling on this exact image. ")
            append("Preserve the exact composition, subject identity, pose, color palette, and framing without altering layout. ")
            append("Enhance fine micro-textures, crisp edges, natural lighting depth, and remove any compression blur or noise.")
            if (promptHint.isNotBlank()) {
                append(" Subject context: ${promptHint.trim()}.")
            }
            if (style.promptSuffix.isNotBlank()) {
                append(" Maintain aesthetic: ${style.promptSuffix}.")
            }
        }

        val response = withTimeout(60_000L) {
            model.generateContent(
                content {
                    image(sourceBitmap)
                    text(instruction)
                }
            )
        }

        val upscaledBitmap = response.candidates.firstOrNull()
            ?.content?.parts?.filterIsInstance<ImagePart>()
            ?.firstOrNull()
            ?.image
            ?: throw IllegalStateException("Upscaler did not return a high-resolution image.")

        val target = UpscaleMath.computeTargetDimensions(
            sourceWidth = sourceBitmap.width,
            sourceHeight = sourceBitmap.height,
            factor = factor
        )
        return if (upscaledBitmap.width < target.width || upscaledBitmap.height < target.height) {
            Bitmap.createScaledBitmap(upscaledBitmap, target.width, target.height, true)
        } else {
            upscaledBitmap
        }
    }

    suspend fun generateInspirationIdea(category: InspirationCategory): PromptStarter {
        val categoryInstruction = if (category == InspirationCategory.ALL) {
            "any surprising, imaginative visual theme"
        } else {
            "the '${category.label}' theme"
        }
        val instruction = """
            Generate one original, inspiring AI image prompt starter focused on $categoryInstruction.
            Make it vivid, concrete, visually cohesive, and ready for instant generation.
        """.trimIndent()

        val response = withTimeout(30_000L) {
            promptInspiratorModel.generateContent(instruction)
        }
        val jsonText = response.text
            ?: throw IllegalStateException("Prompt inspirator returned an empty response.")
        val json = JSONObject(jsonText)
        val styleLabel = json.optString("styleLabel")
        val ratioLabel = json.optString("aspectRatioLabel")
        return PromptStarter(
            id = "ai_${System.currentTimeMillis()}",
            title = json.getString("title").trim(),
            category = category,
            prompt = json.getString("prompt").trim(),
            negativePrompt = json.optString("negativePrompt").trim(),
            style = StylePreset.entries.find { it.label == styleLabel } ?: StylePreset.PHOTOREALISTIC,
            aspectRatio = AspectRatioOption.entries.find { it.label == ratioLabel } ?: AspectRatioOption.SQUARE
        )
    }

    internal fun composeFinalPrompt(
        prompt: String,
        negativePrompt: String,
        style: StylePreset,
        isRemix: Boolean,
        seed: Int? = null,
        variantIndex: Int? = null,
        batchSize: Int = DEFAULT_BATCH_SIZE
    ): String {
        val trimmedPrompt = prompt.trim()
        val styledBody = buildString {
            if (isRemix) {
                append("Edit and remix the provided reference image according to this instruction: ")
            }
            append(trimmedPrompt)
            if (style.promptSuffix.isNotBlank() &&
                !trimmedPrompt.contains(style.promptSuffix, ignoreCase = true)
            ) {
                append(". Style direction: ")
                append(style.promptSuffix)
            }
            if (seed != null) {
                append(". Deterministic seed reference: #$seed")
            }
            if (variantIndex != null && variantIndex >= 0) {
                val cue = VARIANT_DIRECTIONS[variantIndex % VARIANT_DIRECTIONS.size]
                append(". Creative variant ${variantIndex + 1} of $batchSize: $cue")
            }
        }

        val trimmedNegative = negativePrompt.trim()
        return if (trimmedNegative.isNotEmpty()) {
            "$styledBody\n\nNegative prompt — strictly avoid, omit, and exclude all of the following elements, styles, or artifacts: $trimmedNegative"
        } else {
            styledBody
        }
    }

    private fun AspectRatioOption.toSdkAspectRatio(): AspectRatio = when (this) {
        AspectRatioOption.SQUARE -> AspectRatio.SQUARE_1x1
        AspectRatioOption.LANDSCAPE -> AspectRatio.LANDSCAPE_16x9
        AspectRatioOption.PORTRAIT -> AspectRatio.PORTRAIT_9x16
        AspectRatioOption.CLASSIC -> AspectRatio.LANDSCAPE_4x3
        AspectRatioOption.VERTICAL -> AspectRatio.PORTRAIT_3x4
    }
}
