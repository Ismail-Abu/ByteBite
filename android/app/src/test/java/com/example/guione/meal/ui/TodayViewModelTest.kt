package com.example.guione.meal.ui

import com.example.guione.meal.InMemoryMealRepository
import com.example.guione.meal.MealInput
import com.example.guione.meal.Nutrition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Regression tests for Today's partial-total handling (#6) and day rollover (#7). */
@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val start = Instant.parse("2026-01-15T12:00:00Z")
    private lateinit var repo: InMemoryMealRepository

    /** A clock whose instant can be advanced to simulate time passing. */
    private class MutableClock(var current: Instant, private val z: ZoneId) : Clock() {
        override fun getZone(): ZoneId = z
        override fun withZone(zone: ZoneId): Clock = MutableClock(current, zone)
        override fun instant(): Instant = current
    }

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        var n = 0
        repo = InMemoryMealRepository(Clock.fixed(start, ZoneId.of("UTC"))) { "gen-${n++}" }
    }

    @After fun tearDown() = Dispatchers.resetMain()

    private suspend fun save(name: String, nutrition: Nutrition, at: Instant) {
        repo.saveManualMeal(MealInput.Valid(name, nutrition, at, ZoneOffset.UTC), name, "$name-r")
    }

    @Test fun `a protein-only meal counts and marks the totals partial`() = runTest(dispatcher) {
        save("P", Nutrition(proteinGrams = 20.0), start)
        val vm = TodayViewModel(repo, Clock.fixed(start, ZoneId.of("UTC")))
        backgroundScope.launch { vm.ui.collect {} }
        advanceUntilIdle()
        val s = vm.ui.value.summary
        assertEquals(1, s.mealCount)          // counts despite unknown calories/carbs
        assertNull(s.kcal)
        assertEquals(20.0, s.protein!!, 0.0)
        assertTrue(s.partial)
    }

    @Test fun `no meals today is an empty summary`() = runTest(dispatcher) {
        val vm = TodayViewModel(repo, Clock.fixed(start, ZoneId.of("UTC")))
        backgroundScope.launch { vm.ui.collect {} }
        advanceUntilIdle()
        assertEquals(0, vm.ui.value.summary.mealCount)
        assertFalse(vm.ui.value.summary.partial)
    }

    @Test fun `today rolls over to the next day on resume without a save`() = runTest(dispatcher) {
        save("Lunch", Nutrition(caloriesKcal = 500.0, carbsGrams = 45.0), start)
        val clock = MutableClock(start, ZoneId.of("UTC"))
        val vm = TodayViewModel(repo, clock)
        backgroundScope.launch { vm.ui.collect {} }
        advanceUntilIdle()
        assertEquals(1, vm.ui.value.summary.mealCount)

        // Next day arrives; the screen resumes.
        clock.current = start.plusSeconds(60 * 60 * 24)
        vm.onResumed()
        advanceUntilIdle()
        assertEquals(0, vm.ui.value.summary.mealCount) // no longer "today"
        assertEquals(1, vm.ui.value.recent.size)       // but still in recent history
    }
}
