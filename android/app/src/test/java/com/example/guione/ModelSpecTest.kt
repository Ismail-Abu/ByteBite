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
        "standardized": true,
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

    /** A partial test_mae block is dropped rather than half-displayed. */
    @Test
    fun `incomplete test mae is ignored`() {
        val spec = NutritionEstimator.parseSpec(
            sidecar(extra = ""","test_mae":{"calories":42.02}""")
        )
        assertNull(spec.testMae)
    }
}
