package com.example.guione.meal.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.guione.meal.AppGraph
import com.example.guione.meal.InMemoryMealRepository
import com.example.guione.meal.MealInput
import com.example.guione.meal.Nutrition
import com.example.guione.ui.theme.ByteBiteTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** JVM (Robolectric) Compose tests for the Today screen. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TodayScreenRobolectricTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val now = Instant.parse("2026-01-15T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneId.of("UTC"))

    private fun render() {
        compose.setContent {
            ByteBiteTheme {
                TodayScreen(onAddMeal = {}, onOpenMeal = {}, onOpenHistory = {}, onOpenSettings = {}, bottomBar = {})
            }
        }
    }

    @Test fun showsEmptyStateWhenNoMeals() {
        AppGraph.overrideClock(clock)
        AppGraph.overrideRepository(InMemoryMealRepository(clock))
        render()
        compose.onNodeWithText("No meals yet").assertIsDisplayed()
    }

    @Test fun countsPartialMealAndLabelsKnownTotals() {
        AppGraph.overrideClock(clock)
        val repo = InMemoryMealRepository(clock)
        runBlocking {
            // calories only -> meal still counts, totals are partial
            repo.saveManualMeal(MealInput.Valid("Toast", Nutrition(caloriesKcal = 120.0), now, ZoneOffset.UTC), "m1", "r1")
        }
        AppGraph.overrideRepository(repo)
        render()
        compose.onNodeWithText("Known totals").assertIsDisplayed()
        compose.onNodeWithText("Toast").assertIsDisplayed()
    }
}
