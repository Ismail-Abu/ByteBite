package com.example.guione.meal.ui

import androidx.lifecycle.SavedStateHandle
import com.example.guione.meal.InMemoryMealRepository
import com.example.guione.meal.MealInput
import com.example.guione.meal.MealListItem
import com.example.guione.meal.MealRepository
import com.example.guione.meal.MealWithRevisions
import com.example.guione.meal.Nutrition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Regression tests for the add/edit correctness fixes: preserved occurrence time
 * on edit (#1), recoverable save failure (#2), full draft restore (#3), and the
 * duplicate-save guard (#4).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddEditMealViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val now = Instant.parse("2026-01-15T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneId.of("UTC"))
    private lateinit var repo: InMemoryMealRepository

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        var n = 0
        repo = InMemoryMealRepository(clock) { "gen-${n++}" }
    }

    @After fun tearDown() = Dispatchers.resetMain()

    private fun vmNew(state: SavedStateHandle = SavedStateHandle()) =
        AddEditMealViewModel(repo, state, clock)

    private fun vmEdit(mealId: String, state: SavedStateHandle = SavedStateHandle()) =
        AddEditMealViewModel(repo, state.apply { set(AddEditMealViewModel.ARG_MEAL_ID, mealId) }, clock)

    private suspend fun seedMeal(occurredAt: Instant, carbs: Double): String {
        val meal = repo.saveManualMeal(
            MealInput.Valid("Seed", Nutrition(carbsGrams = carbs), occurredAt, ZoneOffset.UTC),
            mealId = "m1", revisionId = "r1",
        )
        return meal.id
    }

    @Test fun `editing preserves the original occurrence time`() = runTest(dispatcher) {
        val past = Instant.parse("2026-01-10T08:30:00Z")
        val id = seedMeal(past, 45.0)
        val vm = vmEdit(id)
        advanceUntilIdle()
        // occurrence loaded from the meal, not reset to now
        assertEquals(past, vm.ui.value.occurredAt)

        vm.onField(MealInput.Field.CARBS, "60")
        vm.save()
        advanceUntilIdle()
        // the correction kept the original instant rather than moving it to now
        assertEquals(past, repo.getMeal(id)!!.meal.occurredAt)
    }

    @Test fun `save failure keeps the draft and surfaces a retryable error`() = runTest(dispatcher) {
        val failing = FailingRepository()
        val vm = AddEditMealViewModel(failing, SavedStateHandle(), clock)
        advanceUntilIdle()
        vm.onName("Lunch")
        vm.onField(MealInput.Field.CARBS, "45")
        vm.save()
        advanceUntilIdle()
        assertNotNull(vm.ui.value.saveError)
        assertFalse(vm.ui.value.saving)
        assertEquals("Lunch", vm.ui.value.name) // draft intact
        assertEquals("45", vm.ui.value.carbs)
    }

    @Test fun `draft is restored after process recreation`() = runTest(dispatcher) {
        val state = SavedStateHandle()
        val first = vmNew(state)
        advanceUntilIdle()
        first.onName("Half bagel")
        first.onField(MealInput.Field.CARBS, "33")

        // New VM over the same SavedStateHandle = process recreation.
        val restored = vmNew(state)
        advanceUntilIdle()
        assertEquals("Half bagel", restored.ui.value.name)
        assertEquals("33", restored.ui.value.carbs)
    }

    @Test fun `rapid double save creates only one meal`() = runTest(dispatcher) {
        val vm = vmNew()
        advanceUntilIdle()
        vm.onName("Lunch")
        vm.onField(MealInput.Field.CARBS, "45")
        vm.save()
        vm.save() // second tap while the first write is in flight
        advanceUntilIdle()
        assertEquals(1, repo.listMeals().size)
    }

    @Test fun `editing twice does not add a second correction from a double tap`() = runTest(dispatcher) {
        val id = seedMeal(now, 45.0)
        val vm = vmEdit(id)
        advanceUntilIdle()
        vm.onField(MealInput.Field.CARBS, "60")
        vm.save()
        vm.save()
        advanceUntilIdle()
        // original + exactly one correction
        assertEquals(2, repo.getMeal(id)!!.revisions.size)
    }

    @Test fun `edited flag is false until a field changes and survives recreation`() = runTest(dispatcher) {
        val state = SavedStateHandle()
        val vm = vmNew(state)
        advanceUntilIdle()
        assertFalse(vm.ui.value.edited)
        vm.onField(MealInput.Field.CARBS, "10")
        assertTrue(vm.ui.value.edited)
        // process recreation keeps the unsaved-changes flag
        val restored = vmNew(state)
        advanceUntilIdle()
        assertTrue(restored.ui.value.edited)
    }

    @Test fun `invalid input blocks save and shows name and form errors separately`() = runTest(dispatcher) {
        val vm = vmNew()
        advanceUntilIdle()
        vm.save() // blank name, no values
        advanceUntilIdle()
        assertNotNull(vm.ui.value.nameError)
        assertNotNull(vm.ui.value.formError)
        assertTrue(repo.listMeals().isEmpty())
    }

    /** A repository that fails every save, for the recoverable-error path. */
    private class FailingRepository : MealRepository {
        override fun observeMeals(): Flow<List<MealListItem>> = kotlinx.coroutines.flow.flowOf(emptyList())
        override fun observeMeal(id: String): Flow<MealWithRevisions?> = kotlinx.coroutines.flow.flowOf(null)
        override suspend fun saveManualMeal(valid: MealInput.Valid, mealId: String, revisionId: String) =
            throw RuntimeException("disk full")
        override suspend fun correctMeal(mealId: String, valid: MealInput.Valid) = throw RuntimeException("disk full")
        override suspend fun getMeal(id: String): MealWithRevisions? = null
        override suspend fun listMeals() = emptyList<com.example.guione.meal.Meal>()
        override suspend fun deleteMeal(id: String) {}
        override suspend fun deleteAll() {}
    }
}
