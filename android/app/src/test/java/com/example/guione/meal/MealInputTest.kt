package com.example.guione.meal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Behaviour of the manual-entry / correction validator. These are the brief's
 * "input and meal values" cases: blank and Unicode names, empty vs explicit
 * zero, decimals and locale input, negative / enormous / NaN / infinity /
 * malformed / partial numbers, the at-least-one-value rule, and the future-time
 * policy. All pure JVM, no device.
 */
class MealInputTest {

    private val now: Instant = Instant.parse("2026-01-15T12:00:00Z")
    private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)

    private fun form(
        name: String = "Lunch",
        calories: String = "",
        mass: String = "",
        carbs: String = "",
        protein: String = "",
        fat: String = "",
        occurredAt: Instant? = null,
    ) = MealInput.Form(name, calories, mass, carbs, protein, fat, occurredAt)

    private fun ok(result: MealInput.Result): MealInput.Valid {
        assertTrue("expected Ok but got $result", result is MealInput.Result.Ok)
        return (result as MealInput.Result.Ok).valid
    }

    private fun rejected(result: MealInput.Result): List<MealInput.Error> {
        assertTrue("expected Rejected but got $result", result is MealInput.Result.Rejected)
        return (result as MealInput.Result.Rejected).errors
    }

    // --- Names ---

    @Test fun `blank name is rejected`() {
        assertTrue(rejected(MealInput.validate(form(name = "   "), clock)).isNotEmpty())
    }

    @Test fun `name is trimmed but inner spacing and unicode are kept`() {
        val v = ok(MealInput.validate(form(name = "  café ❤ 寿司  ", calories = "1"), clock))
        assertEquals("café ❤ 寿司", v.name)
    }

    @Test fun `name past the length cap is rejected`() {
        val long = "x".repeat(MealInput.MAX_NAME_LENGTH + 1)
        assertTrue(rejected(MealInput.validate(form(name = long, calories = "1"), clock)).isNotEmpty())
    }

    @Test fun `name exactly at the cap is accepted`() {
        val atCap = "x".repeat(MealInput.MAX_NAME_LENGTH)
        assertEquals(atCap, ok(MealInput.validate(form(name = atCap, calories = "1"), clock)).name)
    }

    // --- Empty vs explicit zero ---

    @Test fun `blank nutrient field is unknown not zero`() {
        val v = ok(MealInput.validate(form(calories = "100"), clock))
        assertNull(v.nutrition.massGrams)
        assertNull(v.nutrition.carbsGrams)
    }

    @Test fun `typed zero is a known zero`() {
        val v = ok(MealInput.validate(form(calories = "0", fat = "0"), clock))
        assertEquals(0.0, v.nutrition.caloriesKcal!!, 0.0)
        assertEquals(0.0, v.nutrition.fatGrams!!, 0.0)
    }

    // --- Decimals and locale ---

    @Test fun `decimal point parses`() {
        assertEquals(45.6, ok(MealInput.validate(form(carbs = "45.6"), clock)).nutrition.carbsGrams!!, 1e-9)
    }

    @Test fun `single comma is a decimal separator`() {
        assertEquals(1.5, ok(MealInput.validate(form(carbs = "1,5"), clock)).nutrition.carbsGrams!!, 1e-9)
    }

    @Test fun `comma grouping with a dot decimal parses`() {
        assertEquals(1234.5, ok(MealInput.validate(form(calories = "1,234.5"), clock)).nutrition.caloriesKcal!!, 1e-9)
    }

    @Test fun `trailing dot is tolerated`() {
        assertEquals(12.0, ok(MealInput.validate(form(carbs = "12."), clock)).nutrition.carbsGrams!!, 1e-9)
    }

    // --- Bad numbers ---

    @Test fun `negative is rejected`() {
        val e = rejected(MealInput.validate(form(carbs = "-5"), clock))
        assertTrue(e.any { it.field == MealInput.Field.CARBS })
    }

    @Test fun `enormous value past the ceiling is rejected`() {
        val e = rejected(MealInput.validate(form(calories = "999999"), clock))
        assertTrue(e.any { it.field == MealInput.Field.CALORIES })
    }

    @Test fun `NaN text is rejected`() {
        assertTrue(rejected(MealInput.validate(form(fat = "NaN"), clock)).isNotEmpty())
    }

    @Test fun `infinity text is rejected`() {
        assertTrue(rejected(MealInput.validate(form(fat = "Infinity"), clock)).isNotEmpty())
    }

    @Test fun `malformed text is rejected`() {
        assertTrue(rejected(MealInput.validate(form(protein = "12abc"), clock)).isNotEmpty())
    }

    @Test fun `bare sign or dot is not a number`() {
        assertTrue(MealInput.parseNumber("-", MealInput.Field.FAT) is MealInput.NumberResult.Invalid)
        assertTrue(MealInput.parseNumber(".", MealInput.Field.FAT) is MealInput.NumberResult.Invalid)
        assertTrue(MealInput.parseNumber("+", MealInput.Field.FAT) is MealInput.NumberResult.Invalid)
    }

    @Test fun `multiple dots are rejected`() {
        assertTrue(MealInput.parseNumber("1.2.3", MealInput.Field.FAT) is MealInput.NumberResult.Invalid)
    }

    @Test fun `all errors are collected not just the first`() {
        val e = rejected(MealInput.validate(form(name = "", carbs = "-1", fat = "NaN"), clock))
        assertTrue("expected name + carbs + fat errors, got $e", e.size >= 3)
    }

    // --- At least one value ---

    @Test fun `name with all fields blank records nothing and is rejected`() {
        assertTrue(rejected(MealInput.validate(form(), clock)).isNotEmpty())
    }

    // --- Time policy ---

    @Test fun `past occurrence time is accepted`() {
        val past = now.minusSeconds(3600 * 24 * 30)
        assertEquals(past, ok(MealInput.validate(form(calories = "1", occurredAt = past), clock)).occurredAt)
    }

    @Test fun `occurrence within skew tolerance is accepted`() {
        val soon = now.plusSeconds(60)
        ok(MealInput.validate(form(calories = "1", occurredAt = soon), clock))
    }

    @Test fun `far-future occurrence time is rejected`() {
        val future = now.plusSeconds(3600)
        assertTrue(rejected(MealInput.validate(form(calories = "1", occurredAt = future), clock)).isNotEmpty())
    }

    @Test fun `occurrence defaults to now when unset`() {
        assertEquals(now, ok(MealInput.validate(form(calories = "1"), clock)).occurredAt)
    }
}
