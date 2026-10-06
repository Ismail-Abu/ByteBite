package com.example.guione.meal.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.guione.meal.AppGraph
import com.example.guione.meal.MealWithRevisions
import com.example.guione.meal.Nutrition
import com.example.guione.ui.Format
import com.example.guione.ui.components.EmptyState
import com.example.guione.ui.components.LoadingState
import com.example.guione.ui.theme.Spacing
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealDetailScreen(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
) {
    val vm = rememberMealDetailViewModel()
    val state by vm.ui.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.deletedEvents.collectLatest { onDeleted() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.meal != null) {
                        IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "Edit") }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> LoadingState(Modifier.padding(padding))
            state.notFound -> EmptyState(
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                title = "Meal not found",
                message = "This meal may have been deleted.",
                modifier = Modifier.padding(padding),
            )
            else -> MealDetailBody(state.meal!!, Modifier.padding(padding))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this meal?") },
            text = { Text("This permanently removes the meal and its correction history from this device.") },
            confirmButton = {
                TextButton(
                    enabled = !state.deleting,
                    onClick = { confirmDelete = false; vm.delete() },
                ) { Text("Delete meal", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun MealDetailBody(meal: MealWithRevisions, modifier: Modifier) {
    val zone = AppGraph.clock.zone
    val current = meal.current
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = Spacing.lg)) {
        Spacer(Modifier.height(Spacing.md))
        Text(meal.meal.name, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            Format.fullDateTime(meal.meal.occurredAt, meal.meal.occurrenceOffset),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Source: ${Format.sourceLabel(current.source)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Spacing.lg))
        HorizontalDivider()
        NutritionRows(current.nutrition)

        if (meal.revisions.size > 1) {
            Spacer(Modifier.height(Spacing.md))
            ExpandableSection("Original vs corrected") {
                CompareRow("", "Original", "Current")
                val original = meal.original.nutrition
                CompareRow("Energy", Format.kcal(original.caloriesKcal) + " kcal", Format.kcal(current.nutrition.caloriesKcal) + " kcal")
                CompareRow("Mass", Format.grams(original.massGrams) + " g", Format.grams(current.nutrition.massGrams) + " g")
                CompareRow("Carbs", Format.grams(original.carbsGrams) + " g", Format.grams(current.nutrition.carbsGrams) + " g")
                CompareRow("Protein", Format.grams(original.proteinGrams) + " g", Format.grams(current.nutrition.proteinGrams) + " g")
                CompareRow("Fat", Format.grams(original.fatGrams) + " g", Format.grams(current.nutrition.fatGrams) + " g")
            }
        }

        Spacer(Modifier.height(Spacing.md))
        ExpandableSection("Technical details") {
            DetailLine("Meal ID", meal.meal.id)
            DetailLine("Revisions", meal.revisions.size.toString())
            DetailLine("Current source", Format.sourceLabel(current.source))
            DetailLine("Created", Format.fullDateTime(meal.meal.createdAt, meal.meal.occurrenceOffset))
            DetailLine("Updated", Format.fullDateTime(meal.meal.updatedAt, meal.meal.occurrenceOffset))
        }
        Spacer(Modifier.height(Spacing.xxl))
    }
}

@Composable
private fun NutritionRows(n: Nutrition) {
    Column {
        NutritionRow("Energy", Format.kcal(n.caloriesKcal), "kcal")
        NutritionRow("Mass", Format.grams(n.massGrams), "g")
        NutritionRow("Carbohydrates", Format.grams(n.carbsGrams), "g")
        NutritionRow("Protein", Format.grams(n.proteinGrams), "g")
        NutritionRow("Fat", Format.grams(n.fatGrams), "g")
    }
}

@Composable
private fun NutritionRow(label: String, value: String, unit: String) {
    Row(
        Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text("$value ", style = com.example.guione.ui.theme.StatNumberStyle)
        Text(unit, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExpandableSection(title: String, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(
            Modifier.fillMaxWidth().height(48.dp).clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(bottom = Spacing.sm)) { content() }
        }
    }
}

@Composable
private fun CompareRow(label: String, a: String, b: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.2f),
            fontWeight = if (label.isEmpty()) FontWeight.Bold else FontWeight.Normal)
        Text(a, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(b, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.4f))
    }
}
