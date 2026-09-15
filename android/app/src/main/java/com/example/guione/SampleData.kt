package com.example.guione

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Every number, meal, and label in the app lives here.
 * All values are hardcoded from the two HTML mockups.
 * Dates are relative to "today" so the calendar's current week always matches.
 */

data class KitchenMeal(
    val name: String,
    val kcal: Int,
    val slot: String,       // "lunch", "dinner", "breakfast"
    val dot: Color,
    val date: LocalDate
)

data class TryNextItem(
    val name: String,
    val sub: String,
    val tag: String,
    val alt: Boolean,       // false = green pill, true = orange pill
    val dot: Color
)

data class GlucoseMeal(
    val name: String,
    val carbs: Double,
    val time: String,
    val source: String,
    val emoji: String,
    val tint: Color,
    val spike: String,
    val spikeBad: Boolean,
    val date: LocalDate
)

data class SteadySwap(val emoji: String, val text: String, val tag: String)

object SampleData {
    val today: LocalDate = LocalDate.now()
    val weekStart: LocalDate = today.with(DayOfWeek.MONDAY)

    private val d0 = today               // scan day
    private val d1 = today.minusDays(1)  // yesterday
    private val d2 = today.minusDays(2)  // two days ago

    // The scanned dish (shared by both experiences)
    const val DISH_NAME = "Chicken & rice bowl"
    const val DISH_KCAL = 542
    const val DISH_CARBS = 45.6
    const val DISH_PROTEIN = 42.1
    const val DISH_FAT = 18.2
    const val DISH_MASS = 387

    val kitchenMeals = mutableStateListOf(
        KitchenMeal("Chicken & rice bowl", 542, "lunch", Color(0xFFB7CE8F), d0),
        KitchenMeal("Beef chili & cornbread", 731, "dinner", Color(0xFF9C5B4B), d1),
        KitchenMeal("Veggie omelet & toast", 438, "breakfast", Color(0xFFE9C46A), d1),
        KitchenMeal("Salmon & potatoes", 655, "dinner", Color(0xFFD98E73), d2),
        KitchenMeal("Caesar salad & chicken", 512, "lunch", Color(0xFF8FBF9F), d2)
    )

    val tryNext = listOf(
        TryNextItem("Greek yogurt & berries", "quick protein boost, 160 cal", "+protein", false, Color(0xFFF2E3C9)),
        TryNextItem("Lentil soup", "lighter swap for chili night", "+protein", false, Color(0xFFC9D9A8)),
        TryNextItem("White fish tacos", "trims 14g fat off dinner", "less fat", true, Color(0xFFCFE3E0)),
        TryNextItem("Oats with banana", "steadier morning carbs", "fiber", true, Color(0xFFEFD9A8))
    )

    val glucoseMeals = mutableStateListOf(
        GlucoseMeal("Chicken & rice bowl", 45.6, "12:42", "logged from scan", "\uD83C\uDF7D\uFE0F", Color(0xFFE4F3EA), "gentle rise", false, d0),
        GlucoseMeal("Bagel & cream cheese", 52.0, "7:55", "logged from scan", "\uD83E\uDD6F", Color(0xFFFDF0DE), "spiked +64", true, d0),
        GlucoseMeal("Latte, 2% milk", 13.0, "7:20", "quick add", "\u2615", Color(0xFFE7F2F7), "gentle rise", false, d0)
    )

    val steadySwaps = listOf(
        SteadySwap("\uD83C\uDF5E", "Seeded bread instead of bagel", "smaller spike"),
        SteadySwap("\uD83E\uDD57", "Half rice, half roasted veg in bowls", "-15 g carbs"),
        SteadySwap("\uD83E\uDD5A", "Add protein at breakfast", "slows the rise")
    )

    // Today card values (glucose)
    const val GLUCOSE_NOW = 128
    const val TIME_IN_RANGE = "78% in range"
    const val CARB_BUDGET = 180

    /**
     * Carbs logged today in [meals]. Computed rather than hardcoded so logging a
     * scan visibly moves the Today card during a demo.
     */
    fun carbsToday(meals: List<GlucoseMeal>): Int =
        meals.filter { it.date == today }.sumOf { it.carbs }.roundToInt()

    /** Meal slot for a log entry made now. */
    fun slotNow(): String = when (java.time.LocalTime.now().hour) {
        in 4..10 -> "breakfast"
        in 11..15 -> "lunch"
        else -> "dinner"
    }

    // Glucose day stats (Combined experience)
    const val GLUCOSE_AVG = 121
    const val GLUCOSE_PEAK = 168
    const val GLUCOSE_LOW = 84

    // Profile (Combined experience) - kept in memory for the session
    var profileWeight: String by mutableStateOf("")
    var profileHeight: String by mutableStateOf("")
    var profileActivity: String by mutableStateOf("")

    // Combined experience: live in-session log.
    // Seeded WITHOUT today's chicken & rice bowl so "Log meal" after a scan
    // (or the Add page) visibly adds entries during a demo.
    val fusionMeals = mutableStateListOf(
        GlucoseMeal("Bagel & cream cheese", 52.0, "7:55", "logged from scan", "\uD83E\uDD6F", Color(0xFFFDF0DE), "spiked +64", true, d0),
        GlucoseMeal("Latte, 2% milk", 13.0, "7:20", "quick add", "\u2615", Color(0xFFE7F2F7), "gentle rise", false, d0),
        GlucoseMeal("Beef chili & cornbread", 58.4, "18:30", "logged from scan", "\uD83C\uDF72", Color(0xFFF6E3DC), "spiked +52", true, d1),
        GlucoseMeal("Veggie omelet & toast", 21.0, "8:05", "logged from scan", "\uD83C\uDF73", Color(0xFFFBF0DA), "gentle rise", false, d1)
    )
}
