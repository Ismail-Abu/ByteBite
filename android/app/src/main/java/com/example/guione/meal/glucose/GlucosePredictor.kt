package com.example.guione.meal.glucose

/**
 * Versioned, typed boundary for glucose forecasting — a deliberately blank
 * reference for now.
 *
 * The real on-device glucose model and its input contract (ordered features,
 * units, required history, horizon) are not yet available, so the only
 * implementation is [UnavailableGlucosePredictor], which always reports
 * [GlucoseResult.Unavailable]. Nothing is simulated. When the model and its
 * contract arrive, add the concrete inputs to [GlucoseRequest], a
 * `Forecast` case to [GlucoseResult], and a real implementation behind this same
 * interface; no caller needs to change to keep showing an honest state.
 */
interface GlucosePredictor {
    /** Model/contract version, or "none" when unavailable. */
    val version: String

    /** False until a validated model and contract are installed. */
    val isAvailable: Boolean

    suspend fun predict(request: GlucoseRequest): GlucoseResult
}

/**
 * Placeholder request. The exact inputs are defined by the professor's model
 * contract and are intentionally not guessed here; for now it only references
 * the meal revision a forecast would be tied to.
 */
data class GlucoseRequest(val mealRevisionId: String)

/** Explicit outcomes. A `Forecast` case is added once the output contract is known. */
sealed interface GlucoseResult {
    /** No model/contract installed. */
    data object Unavailable : GlucoseResult

    /** The contract's required inputs are missing or stale. */
    data class MissingInput(val reason: String) : GlucoseResult

    /** The model ran but failed; a meal still saves regardless. */
    data class Failed(val reason: String) : GlucoseResult
}

/** The only implementation today: always unavailable, never fabricated. */
object UnavailableGlucosePredictor : GlucosePredictor {
    override val version: String = "none"
    override val isAvailable: Boolean = false
    override suspend fun predict(request: GlucoseRequest): GlucoseResult = GlucoseResult.Unavailable
}
