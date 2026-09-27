package com.example.guione

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM tests for the pure helpers in [SampleData]. They exercise the slot and
 * carb-total logic that drives what the log and Today card show, without a device.
 */
class SampleDataTest {

    @Test
    fun `slotFor maps the day into the three meal slots`() {
        // Early morning before breakfast still reads as the previous evening.
        assertEquals("dinner", SampleData.slotFor(3))
        assertEquals("breakfast", SampleData.slotFor(4))
        assertEquals("breakfast", SampleData.slotFor(10))
        assertEquals("lunch", SampleData.slotFor(11))
        assertEquals("lunch", SampleData.slotFor(15))
        assertEquals("dinner", SampleData.slotFor(16))
        assertEquals("dinner", SampleData.slotFor(23))
    }
}
