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
}
