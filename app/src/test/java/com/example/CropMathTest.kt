package com.example

import com.example.data.repository.ImageStorageHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CropMathTest {

    @Test
    fun computeCropPixelRect_calculatesExpectedCenterSquare() {
        val rect = ImageStorageHelper.computeCropPixelRect(
            imageWidth = 1000,
            imageHeight = 1000,
            leftFraction = 0.25f,
            topFraction = 0.25f,
            rightFraction = 0.75f,
            bottomFraction = 0.75f
        )
        assertEquals(250, rect.x)
        assertEquals(250, rect.y)
        assertEquals(500, rect.width)
        assertEquals(500, rect.height)
    }

    @Test
    fun computeCropPixelRect_clampsOutOfRangeValuesSafely() {
        val rect = ImageStorageHelper.computeCropPixelRect(
            imageWidth = 800,
            imageHeight = 600,
            leftFraction = -0.5f,
            topFraction = -0.2f,
            rightFraction = 1.5f,
            bottomFraction = 1.2f
        )
        assertEquals(0, rect.x)
        assertEquals(0, rect.y)
        assertEquals(800, rect.width)
        assertEquals(600, rect.height)
        assertTrue(rect.width >= 1 && rect.height >= 1)
    }
}
