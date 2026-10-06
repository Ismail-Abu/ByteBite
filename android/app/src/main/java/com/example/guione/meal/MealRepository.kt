package com.example.guione.meal

/**
 * A meal and the full chain of revisions behind it, newest-created last.
 *
 * Carrying every revision, not just the current one, is what lets the detail
 * screen show "original vs corrected" and lets a correction be undone in
 * principle: the model's first estimate is never thrown away.
 */
data class MealWithRevisions(
    val meal: Meal,
    val revisions: List<MealRevision>,
) {
    /** The live revision the meal currently points at. */
    val current: MealRevision
        get() = revisions.first { it.id == meal.currentRevisionId }

    /**
     * The original estimate: the revision that references no earlier one.
     * Identified by [MealRevision.originalEstimateRef] being null rather than by
     * earliest timestamp, so it is stable even when a correction lands in the
     * same millisecond as the original. Falls back to the earliest revision if a
     * chain somehow has no un-referenced root.
     */
    val original: MealRevision
        get() = revisions.firstOrNull { it.originalEstimateRef == null }
            ?: revisions.minWith(compareBy({ it.createdAt }, { it.id }))
}

/**
 * The persistence contract for meals, written once so the UI and view models
 * never touch a storage engine directly. An in-memory implementation
 * ([InMemoryMealRepository]) backs tests and runs the app before a durable
 * store is wired; a Room-backed implementation is the production target and
 * drops in behind the same interface.
 *
 * All operations are suspend so the production implementation can keep database
 * work off the UI thread; the in-memory one completes synchronously.
 *
 * Saves are keyed by the caller-minted [Meal.id] / [MealRevision.id], so a
 * retried or double-tapped save with the same id updates in place instead of
 * creating a second record.
 */
/**
 * A meal and its current revision's nutrition, for list rendering. Produced by a
 * single joined query so a list never does per-row lookups.
 */
data class MealListItem(
    val id: String,
    val name: String,
    val occurredAt: java.time.Instant,
    val occurrenceOffset: java.time.ZoneOffset,
    val nutrition: Nutrition,
    val source: NutritionSource,
)

interface MealRepository {

    /** Observes the whole meal list (meal + current nutrition), newest first. */
    fun observeMeals(): kotlinx.coroutines.flow.Flow<List<MealListItem>>

    /** Observes one meal and its revision chain; emits null when it is deleted. */
    fun observeMeal(id: String): kotlinx.coroutines.flow.Flow<MealWithRevisions?>

    /**
     * Persists a manual meal with a single [NutritionSource.MANUAL] revision.
     *
     * [mealId] and [revisionId] are minted by the caller when the draft is
     * created and held across recomposition and process death, so a double tap,
     * a retry after a failure, or a save after the activity is recreated all
     * carry the same ids and upsert the one record instead of creating a second.
     * Saving the same ids twice is therefore idempotent.
     */
    suspend fun saveManualMeal(
        valid: MealInput.Valid,
        mealId: String = newId(),
        revisionId: String = newId(),
    ): Meal

    /**
     * Appends a [NutritionSource.CORRECTED] revision to an existing meal and moves
     * the meal's current pointer to it. The new revision references the meal's
     * original revision as its [MealRevision.originalEstimateRef], and every
     * earlier revision is kept. Returns the updated meal, or null if [mealId] is
     * unknown.
     */
    suspend fun correctMeal(mealId: String, valid: MealInput.Valid): Meal?

    /** The meal and all its revisions, or null if unknown. */
    suspend fun getMeal(id: String): MealWithRevisions?

    /** Every meal, most recent occurrence first. */
    suspend fun listMeals(): List<Meal>

    /**
     * Deletes a meal and all of its revisions. Independent records that merely
     * referenced the meal (e.g. a glucose observation) are not the repository's
     * to remove and are left alone.
     */
    suspend fun deleteMeal(id: String)

    /** Removes every meal and revision. Backs the settings "delete all data" control. */
    suspend fun deleteAll()
}
