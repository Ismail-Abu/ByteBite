package com.example.guione.meal

import android.content.Context
import com.example.guione.meal.db.MealDatabase
import com.example.guione.meal.db.RoomMealRepository
import com.example.guione.meal.glucose.GlucosePredictor
import com.example.guione.meal.glucose.UnavailableGlucosePredictor
import java.time.Clock

/**
 * The app's one dependency wiring point — a small service locator rather than a
 * DI framework, which suits a single-module app. It hands out the process-wide
 * [MealRepository], building the Room-backed one lazily on first use.
 *
 * [overrideRepository] lets an instrumented test swap in an in-memory store
 * before the UI starts; nothing else mutates the instance.
 */
object AppGraph {

    @Volatile
    private var repository: MealRepository? = null

    /** Process clock for day boundaries, freshness, and timestamps; overridable in tests. */
    @Volatile
    var clock: Clock = Clock.systemDefaultZone()
        private set

    /**
     * Glucose forecasting boundary. A blank reference today
     * ([UnavailableGlucosePredictor]); swap in a real implementation here once
     * the professor's model and its contract are available.
     */
    @Volatile
    var glucosePredictor: GlucosePredictor = UnavailableGlucosePredictor
        private set

    fun mealRepository(context: Context): MealRepository =
        repository ?: synchronized(this) {
            repository ?: RoomMealRepository(MealDatabase.build(context)).also { repository = it }
        }

    @Volatile
    private var settingsStore: SettingsStore? = null

    fun settings(context: Context): SettingsStore =
        settingsStore ?: synchronized(this) {
            settingsStore ?: SettingsStore(context).also { settingsStore = it }
        }

    /** Test seam: install a repository (e.g. in-memory) before the UI reads it. */
    fun overrideRepository(repo: MealRepository) {
        synchronized(this) { repository = repo }
    }

    /** Test seam: pin the clock so day-boundary logic is deterministic. */
    fun overrideClock(c: Clock) {
        clock = c
    }
}
