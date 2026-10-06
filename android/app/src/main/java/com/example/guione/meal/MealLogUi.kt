package com.example.guione.meal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val Cream = Color(0xFFF6F4EE)
private val Ink = Color(0xFF1F2617)
private val Muted = Color(0xFF7A7F6E)
private val CardBg = Color(0xFFFFFFFF)
private val Accent = Color(0xFFE4674E)
private val ErrorRed = Color(0xFFB3261E)

object MealLogTags {
    const val EMPTY = "mealLog.empty"
    const val SAVE = "mealLog.save"
    const val FORM_ERROR = "mealLog.formError"
    const val TODAY = "mealLog.today"
    fun row(id: String) = "mealLog.row.$id"
    fun delete(id: String) = "mealLog.delete.$id"
    fun edit(id: String) = "mealLog.edit.$id"
}

/**
 * The persisted meal surface, stateless: everything it shows comes from [state]
 * and every action is a callback, so it renders identically from a real view
 * model or from crafted state in a test. It is the production Home + manual
 * entry + history, reading only saved data — no sample meals.
 */
@Composable
fun MealLogScreen(
    state: MealLogViewModel.UiState,
    onName: (String) -> Unit,
    onField: (MealInput.Field, String) -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenPrototype: (() -> Unit)? = null,
) {
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    Column(
        modifier
            .fillMaxSize()
            .background(Cream)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("My meals", color = Ink, fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                Text("Saved on this device", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            if (onOpenPrototype != null) {
                TextButton(onClick = onOpenPrototype) { Text("Design prototype ›", color = Muted) }
            }
        }

        Spacer(Modifier.height(16.dp))
        TodayCard(state)

        Spacer(Modifier.height(16.dp))
        EntryForm(state.form, onName, onField, onSave, onCancelEdit)

        Spacer(Modifier.height(20.dp))
        Text(
            if (state.form.isEditing) "History" else "Recent meals",
            color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(8.dp))

        if (!state.loading && state.meals.isEmpty()) {
            Text(
                "No saved meals yet. Log your first meal above.",
                modifier = Modifier.testTag(MealLogTags.EMPTY).padding(vertical = 12.dp),
                color = Muted, fontSize = 14.sp,
            )
        }
        state.meals.forEach { row ->
            MealRow(row, onEdit = onEdit, onDelete = { pendingDelete = row.id })
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(24.dp))
    }

    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this meal?") },
            text = { Text("This permanently removes the meal and its history from this device.") },
            confirmButton = {
                TextButton(onClick = { onDelete(id); pendingDelete = null }) {
                    Text("Delete meal", color = ErrorRed)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun TodayCard(state: MealLogViewModel.UiState) {
    Card(Modifier.testTag(MealLogTags.TODAY)) {
        Text("Today", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        val kcal = state.todayKcal
        val carbs = state.todayCarbs
        if (kcal == null && carbs == null) {
            Text("No meals logged today", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        } else {
            Row {
                Stat(label = "calories", value = kcal?.let { "${it.roundToInt()} kcal" } ?: "—")
                Spacer(Modifier.width(24.dp))
                Stat(label = "carbs", value = carbs?.let { "${gram(it)} g" } ?: "—")
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(value, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EntryForm(
    form: MealLogViewModel.Form,
    onName: (String) -> Unit,
    onField: (MealInput.Field, String) -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit,
) {
    Card {
        Text(
            if (form.isEditing) "Correct meal" else "Log a meal",
            color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Calories in kcal, everything else in grams. Leave a field blank if unknown.",
            color = Muted, fontSize = 12.sp,
        )
        Spacer(Modifier.height(12.dp))

        Field("Meal name", form.name, onName, error = form.errors[null]?.takeIf { it.contains("name") })
        NutrientField("Calories (kcal)", form.calories, MealInput.Field.CALORIES, form, onField)
        NutrientField("Mass (g)", form.mass, MealInput.Field.MASS, form, onField)
        NutrientField("Carbs (g)", form.carbs, MealInput.Field.CARBS, form, onField)
        NutrientField("Protein (g)", form.protein, MealInput.Field.PROTEIN, form, onField)
        NutrientField("Fat (g)", form.fat, MealInput.Field.FAT, form, onField)

        form.errors[null]?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, modifier = Modifier.testTag(MealLogTags.FORM_ERROR), color = ErrorRed, fontSize = 12.sp)
        }
        if (form.justSaved) {
            Spacer(Modifier.height(4.dp))
            Text("Saved ✓", color = Color(0xFF2E7D32), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onSave,
                modifier = Modifier.testTag(MealLogTags.SAVE),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
            ) { Text(if (form.isEditing) "Save correction" else "Save meal") }
            if (form.isEditing) {
                Spacer(Modifier.width(12.dp))
                TextButton(onClick = onCancelEdit) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun NutrientField(
    label: String,
    value: String,
    field: MealInput.Field,
    form: MealLogViewModel.Form,
    onField: (MealInput.Field, String) -> Unit,
) {
    Field(
        label = label,
        value = value,
        onChange = { onField(field, it) },
        error = form.errors[field],
        numeric = true,
    )
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    error: String?,
    numeric: Boolean = false,
) {
    Column(Modifier.padding(top = 8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            singleLine = true,
            isError = error != null,
            keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
            modifier = Modifier.fillMaxWidth(),
        )
        if (error != null) {
            Text(error, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }
    }
}

@Composable
private fun MealRow(
    row: MealLogViewModel.Row,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    Card(Modifier.testTag(MealLogTags.row(row.id))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(row.name, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (row.source == NutritionSource.CORRECTED) {
                        Spacer(Modifier.width(8.dp))
                        Text("edited", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    buildString {
                        append(row.timeLabel)
                        append("  ·  ")
                        append(row.calories?.let { "${it.roundToInt()} kcal" } ?: "— kcal")
                        append("  ·  ")
                        append(row.carbs?.let { "${gram(it)} g carbs" } ?: "— carbs")
                    },
                    color = Muted, fontSize = 12.sp,
                )
            }
            TextButton(onClick = { onEdit(row.id) }, modifier = Modifier.testTag(MealLogTags.edit(row.id))) {
                Text("Edit")
            }
            TextButton(onClick = { onDelete(row.id) }, modifier = Modifier.testTag(MealLogTags.delete(row.id))) {
                Text("Delete", color = ErrorRed)
            }
        }
    }
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(16.dp),
    ) { Column { content() } }
}

/** One decimal place for a gram value, trimming a trailing ".0". */
private fun gram(v: Double): String {
    val rounded = (v * 10).roundToInt() / 10.0
    return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString()
}
