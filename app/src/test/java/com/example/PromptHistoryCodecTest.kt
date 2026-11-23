package com.example

import com.example.data.repository.PromptHistoryCodec
import com.example.data.repository.PromptHistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptHistoryCodecTest {

    @Test
    fun recordEntry_keepsMostRecentTenAndDeduplicatesCaseInsensitive() {
        var entries = emptyList<PromptHistoryEntry>()
        for (i in 1..12) {
            entries = PromptHistoryCodec.recordEntry(
                existing = entries,
                newEntry = PromptHistoryEntry(
                    prompt = "Prompt number $i",
                    styleName = "Cinematic",
                    usedAt = i.toLong()
                )
            )
        }

        assertEquals(10, entries.size)
        assertEquals("Prompt number 12", entries.first().prompt)
        assertEquals("Prompt number 3", entries.last().prompt)

        val bumped = PromptHistoryCodec.recordEntry(
            existing = entries,
            newEntry = PromptHistoryEntry(
                prompt = "  prompt number 5  ",
                styleName = "Oil Painting",
                usedAt = 99L
            )
        )
        assertEquals(10, bumped.size)
        assertEquals("prompt number 5", bumped.first().prompt)
        assertEquals(1, bumped.count { it.prompt.equals("prompt number 5", ignoreCase = true) })
    }

    @Test
    fun filterEntries_matchesPromptNegativeOrStyle() {
        val sample = listOf(
            PromptHistoryEntry(
                prompt = "Sunlit ceramic studio",
                negativePrompt = "clutter, blurry",
                styleName = "Photorealistic"
            ),
            PromptHistoryEntry(
                prompt = "Neon rainy alley",
                negativePrompt = "daylight",
                styleName = "Cyberpunk"
            )
        )

        assertEquals(1, PromptHistoryCodec.filterEntries(sample, "ceramic").size)
        assertEquals(1, PromptHistoryCodec.filterEntries(sample, "cyber").size)
        assertEquals(1, PromptHistoryCodec.filterEntries(sample, "daylight").size)
        assertEquals(2, PromptHistoryCodec.filterEntries(sample, "   ").size)
        assertTrue(PromptHistoryCodec.filterEntries(sample, "watercolor").isEmpty())
    }

    @Test
    fun recordEntryAndFilter_preservesSeedAndSupportsSeedSearch() {
        val recorded = PromptHistoryCodec.recordEntry(
            existing = listOf(
                PromptHistoryEntry(
                    prompt = "Misty fjord cabin",
                    styleName = "Cinematic",
                    aspectRatioLabel = "16:9",
                    seed = null,
                    usedAt = 900L
                )
            ),
            newEntry = PromptHistoryEntry(
                prompt = "Kintsugi obsidian teapot",
                styleName = "Photorealistic",
                aspectRatioLabel = "1:1",
                seed = 847291,
                usedAt = 1_000L
            )
        )

        assertEquals(2, recorded.size)
        assertEquals(847291, recorded[0].seed)
        assertEquals(null, recorded[1].seed)

        val matched = PromptHistoryCodec.filterEntries(recorded, "847291")
        assertEquals(1, matched.size)
        assertEquals("Kintsugi obsidian teapot", matched.first().prompt)
    }
}
