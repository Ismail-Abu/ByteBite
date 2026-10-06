package com.example.guione.meal

/**
 * The five nutrition figures ByteBite tracks, in fixed units: calories in kcal,
 * mass and the three macronutrients in grams.
 *
 * Every field is nullable on purpose. A null means the value is *explicitly
 * unknown* — the user left it blank, or the source did not produce it. It is
 * never silently coerced to zero: a plate with unknown fat is not a plate with
 * zero fat, and the difference matters once a downstream model or a carbohydrate
 * count reads these numbers. Code that needs a concrete number must decide what
 * to do with a null rather than having a zero invented for it.
 *
 * The values carry no source or provenance of their own; that lives on
 * [MealRevision], so the same shape describes a manual entry, a raw model
 * estimate, and a user correction.
 */
data class Nutrition(
    val caloriesKcal: Double? = null,
    val massGrams: Double? = null,
    val carbsGrams: Double? = null,
    val proteinGrams: Double? = null,
    val fatGrams: Double? = null,
) {
    /** True when every field is unknown — an empty shell with nothing recorded. */
    val isEmpty: Boolean
        get() = caloriesKcal == null && massGrams == null && carbsGrams == null &&
            proteinGrams == null && fatGrams == null

    /** The five values in the model's output order, nulls preserved. */
    fun asList(): List<Double?> =
        listOf(caloriesKcal, massGrams, fatGrams, carbsGrams, proteinGrams)

    companion object {
        /** The empty reading: all five fields explicitly unknown. */
        val UNKNOWN = Nutrition()
    }
}

/**
 * Where a [MealRevision]'s numbers came from.
 *
 * - [MANUAL]: typed by the user, no photo involved.
 * - [INFERRED]: produced by the on-device nutrition model from a photo.
 * - [CORRECTED]: the user edited an earlier revision; the revision it replaced
 *   is kept and referenced so the original estimate is never lost.
 */
enum class NutritionSource { MANUAL, INFERRED, CORRECTED }
