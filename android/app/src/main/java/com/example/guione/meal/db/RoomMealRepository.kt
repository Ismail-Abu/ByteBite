package com.example.guione.meal.db

import androidx.room.withTransaction
import com.example.guione.meal.Meal
import com.example.guione.meal.MealInput
import com.example.guione.meal.MealRevision
import com.example.guione.meal.MealRepository
import com.example.guione.meal.MealWithRevisions
import com.example.guione.meal.NutritionSource
import com.example.guione.meal.newId
import java.time.Clock
import java.time.Instant

/**
 * The durable [MealRepository]. Mirrors [com.example.guione.meal.InMemoryMealRepository]'s
 * behaviour on top of Room: the in-memory one is the spec and the test double,
 * this one is what ships.
 *
 * Each save/correction runs inside a single Room transaction so a meal row and
 * its revision row commit together or not at all — a crash between the two
 * cannot leave a meal that points at a revision that was never written. [clock]
 * and [idFactory] are injected for deterministic tests.
 */
class RoomMealRepository(
    private val db: MealDatabase,
    private val clock: Clock = Clock.systemUTC(),
    private val idFactory: () -> String = ::newId,
) : MealRepository {

    private val dao = db.mealDao()

    override suspend fun saveManualMeal(
        valid: MealInput.Valid,
        mealId: String,
        revisionId: String,
    ): Meal = db.withTransaction {
        val now = clock.instant()
        val existing = dao.meal(mealId)
        val createdAt = existing?.let { Instant.ofEpochMilli(it.createdAtEpochMs) } ?: now
        val revision = MealRevision(
            id = revisionId,
            mealId = mealId,
            nutrition = valid.nutrition,
            source = NutritionSource.MANUAL,
            originalEstimateRef = null,
            createdAt = now,
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
        dao.upsertMeal(meal.toEntity())
        dao.upsertRevision(revision.toEntity())
        meal
    }

    override suspend fun correctMeal(mealId: String, valid: MealInput.Valid): Meal? =
        db.withTransaction {
            val existing = dao.meal(mealId) ?: return@withTransaction null
            val chain = dao.revisions(mealId)
            val originalId = chain.firstOrNull()?.id
            val now = clock.instant()
            val revision = MealRevision(
                id = idFactory(),
                mealId = mealId,
                nutrition = valid.nutrition,
                source = NutritionSource.CORRECTED,
                originalEstimateRef = originalId,
                createdAt = now,
            )
            val updated = existing.toDomain().copy(
                name = valid.name,
                occurredAt = valid.occurredAt,
                occurrenceOffset = valid.occurrenceOffset,
                updatedAt = now,
                currentRevisionId = revision.id,
            )
            dao.upsertRevision(revision.toEntity())
            dao.upsertMeal(updated.toEntity())
            updated
        }

    override suspend fun getMeal(id: String): MealWithRevisions? {
        val meal = dao.meal(id) ?: return null
        return MealWithRevisions(meal.toDomain(), dao.revisions(id).map { it.toDomain() })
    }

    override suspend fun listMeals(): List<Meal> = dao.allMeals().map { it.toDomain() }

    override suspend fun deleteMeal(id: String) = dao.deleteMeal(id)

    override suspend fun deleteAll() = dao.deleteAllMeals()
}
