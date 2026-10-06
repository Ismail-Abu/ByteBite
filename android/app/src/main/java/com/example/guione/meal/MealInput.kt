package com.example.guione.meal

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

/**
 * Turns the raw strings a user types on the manual-entry and correction screens
 * into a validated [Nutrition] plus a meal name, or a precise list of what is
 * wrong. Pure and clock-injected so every rule can be unit-tested off-device.
 *
 * The rules it enforces are the policy ByteBite commits to for meal input:
 *
 *  - A blank or whitespace-only name is rejected; a name is required. Names are
 *    trimmed but otherwise kept as typed, so Unicode and spacing inside the name
 *    survive. Absurdly long names are rejected rather than stored unbounded.
 *  - An empty nutrient field is *unknown* (null), distinct from a typed "0"
 *    which is a real, known zero. Unknown never becomes an invented zero.
 *  - Numbers accept a leading sign only when non-negative, a decimal point, and
 *    one comma used as either a decimal separator (locale input) or thousands
 *    grouping; NaN, infinity, negatives, malformed text, and values past a sane
 *    per-field ceiling are rejected with a reason rather than coerced.
 *  - A manual meal must have a name and at least one known nutrient; an entry
 *    with a name and five blank fields records nothing and is rejected.
 *  - The occurrence time may be any time in the past but no more than a small
 *    clock-skew allowance into the future, so a mistyped future date is caught
 *    while an entry logged "just now" is not rejected over millisecond skew.
 */
object MealInput {

    /** Longest accepted meal name, in characters. A product choice, not a limit of storage. */
    const val MAX_NAME_LENGTH = 120

    /** Clock skew tolerated when rejecting future occurrence times. */
    val FUTURE_TOLERANCE: Duration = Duration.ofMinutes(5)

    /**
     * Per-field sanity ceilings. These are not nutrition science — they are the
     * point past which a single logged meal is certainly a typo (e.g. grams
     * entered in milligrams). The lower bound is always zero; negatives are
     * rejected before these apply.
     */
    enum class Field(val label: String, val max: Double) {
        CALORIES("calories", 20_000.0),
        MASS("mass", 20_000.0),
        CARBS("carbs", 5_000.0),
        PROTEIN("protein", 5_000.0),
        FAT("fat", 5_000.0),
    }

    /** One rejected field or form-level rule, paired with a human-readable reason. */
    data class Error(val field: Field?, val reason: String)

    sealed interface NumberResult {
        /** The field was blank: an explicit unknown, stored as null. */
        data object Unknown : NumberResult
        data class Value(val number: Double) : NumberResult
        data class Invalid(val reason: String) : NumberResult
    }

    /**
     * Parses one optional nutrient field. Blank is [NumberResult.Unknown];
     * anything present is validated against [field]'s bounds.
     */
    fun parseNumber(raw: String, field: Field): NumberResult {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return NumberResult.Unknown

        val normalized = normalizeDecimal(trimmed)
            ?: return NumberResult.Invalid("${field.label} is not a number")

        val value = normalized.toDoubleOrNull()
            ?: return NumberResult.Invalid("${field.label} is not a number")

        if (value.isNaN() || value.isInfinite()) {
            return NumberResult.Invalid("${field.label} is not a finite number")
        }
        if (value < 0.0) return NumberResult.Invalid("${field.label} cannot be negative")
        if (value > field.max) {
            return NumberResult.Invalid("${field.label} is larger than ${field.max.toInt()}")
        }
        return NumberResult.Value(value)
    }

    /**
     * Normalizes an entered number to a plain `Double`-parseable string, or null
     * if the shape is not a valid number under ByteBite's explicit policy.
     *
     * The policy is deliberately unambiguous rather than clever, because the one
     * thing a nutrition field must never do is silently change a value's
     * magnitude (turn "1,,2" into 12, or a European "1.234,5" into 1.2345):
     *
     *  - '.' is the ONLY decimal separator, and may appear at most once.
     *  - ',' is ONLY a thousands-grouping separator, allowed in the integer part,
     *    and must form valid groups: a first group of 1-3 digits and every later
     *    group exactly 3 digits, with no empty, leading, trailing, or doubled
     *    comma, and never after the decimal point.
     *  - An optional single leading sign is accepted (a '-' flows through to the
     *    negative check, which rejects it with a clearer message).
     *
     * Anything that does not fit — "1,5", "1.234,5", "1,234,", "1,,2", "1.2.3",
     * letters — is rejected here and surfaces as "not a number", never coerced
     * into a different value. The UI documents "use . for decimals".
     */
    private fun normalizeDecimal(input: String): String? {
        var body = input
        var sign = ""
        if (body.startsWith("+") || body.startsWith("-")) {
            sign = body.take(1)
            body = body.drop(1)
        }
        if (body.isEmpty()) return null
        // Only digits and the two separators may remain.
        if (body.any { it != '.' && it != ',' && !it.isDigit() }) return null
        if (body.count { it == '.' } > 1) return null

        val dot = body.indexOf('.')
        val intPart = if (dot >= 0) body.substring(0, dot) else body
        val fracPart = if (dot >= 0) body.substring(dot + 1) else ""
        // A comma is grouping only: never in the fraction.
        if (fracPart.any { !it.isDigit() }) return null

        val intDigits: String = if (intPart.contains(',')) {
            val groups = intPart.split(',')
            if (groups.size < 2) return null
            if (groups.first().isEmpty() || groups.first().length > 3) return null
            if (groups.drop(1).any { it.length != 3 }) return null
            if (groups.any { g -> g.any { !it.isDigit() } }) return null
            groups.joinToString("")
        } else {
            if (intPart.any { !it.isDigit() }) return null
            intPart
        }

        if (intDigits.isEmpty() && fracPart.isEmpty()) return null // ".", "", "+"
        return buildString {
            append(sign)
            append(if (intDigits.isEmpty()) "0" else intDigits)
            if (dot >= 0) {
                append('.')
                append(fracPart)
            }
        }
    }

    /** A name trimmed and checked for the length/blank rules. */
    sealed interface NameResult {
        data class Value(val name: String) : NameResult
        data class Invalid(val reason: String) : NameResult
    }

    fun parseName(raw: String): NameResult {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return NameResult.Invalid("name is required")
        if (trimmed.length > MAX_NAME_LENGTH) {
            return NameResult.Invalid("name is longer than $MAX_NAME_LENGTH characters")
        }
        return NameResult.Value(trimmed)
    }

    /** The raw text of the manual-entry / correction form. */
    data class Form(
        val name: String,
        val calories: String = "",
        val mass: String = "",
        val carbs: String = "",
        val protein: String = "",
        val fat: String = "",
        /** Occurrence time; defaults to now at validation. */
        val occurredAt: Instant? = null,
        val occurrenceOffset: ZoneOffset = ZoneOffset.UTC,
    )

    /** The validated name, nutrition, and occurrence time ready to persist. */
    data class Valid(
        val name: String,
        val nutrition: Nutrition,
        val occurredAt: Instant,
        val occurrenceOffset: ZoneOffset,
    )

    sealed interface Result {
        data class Ok(val valid: Valid) : Result
        data class Rejected(val errors: List<Error>) : Result
    }

    /**
     * Validates a whole [Form]. Collects every error rather than stopping at the
     * first, so the UI can mark all bad fields at once. [clock] supplies "now"
     * for the future-time check and the default occurrence time.
     */
    fun validate(form: Form, clock: Clock): Result {
        val errors = mutableListOf<Error>()

        val name = when (val n = parseName(form.name)) {
            is NameResult.Value -> n.name
            is NameResult.Invalid -> {
                errors += Error(null, n.reason); null
            }
        }

        val numbers = mutableMapOf<Field, Double?>()
        for (field in Field.entries) {
            val raw = when (field) {
                Field.CALORIES -> form.calories
                Field.MASS -> form.mass
                Field.CARBS -> form.carbs
                Field.PROTEIN -> form.protein
                Field.FAT -> form.fat
            }
            when (val r = parseNumber(raw, field)) {
                is NumberResult.Value -> numbers[field] = r.number
                NumberResult.Unknown -> numbers[field] = null
                is NumberResult.Invalid -> errors += Error(field, r.reason)
            }
        }

        val nutrition = Nutrition(
            caloriesKcal = numbers[Field.CALORIES],
            massGrams = numbers[Field.MASS],
            carbsGrams = numbers[Field.CARBS],
            proteinGrams = numbers[Field.PROTEIN],
            fatGrams = numbers[Field.FAT],
        )
        // Only judge "nothing recorded" when the present fields all parsed; a form
        // whose only number is malformed already has an error to show.
        if (errors.none { it.field != null } && nutrition.isEmpty) {
            errors += Error(null, "enter at least one nutrition value")
        }

        val now = clock.instant()
        val occurredAt = form.occurredAt ?: now
        if (occurredAt.isAfter(now.plus(FUTURE_TOLERANCE))) {
            errors += Error(null, "time cannot be in the future")
        }

        if (errors.isNotEmpty() || name == null) {
            return Result.Rejected(errors)
        }
        return Result.Ok(
            Valid(
                name = name,
                nutrition = nutrition,
                occurredAt = occurredAt,
                occurrenceOffset = form.occurrenceOffset,
            )
        )
    }
}
