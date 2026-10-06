package com.example.guione.meal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/**
 * Hosts [MealLogScreen]: builds the [MealLogViewModel] over the app's Room-backed
 * repository and feeds its state in. Separated from the screen so the screen
 * stays a pure function of state for testing.
 */
@Composable
fun MealLogRoute(onOpenPrototype: () -> Unit) {
    val context = LocalContext.current
    val vm: MealLogViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                MealLogViewModel(
                    repository = AppGraph.mealRepository(context),
                    state = createSavedStateHandle(),
                )
            }
        },
    )
    val state by vm.ui.collectAsState()
    MealLogScreen(
        state = state,
        onName = vm::onName,
        onField = vm::onField,
        onSave = vm::save,
        onCancelEdit = vm::clearForm,
        onEdit = vm::startEdit,
        onDelete = vm::delete,
        onOpenPrototype = onOpenPrototype,
    )
}
