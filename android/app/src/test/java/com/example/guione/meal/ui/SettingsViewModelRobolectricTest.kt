package com.example.guione.meal.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.guione.meal.InMemoryMealRepository
import com.example.guione.meal.MealInput
import com.example.guione.meal.MealRepository
import com.example.guione.meal.Nutrition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Settings delete-all recovery (#24/#52): a failed delete leaves the data intact
 * and releases the in-flight guard; a successful delete clears the data. Both
 * assert deterministic end state rather than a one-shot snackbar message.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsViewModelRobolectricTest {

    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-01-15T12:00:00Z"), ZoneId.of("UTC"))
    private val at = Instant.parse("2026-01-15T12:00:00Z")
    private lateinit var context: Context

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        context = ApplicationProvider.getApplicationContext()
    }

    @After fun tearDown() = Dispatchers.resetMain()

    private suspend fun seededRepo() = InMemoryMealRepository(clock).also {
        it.saveManualMeal(MealInput.Valid("A", Nutrition(carbsGrams = 10.0), at, ZoneOffset.UTC), "m1", "r1")
    }

    @Test fun deleteFailureKeepsDataAndReleasesGuard() = runTest(dispatcher) {
        val base = seededRepo()
        // Same store, but deleteAll always fails.
        val repo = object : MealRepository by base {
            override suspend fun deleteAll(): Unit = throw RuntimeException("disk error")
        }
        val vm = SettingsViewModel(context, repo, dispatcher)
        advanceUntilIdle()
        vm.deleteAll()
        advanceUntilIdle()
        assertFalse("guard must release after failure", vm.deleting.value)
        assertEquals("data must survive a failed delete", 1, base.listMeals().size)
    }

    @Test fun deleteSuccessClearsData() = runTest(dispatcher) {
        val repo = seededRepo()
        val vm = SettingsViewModel(context, repo, dispatcher)
        advanceUntilIdle()
        vm.deleteAll()
        advanceUntilIdle()
        assertFalse(vm.deleting.value)
        assertTrue(repo.listMeals().isEmpty())
    }
}
