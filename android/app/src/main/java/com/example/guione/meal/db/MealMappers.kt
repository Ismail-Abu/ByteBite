package com.example.guione.meal.db

import com.example.guione.meal.Meal
import com.example.guione.meal.MealRevision
import com.example.guione.meal.Nutrition
import com.example.guione.meal.NutritionSource
import java.time.Instant
import java.time.ZoneOffset

/**
 * Pure conversions between the domain types and their Room rows. Kept apart from
 * the DAO so they can be unit-tested on the JVM without a database: a mapping
 * bug that dropped a nutrient or mislabelled a source would otherwise only show
 * up as wrong data on a device.
 */

fun Meal.toEntity(): MealEntity = MealEntity(
    id = id,
    occurredAtEpochMs = occurredAt.toEpochMilli(),
    occurrenceOffsetSeconds = occurrenceOffset.totalSeconds,
    name = name,
    createdAtEpochMs = createdAt.toEpochMilli(),
    updatedAtEpochMs = updatedAt.toEpochMilli(),
    currentRevisionId = currentRevisionId,
)

fun MealEntity.toDomain(): Meal = Meal(
    id = id,
    occurredAt = Instant.ofEpochMilli(occurredAtEpochMs),
    occurrenceOffset = ZoneOffset.ofTotalSeconds(occurrenceOffsetSeconds),
    name = name,
    createdAt = Instant.ofEpochMilli(createdAtEpochMs),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMs),
    currentRevisionId = currentRevisionId,
)

fun MealRevision.toEntity(): MealRevisionEntity = MealRevisionEntity(
    id = id,
    mealId = mealId,
    caloriesKcal = nutrition.caloriesKcal,
    massGrams = nutrition.massGrams,
    carbsGrams = nutrition.carbsGrams,
    proteinGrams = nutrition.proteinGrams,
    fatGrams = nutrition.fatGrams,
    source = source.name,
    originalEstimateRef = originalEstimateRef,
    createdAtEpochMs = createdAt.toEpochMilli(),
)

fun MealListRow.toListItem(): com.example.guione.meal.MealListItem =
    com.example.guione.meal.MealListItem(
        id = id,
        name = name,
        occurredAt = Instant.ofEpochMilli(occurredAtEpochMs),
        occurrenceOffset = ZoneOffset.ofTotalSeconds(occurrenceOffsetSeconds),
        nutrition = Nutrition(
            caloriesKcal = caloriesKcal,
            massGrams = massGrams,
            carbsGrams = carbsGrams,
            proteinGrams = proteinGrams,
            fatGrams = fatGrams,
        ),
        source = NutritionSource.valueOf(source),
    )

fun MealRevisionEntity.toDomain(): MealRevision = MealRevision(
    id = id,
    mealId = mealId,
    nutrition = Nutrition(
        caloriesKcal = caloriesKcal,
        massGrams = massGrams,
        carbsGrams = carbsGrams,
        proteinGrams = proteinGrams,
        fatGrams = fatGrams,
    ),
    source = NutritionSource.valueOf(source),
    originalEstimateRef = originalEstimateRef,
    createdAt = Instant.ofEpochMilli(createdAtEpochMs),
)
