package com.example.guione

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

/**
 * Simple month grid. The current (real) week is softly highlighted,
 * the selected day is a filled circle, today's number is tinted.
 * Chevrons browse months; tapping a day calls [onSelect].
 */
@Composable
fun MonthCalendar(
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    accent: Color,
    weekHighlight: Color,
    textColor: Color,
    mutedColor: Color
) {
    var month by remember(selected) { mutableStateOf(YearMonth.from(selected)) }

    val weekStart = SampleData.weekStart
    val weekEnd = weekStart.plusDays(6)

    Column(Modifier.fillMaxWidth()) {

        // Month title + chevrons
        Row(
            Modifier.fillMaxWidth().padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = month.month.getDisplayName(JTextStyle.FULL, Locale.ENGLISH) + " " + month.year,
                modifier = Modifier.weight(1f),
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            ChevronChip("\u2039", mutedColor) { month = month.minusMonths(1) }
            ChevronChip("\u203A", mutedColor, Modifier.padding(start = 8.dp)) { month = month.plusMonths(1) }
        }

        // Day-of-week header, Monday first
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                Text(
                    text = d,
                    modifier = Modifier.weight(1f),
                    color = mutedColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Day cells
        val first = month.atDay(1)
        val lead = first.dayOfWeek.value - 1 // Monday = 1
        val cells: List<LocalDate?> =
            List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }

        cells.chunked(7).forEach { weekRow ->
            val padded = weekRow + List(7 - weekRow.size) { null }
            Row(Modifier.fillMaxWidth()) {
                padded.forEach { day ->
                    Box(
                        Modifier.weight(1f).height(42.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != null) {
                            val inWeek = !day.isBefore(weekStart) && !day.isAfter(weekEnd)
                            val isSel = day == selected
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSel -> accent
                                            inWeek -> weekHighlight
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable { onSelect(day) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.dayOfMonth.toString(),
                                    color = when {
                                        isSel -> Color.White
                                        day == SampleData.today -> accent
                                        else -> textColor
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChevronChip(
    glyph: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(glyph, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

/** Friendly empty state used by both calendars' day views. */
@Composable
fun EmptyDayNote(text: String, color: Color) {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )
    }
}

/** Arrangement helper referenced by both experiences for evenly-gapped rows. */
val Gap12 = Arrangement.spacedBy(12.dp)
val Gap8 = Arrangement.spacedBy(8.dp)
val Gap10 = Arrangement.spacedBy(10.dp)
