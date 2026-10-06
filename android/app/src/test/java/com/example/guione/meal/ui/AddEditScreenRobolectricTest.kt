package com.example.guione.meal.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.guione.meal.AppGraph
import com.example.guione.meal.InMemoryMealRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** JVM (Robolectric) Compose tests for the add-meal form. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AddEditScreenRobolectricTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val clock = Clock.fixed(Instant.parse("2026-01-15T12:00:00Z"), ZoneId.of("UTC"))

    @Test fun emptyFormShowsValidationOnSaveAndScrollsToIt() {
        AppGraph.overrideClock(clock)
        AppGraph.overrideRepository(InMemoryMealRepository(clock))
        compose.setContent { AddEditMealScreen(onBack = {}, onSaved = {}) }
        compose.onNodeWithText("Save meal").performScrollTo().performClick()
        compose.waitForIdle()
        // The form-level error renders and can be scrolled to.
        compose.onNodeWithText("enter at least one nutrition value").performScrollTo().assertIsDisplayed()
    }
}
