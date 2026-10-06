package com.example.guione.meal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.example.guione.meal.MealListItem
import com.example.guione.meal.NutritionSource
import com.example.guione.ui.Format
import java.time.ZoneId

/**
 * A text-led history row: name, time, and a compact nutrition summary with
 * unknown values shown as an em dash. No image or placeholder is required.
 * The whole row is one button-labelled node for TalkBack.
 */
@Composable
fun MealRow(
    item: MealListItem,
    zone: ZoneId,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val time = Format.time(item.occurredAt, zone)
    val summary = "${Format.kcal(item.nutrition.caloriesKcal)} kcal  ·  " +
        "${Format.grams(item.nutrition.carbsGrams)} g carbs"
    val edited = item.source == NutritionSource.CORRECTED
    val spoken = buildString {
        append(item.name); append(", "); append(time); append(", ")
        append(spokenNutrition(item)); if (edited) append(", corrected")
    }
    ListItem(
        headlineContent = { Text(item.name, style = MaterialTheme.typography.titleMedium) },
        supportingContent = {
            Row {
                Text("$time   ·   $summary", style = MaterialTheme.typography.bodyMedium)
            }
        },
        trailingContent = {
            if (edited) {
                Text(
                    "Corrected",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .clickable(onClickLabel = "Open ${item.name}", role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = spoken },
    )
}

private fun spokenNutrition(item: MealListItem): String {
    val kcal = item.nutrition.caloriesKcal
    val carbs = item.nutrition.carbsGrams
    val kcalText = if (kcal == null) "calories unknown" else "${Format.kcal(kcal)} kilocalories"
    val carbText = if (carbs == null) "carbs unknown" else "${Format.grams(carbs)} grams carbs"
    return "$kcalText, $carbText"
}
