package com.example.guione.meal

import android.content.Context
import com.example.guione.meal.db.MealDatabase
import com.example.guione.meal.db.RoomMealRepository

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

    fun mealRepository(context: Context): MealRepository =
        repository ?: synchronized(this) {
            repository ?: RoomMealRepository(MealDatabase.build(context)).also { repository = it }
        }

    /** Test seam: install a repository (e.g. in-memory) before the UI reads it. */
    fun overrideRepository(repo: MealRepository) {
        synchronized(this) { repository = repo }
    }
}
