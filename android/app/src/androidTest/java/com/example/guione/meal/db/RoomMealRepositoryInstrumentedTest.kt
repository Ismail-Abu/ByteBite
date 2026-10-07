package com.example.guione.meal.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.guione.meal.MealInput
import com.example.guione.meal.Nutrition
import com.example.guione.meal.NutritionSource
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * The Room-backed repository on real SQLite. Mirrors the JVM in-memory
 * repository tests so the two implementations are held to the same behaviour,
 * and additionally proves the things only a database can show: the foreign-key
 * cascade on delete, NULL nutrient columns, and transactional save.
 */
@RunWith(AndroidJUnit4::class)
class RoomMealRepositoryInstrumentedTest {

    private val now: Instant = Instant.parse("2026-01-15T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private lateinit var db: MealDatabase
    private lateinit var repo: RoomMealRepository

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MealDatabase::class.java,
        ).build()
        var n = 0
        repo = RoomMealRepository(db, clock) { "gen-${n++}" }
    }

    @After fun tearDown() = db.close()

    private fun valid(
        name: String = "Lunch",
        nutrition: Nutrition = Nutrition(caloriesKcal = 500.0, carbsGrams = 45.6),
        occurredAt: Instant = now,
    ) = MealInput.Valid(name, nutrition, occurredAt, ZoneOffset.UTC)

    @Test fun savedMealReloadsWithAllNutrients() = runBlocking {
        val n = Nutrition(caloriesKcal = 0.0, carbsGrams = 12.5) // mass/protein/fat unknown
        repo.saveManualMeal(valid(nutrition = n), "m1", "r1")
        val got = repo.getMeal("m1")!!
        assertEquals(NutritionSource.MANUAL, got.current.source)
        assertEquals(0.0, got.current.nutrition.caloriesKcal!!, 0.0)
        assertEquals(12.5, got.current.nutrition.carbsGrams!!, 0.0)
        assertNull(got.current.nutrition.massGrams)
        assertNull(got.current.nutrition.proteinGrams)
        assertNull(got.current.nutrition.fatGrams)
    }

    @Test fun repeatedSaveWithSameIdsDoesNotDuplicate() = runBlocking {
        repo.saveManualMeal(valid(), "m1", "r1")
        repo.saveManualMeal(valid(), "m1", "r1")
        assertEquals(1, repo.listMeals().size)
        assertEquals(1, repo.getMeal("m1")!!.revisions.size)
    }

    @Test fun listIsNewestOccurrenceFirst() = runBlocking {
        repo.saveManualMeal(valid(occurredAt = now.minusSeconds(100)), "old", "ro")
        repo.saveManualMeal(valid(occurredAt = now), "new", "rn")
        assertEquals(listOf("new", "old"), repo.listMeals().map { it.id })
    }

    @Test fun correctionAppendsRevisionKeepsOriginalAndReferencesIt() = runBlocking {
        repo.saveManualMeal(valid(nutrition = Nutrition(carbsGrams = 45.0)), "m1", "orig")
        val updated = repo.correctMeal("m1", valid(nutrition = Nutrition(carbsGrams = 60.0)))!!
        val got = repo.getMeal("m1")!!
        assertEquals(2, got.revisions.size)
        assertEquals(updated.currentRevisionId, got.current.id)
        assertEquals(NutritionSource.CORRECTED, got.current.source)
        assertEquals(60.0, got.current.nutrition.carbsGrams!!, 0.0)
        // original kept, unchanged, and referenced
        assertEquals(45.0, got.original.nutrition.carbsGrams!!, 0.0)
        assertEquals("orig", got.original.id)
        assertEquals("orig", got.current.originalEstimateRef)
    }

    @Test fun deleteCascadesToRevisions() = runBlocking {
        repo.saveManualMeal(valid(), "m1", "r1")
        repo.correctMeal("m1", valid(nutrition = Nutrition(carbsGrams = 60.0)))
        assertEquals(2, db.mealDao().revisions("m1").size)
        repo.deleteMeal("m1")
        assertNull(repo.getMeal("m1"))
        // foreign-key cascade removed the revision rows too
        assertTrue(db.mealDao().revisions("m1").isEmpty())
    }

    @Test fun deleteAllClearsEverything() = runBlocking {
        repo.saveManualMeal(valid(), "m1", "r1")
        repo.saveManualMeal(valid(), "m2", "r2")
        repo.deleteAll()
        assertTrue(repo.listMeals().isEmpty())
    }

    @Test fun correctingUnknownMealReturnsNull() = runBlocking {
        assertNull(repo.correctMeal("missing", valid()))
    }

    @Test fun inferredSavePersistsOnRealDatabase() = runBlocking {
        // Regression: the revision FK references the meal row, so the meal must be
        // written first. In-memory tests can't catch this; real SQLite enforces it.
        val raw = Nutrition(caloriesKcal = 500.0, carbsGrams = 45.0)
        repo.saveInferredMeal(valid(nutrition = raw), raw, "m1", "r1", "c1")
        val got = repo.getMeal("m1")!!
        assertEquals(1, got.revisions.size)
        assertEquals(NutritionSource.INFERRED, got.current.source)
        assertEquals(500.0, got.current.nutrition.caloriesKcal!!, 0.0)
    }

    @Test fun inferredSaveWithEditKeepsOriginalAndCorrection() = runBlocking {
        val raw = Nutrition(caloriesKcal = 500.0, carbsGrams = 45.0)
        val reviewed = Nutrition(caloriesKcal = 520.0, carbsGrams = 50.0)
        repo.saveInferredMeal(valid(nutrition = reviewed), raw, "m1", "r1", "c1")
        val got = repo.getMeal("m1")!!
        assertEquals(2, got.revisions.size)
        assertEquals(NutritionSource.INFERRED, got.original.source)
        assertEquals(45.0, got.original.nutrition.carbsGrams!!, 0.0)
        assertEquals(NutritionSource.CORRECTED, got.current.source)
        assertEquals("r1", got.current.originalEstimateRef)
    }
}
