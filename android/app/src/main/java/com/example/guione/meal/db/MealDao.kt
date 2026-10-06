package com.example.guione.meal.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface MealDao {

    // @Upsert (insert-or-UPDATE), deliberately not @Insert(REPLACE): INSERT OR
    // REPLACE on the meals row would DELETE the existing row before re-inserting
    // it, and the meal_revisions foreign key cascades on that delete — so a
    // correction, which updates the meal row, would silently wipe the meal's
    // revisions. @Upsert updates in place and leaves the children untouched.
    @Upsert
    suspend fun upsertMeal(meal: MealEntity)

    @Upsert
    suspend fun upsertRevision(revision: MealRevisionEntity)

    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun meal(id: String): MealEntity?

    @Query("SELECT * FROM meal_revisions WHERE mealId = :mealId ORDER BY createdAtEpochMs ASC, id ASC")
    suspend fun revisions(mealId: String): List<MealRevisionEntity>

    @Query("SELECT * FROM meals ORDER BY occurredAtEpochMs DESC, id ASC")
    suspend fun allMeals(): List<MealEntity>

    /** Cascades to the meal's revisions via the foreign key. */
    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun deleteMeal(id: String)

    @Query("DELETE FROM meals")
    suspend fun deleteAllMeals()
}
