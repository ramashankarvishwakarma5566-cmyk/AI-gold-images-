package com.example

import com.example.data.repository.PdfPageMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfPageMathTest {

    @Test
    fun fitAspectRect_centersLandscapeInsideSquareBox() {
        val fitted = PdfPageMath.fitAspectRect(
            imageWidth = 1600,
            imageHeight = 900,
            boxLeft = 50f,
            boxTop = 100f,
            boxWidth = 400f,
            boxHeight = 400f
        )

        assertEquals(50f, fitted.left, 0.01f)
        assertEquals(400f, fitted.width, 0.01f)
        assertEquals(225f, fitted.height, 0.01f)
        assertEquals(100f + (400f - 225f) / 2f, fitted.top, 0.01f)
    }

    @Test
    fun fitAspectRect_centersPortraitInsideLandscapeBox() {
        val fitted = PdfPageMath.fitAspectRect(
            imageWidth = 900,
            imageHeight = 1600,
            boxLeft = 0f,
            boxTop = 0f,
            boxWidth = 500f,
            boxHeight = 400f
        )

        assertEquals(400f, fitted.height, 0.01f)
        assertEquals(225f, fitted.width, 0.01f)
        assertEquals((500f - 225f) / 2f, fitted.left, 0.01f)
        assertTrue(fitted.right <= 500f)
    }

    @Test
    fun sanitizeFileName_handlesBlankAndSpecialChars() {
        assertEquals("Lumina_Portfolio", PdfPageMath.sanitizeFileName("   "))
        assertEquals("Spring_Collection_2026", PdfPageMath.sanitizeFileName("Spring Collection / 2026!"))
    }
}
