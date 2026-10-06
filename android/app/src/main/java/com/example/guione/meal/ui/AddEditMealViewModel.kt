package com.example.guione.meal.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guione.meal.MealInput
import com.example.guione.meal.MealRepository
import com.example.guione.meal.newId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * State and actions for the dedicated add/edit meal screen.
 *
 * Correctness properties this type is responsible for:
 *  - #1 occurrence time: when editing, the meal's original instant and offset
 *    are loaded and preserved; a correction keeps that time unless the user
 *    explicitly changes it (the form never revalidates with "now").
 *  - #3 draft restoration: every entered value, the editing identity, and the
 *    occurrence time are mirrored into SavedStateHandle, so process recreation
 *    restores the full in-progress form, not just ids.
 *  - #4 duplicate saves: stable ids and a validated snapshot are captured before
 *    the async write, and an in-flight guard drops repeated taps, so rapid taps
 *    or a retry cannot create a second meal or an extra correction revision.
 *  - #2 save failure: a thrown repository error leaves the draft intact, clears
 *    the in-flight flag, and surfaces a retryable error instead of claiming
 *    success.
 */
class AddEditMealViewModel(
    private val repository: MealRepository,
    private val state: SavedStateHandle,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    data class UiState(
        val editing: Boolean = false,
        val name: String = "",
        val calories: String = "",
        val mass: String = "",
        val carbs: String = "",
        val protein: String = "",
        val fat: String = "",
        val occurredAt: Instant = Instant.EPOCH,
        val offset: ZoneOffset = ZoneOffset.UTC,
        val nameError: String? = null,
        val fieldErrors: Map<MealInput.Field, String> = emptyMap(),
        val formError: String? = null,
        val saving: Boolean = false,
        val saveError: String? = null,
        val loadError: String? = null,
        val initialized: Boolean = false,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val saved = Channel<Unit>(Channel.BUFFERED)
    /** Emits once a save has durably succeeded; the screen navigates away on it. */
    val savedEvents = saved.receiveAsFlow()

    /** The meal being edited, from the nav argument; null for a new meal. */
    private val editMealId: String? = state.get<String>(ARG_MEAL_ID)?.takeIf { it.isNotBlank() }

    init {
        if (state.get<Boolean>(KEY_INITIALIZED) == true) {
            restoreFromSavedState()
        } else {
            seed()
        }
    }

    private fun seed() {
        val editId = editMealId
        if (editId != null) {
            viewModelScope.launch {
                try {
                    val meal = repository.getMeal(editId)
                    if (meal == null) {
                        _ui.update { it.copy(initialized = true, loadError = "This meal no longer exists.") }
                        return@launch
                    }
                    val n = meal.current.nutrition
                    val s = UiState(
                        editing = true,
                        name = meal.meal.name,
                        calories = n.caloriesKcal.toField(),
                        mass = n.massGrams.toField(),
                        carbs = n.carbsGrams.toField(),
                        protein = n.proteinGrams.toField(),
                        fat = n.fatGrams.toField(),
                        occurredAt = meal.meal.occurredAt,
                        offset = meal.meal.occurrenceOffset,
                        initialized = true,
                    )
                    _ui.value = s
                    persist(s)
                } catch (e: Exception) {
                    _ui.update { it.copy(initialized = true, loadError = "Could not open this meal. ${e.readableMessage()}") }
                }
            }
        } else {
            state[KEY_DRAFT_MEAL_ID] = newId()
            state[KEY_DRAFT_REVISION_ID] = newId()
            val now = clock.instant()
            val offset = clock.zone.rules.getOffset(now)
            val s = UiState(editing = false, occurredAt = now, offset = offset, initialized = true)
            _ui.value = s
            persist(s)
        }
    }

    private fun restoreFromSavedState() {
        _ui.value = UiState(
            editing = editMealId != null,
            name = state.get<String>(KEY_NAME).orEmpty(),
            calories = state.get<String>(KEY_CALORIES).orEmpty(),
            mass = state.get<String>(KEY_MASS).orEmpty(),
            carbs = state.get<String>(KEY_CARBS).orEmpty(),
            protein = state.get<String>(KEY_PROTEIN).orEmpty(),
            fat = state.get<String>(KEY_FAT).orEmpty(),
            occurredAt = Instant.ofEpochMilli(state.get<Long>(KEY_OCCURRED_MS) ?: clock.millis()),
            offset = ZoneOffset.ofTotalSeconds(state.get<Int>(KEY_OFFSET_SECONDS) ?: 0),
            initialized = true,
        )
    }

    private fun persist(s: UiState) {
        state[KEY_INITIALIZED] = true
        state[KEY_NAME] = s.name
        state[KEY_CALORIES] = s.calories
        state[KEY_MASS] = s.mass
        state[KEY_CARBS] = s.carbs
        state[KEY_PROTEIN] = s.protein
        state[KEY_FAT] = s.fat
        state[KEY_OCCURRED_MS] = s.occurredAt.toEpochMilli()
        state[KEY_OFFSET_SECONDS] = s.offset.totalSeconds
    }

    fun onName(value: String) = edit { it.copy(name = value, nameError = null, formError = null) }

    fun onField(field: MealInput.Field, value: String) = edit {
        val next = when (field) {
            MealInput.Field.CALORIES -> it.copy(calories = value)
            MealInput.Field.MASS -> it.copy(mass = value)
            MealInput.Field.CARBS -> it.copy(carbs = value)
            MealInput.Field.PROTEIN -> it.copy(protein = value)
            MealInput.Field.FAT -> it.copy(fat = value)
        }
        next.copy(fieldErrors = next.fieldErrors - field, formError = null)
    }

    /** Explicit user change to the occurrence time. */
    fun onOccurredAt(instant: Instant) = edit { it.copy(occurredAt = instant, formError = null) }

    private inline fun edit(block: (UiState) -> UiState) {
        _ui.update { block(it).copy(saveError = null) }
        persist(_ui.value)
    }

    fun save() {
        val s = _ui.value
        if (s.saving) return // in-flight guard (#4)
        val form = MealInput.Form(
            name = s.name,
            calories = s.calories,
            mass = s.mass,
            carbs = s.carbs,
            protein = s.protein,
            fat = s.fat,
            occurredAt = s.occurredAt, // preserve the loaded/edited instant (#1)
            occurrenceOffset = s.offset,
        )
        when (val result = MealInput.validate(form, clock)) {
            is MealInput.Result.Rejected -> {
                val nameErr = result.errors.firstOrNull { it.field == null && it.reason.contains("name") }?.reason
                val formErr = result.errors.firstOrNull { it.field == null && it.reason != nameErr }?.reason
                val fieldErrs = result.errors.mapNotNull { e -> e.field?.let { it to e.reason } }.toMap()
                _ui.update { it.copy(nameError = nameErr, formError = formErr, fieldErrors = fieldErrs, saveError = null) }
            }
            is MealInput.Result.Ok -> {
                val editId = editMealId
                val draftMealId = state.get<String>(KEY_DRAFT_MEAL_ID) ?: newId()
                val draftRevisionId = state.get<String>(KEY_DRAFT_REVISION_ID) ?: newId()
                val valid = result.valid
                _ui.update { it.copy(saving = true, nameError = null, fieldErrors = emptyMap(), formError = null, saveError = null) }
                viewModelScope.launch {
                    try {
                        if (editId != null) {
                            val updated = repository.correctMeal(editId, valid)
                                ?: error("This meal no longer exists.")
                            check(updated.id == editId)
                        } else {
                            repository.saveManualMeal(valid, draftMealId, draftRevisionId)
                        }
                        saved.send(Unit)
                    } catch (e: Exception) {
                        _ui.update { it.copy(saving = false, saveError = "Couldn't save. ${e.readableMessage()}") }
                    }
                }
            }
        }
    }

    companion object {
        const val ARG_MEAL_ID = "mealId"
        private const val KEY_INITIALIZED = "ae.init"
        private const val KEY_NAME = "ae.name"
        private const val KEY_CALORIES = "ae.cal"
        private const val KEY_MASS = "ae.mass"
        private const val KEY_CARBS = "ae.carbs"
        private const val KEY_PROTEIN = "ae.protein"
        private const val KEY_FAT = "ae.fat"
        private const val KEY_OCCURRED_MS = "ae.occ"
        private const val KEY_OFFSET_SECONDS = "ae.off"
        private const val KEY_DRAFT_MEAL_ID = "ae.draftMeal"
        private const val KEY_DRAFT_REVISION_ID = "ae.draftRev"
    }
}

/** Double -> field text: empty for unknown, no trailing ".0" for integers. */
internal fun Double?.toField(): String = when {
    this == null -> ""
    this == toLong().toDouble() -> toLong().toString()
    else -> toString()
}

private fun Exception.readableMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: "Please try again."
