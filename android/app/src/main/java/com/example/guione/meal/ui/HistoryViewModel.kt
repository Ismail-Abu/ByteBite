package com.example.guione.meal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guione.meal.MealListItem
import com.example.guione.meal.MealRepository
import com.example.guione.ui.Format
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate

/**
 * The full history, grouped by day. Reads the repository's single observed
 * list query (no per-row lookups) and the screen renders it with a LazyColumn,
 * so neither the query nor the UI materializes the whole database eagerly
 * (fix #8).
 */
class HistoryViewModel(
    repository: MealRepository,
    private val clock: Clock,
) : ViewModel() {

    data class DayGroup(val date: LocalDate, val header: String, val items: List<MealListItem>)

    data class UiState(
        val loading: Boolean = true,
        val groups: List<DayGroup> = emptyList(),
        /** When set, only this day is shown. */
        val filterDate: LocalDate? = null,
        /** Dates that have at least one meal, newest first — for the date picker. */
        val datesWithMeals: Set<LocalDate> = emptySet(),
        /** True when there are meals but the current filter hides them all. */
        val filteredEmpty: Boolean = false,
    )

    private val filter = MutableStateFlow<LocalDate?>(null)

    /** Filter History to a single day, or clear with null. */
    fun setFilter(date: LocalDate?) {
        filter.value = date
    }

    val ui =
        combine(repository.observeMeals(), filter) { meals, filterDate ->
            val today = LocalDate.now(clock)
            val byDay = meals.groupBy { it.occurredAt.atZone(clock.zone).toLocalDate() }
            val shown = if (filterDate == null) byDay else byDay.filterKeys { it == filterDate }
            val groups = shown
                .toSortedMap(compareByDescending { it })
                .map { (date, items) ->
                    DayGroup(date, Format.relativeDate(date, today), items.sortedByDescending { it.occurredAt })
                }
            UiState(
                loading = false,
                groups = groups,
                filterDate = filterDate,
                datesWithMeals = byDay.keys,
                filteredEmpty = meals.isNotEmpty() && groups.isEmpty(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())
}
