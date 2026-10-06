package com.example.guione.meal

import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

/**
 * A logged meal.
 *
 * Identity is a stable [UUID] string, not the row number or the creation time:
 * two meals can be created in the same millisecond, and a wall-clock identity
 * would collide on a device-time change. The id is minted once, in the domain,
 * so a record can be referenced before it is ever persisted.
 *
 * Time is stored as the UTC [Instant] it occurred plus the [ZoneOffset] in
 * effect where it was logged. Keeping the offset means a meal eaten at 7pm in
 * one time zone still reads as 7pm after the user travels, and day-boundary and
 * freshness logic can reconstruct the local wall time deterministically rather
 * than guessing from the device's current zone.
 *
 * [currentRevisionId] points at the live [MealRevision]. Corrections append a
 * new revision and move this pointer; the superseded revisions stay, so the
 * original estimate behind a corrected meal is always recoverable.
 */
data class Meal(
    val id: String,
    val occurredAt: Instant,
    val occurrenceOffset: ZoneOffset,
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val currentRevisionId: String,
)

/**
 * One version of a meal's nutrition. A meal has at least one revision (its
 * original) and gains another each time the user corrects it.
 *
 * [originalEstimateRef] links a [NutritionSource.CORRECTED] revision back to the
 * revision it was derived from, so "original vs corrected" can be shown on the
 * detail screen and so a correction never overwrites the model's first answer.
 * It is null for the first revision of a meal.
 */
data class MealRevision(
    val id: String,
    val mealId: String,
    val nutrition: Nutrition,
    val source: NutritionSource,
    val originalEstimateRef: String?,
    val createdAt: Instant,
)

/** Mints a new random identifier for a [Meal] or [MealRevision]. */
fun newId(): String = UUID.randomUUID().toString()
