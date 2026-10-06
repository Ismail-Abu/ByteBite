package com.example.guione.meal.db

import com.example.guione.meal.Meal
import com.example.guione.meal.MealRevision
import com.example.guione.meal.Nutrition
import com.example.guione.meal.NutritionSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

/**
 * The domain <-> Room-row conversions, checked on the JVM. A dropped nutrient,
 * a swapped field, or a lost source would corrupt stored history, so the
 * mappings are pinned here independently of the database.
 */
class MealMappersTest {

    private val t = Instant.parse("2026-01-15T12:00:00Z")

    @Test fun `meal round-trips through its entity`() {
        val meal = Meal(
            id = "m1",
            occurredAt = t,
            occurrenceOffset = ZoneOffset.ofHours(-5),
            name = "Lunch 寿司",
            createdAt = t.plusSeconds(1),
            updatedAt = t.plusSeconds(2),
            currentRevisionId = "r1",
        )
        assertEquals(meal, meal.toEntity().toDomain())
    }

    @Test fun `revision round-trips and preserves explicit unknowns`() {
        val rev = MealRevision(
            id = "r1",
            mealId = "m1",
            nutrition = Nutrition(caloriesKcal = 0.0, carbsGrams = 45.6), // rest unknown
            source = NutritionSource.CORRECTED,
            originalEstimateRef = "r0",
            createdAt = t,
        )
        val back = rev.toEntity().toDomain()
        assertEquals(rev, back)
        assertEquals(0.0, back.nutrition.caloriesKcal!!, 0.0)
        assertNull(back.nutrition.massGrams)
        assertNull(back.nutrition.proteinGrams)
        assertNull(back.nutrition.fatGrams)
    }

    @Test fun `null nutrition columns map to a fully unknown reading`() {
        val entity = MealRevisionEntity(
            id = "r1", mealId = "m1",
            caloriesKcal = null, massGrams = null, carbsGrams = null,
            proteinGrams = null, fatGrams = null,
            source = "MANUAL", originalEstimateRef = null,
            createdAtEpochMs = t.toEpochMilli(),
        )
        assertEquals(Nutrition.UNKNOWN, entity.toDomain().nutrition)
    }
}
