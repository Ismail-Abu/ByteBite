package com.example.guione.meal.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
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

/** JVM (Robolectric) Compose tests for History, Insights, and Settings content. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScreensRobolectricTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val now = Instant.parse("2026-01-15T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneId.of("UTC"))

    @Test fun historyGroupsByDay() {
        AppGraph.overrideClock(clock)
        val repo = InMemoryMealRepository(clock)
        runBlocking {
            repo.saveManualMeal(MealInput.Valid("Breakfast", Nutrition(carbsGrams = 30.0), now, ZoneOffset.UTC), "m1", "r1")
        }
        AppGraph.overrideRepository(repo)
        compose.setContent { ByteBiteTheme { HistoryScreen(onOpenMeal = {}, onOpenSettings = {}, bottomBar = {}) } }
        compose.onNodeWithText("Today").assertIsDisplayed()
        compose.onNodeWithText("Breakfast").assertIsDisplayed()
    }

    @Test fun insightsShowsHonestUnavailableState() {
        AppGraph.overrideClock(clock)
        AppGraph.overrideRepository(InMemoryMealRepository(clock))
        compose.setContent { ByteBiteTheme { InsightsScreen(onOpenSettings = {}, bottomBar = {}) } }
        compose.onNodeWithText("No insights available yet").assertIsDisplayed()
    }

    @Test fun settingsShowsHonestModelStatus() {
        AppGraph.overrideClock(clock)
        AppGraph.overrideRepository(InMemoryMealRepository(clock))
        compose.setContent { ByteBiteTheme { SettingsScreen(onBack = {}) } }
        compose.onNodeWithText("Nutrition estimation").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Not installed").performScrollTo().assertIsDisplayed()
    }
}
