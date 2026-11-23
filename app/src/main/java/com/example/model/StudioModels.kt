package com.example.model

import androidx.annotation.DrawableRes
import com.example.R

enum class StudioMode {
    TEXT_TO_IMAGE,
    IMAGE_REMIX
}

sealed interface SeedParseResult {
    data object Empty : SeedParseResult
    data class Valid(val seed: Int) : SeedParseResult
    data class Invalid(val error: String) : SeedParseResult
}

object SeedUtils {
    const val MAX_SEED_DIGITS = 10

    fun sanitizeDigits(input: String): String =
        input.filter { it.isDigit() }.take(MAX_SEED_DIGITS)

    fun parseSeed(input: String): SeedParseResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return SeedParseResult.Empty
        if (!trimmed.all { it.isDigit() }) {
            return SeedParseResult.Invalid("Digits only (0 – 2,147,483,647)")
        }
        val parsed = trimmed.toLongOrNull()
        if (parsed == null || parsed < 0L || parsed > Int.MAX_VALUE.toLong()) {
            return SeedParseResult.Invalid("Seed must be between 0 and 2,147,483,647")
        }
        return SeedParseResult.Valid(parsed.toInt())
    }

    fun deriveVariantSeed(baseSeed: Int?, variantIndex: Int?): Int? {
        if (baseSeed == null) return null
        if (variantIndex == null || variantIndex <= 0) return baseSeed
        return ((baseSeed.toLong() + variantIndex.toLong()) % Int.MAX_VALUE.toLong()).toInt()
    }
}

enum class UpscaleFactor(val multiplier: Int, val label: String) {
    X2(2, "2×"),
    X4(4, "4×")
}

object UpscaleMath {
    const val MAX_OUTPUT_DIMENSION = 4096

    data class Dimensions(val width: Int, val height: Int)

    fun computeTargetDimensions(
        sourceWidth: Int,
        sourceHeight: Int,
        factor: UpscaleFactor,
        maxDimension: Int = MAX_OUTPUT_DIMENSION
    ): Dimensions {
        val safeW = sourceWidth.coerceAtLeast(1)
        val safeH = sourceHeight.coerceAtLeast(1)
        val desiredW = safeW.toLong() * factor.multiplier
        val desiredH = safeH.toLong() * factor.multiplier
        val longest = maxOf(desiredW, desiredH)
        return if (longest <= maxDimension) {
            Dimensions(desiredW.toInt(), desiredH.toInt())
        } else {
            val scale = maxDimension.toDouble() / longest.toDouble()
            Dimensions(
                width = (desiredW * scale).toInt().coerceAtLeast(1),
                height = (desiredH * scale).toInt().coerceAtLeast(1)
            )
        }
    }
}

enum class ImageFilterPreset(val label: String) {
    ORIGINAL("Original"),
    GRAYSCALE("Grayscale"),
    SEPIA("Sepia"),
    HIGH_CONTRAST("High Contrast"),
    WARM_FILM("Warm Film"),
    COOL_NOIR("Cool Noir")
}

object ImageFilterUtils {
    fun colorMatrixValues(preset: ImageFilterPreset): FloatArray? = when (preset) {
        ImageFilterPreset.ORIGINAL -> null
        ImageFilterPreset.GRAYSCALE -> floatArrayOf(
            0.2126f, 0.7152f, 0.0722f, 0f, 0f,
            0.2126f, 0.7152f, 0.0722f, 0f, 0f,
            0.2126f, 0.7152f, 0.0722f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        ImageFilterPreset.SEPIA -> floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        ImageFilterPreset.HIGH_CONTRAST -> {
            val c = 1.45f
            val t = (-0.5f * c + 0.5f) * 255f
            floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
        }
        ImageFilterPreset.WARM_FILM -> floatArrayOf(
            1.12f, 0.05f, 0f, 0f, 12f,
            0.02f, 1.03f, 0f, 0f, 4f,
            0f, 0f, 0.86f, 0f, -8f,
            0f, 0f, 0f, 1f, 0f
        )
        ImageFilterPreset.COOL_NOIR -> floatArrayOf(
            0.90f, 0f, 0.04f, 0f, -6f,
            0f, 0.96f, 0.05f, 0f, 0f,
            0.04f, 0.08f, 1.15f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
        )
    }
}

object TagUtils {
    fun normalizeSingleTag(raw: String): String =
        raw.trim().removePrefix("#").replace(Regex("\\s+"), " ").lowercase()

    fun parseTags(raw: String): List<String> =
        raw.split(",", "\n", ";")
            .map { normalizeSingleTag(it) }
            .filter { it.isNotEmpty() }
            .distinct()

    fun serializeTags(tags: List<String>): String =
        tags.map { normalizeSingleTag(it) }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(", ")

    fun mergeTags(existingRaw: String, additionsRaw: String): String {
        val merged = parseTags(existingRaw) + parseTags(additionsRaw)
        return serializeTags(merged)
    }

    fun removeTag(existingRaw: String, tagToRemove: String): String {
        val target = normalizeSingleTag(tagToRemove)
        return serializeTags(parseTags(existingRaw).filterNot { it == target })
    }
}

enum class InspirationCategory(val label: String) {
    ALL("Surprise Me"),
    LANDSCAPE("Landscapes"),
    CYBERPUNK("Sci-Fi"),
    ARCHITECTURE("Architecture"),
    PORTRAIT("Portraits"),
    FANTASY("Fantasy"),
    PRODUCT("Product & Macro")
}

data class PromptStarter(
    val id: String,
    val title: String,
    val category: InspirationCategory,
    val prompt: String,
    val negativePrompt: String,
    val style: StylePreset,
    val aspectRatio: AspectRatioOption
)

enum class AspectRatioOption(
    val label: String,
    val ratioFloat: Float,
    val subtitle: String
) {
    SQUARE("1:1", 1f, "Square"),
    LANDSCAPE("16:9", 16f / 9f, "Cinema"),
    PORTRAIT("9:16", 9f / 16f, "Story"),
    CLASSIC("4:3", 4f / 3f, "Photo"),
    VERTICAL("3:4", 3f / 4f, "Poster")
}

enum class ImageModelOption(
    val modelId: String,
    val displayName: String,
    val badge: String
) {
    NANO_BANANA_2_1(
        modelId = "gemini-nano-banana-2.1",
        displayName = "Nano Banana 2.1",
        badge = "Fast"
    ),
    STUDIO_PRO(
        modelId = "gemini-3-pro-image",
        displayName = "Studio Pro",
        badge = "Pro"
    )
}

enum class StylePreset(
    val label: String,
    val promptSuffix: String
) {
    NONE("No Style", ""),
    CINEMATIC(
        "Cinematic",
        "cinematic anamorphic film still, dramatic atmospheric lighting, rich color grading"
    ),
    OIL_PAINTING(
        "Oil Painting",
        "expressive impasto oil painting on linen canvas, rich brushwork, museum gallery lighting"
    ),
    CYBERPUNK(
        "Cyberpunk",
        "futuristic cyberpunk neon noir aesthetic, rain-slicked reflections, moody volumetric glow"
    ),
    PHOTOREALISTIC(
        "Photorealistic",
        "photorealistic 35mm editorial photography, natural lighting, crisp depth of field"
    ),
    ANIME(
        "Anime",
        "high-detail studio anime illustration, crisp cel shading, expressive atmospheric sky"
    ),
    CLAY_3D(
        "3D Clay",
        "tactile 3D claymation render, soft studio global illumination, miniature diorama feel"
    ),
    WATERCOLOR(
        "Watercolor",
        "delicate botanical watercolor and ink wash on textured cold-press cotton paper"
    ),
    MINIMAL_VECTOR(
        "Vector Art",
        "clean minimalist geometric vector art, bold negative space, balanced editorial composition"
    )
}

data class NegativePromptPreset(
    val id: String,
    val label: String,
    val keywords: String
)

data class NegativePromptExample(
    val id: String,
    val category: String,
    val title: String,
    val positivePrompt: String,
    val negativePrompt: String,
    val style: StylePreset,
    val whyItWorks: String
)

data class InspirationTemplate(
    val id: String,
    val title: String,
    val subtitle: String,
    @DrawableRes val imageRes: Int,
    val prompt: String,
    val negativePrompt: String,
    val style: StylePreset,
    val aspectRatio: AspectRatioOption
)

object StudioCatalog {
    val negativePresets = listOf(
        NegativePromptPreset(
            id = "no_watermark",
            label = "No Watermark / Text",
            keywords = "text, watermark, signature, logo, typography, captions"
        ),
        NegativePromptPreset(
            id = "no_blur",
            label = "No Blur / Artifacts",
            keywords = "blurry, out of focus, low resolution, jpeg artifacts, pixelation"
        ),
        NegativePromptPreset(
            id = "no_anatomy",
            label = "Clean Anatomy",
            keywords = "extra fingers, deformed hands, distorted face, unnatural proportions"
        ),
        NegativePromptPreset(
            id = "clean_bg",
            label = "Clean Background",
            keywords = "cluttered background, visual noise, distracting objects, photobombers"
        ),
        NegativePromptPreset(
            id = "no_people",
            label = "No People",
            keywords = "people, crowds, pedestrians, human figures"
        ),
        NegativePromptPreset(
            id = "natural_color",
            label = "No Oversaturation",
            keywords = "oversaturated, neon glare, harsh artificial contrast, plastic skin"
        )
    )

    val negativePromptExamples = listOf(
        NegativePromptExample(
            id = "ex_product",
            category = "Product Studio",
            title = "Clean Ceramic Product Shot",
            positivePrompt = "Matte obsidian ceramic vase with subtle glowing copper kintsugi seams resting on dark basalt stone, moody studio rim lighting",
            negativePrompt = "cluttered background, harsh reflections, watermark, text, dust, props, bright white studio",
            style = StylePreset.PHOTOREALISTIC,
            whyItWorks = "Excludes distracting props, stray text labels, and harsh specular glare so the silhouette and copper seams remain the hero."
        ),
        NegativePromptExample(
            id = "ex_portrait",
            category = "Editorial Portrait",
            title = "Natural Window-Lit Portrait",
            positivePrompt = "Close-up editorial portrait of an artisan ceramicist in a sunlit studio, soft golden window light, shallow depth of field",
            negativePrompt = "plastic skin, airbrushed, extra fingers, deformed hands, heavy makeup, studio flash, uncanny eyes",
            style = StylePreset.PHOTOREALISTIC,
            whyItWorks = "Prevents over-smoothed 'AI plastic' skin textures and common hand/facial anatomy distortions while preserving natural grain."
        ),
        NegativePromptExample(
            id = "ex_architecture",
            category = "Architecture",
            title = "Serene Nordic Timber Cabin",
            positivePrompt = "Minimalist Nordic timber cabin nestled beside a mirror-still misty fjord at dawn, soft warm interior lantern glow",
            negativePrompt = "power lines, tourists, modern cars, harsh midday shadows, oversaturated colors, HDR halos",
            style = StylePreset.CINEMATIC,
            whyItWorks = "Strips out modern visual clutter like utility poles, cars, and tourists while guarding against artificial HDR color halos."
        ),
        NegativePromptExample(
            id = "ex_illustration",
            category = "Illustration",
            title = "Botanical Field Guide Print",
            positivePrompt = "Detailed vintage botanical illustration of wild amber poppies and fern fronds on warm cream archival paper",
            negativePrompt = "3D render, photorealistic, drop shadow, dark background, messy ink splatters, modern frame",
            style = StylePreset.WATERCOLOR,
            whyItWorks = "Steers the model away from photographic or 3D shading so the artwork stays faithful to flat archival watercolor printmaking."
        )
    )

    val inspirationTemplates = listOf(
        InspirationTemplate(
            id = "insp_atelier",
            title = "Bioluminescent Greenhouse",
            subtitle = "Golden hour glass atelier",
            imageRes = R.drawable.img_inspiration_atelier_1795415244772,
            prompt = "Architectural sunlit glass greenhouse atelier filled with floating bioluminescent botanical sculptures, warm amber golden hour rays",
            negativePrompt = "crowds, modern signage, harsh neon glare, blurry, text, watermark",
            style = StylePreset.PHOTOREALISTIC,
            aspectRatio = AspectRatioOption.CLASSIC
        ),
        InspirationTemplate(
            id = "insp_ceramic",
            title = "Obsidian Kintsugi Vessel",
            subtitle = "Moody studio product study",
            imageRes = R.drawable.img_inspiration_cyber_ceramic_1795415246028,
            prompt = "Minimalist matte obsidian ceramic vase with subtle glowing copper kintsugi seams resting on dark basalt stone, moody studio rim lighting",
            negativePrompt = "cluttered background, harsh reflections, text, watermark, bright colors",
            style = StylePreset.PHOTOREALISTIC,
            aspectRatio = AspectRatioOption.CLASSIC
        )
    )

    val suggestedGalleryTags = listOf(
        "portfolio",
        "concept-art",
        "editorial",
        "wallpaper",
        "product",
        "character",
        "architecture",
        "print-ready"
    )

    val promptStarters = listOf(
        PromptStarter(
            id = "ps_fjord_dawn",
            title = "Mirror Fjord Cabin",
            category = InspirationCategory.LANDSCAPE,
            prompt = "Minimalist charred-cedar Nordic cabin perched above a mirror-still misty fjord at blue hour, warm amber lantern light reflecting on dark water",
            negativePrompt = "power lines, tourists, harsh midday sun, oversaturated colors, watermark",
            style = StylePreset.CINEMATIC,
            aspectRatio = AspectRatioOption.LANDSCAPE
        ),
        PromptStarter(
            id = "ps_autumn_canyon",
            title = "Redwood Mist Valley",
            category = InspirationCategory.LANDSCAPE,
            prompt = "Towering ancient coastal redwoods pierced by silver morning sunbeams above a mossy fern canyon and winding emerald stream",
            negativePrompt = "hikers, artificial neon, blurry, low contrast, text",
            style = StylePreset.PHOTOREALISTIC,
            aspectRatio = AspectRatioOption.VERTICAL
        ),
        PromptStarter(
            id = "ps_neon_tram",
            title = "Rain-Slicked Sky Tram",
            category = InspirationCategory.CYBERPUNK,
            prompt = "Elevated glass monorail gliding between rain-drenched brutalist megastructures wrapped in holographic kanji signage, moody cyan and amber reflections",
            negativePrompt = "daylight, cartoon, flat lighting, distorted text, blurry",
            style = StylePreset.CYBERPUNK,
            aspectRatio = AspectRatioOption.LANDSCAPE
        ),
        PromptStarter(
            id = "ps_orbital_botany",
            title = "Orbital Terrarium Deck",
            category = InspirationCategory.CYBERPUNK,
            prompt = "Curved zero-gravity botanical conservatory aboard a quiet orbital station overlooking a crescent planet, crisp sunlight filtering through lush ferns",
            negativePrompt = "cluttered UI, low detail, murky shadows, watermark",
            style = StylePreset.CINEMATIC,
            aspectRatio = AspectRatioOption.CLASSIC
        ),
        PromptStarter(
            id = "ps_travertine_courtyard",
            title = "Travertine Sun Courtyard",
            category = InspirationCategory.ARCHITECTURE,
            prompt = "Sculptural sunlit travertine courtyard with geometric arches casting long diagonal afternoon shadows over a shallow reflecting pool and olive tree",
            negativePrompt = "modern signage, clutter, pedestrians, warped perspective, HDR halos",
            style = StylePreset.PHOTOREALISTIC,
            aspectRatio = AspectRatioOption.CLASSIC
        ),
        PromptStarter(
            id = "ps_terracotta_library",
            title = "Spiral Atelier Library",
            category = InspirationCategory.ARCHITECTURE,
            prompt = "Double-height artisan library with curved walnut bookshelves, terracotta plaster walls, and a central brass reading table beneath a frosted oculus",
            negativePrompt = "messy piles, fluorescent tubes, lens distortion, watermark",
            style = StylePreset.OIL_PAINTING,
            aspectRatio = AspectRatioOption.PORTRAIT
        ),
        PromptStarter(
            id = "ps_glassblower_portrait",
            title = "Atelier Glassblower",
            category = InspirationCategory.PORTRAIT,
            prompt = "Editorial 50mm portrait of a seasoned glassblower beside a glowing furnace, molten amber glass catching warm rim light on weathered hands and goggles",
            negativePrompt = "plastic skin, extra fingers, uncanny eyes, harsh flash, watermark",
            style = StylePreset.PHOTOREALISTIC,
            aspectRatio = AspectRatioOption.VERTICAL
        ),
        PromptStarter(
            id = "ps_ink_samurai",
            title = "Moonlit Courtyard Wanderer",
            category = InspirationCategory.PORTRAIT,
            prompt = "Expressive anime key visual of a lone traveler in an indigo haori beneath drifting white plum blossoms and a pale full moon",
            negativePrompt = "3D render, stiff pose, muddy colors, extra limbs, text",
            style = StylePreset.ANIME,
            aspectRatio = AspectRatioOption.PORTRAIT
        ),
        PromptStarter(
            id = "ps_cloud_lighthouse",
            title = "Sky-Sea Lighthouse",
            category = InspirationCategory.FANTASY,
            prompt = "Brass-and-stone lighthouse built atop a floating basalt archipelago surrounded by luminouscumulus clouds and tiny paper-winged airships at dusk",
            negativePrompt = "photorealistic modern buildings, blurry, muddy palette, text",
            style = StylePreset.WATERCOLOR,
            aspectRatio = AspectRatioOption.VERTICAL
        ),
        PromptStarter(
            id = "ps_clockwork_owl",
            title = "Clay Automaton Owl",
            category = InspirationCategory.FANTASY,
            prompt = "Hand-sculpted miniature diorama of a curious brass-and-porcelain owl perched on stacked vintage leather books inside a warm cozy study",
            negativePrompt = "flat 2D, harsh glare, plastic texture, cluttered background",
            style = StylePreset.CLAY_3D,
            aspectRatio = AspectRatioOption.SQUARE
        ),
        PromptStarter(
            id = "ps_espresso_macro",
            title = "Brushed Brass Espresso",
            category = InspirationCategory.PRODUCT,
            prompt = "Studio macro product photograph of a brushed brass and walnut espresso portafilter resting on honed slate with rising steam curls in dramatic side light",
            negativePrompt = "logos, text labels, cluttered props, harsh specular clipping",
            style = StylePreset.PHOTOREALISTIC,
            aspectRatio = AspectRatioOption.SQUARE
        ),
        PromptStarter(
            id = "ps_bauhaus_poster",
            title = "Bauhaus Solar Poster",
            category = InspirationCategory.PRODUCT,
            prompt = "Mid-century geometric vector composition of nested terracotta circles, cobalt arches, and crisp ivory negative space balanced in Swiss grid harmony",
            negativePrompt = "photorealism, gradients, drop shadow, rough sketch, watermark",
            style = StylePreset.MINIMAL_VECTOR,
            aspectRatio = AspectRatioOption.VERTICAL
        )
    )
}
