package com.example.guione.meal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guione.meal.MealListItem
import com.example.guione.meal.MealRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate

/**
 * Today's summary and a short recent-meals preview.
 *
 * Fix #6: a meal with unknown calories/carbs still counts. The summary reports a
 * meal count distinct from the per-nutrient known totals, flags when any logged
 * meal has unknowns ([Summary.partial]), and only shows the empty state at zero
 * meals — never because a value happens to be unknown.
 *
 * Fix #7: "today" is re-derived from the injected clock whenever [onResumed] is
 * called (and the meal list changes), so the screen rolls over after midnight or
 * on resume, not only after the user saves something.
 */
class TodayViewModel(
    repository: MealRepository,
    private val clock: Clock,
) : ViewModel() {

    data class Summary(
        val mealCount: Int,
        val kcal: Double?,
        val carbs: Double?,
        val protein: Double?,
        val fat: Double?,
        /** True when at least one of today's meals has an unknown shown value. */
        val partial: Boolean,
    )

    data class UiState(
        val loading: Boolean = true,
        val date: LocalDate = LocalDate.EPOCH,
        val summary: Summary = Summary(0, null, null, null, null, false),
        val recent: List<MealListItem> = emptyList(),
    )

    private val today = MutableStateFlow(LocalDate.now(clock))

    /** Call from the screen's ON_RESUME so the day rolls over without a save. */
    fun onResumed() {
        today.value = LocalDate.now(clock)
    }

    val ui: StateFlow<UiState> =
        combine(repository.observeMeals(), today) { meals, day ->
            val todays = meals.filter { it.occurredAt.atZone(clock.zone).toLocalDate() == day }
            UiState(
                loading = false,
                date = day,
                summary = summarize(todays),
                recent = meals.take(RECENT_PREVIEW),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    private fun summarize(items: List<MealListItem>): Summary {
        fun sumOf(sel: (MealListItem) -> Double?): Double? {
            val known = items.mapNotNull(sel)
            return if (known.isEmpty()) null else known.sum()
        }
        val partial = items.any {
            it.nutrition.caloriesKcal == null || it.nutrition.carbsGrams == null ||
                it.nutrition.proteinGrams == null || it.nutrition.fatGrams == null
        }
        return Summary(
            mealCount = items.size,
            kcal = sumOf { it.nutrition.caloriesKcal },
            carbs = sumOf { it.nutrition.carbsGrams },
            protein = sumOf { it.nutrition.proteinGrams },
            fat = sumOf { it.nutrition.fatGrams },
            partial = partial,
        )
    }

    private companion object {
        const val RECENT_PREVIEW = 3
    }
}
