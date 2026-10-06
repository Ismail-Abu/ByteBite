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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Measured storage usage and the destructive "delete all" control. */
class SettingsViewModel(
    private val appContext: Context,
    private val repository: MealRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    data class Storage(val dbBytes: Long, val mealCount: Int)

    private val _storage = MutableStateFlow<Storage?>(null)
    val storage = _storage.asStateFlow()

    private val _deleting = MutableStateFlow(false)
    val deleting = _deleting.asStateFlow()

    /** One-shot user messages (success/failure) for a snackbar. */
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            try {
                val count = repository.listMeals().size
                val bytes = withContext(ioDispatcher) {
                    val base = appContext.getDatabasePath(MealDatabase.DB_NAME)
                    listOf(base, File(base.path + "-wal"), File(base.path + "-shm"))
                        .filter { it.exists() }.sumOf { it.length() }
                }
                _storage.value = Storage(bytes, count)
            } catch (e: CancellationException) {
                throw e // never swallow cancellation into a user error
            } catch (_: Exception) {
                // A storage-read failure leaves the figure unknown rather than crashing.
                if (_storage.value == null) _storage.value = Storage(-1, -1)
            }
        }
    }

    fun deleteAll() {
        if (_deleting.value) return // in-flight guard
        _deleting.value = true
        viewModelScope.launch {
            try {
                repository.deleteAll()
                _messages.send("All meal data deleted")
                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Data is left intact; the user can retry.
                _messages.send("Couldn't delete your data. Please try again.")
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
