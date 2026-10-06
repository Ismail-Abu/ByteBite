package com.example.guione.meal.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.guione.meal.AppGraph
import com.example.guione.meal.ThemeMode
import com.example.guione.ui.theme.Spacing
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember(context) { AppGraph.settings(context) }
    val vm = rememberSettingsViewModel()
    val storage by vm.storage.collectAsStateWithLifecycle()
    val deleting by vm.deleting.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg),
        ) {
            SettingsSection("Appearance") {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .selectable(
                                selected = settings.themeMode == mode,
                                role = Role.RadioButton,
                                onClick = { settings.setTheme(mode) },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = settings.themeMode == mode, onClick = null)
                        Spacer(Modifier.width(Spacing.md))
                        Text(
                            mode.name.lowercase(Locale.US).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            SettingsSection("Storage & data") {
                val usage = storage
                Text(
                    if (usage == null) "Measuring…"
                    else "${formatBytes(usage.dbBytes)} · ${usage.mealCount} ${if (usage.mealCount == 1) "meal" else "meals"}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "Meal history is stored only on this device. There is no account, cloud sync, or network access, so uninstalling the app or losing the device loses the history.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
                Text(
                    "No meal photos are stored yet; image capture and thumbnail retention arrive with on-device estimation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
                Spacer(Modifier.height(Spacing.md))
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    enabled = !deleting && (storage?.mealCount ?: 0) > 0,
                ) { Text("Delete all data", color = MaterialTheme.colorScheme.error) }
            }

            SettingsSection("Model status") {
                CapabilityRow("Nutrition estimation", "Not installed", "Photo-based estimates are off until the trained model is added to the app.")
                CapabilityRow("Food check", "Unavailable", "A validated food/non-food gate is required before photo estimation is enabled.")
                CapabilityRow("Glucose forecast", "Unavailable", "Awaiting the glucose model and its input contract. No values are simulated.")
            }

            SettingsSection("About") {
                Text("ByteBite", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "An on-device meal log. Works fully offline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(Spacing.xxl))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete all meals?") },
            text = { Text("This permanently removes every meal and its history from this device. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.deleteAll() }) {
                    Text("Delete everything", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(Spacing.lg))
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(Spacing.xs))
    content()
    Spacer(Modifier.height(Spacing.sm))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun CapabilityRow(name: String, status: String, explanation: String) {
    Column(Modifier.padding(vertical = Spacing.xs)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(status, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(explanation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}
