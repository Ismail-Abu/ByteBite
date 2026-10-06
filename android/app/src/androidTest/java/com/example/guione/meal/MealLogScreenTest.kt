package com.example.guione.meal

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

/**
 * The stateless meal-log screen, rendered from crafted state. Verifies the
 * honest empty state, error surfacing, a rendered saved meal, and that the
 * action buttons invoke their callbacks — without a view model or database.
 *
 * Compose UI tests drive input through Espresso. Espresso 3.6.1 (the current
 * stable runner) reflects on `InputManager.getInstance()`, which the API 37
 * preview system image removed, so on that image these tests skip rather than
 * fail; they execute on a stable API <= 35 emulator. The same screen logic is
 * covered device-independently by MealLogViewModelTest.
 */
@RunWith(AndroidJUnit4::class)
class MealLogScreenTest {

    private val compose = createComposeRule()

    // Gate the Compose rule: on an API Espresso cannot drive, throw the
    // assumption BEFORE delegating, so the Compose rule is never applied and its
    // Espresso-syncing teardown never runs. Each test is then reported as a
    // clean per-method skip (the test count stays intact, unlike a @BeforeClass
    // skip which the device runner counts as an incomplete run).
    @get:Rule
    val rule = TestRule { base, description ->
        object : Statement() {
            override fun evaluate() {
                assumeTrue(
                    "Espresso 3.6.1 is incompatible with the API ${Build.VERSION.SDK_INT} preview image",
                    Build.VERSION.SDK_INT <= 35,
                )
                compose.apply(base, description).evaluate()
            }
        }
    }

    private fun state(
        meals: List<MealLogViewModel.Row> = emptyList(),
        form: MealLogViewModel.Form = MealLogViewModel.Form(),
        todayKcal: Double? = null,
        todayCarbs: Double? = null,
    ) = MealLogViewModel.UiState(
        loading = false, meals = meals, todayKcal = todayKcal, todayCarbs = todayCarbs, form = form,
    )

    private fun render(
        state: MealLogViewModel.UiState,
        onSave: () -> Unit = {},
        onDelete: (String) -> Unit = {},
        onEdit: (String) -> Unit = {},
    ) {
        compose.setContent {
            MealLogScreen(
                state = state,
                onName = {}, onField = { _, _ -> }, onSave = onSave,
                onCancelEdit = {}, onEdit = onEdit, onDelete = onDelete,
            )
        }
    }

    @Test fun emptyStateShownWhenNoMeals() {
        render(state())
        compose.onNodeWithTag(MealLogTags.EMPTY).assertIsDisplayed()
        compose.onNodeWithText("No meals logged today").assertIsDisplayed()
    }

    @Test fun formLevelErrorIsShown() {
        render(state(form = MealLogViewModel.Form(errors = mapOf(null to "enter at least one nutrition value"))))
        compose.onNodeWithTag(MealLogTags.FORM_ERROR).assertIsDisplayed()
    }

    @Test fun savedConfirmationIsShown() {
        render(state(form = MealLogViewModel.Form(justSaved = true)))
        compose.onNodeWithText("Saved ✓").assertIsDisplayed()
    }

    @Test fun savedMealRowRenders() {
        val row = MealLogViewModel.Row("m1", "Chicken bowl", "12:42", 542.0, 45.6, NutritionSource.MANUAL)
        render(state(meals = listOf(row), todayKcal = 542.0, todayCarbs = 45.6))
        compose.onNodeWithText("Chicken bowl").assertIsDisplayed()
        compose.onNodeWithTag(MealLogTags.row("m1")).assertIsDisplayed()
    }

    @Test fun saveButtonInvokesCallback() {
        var saved = 0
        render(state(form = MealLogViewModel.Form(name = "x")), onSave = { saved++ })
        compose.onNodeWithTag(MealLogTags.SAVE).performClick()
        assertEquals(1, saved)
    }

    @Test fun deleteGoesThroughConfirmationDialog() {
        var deleted: String? = null
        val row = MealLogViewModel.Row("m1", "Lunch", "12:42", null, null, NutritionSource.MANUAL)
        render(state(meals = listOf(row)), onDelete = { deleted = it })
        // Tapping Delete on the row opens the confirm dialog; nothing deleted yet.
        compose.onNodeWithTag(MealLogTags.delete("m1")).performClick()
        assertTrue(deleted == null)
        // Confirm in the dialog.
        compose.onNodeWithText("Delete this meal?").assertIsDisplayed()
        compose.onNodeWithText("Delete meal").performClick()
        assertEquals("m1", deleted)
    }
}
