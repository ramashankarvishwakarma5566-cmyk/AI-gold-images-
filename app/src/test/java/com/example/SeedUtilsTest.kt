package com.example

import com.example.model.SeedParseResult
import com.example.model.SeedUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedUtilsTest {

    @Test
    fun sanitizeDigits_stripsNonDigitsAndLimitsLength() {
        assertEquals("1234567890", SeedUtils.sanitizeDigits("abc12-34 56.7890999"))
        assertEquals("", SeedUtils.sanitizeDigits("-xyz!"))
    }

    @Test
    fun parseSeed_handlesEmptyValidAndInvalidInputs() {
        assertEquals(SeedParseResult.Empty, SeedUtils.parseSeed(""))
        assertEquals(SeedParseResult.Empty, SeedUtils.parseSeed("   "))

        assertEquals(SeedParseResult.Valid(0), SeedUtils.parseSeed("0"))
        assertEquals(SeedParseResult.Valid(42), SeedUtils.parseSeed(" 42 "))
        assertEquals(
            SeedParseResult.Valid(Int.MAX_VALUE),
            SeedUtils.parseSeed(Int.MAX_VALUE.toString())
        )

        assertTrue(SeedUtils.parseSeed("-1") is SeedParseResult.Invalid)
        assertTrue(SeedUtils.parseSeed("12ab") is SeedParseResult.Invalid)
        assertTrue(SeedUtils.parseSeed("2147483648") is SeedParseResult.Invalid)
    }

    @Test
    fun deriveVariantSeed_derivesDeterministicSequenceAndWrapsSafely() {
        assertNull(SeedUtils.deriveVariantSeed(null, 0))
        assertNull(SeedUtils.deriveVariantSeed(null, 3))

        assertEquals(100, SeedUtils.deriveVariantSeed(100, null))
        assertEquals(100, SeedUtils.deriveVariantSeed(100, 0))
        assertEquals(101, SeedUtils.deriveVariantSeed(100, 1))
        assertEquals(102, SeedUtils.deriveVariantSeed(100, 2))
        assertEquals(103, SeedUtils.deriveVariantSeed(100, 3))

        assertEquals(0, SeedUtils.deriveVariantSeed(Int.MAX_VALUE - 1, 1))
        assertEquals(1, SeedUtils.deriveVariantSeed(Int.MAX_VALUE - 1, 2))
    }
}
