package com.example.guione

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the downsample math in DishCapture. A wrong sample size either
 * decodes a huge frame (slow, risks OOM) or one smaller than the model input.
 */
class DishCaptureTest {

    @Test
    fun `small images are not downsampled`() {
        assertEquals(1, sampleSizeFor(1000, 1000, 1280))
        // Exactly at the edge: halving would drop below maxEdge, so keep full size.
        assertEquals(1, sampleSizeFor(1280, 1280, 1280))
    }

    @Test
    fun `large images halve until the long edge is near the cap`() {
        assertEquals(2, sampleSizeFor(4000, 3000, 1280))
        assertEquals(4, sampleSizeFor(8000, 6000, 1280))
    }

    @Test
    fun `orientation does not change the sample size`() {
        assertEquals(
            sampleSizeFor(4000, 3000, 1280),
            sampleSizeFor(3000, 4000, 1280),
        )
    }
}
