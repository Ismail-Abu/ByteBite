package com.example.guione.meal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.guione.meal.AppGraph
import com.example.guione.ui.Format
import com.example.guione.ui.components.EmptyState
import com.example.guione.ui.components.NutrientStat
import com.example.guione.ui.components.SectionHeader
import com.example.guione.ui.theme.Spacing
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onAddMeal: () -> Unit,
    onScan: () -> Unit,
    onOpenMeal: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    val vm = rememberTodayViewModel()
    val state by vm.ui.collectAsStateWithLifecycle()
    // Roll the day over on resume / after midnight without needing a save (#7).
    LifecycleResumeEffect(Unit) {
        vm.onResumed()
        onPauseOrDispose { }
    }
    val zone = AppGraph.clock.zone

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today") },
                actions = {
                    IconButton(onClick = onScan) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = "Scan a meal")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        bottomBar = bottomBar,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddMeal,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add meal") },
            )
        },
    ) { padding ->
        if (!state.loading && state.recent.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.RestaurantMenu,
                title = "No meals yet",
                message = "Log a meal to start building your history. Everything stays on this device.",
                actionLabel = "Add your first meal",
                onAction = onAddMeal,
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg),
        ) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                state.date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.md))
            TodaySummary(state.summary)

            if (state.recent.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.lg))
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().padding(top = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    SectionHeader("Recent meals", Modifier.weight(1f))
                    TextButton(onClick = onOpenHistory) { Text("View all") }
                }
                state.recent.forEach { item ->
                    MealRow(item = item, zone = zone, onClick = { onOpenMeal(item.id) })
                }
            }
            Spacer(Modifier.height(Spacing.xxl))
        }
    }
}

@Composable
private fun TodaySummary(summary: TodayViewModel.Summary) {
    if (summary.mealCount == 0) {
        Text(
            "No meals logged today.",
            style = MaterialTheme.typography.titleMedium,
        )
        return
    }
    Text(
        if (summary.partial) "Known totals" else "Today's totals",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.sm))
    // Responsive: four metrics in one row where they fit, otherwise a 2x2 grid
    // with equal-width columns so values and units stay aligned on narrow screens.
    val stats = listOf(
        Triple("Energy", Format.kcal(summary.kcal), "kcal"),
        Triple("Carbs", Format.grams(summary.carbs), "g"),
        Triple("Protein", Format.grams(summary.protein), "g"),
        Triple("Fat", Format.grams(summary.fat), "g"),
    )
    // Fold to two columns when the metrics can't comfortably fit one row — both
    // on narrow screens and at large font scales (so units never wrap mid-word).
    val fontScale = androidx.compose.ui.platform.LocalConfiguration.current.fontScale
    BoxWithConstraints {
        if (maxWidth >= 340.dp * fontScale) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                stats.forEach { (l, v, u) -> NutrientStat(l, v, u, Modifier.weight(1f)) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                stats.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        pair.forEach { (l, v, u) -> NutrientStat(l, v, u, Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(Spacing.md))
    val mealWord = if (summary.mealCount == 1) "meal" else "meals"
    Text(
        buildString {
            append("${summary.mealCount} $mealWord logged")
            if (summary.partial) append(" · some values unknown")
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
