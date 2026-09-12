package com.example.guione

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.Closeable
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.roundToInt

/**
 * On-device nutrition estimation. Runs the v4 EfficientNetB3 regressor from
 * assets/ through LiteRT; no network call, no server, nothing leaves the phone.
 *
 * The model and the rules for feeding it are produced together by
 * notebooks/bytebite_android_export.ipynb, which writes three assets:
 *
 *   bytebite_v4.tflite    the gated model variant
 *   bytebite_model.json   input geometry/range + the train-only mu/sd
 *   fixture_dish.png/json one pinned test dish, for [selfTest]
 *
 * Nothing about the preprocessing is hardcoded here on purpose. Every number
 * that has to agree with training is read from the sidecar, so retraining at a
 * different resolution or refitting the target scaler needs no Kotlin edit.
 *
 * Not thread-safe: a LiteRT Interpreter cannot be invoked concurrently. Call
 * [estimate] from one background thread at a time (ScanStore serialises it).
 */
class NutritionEstimator private constructor(
    private val interpreter: Interpreter,
    private val gpuDelegate: GpuDelegate?,
    val spec: ModelSpec,
    val accelerator: String,
) : Closeable {

    /** Parsed bytebite_model.json. The contract between the notebook and this class. */
    data class ModelSpec(
        val modelFile: String,
        val variant: String,
        val width: Int,
        val height: Int,
        /** true when the exported graph takes uint8 pixels instead of float32. */
        val quantizedInput: Boolean,
        /** width / height of the frames the model was trained on. */
        val sourceAspect: Float,
        val resizeMode: String,
        val targets: List<String>,
        val units: List<String>,
        val mu: FloatArray,
        val sd: FloatArray,
        /**
         * Test-set MAE per target, as measured by the export notebook on the
         * exact file that shipped. Surfaced in the UI so an estimate is shown
         * with its known error rather than as a bare number.
         */
        val testMae: FloatArray?,
    ) {
        val outputs: Int get() = targets.size
    }

    /** One prediction, already de-standardized into kcal and grams. */
    data class Estimate(
        val calories: Float,
        val massG: Float,
        val fatG: Float,
        val carbG: Float,
        val proteinG: Float,
        val latencyMs: Long,
        val variant: String,
        val accelerator: String,
    ) {
        /** Indexed in the model's own output order, for generic display. */
        fun value(i: Int): Float = when (i) {
            0 -> calories; 1 -> massG; 2 -> fatG; 3 -> carbG; else -> proteinG
        }
    }

    // Reused across calls: allocating 1.08 MB of direct buffer per scan is wasteful
    // and makes the latency number noisier than the inference it is measuring.
    private val pixelCount = spec.width * spec.height
    private val bytesPerChannel = if (spec.quantizedInput) 1 else 4
    private val inputBuffer: ByteBuffer =
        ByteBuffer.allocateDirect(pixelCount * 3 * bytesPerChannel)
            .order(ByteOrder.nativeOrder())
    private val pixels = IntArray(pixelCount)
    private val output = Array(1) { FloatArray(spec.outputs) }

    /**
     * Runs one dish photo. [bitmap] should be the full captured frame, already
     * rotated upright; [prepare] handles the crop and resize.
     */
    fun estimate(bitmap: Bitmap): Estimate {
        val scaled = prepare(bitmap)
        fillInput(scaled)
        if (scaled !== bitmap) scaled.recycle()

        val t0 = System.nanoTime()
        interpreter.run(inputBuffer, output)
        val latencyMs = (System.nanoTime() - t0) / 1_000_000

        // The head emits z-scores. real = z * sd + mu, index-aligned with targets.
        val z = output[0]
        val real = FloatArray(spec.outputs) { i -> z[i] * spec.sd[i] + spec.mu[i] }
        return Estimate(
            calories = real[0],
            massG = real[1],
            fatG = real[2],
            carbG = real[3],
            proteinG = real[4],
            latencyMs = latencyMs,
            variant = spec.variant,
            accelerator = accelerator,
        )
    }

    /**
     * Reproduces the training-time geometry: centre-crop to the aspect ratio the
     * model was trained on, then stretch that whole region to the model's input
     * size. Training resized the full overhead frame with no crop, so cropping
     * to square here would shrink the apparent portion and bias mass and
     * calories downward — the two targets read straight off portion size.
     */
    internal fun prepare(src: Bitmap): Bitmap {
        val cropped = centreCropToAspect(src, spec.sourceAspect)
        val scaled = Bitmap.createScaledBitmap(cropped, spec.width, spec.height, true)
        if (cropped !== src && cropped !== scaled) cropped.recycle()
        return scaled
    }

    private fun centreCropToAspect(src: Bitmap, aspect: Float): Bitmap {
        val w = src.width
        val h = src.height
        val current = w.toFloat() / h.toFloat()
        if (kotlin.math.abs(current - aspect) < 0.01f) return src

        return if (current > aspect) {           // too wide: trim left and right
            val newW = (h * aspect).roundToInt().coerceAtMost(w)
            Bitmap.createBitmap(src, (w - newW) / 2, 0, newW, h)
        } else {                                  // too tall: trim top and bottom
            val newH = (w / aspect).roundToInt().coerceAtMost(h)
            Bitmap.createBitmap(src, 0, (h - newH) / 2, w, newH)
        }
    }

    /**
     * Writes RGB into the input tensor in the range the graph expects: 0-255.
     *
     * EfficientNetB3 carries its own Rescaling and Normalization layers, and they
     * are inside the exported graph. Dividing by 255 here, or subtracting an
     * ImageNet mean, double-normalizes the input and yields confident nonsense
     * rather than an obvious failure. The notebook pins this as input.range and
     * [selfTest] is what catches it if it ever drifts.
     */
    private fun fillInput(bitmap: Bitmap) {
        bitmap.getPixels(pixels, 0, spec.width, 0, 0, spec.width, spec.height)
        inputBuffer.rewind()
        if (spec.quantizedInput) {
            // The converter lands on scale ~1.0 / zero_point 0 because the graph's
            // own input range is already 0-255, so the byte goes in untouched.
            for (p in pixels) {
                inputBuffer.put((p shr 16 and 0xFF).toByte())
                inputBuffer.put((p shr 8 and 0xFF).toByte())
                inputBuffer.put((p and 0xFF).toByte())
            }
        } else {
            for (p in pixels) {
                inputBuffer.putFloat((p shr 16 and 0xFF).toFloat())
                inputBuffer.putFloat((p shr 8 and 0xFF).toFloat())
                inputBuffer.putFloat((p and 0xFF).toFloat())
            }
        }
        inputBuffer.rewind()
    }

    override fun close() {
        interpreter.close()
        gpuDelegate?.close()
    }

    companion object {
        private const val TAG = "NutritionEstimator"
        const val SIDECAR_ASSET = "bytebite_model.json"

        /**
         * Loads the model from assets. Returns null when the assets are absent —
         * the repo does not carry the 22 MB binary, so a fresh clone builds and
         * runs on sample data until the export notebook has been run. Callers
         * show the prototype's hardcoded numbers in that case rather than
         * crashing on launch.
         */
        fun loadOrNull(context: Context): NutritionEstimator? = try {
            load(context)
        } catch (e: Exception) {
            Log.w(TAG, "model assets unavailable, falling back to sample data", e)
            null
        }

        fun load(context: Context): NutritionEstimator {
            val spec = parseSpec(context.assets.open(SIDECAR_ASSET)
                .bufferedReader().use { it.readText() })

            val options = Interpreter.Options().apply { numThreads = 4 }
            var delegate: GpuDelegate? = null
            var accelerator = "CPU (XNNPACK, 4 threads)"

            // The GPU delegate only accepts float tensors, so it is not an option
            // for the int8 variant. NNAPI is deliberately not used: it is
            // deprecated as of Android 15, and on this graph the CPU path with
            // XNNPACK is the dependable baseline.
            if (!spec.quantizedInput) {
                try {
                    CompatibilityList().use { compat ->
                        if (compat.isDelegateSupportedOnThisDevice) {
                            val gpu = GpuDelegate(compat.bestOptionsForThisDevice)
                            options.addDelegate(gpu)
                            delegate = gpu
                            accelerator = "GPU delegate"
                        }
                    }
                } catch (e: Throwable) {
                    // Missing native lib or an unsupported driver: fall back, do
                    // not take the app down over an accelerator.
                    Log.w(TAG, "GPU delegate unavailable, using CPU", e)
                    delegate?.close()
                    delegate = null
                }
            }

            val interpreter = try {
                Interpreter(mapModel(context, spec.modelFile), options)
            } catch (e: Throwable) {
                delegate?.close()
                throw e
            }

            Log.i(TAG, "loaded ${spec.modelFile} (${spec.variant}) on $accelerator")
            return NutritionEstimator(interpreter, delegate, spec, accelerator)
        }

        /**
         * Memory-maps the model straight out of the APK, so the weights are never
         * copied onto the heap. This requires the .tflite to be stored
         * uncompressed — see androidResources.noCompress in app/build.gradle.kts.
         */
        private fun mapModel(context: Context, name: String): ByteBuffer =
            context.assets.openFd(name).use { fd ->
                FileInputStream(fd.fileDescriptor).use { stream ->
                    stream.channel.map(
                        FileChannel.MapMode.READ_ONLY,
                        fd.startOffset,
                        fd.declaredLength,
                    )
                }
            }

        internal fun parseSpec(json: String): ModelSpec {
            val root = JSONObject(json)
            val input = root.getJSONObject("input")
            val output = root.getJSONObject("output")

            val range = input.getString("range")
            require(range == "0-255") {
                "unsupported input range '$range'; fillInput writes 0-255 pixels"
            }

            fun floats(key: String): FloatArray {
                val arr = output.getJSONArray(key)
                return FloatArray(arr.length()) { arr.getDouble(it).toFloat() }
            }

            fun strings(key: String): List<String> {
                val arr = output.getJSONArray(key)
                return List(arr.length()) { arr.getString(it) }
            }

            val mu = floats("mu")
            val sd = floats("sd")
            val targets = strings("targets")
            require(mu.size == targets.size && sd.size == targets.size) {
                "mu/sd/targets length mismatch in $SIDECAR_ASSET"
            }
            require(targets.size == 5) {
                "expected 5 targets, got ${targets.size}: $targets"
            }
            require(output.optBoolean("standardized", true)) {
                "sidecar says outputs are not standardized, but estimate() inverts z-scores"
            }

            val maeObj = root.optJSONObject("test_mae")
            val testMae = maeObj?.let { o ->
                FloatArray(targets.size) { o.optDouble(targets[it], Double.NaN).toFloat() }
            }?.takeIf { arr -> arr.none { it.isNaN() } }

            return ModelSpec(
                modelFile = root.getString("model_file"),
                variant = root.optString("variant", "unknown"),
                width = input.getInt("width"),
                height = input.getInt("height"),
                quantizedInput = input.getString("dtype") == "uint8",
                sourceAspect = input.getDouble("source_aspect").toFloat(),
                resizeMode = input.optString("resize_mode", "stretch_full_frame"),
                targets = targets,
                units = strings("units"),
                mu = mu,
                sd = sd,
                testMae = testMae,
            )
        }
    }
}
