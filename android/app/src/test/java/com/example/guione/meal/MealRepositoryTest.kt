package com.example.guione.meal

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Behaviour the [InMemoryMealRepository] guarantees for the domain, independent
 * of any storage engine: these are the brief's persistence cases that do not
 * need a device — every nutrient survives save/reload/edit, a correction keeps
 * the original estimate, idempotent saves do not duplicate, and delete cleans
 * up the meal's own revisions.
 */
class MealRepositoryTest {

    private val now: Instant = Instant.parse("2026-01-15T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    /** Deterministic ids for correctMeal's minted revisions. */
    private fun repoWithCounter(): InMemoryMealRepository {
        var n = 0
        return InMemoryMealRepository(clock) { "gen-${n++}" }
    }

    private fun valid(
        name: String = "Lunch",
        nutrition: Nutrition = Nutrition(caloriesKcal = 500.0, carbsGrams = 45.6),
        occurredAt: Instant = now,
    ) = MealInput.Valid(name, nutrition, occurredAt, ZoneOffset.UTC)

    @Test fun `saved meal is retrievable with one manual revision`() = runBlocking {
        val repo = repoWithCounter()
        val meal = repo.saveManualMeal(valid(), mealId = "m1", revisionId = "r1")
        val loaded = repo.getMeal(meal.id)
        assertNotNull(loaded)
        assertEquals(1, loaded!!.revisions.size)
        assertEquals(NutritionSource.MANUAL, loaded.current.source)
        assertEquals("r1", loaded.meal.currentRevisionId)
    }

    @Test fun `every nutrient survives save including explicit unknowns`() = runBlocking {
        val repo = repoWithCounter()
        // calories known zero, carbs known, the rest explicitly unknown.
        val n = Nutrition(caloriesKcal = 0.0, carbsGrams = 12.5)
        repo.saveManualMeal(valid(nutrition = n), mealId = "m1", revisionId = "r1")
        val got = repo.getMeal("m1")!!.current.nutrition
        assertEquals(0.0, got.caloriesKcal!!, 0.0)
        assertEquals(12.5, got.carbsGrams!!, 0.0)
        assertNull(got.massGrams)
        assertNull(got.proteinGrams)
        assertNull(got.fatGrams)
    }

    @Test fun `repeated save with the same ids does not duplicate`() = runBlocking {
        val repo = repoWithCounter()
        repo.saveManualMeal(valid(), mealId = "m1", revisionId = "r1")
        repo.saveManualMeal(valid(), mealId = "m1", revisionId = "r1")
        assertEquals(1, repo.listMeals().size)
        assertEquals(1, repo.getMeal("m1")!!.revisions.size)
    }

    @Test fun `listMeals is newest occurrence first`() = runBlocking {
        val repo = repoWithCounter()
        repo.saveManualMeal(valid(occurredAt = now.minusSeconds(100)), mealId = "old", revisionId = "ro")
        repo.saveManualMeal(valid(occurredAt = now), mealId = "new", revisionId = "rn")
        assertEquals(listOf("new", "old"), repo.listMeals().map { it.id })
    }

    @Test fun `correction appends a revision and moves the pointer`() = runBlocking {
        val repo = repoWithCounter()
        repo.saveManualMeal(valid(nutrition = Nutrition(carbsGrams = 45.0)), mealId = "m1", revisionId = "orig")
        val updated = repo.correctMeal("m1", valid(nutrition = Nutrition(carbsGrams = 60.0)))!!
        val loaded = repo.getMeal("m1")!!
        assertEquals(2, loaded.revisions.size)
        assertEquals(updated.currentRevisionId, loaded.current.id)
        assertEquals(NutritionSource.CORRECTED, loaded.current.source)
        assertEquals(60.0, loaded.current.nutrition.carbsGrams!!, 0.0)
    }

    @Test fun `correction keeps the original estimate and references it`() = runBlocking {
        val repo = repoWithCounter()
        repo.saveManualMeal(valid(nutrition = Nutrition(carbsGrams = 45.0)), mealId = "m1", revisionId = "orig")
        repo.correctMeal("m1", valid(nutrition = Nutrition(carbsGrams = 60.0)))
        val loaded = repo.getMeal("m1")!!
        // original revision still present and unchanged
        assertEquals(45.0, loaded.original.nutrition.carbsGrams!!, 0.0)
        assertEquals("orig", loaded.original.id)
        // the correction points back at the original
        assertEquals("orig", loaded.current.originalEstimateRef)
    }

    @Test fun `correcting an unknown meal returns null`() = runBlocking {
        assertNull(repoWithCounter().correctMeal("missing", valid()))
    }

    @Test fun `delete removes the meal and its revisions`() = runBlocking {
        val repo = repoWithCounter()
        repo.saveManualMeal(valid(), mealId = "m1", revisionId = "r1")
        repo.deleteMeal("m1")
        assertNull(repo.getMeal("m1"))
        assertTrue(repo.listMeals().isEmpty())
    }

    @Test fun `deleteAll clears everything`() = runBlocking {
        val repo = repoWithCounter()
        repo.saveManualMeal(valid(), mealId = "m1", revisionId = "r1")
        repo.saveManualMeal(valid(), mealId = "m2", revisionId = "r2")
        repo.deleteAll()
        assertTrue(repo.listMeals().isEmpty())
    }
}
