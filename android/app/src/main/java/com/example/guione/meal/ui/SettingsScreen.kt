package com.example.guione.meal.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember(context) { AppGraph.settings(context) }
    val vm = rememberSettingsViewModel()
    val storage by vm.storage.collectAsStateWithLifecycle()
    val deleting by vm.deleting.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.messages.collectLatest { snackbar.showSnackbar(it) } }

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
        snackbarHost = { SnackbarHost(snackbar) },
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

            SettingsSection("Storage") {
                val usage = storage
                val sizeText = when {
                    usage == null -> "Measuring…"
                    usage.dbBytes < 0 -> "Unavailable"
                    else -> formatBytes(usage.dbBytes)
                }
                val countText = usage?.takeIf { it.mealCount >= 0 }?.let {
                    " · ${it.mealCount} ${if (it.mealCount == 1) "meal" else "meals"}"
                }.orEmpty()
                Text("Database size: $sizeText$countText", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "This is the meal database only, not the app's total installed size.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Your meals stay on this device — no account, cloud sync, or network. Uninstalling or losing the device loses the history.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }

            SettingsSection("Capabilities") {
                CapabilityRow("Photo nutrition estimates", "Not available")
                CapabilityRow("Food photo check", "Not available")
                CapabilityRow("Glucose forecast", "Not available")
                ExpandableRow("Technical details") {
                    TechLine("Nutrition model: the trained weights and preprocessing contract are not bundled in this build; manual entry is used until they are.")
                    TechLine("Food gate: a validated on-device food/non-food classifier and its held-out evaluation are required before photo estimation is enabled.")
                    TechLine("Glucose: awaiting the external glucose model and its input/units/horizon contract. No values are simulated.")
                }
            }

            // Destructive actions, kept apart from ordinary settings.
            SettingsSection("Danger zone") {
                Text(
                    "Permanently delete every meal and its history from this device. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.md))
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    enabled = !deleting && (storage?.mealCount ?: 0) > 0,
                ) {
                    if (deleting) {
                        CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Delete all data", color = MaterialTheme.colorScheme.error)
                    }
                }
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
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(Spacing.xs))
    content()
    Spacer(Modifier.height(Spacing.sm))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun CapabilityRow(name: String, status: String) {
    Row(
        Modifier.fillMaxWidth().height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(status, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExpandableRow(title: String, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(48.dp).clickable { expanded = !expanded },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        Icon(
            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = if (expanded) "Collapse" else "Expand",
        )
    }
    AnimatedVisibility(visible = expanded) {
        Column { content() }
    }
}

@Composable
private fun TechLine(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = Spacing.xs),
    )
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}
