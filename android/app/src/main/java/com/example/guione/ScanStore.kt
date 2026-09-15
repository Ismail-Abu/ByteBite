package com.example.guione

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Holds the result of the most recent scan and owns the one [NutritionEstimator]
 * instance for the process.
 *
 * Process-scoped rather than per-screen: both experiences (Kitchen and Glucose)
 * read the same scan, which is what the prototype's shared "scanned dish" already
 * implied, and loading a 22 MB interpreter per navigation would be wasteful.
 *
 * A Mutex serialises inference because a LiteRT Interpreter cannot be invoked
 * concurrently; a second shutter tap while one scan is in flight queues instead
 * of corrupting the input tensor.
 */
object ScanStore {

    sealed interface State {
        /** Nothing scanned yet this session; screens show SampleData. */
        data object Idle : State

        /** A photo is being preprocessed and run. */
        data object Running : State

        data class Ready(val estimate: NutritionEstimator.Estimate) : State

        /** Inference was attempted and failed. [message] is safe to surface. */
        data class Failed(val message: String) : State

        /**
         * assets/ has no model, so the repo's committed state runs as the UI
         * prototype it was. Not an error; the export notebook has not been run.
         */
        data object NoModel : State
    }

    var state: State by mutableStateOf(State.Idle)
        private set

    /** Last good estimate, kept while a new scan runs so the UI does not flicker. */
    var lastEstimate: NutritionEstimator.Estimate? by mutableStateOf(null)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Mutex()

    @Volatile
    private var estimator: NutritionEstimator? = null

    @Volatile
    private var triedLoad = false

    /**
     * True once the model has loaded. Compose state, so captions that read it
     * update when the background load finishes rather than keeping whatever
     * they showed on the first frame.
     */
    var modelAvailable: Boolean by mutableStateOf(false)
        private set

    val modelLabel: String
        get() = estimator?.let { "${it.spec.variant} · ${it.accelerator}" } ?: "sample data"

    /**
     * The shipped model's own test error, for display next to an estimate.
     * One photo gives a point estimate; showing it without its error bar would
     * overstate what the model knows, which matters when someone is deciding a
     * carbohydrate count.
     */
    val maeNote: String?
        get() = estimator?.spec?.testMae?.let { m ->
            "typical error ±${m[0].toInt()} kcal · ±${"%.1f".format(m[3])} g carbs"
        }

    /**
     * Loads the interpreter if it has not been loaded. Safe to call from any
     * thread; cheap after the first call. Worth calling when the camera screen
     * appears so the ~100 ms load is not charged to the shutter tap.
     */
    fun warmUp(context: Context) {
        if (triedLoad) return
        val app = context.applicationContext
        scope.launch {
            lock.withLock {
                if (triedLoad) return@withLock
                triedLoad = true
                estimator = NutritionEstimator.loadOrNull(app)
                modelAvailable = estimator != null
                if (estimator == null && state is State.Idle) state = State.NoModel
            }
        }
    }

    /**
     * Runs one captured frame. [bitmap] is recycled once consumed, so callers
     * must not reuse it afterwards.
     */
    fun scan(context: Context, bitmap: Bitmap) {
        val app = context.applicationContext
        state = State.Running
        scope.launch {
            lock.withLock {
                if (!triedLoad) {
                    triedLoad = true
                    estimator = NutritionEstimator.loadOrNull(app)
                    modelAvailable = estimator != null
                }
                val engine = estimator
                if (engine == null) {
                    withContext(Dispatchers.Main) { state = State.NoModel }
                    bitmap.recycle()
                    return@withLock
                }
                val next = try {
                    State.Ready(engine.estimate(bitmap))
                } catch (e: Exception) {
                    State.Failed(e.message ?: e.javaClass.simpleName)
                } finally {
                    bitmap.recycle()
                }
                withContext(Dispatchers.Main) {
                    if (next is State.Ready) lastEstimate = next.estimate
                    state = next
                }
            }
        }
    }

    private val loggedScans = mutableSetOf<String>()

    /**
     * Records that the scan behind [view] went into [log], returning false if it
     * already did. Stops a double tap on "Log meal", or re-entering the result
     * screen, from adding the same plate twice. Keyed per log because each
     * experience keeps its own list.
     */
    fun claimForLog(view: DishView, log: String): Boolean =
        !view.live || loggedScans.add("$log:${view.scanId}")

    /** True while a photo is being run; logging then would record the previous plate. */
    val busy: Boolean get() = state is State.Running

    /** Current wall-clock time as the log screens print it. */
    fun nowLabel(): String = java.time.LocalTime.now()
        .format(java.time.format.DateTimeFormatter.ofPattern("H:mm"))

    /** Drops back to the camera stage without discarding the last good estimate. */
    fun reset() {
        state = if (estimator == null && triedLoad) State.NoModel else State.Idle
    }
}

/**
 * What the result screens should draw: the real estimate when there is one,
 * otherwise the prototype's hardcoded dish.
 *
 * Keeping the fallback means a fresh clone with no exported model still
 * demonstrates the full flow, and the poster demo never shows an empty screen.
 */
data class DishView(
    val name: String,
    val kcal: Int,
    val massG: Int,
    val carbG: Double,
    val proteinG: Double,
    val fatG: Double,
    val live: Boolean,
    val detail: String,
    /** Non-null only for a live estimate: the model's measured test error. */
    val errorNote: String?,
    /** Identity of the scan behind a live view; 0 for the sample dish. */
    val scanId: Long,
) {
    companion object {
        /**
         * One decimal place, floored at zero. The regression head is unconstrained
         * and dips slightly below zero on lean plates (fat on plain vegetables);
         * a negative gram count is never a real reading, so it shows as 0.
         */
        private fun Float.g1(): Double =
            (coerceAtLeast(0f) * 10f).toDouble().let { Math.round(it) / 10.0 }

        fun current(): DishView {
            val e = ScanStore.lastEstimate
            return if (e != null) {
                DishView(
                    name = "Scanned dish",
                    kcal = e.calories.toInt().coerceAtLeast(0),
                    massG = e.massG.toInt().coerceAtLeast(0),
                    carbG = e.carbG.g1(),
                    proteinG = e.proteinG.g1(),
                    fatG = e.fatG.g1(),
                    live = true,
                    detail = "on-device · ${e.variant} · ${e.latencyMs} ms",
                    errorNote = ScanStore.maeNote,
                    scanId = e.scanId,
                )
            } else {
                DishView(
                    name = SampleData.DISH_NAME,
                    kcal = SampleData.DISH_KCAL,
                    massG = SampleData.DISH_MASS,
                    carbG = SampleData.DISH_CARBS,
                    proteinG = SampleData.DISH_PROTEIN,
                    fatG = SampleData.DISH_FAT,
                    live = false,
                    detail = "sample data · no model installed",
                    errorNote = null,
                    scanId = 0L,
                )
            }
        }
    }
}
