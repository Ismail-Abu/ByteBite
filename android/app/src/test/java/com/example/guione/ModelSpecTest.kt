package com.example.guione

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for the sidecar contract. These need no device and no model file, so
 * they catch a malformed or mismatched bytebite_model.json before anything is
 * installed.
 */
class ModelSpecTest {

    private fun sidecar(
        range: String = "0-255",
        dtype: String = "float32",
        targets: String = """["calories","mass","fat","carb","protein"]""",
        mu: String = "[255.1,214.9,12.7,19.4,18.2]",
        sd: String = "[230.4,162.3,11.8,17.1,16.5]",
        standardized: Boolean = true,
        extra: String = "",
    ) = """
    {
      "model_file": "bytebite_v4.tflite",
      "variant": "float16",
      "input": {
        "width": 300, "height": 300, "channels": 3,
        "dtype": "$dtype", "range": "$range",
        "channel_order": "RGB", "resize_mode": "stretch_full_frame",
        "source_width": 640, "source_height": 480, "source_aspect": 1.333333
      },
      "output": {
        "targets": $targets,
        "units": ["kcal","g","g","g","g"],
        "standardized": $standardized,
        "mu": $mu,
        "sd": $sd
      }$extra
    }
    """.trimIndent()

    @Test
    fun `parses a well formed sidecar`() {
        val spec = NutritionEstimator.parseSpec(sidecar())
        assertEquals(300, spec.width)
        assertEquals(300, spec.height)
        assertEquals(5, spec.outputs)
        assertFalse(spec.quantizedInput)
        assertEquals(1.333333f, spec.sourceAspect, 1e-5f)
        assertEquals(listOf("calories", "mass", "fat", "carb", "protein"), spec.targets)
        assertArrayEquals(floatArrayOf(230.4f, 162.3f, 11.8f, 17.1f, 16.5f), spec.sd, 1e-3f)
    }

    @Test
    fun `uint8 dtype selects the quantized input path`() {
        assertTrue(NutritionEstimator.parseSpec(sidecar(dtype = "uint8")).quantizedInput)
    }

    /**
     * The guard that matters most. fillInput writes 0-255 pixels because
     * EfficientNetB3 normalizes inside the graph; an export that wanted 0-1 would
     * otherwise be fed values 255x too large and still return plausible numbers.
     */
    @Test(expected = IllegalArgumentException::class)
    fun `rejects an input range fillInput cannot honour`() {
        NutritionEstimator.parseSpec(sidecar(range = "0-1"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects mu sd targets length mismatch`() {
        NutritionEstimator.parseSpec(sidecar(mu = "[1.0,2.0]"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a head that is not five outputs`() {
        NutritionEstimator.parseSpec(
            sidecar(
                targets = """["calories","mass","carb"]""",
                mu = "[1.0,2.0,3.0]",
                sd = "[1.0,1.0,1.0]",
            )
        )
    }

    @Test
    fun `test mae is optional and read when present`() {
        assertNull(NutritionEstimator.parseSpec(sidecar()).testMae)

        val withMae = NutritionEstimator.parseSpec(
            sidecar(
                extra = ""","test_mae":{"calories":42.02,"mass":27.0,"fat":3.17,""" +
                    """"carb":4.12,"protein":4.29}"""
            )
        )
        assertArrayEquals(
            floatArrayOf(42.02f, 27.0f, 3.17f, 4.12f, 4.29f),
            withMae.testMae,
            1e-3f,
        )
    }

    /**
     * The v1 model was trained on raw kcal and grams. Its sidecar says so, and the
     * scaling must collapse to the identity whatever mu/sd happen to be present.
     */
    @Test
    fun `raw unit head gets identity scaling`() {
        val spec = NutritionEstimator.parseSpec(sidecar(standardized = false))
        assertArrayEquals(FloatArray(5), spec.mu, 0f)
        assertArrayEquals(FloatArray(5) { 1f }, spec.sd, 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `standardized head without stats is rejected`() {
        NutritionEstimator.parseSpec(
            sidecar().replace(Regex(""""mu": \[[^\]]*\],"""), "")
        )
    }

    /** Missing sd is as fatal as missing mu: half a scaler inverts to nonsense. */
    @Test(expected = IllegalArgumentException::class)
    fun `standardized head without sd is rejected`() {
        NutritionEstimator.parseSpec(
            sidecar().replace(Regex(""",\s*"sd": \[[^\]]*\]"""), "")
        )
    }

    /** The int8 export still ships a z-scored head, so mu/sd must still be read. */
    @Test
    fun `quantized input still parses the standardized scaler`() {
        val spec = NutritionEstimator.parseSpec(sidecar(dtype = "uint8"))
        assertTrue(spec.quantizedInput)
        assertArrayEquals(
            floatArrayOf(255.1f, 214.9f, 12.7f, 19.4f, 18.2f), spec.mu, 1e-3f,
        )
    }

    /** A partial test_mae block is dropped rather than half-displayed. */
    @Test
    fun `incomplete test mae is ignored`() {
        val spec = NutritionEstimator.parseSpec(
            sidecar(extra = ""","test_mae":{"calories":42.02}""")
        )
        assertNull(spec.testMae)
    }

    @Test
    fun `parses geometry the preprocessor depends on`() {
        val spec = NutritionEstimator.parseSpec(sidecar())
        assertEquals(1.333333f, spec.sourceAspect, 1e-5f)
        // resize_mode is what tells prepare() to stretch the full frame rather than
        // crop to square; a wrong default here silently biases mass and calories.
        assertEquals("stretch_full_frame", spec.resizeMode)
        assertEquals(listOf("kcal", "g", "g", "g", "g"), spec.units)
    }

    @Test
    fun `variant defaults to unknown when the sidecar omits it`() {
        val spec = NutritionEstimator.parseSpec(
            sidecar().replace(""""variant": "float16",""", "")
        )
        assertEquals("unknown", spec.variant)
    }

    @Test
    fun `resize_mode falls back to stretching the full frame`() {
        val spec = NutritionEstimator.parseSpec(
            sidecar().replace(""""resize_mode": "stretch_full_frame",""", "")
        )
        assertEquals("stretch_full_frame", spec.resizeMode)
    }
}
