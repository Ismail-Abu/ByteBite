package com.example.guione.meal.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.guione.meal.AppGraph
import com.example.guione.meal.MealRepository
import com.example.guione.meal.db.MealDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Measured storage usage and the destructive "delete all" control. */
class SettingsViewModel(
    private val appContext: Context,
    private val repository: MealRepository,
) : ViewModel() {

    data class Storage(val dbBytes: Long, val mealCount: Int)

    private val _storage = MutableStateFlow<Storage?>(null)
    val storage = _storage.asStateFlow()

    private val _deleting = MutableStateFlow(false)
    val deleting = _deleting.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val count = repository.listMeals().size
            val bytes = withContext(Dispatchers.IO) {
                val base = appContext.getDatabasePath(MealDatabase.DB_NAME)
                listOf(base, File(base.path + "-wal"), File(base.path + "-shm"))
                    .filter { it.exists() }.sumOf { it.length() }
            }
            _storage.value = Storage(bytes, count)
        }
    }

    fun deleteAll() {
        if (_deleting.value) return
        _deleting.value = true
        viewModelScope.launch {
            try {
                repository.deleteAll()
                refresh()
            } finally {
                _deleting.value = false
            }
        }
    }
}

@Composable
fun rememberSettingsViewModel(): SettingsViewModel {
    val context = LocalContext.current.applicationContext
    return viewModel(factory = viewModelFactory {
        initializer { SettingsViewModel(context, AppGraph.mealRepository(context)) }
    })
}
