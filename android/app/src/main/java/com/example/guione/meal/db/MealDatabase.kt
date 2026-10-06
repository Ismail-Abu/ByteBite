package com.example.guione.meal.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The on-device meal store.
 *
 * Schema export is on (see app/schemas), so every version is captured for
 * review and replayed by the migration tests. There is deliberately no
 * destructive-migration fallback: user meal history must survive an app update,
 * and a fallback would silently wipe it on the first schema change. When the
 * schema changes, bump [DB_VERSION] and add an explicit `Migration` here.
 */
@Database(
    entities = [MealEntity::class, MealRevisionEntity::class],
    version = MealDatabase.DB_VERSION,
    exportSchema = true,
)
abstract class MealDatabase : RoomDatabase() {

    abstract fun mealDao(): MealDao

    companion object {
        const val DB_VERSION = 1
        const val DB_NAME = "bytebite-meals.db"

        fun build(context: Context): MealDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                MealDatabase::class.java,
                DB_NAME,
            ).build()
    }
}
