package com.example.guione

import androidx.compose.ui.graphics.Color
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

    private fun meal(carbs: Double, date: java.time.LocalDate) = GlucoseMeal(
        name = "test", carbs = carbs, time = "12:00", source = "test",
        emoji = "", tint = Color.White, spike = "gentle rise", spikeBad = false, date = date,
    )

    @Test
    fun `carbsToday sums only todays meals and rounds`() {
        val meals = listOf(
            meal(45.6, SampleData.today),
            meal(13.0, SampleData.today),
            meal(52.0, SampleData.today.minusDays(1)),   // yesterday: excluded
        )
        // 45.6 + 13.0 = 58.6 -> 59; the 52.0 from yesterday must not count.
        assertEquals(59, SampleData.carbsToday(meals))
    }

    @Test
    fun `carbsToday is zero when nothing was logged today`() {
        val meals = listOf(meal(80.0, SampleData.today.minusDays(1)))
        assertEquals(0, SampleData.carbsToday(meals))
        assertEquals(0, SampleData.carbsToday(emptyList()))
    }
}
