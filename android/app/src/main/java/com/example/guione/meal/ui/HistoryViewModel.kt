package com.example.guione.meal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guione.meal.MealListItem
import com.example.guione.meal.MealRepository
import com.example.guione.ui.Format
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
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
    )

    val ui =
        repository.observeMeals().map { meals ->
            val today = LocalDate.now(clock)
            val groups = meals
                .groupBy { it.occurredAt.atZone(clock.zone).toLocalDate() }
                .toSortedMap(compareByDescending { it })
                .map { (date, items) ->
                    DayGroup(date, Format.relativeDate(date, today), items.sortedByDescending { it.occurredAt })
                }
            UiState(loading = false, groups = groups)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())
}
