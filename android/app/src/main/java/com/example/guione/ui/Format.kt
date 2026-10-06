package com.example.guione.ui

import com.example.guione.meal.NutritionSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Display formatting for nutrition and time. Centralized so every screen shows
 * values the same way: energy as whole kcal, mass/macros to one decimal, and an
 * em dash for an explicitly-unknown value (never a fabricated 0).
 */
object Format {

    /** The glyph for an unknown value. An em dash, not an emoji or icon. */
    const val UNKNOWN = "—"

    fun kcal(value: Double?): String =
        if (value == null) UNKNOWN else value.roundToLong().toString()

    /** One decimal place, trailing ".0" trimmed. */
    fun grams(value: Double?): String {
        if (value == null) return UNKNOWN
        val rounded = (value * 10).roundToLong() / 10.0
        return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString()
        else String.format(Locale.US, "%.1f", rounded)
    }

    private val timeFmt = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)
    private val fullDateFmt = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US)

    fun time(instant: Instant, zone: ZoneId): String =
        instant.atZone(zone).toLocalTime().format(timeFmt)

    fun fullDateTime(instant: Instant, zone: ZoneId): String =
        instant.atZone(zone).format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy  h:mm a", Locale.US))

    /** A date header relative to [today]: "Today", "Yesterday", else the date. */
    fun relativeDate(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(if (date.year == today.year) dateFmt else fullDateFmt)
    }

    fun sourceLabel(source: NutritionSource): String = when (source) {
        NutritionSource.MANUAL -> "Manual"
        NutritionSource.INFERRED -> "Estimated"
        NutritionSource.CORRECTED -> "Corrected"
    }
}
