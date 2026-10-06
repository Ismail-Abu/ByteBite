package com.example.guione.meal.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.guione.meal.AppGraph

// Composable factories that wire each view model to the app's repository and
// clock. VMs that take a mealId/draft read it from the NavBackStackEntry's
// SavedStateHandle via createSavedStateHandle().

@Composable
fun rememberTodayViewModel(): TodayViewModel {
    val context = LocalContext.current.applicationContext
    return viewModel(factory = viewModelFactory {
        initializer { TodayViewModel(AppGraph.mealRepository(context), AppGraph.clock) }
    })
}

@Composable
fun rememberHistoryViewModel(): HistoryViewModel {
    val context = LocalContext.current.applicationContext
    return viewModel(factory = viewModelFactory {
        initializer { HistoryViewModel(AppGraph.mealRepository(context), AppGraph.clock) }
    })
}

@Composable
fun rememberMealDetailViewModel(): MealDetailViewModel {
    val context = LocalContext.current.applicationContext
    return viewModel(factory = viewModelFactory {
        initializer { MealDetailViewModel(AppGraph.mealRepository(context), createSavedStateHandle()) }
    })
}

@Composable
fun rememberAddEditViewModel(): AddEditMealViewModel {
    val context = LocalContext.current.applicationContext
    return viewModel(factory = viewModelFactory {
        initializer { AddEditMealViewModel(AppGraph.mealRepository(context), createSavedStateHandle(), AppGraph.clock) }
    })
}
