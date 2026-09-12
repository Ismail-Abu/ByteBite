package com.example.guione

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * On-device parity check against the fixture the export notebook pinned.
 *
 * This is the test that makes the port trustworthy. It runs the real interpreter
 * on a real Nutrition5k test dish and asserts the phone reproduces the number the
 * notebook measured for that same dish, within a tolerance the notebook chose. If
 * the Kotlin preprocessing drifts from the training pipeline — a stray /255, a
 * square crop, BGR instead of RGB, mu/sd applied in the wrong order — the
 * estimate moves well outside tolerance and this fails.
 *
 * Skipped (not failed) when assets/ has no model, so a fresh clone still has a
 * green test run before the notebook has been executed.
 */
@RunWith(AndroidJUnit4::class)
class ModelFixtureTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * All three generated assets must be present, the .tflite included. Checking
     * only the sidecar would let a half-installed assets/ fail the suite instead
     * of skipping it.
     */
    private fun assetsPresent(): Boolean {
        val names = context.assets.list("")?.toSet() ?: emptySet()
        if (NutritionEstimator.SIDECAR_ASSET !in names || "fixture_dish.json" !in names) {
            return false
        }
        val modelFile = JSONObject(
            context.assets.open(NutritionEstimator.SIDECAR_ASSET).bufferedReader()
                .use { it.readText() }
        ).getString("model_file")
        return modelFile in names && "fixture_dish.png" in names
    }

    @Test
    fun reproducesNotebookFixture() {
        assumeTrue("no exported model in assets/ - run the export notebook", assetsPresent())

        val fixture = JSONObject(
            context.assets.open("fixture_dish.json").bufferedReader().use { it.readText() }
        )
        val expected = fixture.getJSONArray("expected_real")
        val tolerance = fixture.getJSONArray("tolerance_real")

        // The fixture image is already 300x300, so prepare() should be a no-op on
        // it: aspect matches within epsilon and the scale is identity. That makes
        // this a test of the tensor path, not of the resize.
        val bitmap = context.assets.open(fixture.getString("image_asset")).use {
            BitmapFactory.decodeStream(it)
        }.copy(Bitmap.Config.ARGB_8888, false)

        NutritionEstimator.load(context).use { estimator ->
            val e = estimator.estimate(bitmap)
            val targets = estimator.spec.targets

            for (i in targets.indices) {
                val got = e.value(i)
                val want = expected.getDouble(i).toFloat()
                val tol = tolerance.getDouble(i).toFloat()
                assertTrue(
                    "${targets[i]}: got $got, notebook said $want (tolerance $tol). " +
                        "A large miss here usually means the preprocessing diverged " +
                        "from the training pipeline, not that the model is wrong.",
                    abs(got - want) <= tol,
                )
            }
        }
    }

    @Test
    fun sidecarMatchesTheLoadedGraph() {
        assumeTrue("no exported model in assets/", assetsPresent())

        NutritionEstimator.load(context).use { estimator ->
            val spec = estimator.spec
            assertEquals(300, spec.width)
            assertEquals(300, spec.height)
            assertEquals(5, spec.outputs)
            // mu/sd have to be finite and non-degenerate or the inverse z-score
            // silently collapses every dish onto the training mean.
            for (i in 0 until spec.outputs) {
                assertTrue("sd[$i] must be positive", spec.sd[i] > 0f)
                assertTrue("mu[$i] must be finite", spec.mu[i].isFinite())
            }
        }
    }

    /**
     * Feeding the same dish twice must give the same answer. Catches a buffer that
     * is not rewound between runs, which shows up as a correct first scan followed
     * by garbage.
     */
    @Test
    fun repeatedScansAreStable() {
        assumeTrue("no exported model in assets/", assetsPresent())

        val fixture = JSONObject(
            context.assets.open("fixture_dish.json").bufferedReader().use { it.readText() }
        )
        fun frame() = context.assets.open(fixture.getString("image_asset")).use {
            BitmapFactory.decodeStream(it)
        }.copy(Bitmap.Config.ARGB_8888, false)

        NutritionEstimator.load(context).use { estimator ->
            val first = estimator.estimate(frame())
            val second = estimator.estimate(frame())
            assertEquals(first.calories, second.calories, 1e-3f)
            assertEquals(first.carbG, second.carbG, 1e-3f)
        }
    }
}
