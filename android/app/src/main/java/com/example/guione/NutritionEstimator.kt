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
        /**
         * Unique per scan. Two dishes can legitimately produce identical numbers,
         * so logging de-duplicates on this rather than on the values.
         */
        val scanId: Long = System.nanoTime(),
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
        try {
            return estimatePrepared(scaled)
        } finally {
            if (scaled !== bitmap) scaled.recycle()
        }
    }

    /**
     * Runs a bitmap that is already the model-sized frame, skipping [prepare].
     * The fixture test enters here: its image is the resized frame itself, and
     * pushing it through [prepare] again would crop a square image to the source
     * aspect ratio and change what the model sees.
     */
    internal fun estimatePrepared(scaled: Bitmap): Estimate {
        require(scaled.width == spec.width && scaled.height == spec.height) {
            "expected a ${spec.width}x${spec.height} frame, got ${scaled.width}x${scaled.height}"
        }
        fillInput(scaled)

        val t0 = System.nanoTime()
        interpreter.run(inputBuffer, output)
        val latencyMs = (System.nanoTime() - t0) / 1_000_000

        // real = z * sd + mu, index-aligned with targets. A head trained on raw
        // kcal and grams ships mu = 0 and sd = 1, so the same line is the identity.
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
        val turned = matchSourceOrientation(src, spec.sourceAspect)
        val cropped = centreCropToAspect(turned, spec.sourceAspect)
        val scaled = Bitmap.createScaledBitmap(cropped, spec.width, spec.height, true)
        if (cropped !== turned && cropped !== scaled) cropped.recycle()
        if (turned !== src && turned !== scaled) turned.recycle()
        return scaled
    }

    /**
     * Rotates a portrait frame to landscape when the training frames were
     * landscape, or the reverse. Someone holding a phone upright over a plate
     * takes a 3:4 photo; the Nutrition5k rig produced 4:3. Cropping 3:4 down to
     * 4:3 would discard almost half the plate, while a quarter turn discards
     * nothing - and the v4 line trained with random 90-degree rotations, so an
     * overhead dish has no orientation the model prefers.
     */
    private fun matchSourceOrientation(src: Bitmap, aspect: Float): Bitmap {
        if (src.width == src.height) return src
        val portrait = src.width < src.height
        val sourcePortrait = aspect < 1f
        if (portrait == sourcePortrait) return src
        val quarterTurn = android.graphics.Matrix().apply { postRotate(90f) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, quarterTurn, true)
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
                Interpreter(mapModel(context, spec.modelFile), options).also {
                    checkGraphMatchesSpec(it, spec)
                }
            } catch (e: Throwable) {
                delegate?.close()
                throw e
            }

            Log.i(TAG, "loaded ${spec.modelFile} (${spec.variant}) on $accelerator")
            return NutritionEstimator(interpreter, delegate, spec, accelerator)
        }

        /**
         * The sidecar and the .tflite are written together by the export notebook,
         * but they are separate files and can drift (a model swapped by hand, a
         * stale JSON). A mismatch here would otherwise surface as a buffer-size
         * crash on the first scan, or worse, as silently wrong numbers.
         */
        private fun checkGraphMatchesSpec(interpreter: Interpreter, spec: ModelSpec) {
            val input = interpreter.getInputTensor(0).shape()     // [1, h, w, 3]
            val output = interpreter.getOutputTensor(0).shape()   // [1, targets]
            val ok = input.size == 4 && input[1] == spec.height && input[2] == spec.width &&
                input[3] == 3 && output.last() == spec.outputs
            if (!ok) {
                interpreter.close()
                error(
                    "${spec.modelFile} is ${input.contentToString()} -> " +
                        "${output.contentToString()} but $SIDECAR_ASSET describes " +
                        "${spec.width}x${spec.height} -> ${spec.outputs}. Re-run the export notebook."
                )
            }
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

            fun floatsOrNull(key: String): FloatArray? = output.optJSONArray(key)?.let { arr ->
                FloatArray(arr.length()) { arr.getDouble(it).toFloat() }
            }

            fun strings(key: String): List<String> {
                val arr = output.getJSONArray(key)
                return List(arr.length()) { arr.getString(it) }
            }

            val targets = strings("targets")
            // The v4 line trains on z-scores; the earlier v1 model trained on raw
            // units. The export notebook measures which one a head is and records
            // it, because inverting a raw head (or not inverting a z-scored one)
            // produces numbers that are wrong by orders of magnitude.
            val standardized = output.optBoolean("standardized", true)
            val mu = if (standardized) {
                requireNotNull(floatsOrNull("mu")) { "standardized head but no mu in $SIDECAR_ASSET" }
            } else {
                FloatArray(targets.size)
            }
            val sd = if (standardized) {
                requireNotNull(floatsOrNull("sd")) { "standardized head but no sd in $SIDECAR_ASSET" }
            } else {
                FloatArray(targets.size) { 1f }
            }
            require(mu.size == targets.size && sd.size == targets.size) {
                "mu/sd/targets length mismatch in $SIDECAR_ASSET"
            }
            require(targets.size == 5) {
                "expected 5 targets, got ${targets.size}: $targets"
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
