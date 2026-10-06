package com.example.guione.meal.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.guione.meal.AppGraph
import com.example.guione.ui.Format
import com.example.guione.ui.components.EmptyState
import com.example.guione.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onOpenMeal: (String) -> Unit,
    onOpenSettings: () -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    val vm = rememberHistoryViewModel()
    val state by vm.ui.collectAsStateWithLifecycle()
    val zone = AppGraph.clock.zone
    val listState = rememberLazyListState()
    var showPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                actions = {
                    IconButton(onClick = { showPicker = true }, enabled = state.datesWithMeals.isNotEmpty()) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "Filter by date")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        if (!state.loading && state.datesWithMeals.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Outlined.ListAlt,
                title = "No history yet",
                message = "Meals you log will appear here, grouped by day.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        androidx.compose.foundation.layout.Column(Modifier.padding(padding).fillMaxSize()) {
            state.filterDate?.let { date ->
                Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
                    FilterChip(
                        selected = true,
                        onClick = { vm.setFilter(null) },
                        label = { Text(Format.relativeDate(date, LocalDate.now(AppGraph.clock))) },
                        trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = "Clear filter") },
                    )
                }
            }

            if (state.filteredEmpty) {
                EmptyState(
                    icon = Icons.Outlined.CalendarMonth,
                    title = "No meals on this day",
                    message = "Pick another day or clear the filter.",
                    actionLabel = "Clear filter",
                    onAction = { vm.setFilter(null) },
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = Spacing.lg, end = Spacing.lg, top = Spacing.sm, bottom = Spacing.xxl,
                    ),
                ) {
                    state.groups.forEach { group ->
                        item(key = "header-${group.date}") {
                            Text(
                                group.header,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth().padding(top = Spacing.md, bottom = Spacing.xs),
                            )
                        }
                        items(group.items, key = { it.id }) { item ->
                            MealRow(item = item, zone = zone, onClick = { onOpenMeal(item.id) })
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (state.filterDate ?: LocalDate.now(AppGraph.clock))
                .atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        vm.setFilter(Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate())
                    }
                    showPicker = false
                }) { Text("Show day") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = pickerState) }
    }
}
