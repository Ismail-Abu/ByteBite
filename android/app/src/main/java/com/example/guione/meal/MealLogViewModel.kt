package com.example.guione.meal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * State and actions for the persisted meal-logging surface: the saved-meal list
 * with a today summary, and the manual-entry / correction form.
 *
 * The form's draft ids ([DRAFT_MEAL_ID] / [DRAFT_REVISION_ID]) live in
 * [SavedStateHandle], so a meal half-entered when the process is killed keeps
 * the same ids when the activity is recreated — the eventual save upserts the
 * one record rather than creating a duplicate. Validation runs through
 * [MealInput] with the injected [clock], which also fixes "today".
 */
class MealLogViewModel(
    private val repository: MealRepository,
    private val state: SavedStateHandle,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    data class Row(
        val id: String,
        val name: String,
        val timeLabel: String,
        val calories: Double?,
        val carbs: Double?,
        val source: NutritionSource,
    )

    data class Form(
        /** Non-null when the form is editing an existing meal (a correction). */
        val editingMealId: String? = null,
        val name: String = "",
        val calories: String = "",
        val mass: String = "",
        val carbs: String = "",
        val protein: String = "",
        val fat: String = "",
        /** Field -> reason; the null key is a form-level error. */
        val errors: Map<MealInput.Field?, String> = emptyMap(),
        val justSaved: Boolean = false,
    ) {
        val isEditing: Boolean get() = editingMealId != null
    }

    data class UiState(
        val loading: Boolean = true,
        val meals: List<Row> = emptyList(),
        val todayKcal: Double? = null,
        val todayCarbs: Double? = null,
        val form: Form = Form(),
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val zone: ZoneId get() = clock.zone
    private val timeFmt = DateTimeFormatter.ofPattern("H:mm")

    init {
        ensureDraftIds()
        refresh()
    }

    private fun ensureDraftIds() {
        if (state.get<String>(DRAFT_MEAL_ID) == null) state[DRAFT_MEAL_ID] = newId()
        if (state.get<String>(DRAFT_REVISION_ID) == null) state[DRAFT_REVISION_ID] = newId()
    }

    private fun rotateDraftIds() {
        state[DRAFT_MEAL_ID] = newId()
        state[DRAFT_REVISION_ID] = newId()
    }

    fun refresh() {
        viewModelScope.launch {
            val meals = repository.listMeals()
            val rows = meals.map { meal ->
                val revision = repository.getMeal(meal.id)?.current
                Row(
                    id = meal.id,
                    name = meal.name,
                    timeLabel = meal.occurredAt.atZone(zone).format(timeFmt),
                    calories = revision?.nutrition?.caloriesKcal,
                    carbs = revision?.nutrition?.carbsGrams,
                    source = revision?.source ?: NutritionSource.MANUAL,
                )
            }
            val today = java.time.LocalDate.now(clock)
            val todayRows = meals.filter { it.occurredAt.atZone(zone).toLocalDate() == today }
                .mapNotNull { repository.getMeal(it.id)?.current?.nutrition }
            _ui.update {
                it.copy(
                    loading = false,
                    meals = rows,
                    todayKcal = todayRows.mapNotNull { n -> n.caloriesKcal }.ifEmpty { null }?.sum(),
                    todayCarbs = todayRows.mapNotNull { n -> n.carbsGrams }.ifEmpty { null }?.sum(),
                )
            }
        }
    }

    // --- Form editing ---

    fun onName(value: String) = editForm { it.copy(name = value, errors = it.errors - null) }

    fun onField(field: MealInput.Field, value: String) = editForm {
        val next = when (field) {
            MealInput.Field.CALORIES -> it.copy(calories = value)
            MealInput.Field.MASS -> it.copy(mass = value)
            MealInput.Field.CARBS -> it.copy(carbs = value)
            MealInput.Field.PROTEIN -> it.copy(protein = value)
            MealInput.Field.FAT -> it.copy(fat = value)
        }
        next.copy(errors = next.errors - field)
    }

    private inline fun editForm(block: (Form) -> Form) {
        _ui.update { it.copy(form = block(it.form).copy(justSaved = false)) }
    }

    /** Loads an existing meal's current values into the form for correction. */
    fun startEdit(mealId: String) {
        viewModelScope.launch {
            val meal = repository.getMeal(mealId) ?: return@launch
            val n = meal.current.nutrition
            _ui.update {
                it.copy(
                    form = Form(
                        editingMealId = mealId,
                        name = meal.meal.name,
                        calories = n.caloriesKcal.toField(),
                        mass = n.massGrams.toField(),
                        carbs = n.carbsGrams.toField(),
                        protein = n.proteinGrams.toField(),
                        fat = n.fatGrams.toField(),
                    )
                )
            }
        }
    }

    fun clearForm() {
        _ui.update { it.copy(form = Form()) }
    }

    fun save() {
        val form = _ui.value.form
        val input = MealInput.Form(
            name = form.name,
            calories = form.calories,
            mass = form.mass,
            carbs = form.carbs,
            protein = form.protein,
            fat = form.fat,
            occurredAt = null,
            occurrenceOffset = currentOffset(),
        )
        when (val result = MealInput.validate(input, clock)) {
            is MealInput.Result.Rejected -> {
                val errors = result.errors.associate { it.field to it.reason }
                _ui.update { it.copy(form = form.copy(errors = errors, justSaved = false)) }
            }
            is MealInput.Result.Ok -> viewModelScope.launch {
                if (form.editingMealId != null) {
                    repository.correctMeal(form.editingMealId, result.valid)
                } else {
                    repository.saveManualMeal(
                        result.valid,
                        mealId = requireDraft(DRAFT_MEAL_ID),
                        revisionId = requireDraft(DRAFT_REVISION_ID),
                    )
                    rotateDraftIds()
                }
                _ui.update { it.copy(form = Form(justSaved = true)) }
                refresh()
            }
        }
    }

    fun delete(mealId: String) {
        viewModelScope.launch {
            repository.deleteMeal(mealId)
            refresh()
        }
    }

    private fun currentOffset() = clock.zone.rules.getOffset(clock.instant())
    private fun requireDraft(key: String): String =
        state.get<String>(key) ?: newId().also { state[key] = it }

    companion object {
        const val DRAFT_MEAL_ID = "draftMealId"
        const val DRAFT_REVISION_ID = "draftRevisionId"

        /** Double -> field text: empty for unknown, no trailing ".0" for integers. */
        internal fun Double?.toField(): String = when {
            this == null -> ""
            this == toLong().toDouble() -> toLong().toString()
            else -> toString()
        }
    }
}
