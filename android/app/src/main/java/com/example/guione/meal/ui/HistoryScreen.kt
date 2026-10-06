package com.example.guione.meal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.guione.meal.AppGraph
import com.example.guione.ui.components.EmptyState
import com.example.guione.ui.theme.Spacing

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        if (!state.loading && state.groups.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Outlined.ListAlt,
                title = "No history yet",
                message = "Meals you log will appear here, grouped by day.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
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
