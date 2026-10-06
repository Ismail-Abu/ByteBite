package com.example.guione.meal.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.guione.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onOpenSettings: () -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Insights") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        // Honest unavailable state: no glucose model or readings are integrated,
        // so there is nothing to chart. No placeholder curves or invented axes.
        EmptyState(
            icon = Icons.Outlined.ShowChart,
            title = "No insights available yet",
            message = "Glucose insights will appear here once a glucose model and your " +
                "readings are available. Nothing is simulated — until then there is " +
                "nothing to show. See Settings for what is and isn't installed.",
            actionLabel = "Open Settings",
            onAction = onOpenSettings,
            modifier = Modifier.padding(padding),
        )
    }
}
