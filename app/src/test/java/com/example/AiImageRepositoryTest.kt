package com.example

import com.example.data.repository.AiImageRepository
import com.example.model.StylePreset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiImageRepositoryTest {

    private val repository = AiImageRepository()

    @Test
    fun composeFinalPrompt_includesNegativePromptWhenProvided() {
        val result = repository.composeFinalPrompt(
            prompt = "Obsidian ceramic vase",
            negativePrompt = "blurry, watermark, cluttered background",
            style = StylePreset.PHOTOREALISTIC,
            isRemix = false
        )
        assertTrue(result.contains("Obsidian ceramic vase"))
        assertTrue(result.contains("Style direction:"))
        assertTrue(result.contains("Negative prompt"))
        assertTrue(result.contains("blurry, watermark, cluttered background"))
    }

    @Test
    fun composeFinalPrompt_omitsNegativeSectionWhenBlank() {
        val result = repository.composeFinalPrompt(
            prompt = "Nordic cabin by a misty fjord",
            negativePrompt = "   ",
            style = StylePreset.NONE,
            isRemix = true
        )
        assertTrue(result.startsWith("Edit and remix"))
        assertFalse(result.contains("Negative prompt"))
    }

    @Test
    fun composeFinalPrompt_generatesDistinctCuesAcrossFourBatchVariants() {
        val prompts = (0 until AiImageRepository.DEFAULT_BATCH_SIZE).map { idx ->
            repository.composeFinalPrompt(
                prompt = "Sunlit botanical conservatory",
                negativePrompt = "blurry, watermark",
                style = StylePreset.WATERCOLOR,
                isRemix = false,
                variantIndex = idx,
                batchSize = AiImageRepository.DEFAULT_BATCH_SIZE
            )
        }

        assertTrue(prompts.distinct().size == 4)
        prompts.forEachIndexed { idx, composed ->
            assertTrue(composed.contains("Sunlit botanical conservatory"))
            assertTrue(composed.contains("Creative variant ${idx + 1} of 4"))
            assertTrue(composed.contains("Negative prompt"))
            assertTrue(composed.contains("blurry, watermark"))
        }
    }

    @Test
    fun composeFinalPrompt_includesSeedReferenceWhenProvided() {
        val withSeed = repository.composeFinalPrompt(
            prompt = "Amber glass lantern",
            negativePrompt = "",
            style = StylePreset.CINEMATIC,
            isRemix = false,
            seed = 123456
        )
        val withoutSeed = repository.composeFinalPrompt(
            prompt = "Amber glass lantern",
            negativePrompt = "",
            style = StylePreset.CINEMATIC,
            isRemix = false,
            seed = null
        )

        assertTrue(withSeed.contains("#123456"))
        assertFalse(withoutSeed.contains("Deterministic seed reference"))
    }
}
