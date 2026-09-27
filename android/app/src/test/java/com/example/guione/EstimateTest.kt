package com.example.guione

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The result screens read an [NutritionEstimator.Estimate] generically by output
 * index (`value(i)`), so the index-to-field mapping is a contract: if it drifts,
 * every screen shows the right numbers under the wrong labels. These JVM tests
 * pin it without needing a model or a device.
 */
class EstimateTest {

    private fun estimate() = NutritionEstimator.Estimate(
        calories = 542f,
        massG = 387f,
        fatG = 18.2f,
        carbG = 45.6f,
        proteinG = 42.1f,
        latencyMs = 12,
        variant = "float16",
        accelerator = "CPU",
    )

    @Test
    fun `value maps each index to the matching field`() {
        val e = estimate()
        assertEquals(e.calories, e.value(0), 0f)
        assertEquals(e.massG, e.value(1), 0f)
        assertEquals(e.fatG, e.value(2), 0f)
        assertEquals(e.carbG, e.value(3), 0f)
        assertEquals(e.proteinG, e.value(4), 0f)
    }

    /** The `else` branch: any index past the fifth reads protein, by design. */
    @Test
    fun `value past the last index falls through to protein`() {
        val e = estimate()
        assertEquals(e.proteinG, e.value(5), 0f)
    }
}
