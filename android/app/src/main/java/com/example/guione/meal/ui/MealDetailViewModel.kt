package com.example.guione.meal.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.guione.meal.MealRepository
import com.example.guione.meal.MealWithRevisions
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Meal details, observed so edits/corrections refresh in place. */
class MealDetailViewModel(
    private val repository: MealRepository,
    state: SavedStateHandle,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val meal: MealWithRevisions? = null,
        val notFound: Boolean = false,
        val deleting: Boolean = false,
        val deleteError: String? = null,
    )

    private val mealId: String = requireNotNull(state.get<String>(ARG_MEAL_ID))

    private val transient = MutableStateFlow(Transient())
    private data class Transient(val deleting: Boolean = false, val deleteError: String? = null)

    private val deleted = Channel<Unit>(Channel.BUFFERED)
    val deletedEvents = deleted.receiveAsFlow()

    val ui =
        combine(repository.observeMeal(mealId), transient) { meal, t ->
            UiState(
                loading = false,
                meal = meal,
                notFound = meal == null,
                deleting = t.deleting,
                deleteError = t.deleteError,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun delete() {
        if (transient.value.deleting) return
        transient.value = Transient(deleting = true)
        viewModelScope.launch {
            try {
                repository.deleteMeal(mealId)
                deleted.send(Unit)
            } catch (e: Exception) {
                transient.value = Transient(deleting = false, deleteError = e.message ?: "Couldn't delete. Please try again.")
            }
        }
    }

    companion object {
        const val ARG_MEAL_ID = "mealId"
    }
}
