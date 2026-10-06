package com.example.guione.meal

import androidx.lifecycle.SavedStateHandle
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * The meal-log view model driven over the in-memory repository, so the form ->
 * persistence -> list wiring is verified on the JVM. The Room path is covered
 * separately by the instrumented repository tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MealLogViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-01-15T12:00:00Z"), ZoneId.of("UTC"))
    private lateinit var repo: InMemoryMealRepository

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        var n = 0
        repo = InMemoryMealRepository(clock) { "gen-${n++}" }
    }

    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm() = MealLogViewModel(repo, SavedStateHandle(), clock)

    private fun fill(vm: MealLogViewModel, name: String = "Lunch", carbs: String = "45") {
        vm.onName(name)
        vm.onField(MealInput.Field.CARBS, carbs)
    }

    @Test fun `starts empty after initial load`() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        assertFalse(vm.ui.value.loading)
        assertTrue(vm.ui.value.meals.isEmpty())
    }

    @Test fun `valid save persists and appears in the list`() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        fill(vm, "Lunch", "45")
        vm.save()
        advanceUntilIdle()
        assertEquals(1, vm.ui.value.meals.size)
        assertEquals("Lunch", vm.ui.value.meals.first().name)
        assertTrue(vm.ui.value.form.justSaved)
        assertEquals("", vm.ui.value.form.name) // form reset
    }

    @Test fun `two valid saves create two meals`() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        fill(vm, "A", "10"); vm.save(); advanceUntilIdle()
        fill(vm, "B", "20"); vm.save(); advanceUntilIdle()
        assertEquals(2, vm.ui.value.meals.size)
    }

    @Test fun `invalid form shows errors and saves nothing`() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onName("   ") // blank
        vm.onField(MealInput.Field.CARBS, "45")
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.ui.value.form.errors.containsKey(null))
        assertTrue(vm.ui.value.meals.isEmpty())
    }

    @Test fun `editing a field clears its error`() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onName("Lunch")
        vm.onField(MealInput.Field.FAT, "-5")
        vm.save(); advanceUntilIdle()
        assertTrue(vm.ui.value.form.errors.containsKey(MealInput.Field.FAT))
        vm.onField(MealInput.Field.FAT, "5")
        assertFalse(vm.ui.value.form.errors.containsKey(MealInput.Field.FAT))
    }

    @Test fun `startEdit loads values and save corrects in place`() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        fill(vm, "Lunch", "45"); vm.save(); advanceUntilIdle()
        val id = vm.ui.value.meals.first().id

        vm.startEdit(id); advanceUntilIdle()
        assertEquals("Lunch", vm.ui.value.form.name)
        assertEquals("45", vm.ui.value.form.carbs)
        assertEquals(id, vm.ui.value.form.editingMealId)

        vm.onField(MealInput.Field.CARBS, "60")
        vm.save(); advanceUntilIdle()
        assertEquals(1, vm.ui.value.meals.size) // still one meal
        val loaded = repo.getMeal(id)!!
        assertEquals(NutritionSource.CORRECTED, loaded.current.source)
        assertEquals(60.0, loaded.current.nutrition.carbsGrams!!, 0.0)
        assertEquals(45.0, loaded.original.nutrition.carbsGrams!!, 0.0) // original kept
    }

    @Test fun `delete removes the meal`() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        fill(vm, "Lunch", "45"); vm.save(); advanceUntilIdle()
        val id = vm.ui.value.meals.first().id
        vm.delete(id); advanceUntilIdle()
        assertTrue(vm.ui.value.meals.isEmpty())
    }

    @Test fun `today summary sums known values only`() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        fill(vm, "A", "45"); vm.onField(MealInput.Field.CALORIES, "500"); vm.save(); advanceUntilIdle()
        fill(vm, "B", "15"); vm.save(); advanceUntilIdle() // no calories -> unknown
        assertEquals(500.0, vm.ui.value.todayKcal!!, 0.0)   // only A's calories
        assertEquals(60.0, vm.ui.value.todayCarbs!!, 0.0)   // 45 + 15
    }

    @Test fun `toField formats integers without a trailing decimal`() {
        with(MealLogViewModel.Companion) {
            assertEquals("45", 45.0.toField())
            assertEquals("45.6", 45.6.toField())
            assertEquals("", null.toField())
        }
    }
}
