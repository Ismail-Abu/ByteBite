package com.example.guione.meal

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock

/**
 * A [MealRepository] that keeps everything in process memory.
 *
 * It is the test double for the domain's behaviour and the store the app can
 * run on before Room is wired; it is not durable and loses everything when the
 * process dies, which the production build must not rely on.
 *
 * [clock] supplies creation/update timestamps and [idFactory] the record ids,
 * both injected so tests can make saves deterministic. A [Mutex] serialises
 * mutations so concurrent saves/corrections cannot interleave into a torn state.
 */
class InMemoryMealRepository(
    private val clock: Clock = Clock.systemUTC(),
    private val idFactory: () -> String = ::newId,
) : MealRepository {

    private val meals = LinkedHashMap<String, Meal>()
    private val revisions = LinkedHashMap<String, MutableList<MealRevision>>()
    private val lock = Mutex()

    override suspend fun saveManualMeal(
        valid: MealInput.Valid,
        mealId: String,
        revisionId: String,
    ): Meal = lock.withLock {
        val now = clock.instant()
        // Idempotent upsert: a repeated save with the same ids keeps the original
        // creation time and does not append a second revision, so a double tap or
        // a retry cannot duplicate the meal.
        val createdAt = meals[mealId]?.createdAt ?: now
        val revision = MealRevision(
            id = revisionId,
            mealId = mealId,
            nutrition = valid.nutrition,
            source = NutritionSource.MANUAL,
            originalEstimateRef = null,
            createdAt = revisions[mealId]?.firstOrNull { it.id == revisionId }?.createdAt ?: now,
        )
        val meal = Meal(
            id = mealId,
            occurredAt = valid.occurredAt,
            occurrenceOffset = valid.occurrenceOffset,
            name = valid.name,
            createdAt = createdAt,
            updatedAt = now,
            currentRevisionId = revisionId,
        )
        meals[mealId] = meal
        revisions[mealId] = mutableListOf(revision)
        meal
    }

    override suspend fun correctMeal(mealId: String, valid: MealInput.Valid): Meal? = lock.withLock {
        val existing = meals[mealId] ?: return@withLock null
        val chain = revisions.getValue(mealId)
        val original = chain.minByOrNull { it.createdAt }!!
        val now = clock.instant()
        val revision = MealRevision(
            id = idFactory(),
            mealId = mealId,
            nutrition = valid.nutrition,
            source = NutritionSource.CORRECTED,
            originalEstimateRef = original.id,
            createdAt = now,
        )
        chain += revision
        val updated = existing.copy(
            name = valid.name,
            occurredAt = valid.occurredAt,
            occurrenceOffset = valid.occurrenceOffset,
            updatedAt = now,
            currentRevisionId = revision.id,
        )
        meals[mealId] = updated
        updated
    }

    override suspend fun getMeal(id: String): MealWithRevisions? = lock.withLock {
        val meal = meals[id] ?: return@withLock null
        MealWithRevisions(meal, revisions.getValue(id).toList())
    }

    override suspend fun listMeals(): List<Meal> = lock.withLock {
        meals.values.sortedByDescending { it.occurredAt }
    }

    override suspend fun deleteMeal(id: String): Unit = lock.withLock {
        meals.remove(id)
        revisions.remove(id)
    }

    override suspend fun deleteAll(): Unit = lock.withLock {
        meals.clear()
        revisions.clear()
    }
}
