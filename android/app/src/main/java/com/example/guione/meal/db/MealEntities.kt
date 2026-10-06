package com.example.guione.meal.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room row for a meal. Time is stored as a UTC epoch-millisecond value plus the
 * zone offset in seconds, the primitive form of the domain's instant-and-offset
 * pair; millisecond resolution is ample for a logged meal.
 */
@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey val id: String,
    val occurredAtEpochMs: Long,
    val occurrenceOffsetSeconds: Int,
    val name: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val currentRevisionId: String,
)

/**
 * Room row for one revision of a meal's nutrition.
 *
 * The five figures are nullable columns so an explicitly-unknown value is stored
 * as SQL NULL, never a zero. The foreign key cascades on delete: removing a meal
 * removes its revisions in the same statement, which is how "deleting a meal
 * cleans up its dependent records" is enforced at the storage layer rather than
 * in application code that a crash could skip.
 */
@Entity(
    tableName = "meal_revisions",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("mealId")],
)
data class MealRevisionEntity(
    @PrimaryKey val id: String,
    val mealId: String,
    val caloriesKcal: Double?,
    val massGrams: Double?,
    val carbsGrams: Double?,
    val proteinGrams: Double?,
    val fatGrams: Double?,
    /** [com.example.guione.meal.NutritionSource] name. */
    val source: String,
    val originalEstimateRef: String?,
    val createdAtEpochMs: Long,
)
