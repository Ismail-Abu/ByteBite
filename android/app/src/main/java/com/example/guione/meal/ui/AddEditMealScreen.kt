package com.example.guione.meal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.guione.meal.AppGraph
import com.example.guione.meal.MealInput
import com.example.guione.ui.Format
import com.example.guione.ui.components.InlineError
import com.example.guione.ui.theme.Spacing
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.LaunchedEffect
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMealScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val vm = rememberAddEditViewModel()
    val state by vm.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmDiscard by remember { mutableStateOf(false) }

    fun handleBack() {
        if (state.edited) confirmDiscard = true else onBack()
    }
    BackHandler(enabled = state.edited) { confirmDiscard = true }

    LaunchedEffect(Unit) {
        vm.savedEvents.collectLatest { onSaved() }
    }
    LaunchedEffect(state.saveError) {
        state.saveError?.let { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.editing) "Edit meal" else "Add meal") },
                navigationIcon = {
                    IconButton(onClick = { handleBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (state.loadError != null) {
            com.example.guione.ui.components.ErrorRetry(
                message = state.loadError!!,
                onRetry = onBack,
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = Spacing.lg),
        ) {
            Spacer(Modifier.height(Spacing.sm))

            OutlinedTextField(
                value = state.name,
                onValueChange = vm::onName,
                label = { Text("Meal name") },
                singleLine = true,
                isError = state.nameError != null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            state.nameError?.let { InlineError(it) }

            Spacer(Modifier.height(Spacing.md))
            OccurrenceField(
                occurredAt = state.occurredAt,
                offset = state.offset,
                onChange = vm::onOccurredAt,
            )

            Spacer(Modifier.height(Spacing.lg))
            Text(
                "Nutrition",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Calories in kcal, the rest in grams. Leave a field blank if it is unknown; use \".\" for decimals.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.sm))

            NutrientInput("Calories (kcal)", state.calories, MealInput.Field.CALORIES, state.fieldErrors, vm::onField, last = false)
            NutrientInput("Mass (g)", state.mass, MealInput.Field.MASS, state.fieldErrors, vm::onField, last = false)
            NutrientInput("Carbs (g)", state.carbs, MealInput.Field.CARBS, state.fieldErrors, vm::onField, last = false)
            NutrientInput("Protein (g)", state.protein, MealInput.Field.PROTEIN, state.fieldErrors, vm::onField, last = false)
            NutrientInput("Fat (g)", state.fat, MealInput.Field.FAT, state.fieldErrors, vm::onField, last = true)

            state.formError?.let { InlineError(it) }

            Spacer(Modifier.height(Spacing.xl))
            Button(
                onClick = vm::save,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
            ) {
                if (state.saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.height(1.dp))
                } else {
                    Text(if (state.editing) "Save correction" else "Save meal")
                }
            }
            Spacer(Modifier.height(Spacing.xl))
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard changes?") },
            text = { Text("Your edits to this meal haven't been saved. Leave without saving?") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onBack() }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } },
        )
    }
}

@Composable
private fun NutrientInput(
    label: String,
    value: String,
    field: MealInput.Field,
    errors: Map<MealInput.Field, String>,
    onField: (MealInput.Field, String) -> Unit,
    last: Boolean,
) {
    Column(Modifier.padding(top = Spacing.sm)) {
        OutlinedTextField(
            value = value,
            onValueChange = { onField(field, it) },
            label = { Text(label) },
            singleLine = true,
            isError = errors[field] != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = if (last) ImeAction.Done else ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        errors[field]?.let { InlineError(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OccurrenceField(
    occurredAt: Instant,
    offset: ZoneOffset,
    onChange: (Instant) -> Unit,
) {
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    val local = occurredAt.atOffset(offset)

    Column {
        Text("When", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Event, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Spacing.sm))
            Text(
                Format.fullDateTime(occurredAt, offset),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = Spacing.sm).weight(1f),
            )
            TextButton(onClick = { showDate = true }) { Text("Date") }
            TextButton(onClick = { showTime = true }) { Text("Time") }
        }
    }

    if (showDate) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = local.toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        val newDate = Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate()
                        onChange(combine(newDate, local.toLocalTime(), offset))
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } },
        ) { DatePicker(state = dateState) }
    }

    if (showTime) {
        val timeState = rememberTimePickerState(
            initialHour = local.hour, initialMinute = local.minute, is24Hour = false,
        )
        androidx.compose.ui.window.Dialog(onDismissRequest = { showTime = false }) {
            androidx.compose.material3.Surface(
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp,
            ) {
                Column(Modifier.padding(Spacing.xl), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Select time", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Spacing.lg))
                    TimePicker(state = timeState)
                    Spacer(Modifier.height(Spacing.md))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTime = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            onChange(combine(local.toLocalDate(), LocalTime.of(timeState.hour, timeState.minute), offset))
                            showTime = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }
}

/** Rebuild an Instant from an edited local date/time at the meal's own offset. */
private fun combine(date: LocalDate, time: LocalTime, offset: ZoneOffset): Instant =
    date.atTime(time).toInstant(offset)
