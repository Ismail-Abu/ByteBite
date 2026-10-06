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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** History groups meals by day, newest first (part of fix #8's behaviour). */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val today = Instant.parse("2026-01-15T12:00:00Z")
    private val clock = Clock.fixed(today, ZoneId.of("UTC"))
    private lateinit var repo: InMemoryMealRepository

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        var n = 0
        repo = InMemoryMealRepository(clock) { "gen-${n++}" }
    }

    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `meals are grouped by day newest first`() = runTest(dispatcher) {
        repo.saveManualMeal(MealInput.Valid("A", Nutrition(carbsGrams = 10.0), today, ZoneOffset.UTC), "a", "ar")
        repo.saveManualMeal(MealInput.Valid("B", Nutrition(carbsGrams = 20.0), today.minusSeconds(60L * 60 * 24), ZoneOffset.UTC), "b", "br")
        repo.saveManualMeal(MealInput.Valid("C", Nutrition(carbsGrams = 30.0), today.minusSeconds(60L * 30), ZoneOffset.UTC), "c", "cr")

        val vm = HistoryViewModel(repo, clock)
        backgroundScope.launch { vm.ui.collect {} }
        advanceUntilIdle()

        val groups = vm.ui.value.groups
        assertEquals(2, groups.size)                 // today + yesterday
        assertEquals("Today", groups.first().header)
        assertEquals(2, groups.first().items.size)   // A and C are today
        assertEquals("Yesterday", groups[1].header)
        assertEquals(1, groups[1].items.size)
    }

    @Test fun `filter shows only the selected day`() = runTest(dispatcher) {
        repo.saveManualMeal(MealInput.Valid("A", Nutrition(carbsGrams = 10.0), today, ZoneOffset.UTC), "a", "ar")
        repo.saveManualMeal(MealInput.Valid("B", Nutrition(carbsGrams = 20.0), today.minusSeconds(60L * 60 * 24), ZoneOffset.UTC), "b", "br")
        val vm = HistoryViewModel(repo, clock)
        backgroundScope.launch { vm.ui.collect {} }
        advanceUntilIdle()

        vm.setFilter(java.time.LocalDate.of(2026, 1, 15)) // the "today" date
        advanceUntilIdle()
        assertEquals(1, vm.ui.value.groups.size)
        assertEquals(1, vm.ui.value.groups.first().items.size)
        assertEquals("A", vm.ui.value.groups.first().items.first().name)
    }

    @Test fun `filter to a day with no meals reports filteredEmpty`() = runTest(dispatcher) {
        repo.saveManualMeal(MealInput.Valid("A", Nutrition(carbsGrams = 10.0), today, ZoneOffset.UTC), "a", "ar")
        val vm = HistoryViewModel(repo, clock)
        backgroundScope.launch { vm.ui.collect {} }
        advanceUntilIdle()

        vm.setFilter(java.time.LocalDate.of(2020, 1, 1))
        advanceUntilIdle()
        assertTrue(vm.ui.value.filteredEmpty)
        assertTrue(vm.ui.value.groups.isEmpty())
    }

    @Test fun `grouping follows the clock zone, not UTC`() = runTest(dispatcher) {
        // 02:30 UTC on Jan 15 is still Jan 14 at 21:30 in New York.
        val instant = Instant.parse("2026-01-15T02:30:00Z")
        repo.saveManualMeal(MealInput.Valid("Late dinner", Nutrition(carbsGrams = 30.0), instant, ZoneOffset.UTC), "m", "r")
        val nyClock = Clock.fixed(Instant.parse("2026-01-15T18:00:00Z"), ZoneId.of("America/New_York"))
        val vm = HistoryViewModel(repo, nyClock)
        backgroundScope.launch { vm.ui.collect {} }
        advanceUntilIdle()
        assertEquals(java.time.LocalDate.of(2026, 1, 14), vm.ui.value.groups.first().date)
    }
}
