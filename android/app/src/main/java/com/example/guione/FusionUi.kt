package com.example.guione

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

// ---------- Diabetes-awareness theme ----------
// Primary is the World Diabetes Day blue circle (Pantone 285C, #0072CE).
private val FBlue = Color(0xFF0072CE)
private val FBlueDeep = Color(0xFF00539B)
private val FBg = Color(0xFFF2F6FA)
private val FInk = Color(0xFF1E2A44)
private val FGreen = Color(0xFF4E9B6F)
private val FAmber = Color(0xFFC77B21)
private val FAmberDot = Color(0xFFE8A44C)
private val FMute = Color(0xFF8FA1B3)
private val FSlate = Color(0xFF54677B)
private val FBand = Color(0xFFE0F1E7)
private val FOkBg = Color(0xFFDEF3E7)
private val FOkTx = Color(0xFF2E9E6B)
private val FHiBg = Color(0xFFFDE6DE)
private val FHiTx = Color(0xFFC4502E)
private val FTileNeutral = Color(0xFFEAF1F8)
private val FSoftCard = Color(0xFFE3EEF8)
private val FCamDark = Color(0xFF10141C)
private val FRust = Color(0xFF9C5B4B)
private val FLine = Color(0xFFE4EBF2)
private val FChipEdge = Color(0xFFD5E2EE)
private val FWarnBg = Color(0xFFFFF7EC)
private val FWarnEdge = Color(0xFFF0D9B4)
private val FWarnTitle = Color(0xFF8A5B14)
private val FWarnBody = Color(0xFF6E5A34)

private val FSerif = FontFamily.Serif

@Composable
fun FusionExperience() {
    // Tab order: 0 Scan (start) / 1 Glucose / 2 Log / 3 Manual
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var scanned by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(SampleData.today) }
    var showCal by remember { mutableStateOf(false) }

    // Manual-entry form state (hoisted so "Edit details" on a scan can prefill it)
    var fName by rememberSaveable { mutableStateOf("") }
    var fCarbs by rememberSaveable { mutableStateOf("") }
    var fCal by rememberSaveable { mutableStateOf("") }
    var fProtein by rememberSaveable { mutableStateOf("") }
    var fFat by rememberSaveable { mutableStateOf("") }
    var fTime by rememberSaveable { mutableStateOf("") }
    var addError by remember { mutableStateOf("") }

    val context = LocalContext.current
    // Load the interpreter while the user is framing the shot, so the shutter
    // tap is not charged for it.
    LaunchedEffect(Unit) { ScanStore.warmUp(context) }
    val capture = rememberDishCapture { bitmap ->
        ScanStore.scan(context, bitmap)
        showSettings = false
        scanned = true                 // the result screen renders the running state
    }

    fun logScannedDish() {
        if (ScanStore.busy) return      // would log the previous plate
        val dish = DishView.current()
        if (dish.live) {
            // One entry per scan. The spike label stays neutral: nothing here
            // measures glucose, so "gentle rise" would be invented.
            if (ScanStore.claimForLog(dish, "fusion")) {
                SampleData.fusionMeals.add(
                    0,
                    GlucoseMeal(
                        dish.name, dish.carbG, ScanStore.nowLabel(), "logged from scan",
                        "\uD83C\uDF7D\uFE0F", FBand, "logged", false, SampleData.today
                    )
                )
            }
            return
        }
        val already = SampleData.fusionMeals.any {
            it.date == SampleData.today && it.name == SampleData.DISH_NAME
        }
        if (!already) {
            SampleData.fusionMeals.add(
                0,
                GlucoseMeal(
                    SampleData.DISH_NAME, SampleData.DISH_CARBS, "12:42", "logged from scan",
                    "\uD83C\uDF7D\uFE0F", FBand, "gentle rise", false, SampleData.today
                )
            )
        }
    }

    fun tryAddMeal() {
        val c = fCarbs.trim().toDoubleOrNull()
        if (fName.isBlank() || c == null) {
            addError = "Give it a name and a carb number."
        } else {
            SampleData.fusionMeals.add(
                0,
                GlucoseMeal(
                    fName.trim(), c, fTime.trim().ifBlank { "now" }, "added by hand",
                    "\uD83C\uDF7D\uFE0F", FTileNeutral, "logged", false, SampleData.today
                )
            )
            addError = ""
            fName = ""; fCarbs = ""; fCal = ""; fProtein = ""; fFat = ""; fTime = ""
            selectedDate = SampleData.today
            tab = 2
        }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = FBlue,
            background = FBg,
            surface = Color.White,
            onBackground = FInk,
            onSurface = FInk
        )
    ) {
        Box(Modifier.fillMaxSize().background(FBg)) {
            when (tab) {
                0 -> when {
                    showSettings -> FSettingsScreen(onDone = { showSettings = false })
                    !scanned -> FCameraScreen(
                        onShutter = { capture.fromCamera() },
                        onPick = { capture.fromGallery() },
                        onManual = { tab = 3 },
                        onSettings = { showSettings = true }
                    )
                    else -> FScanResultScreen(
                        onLog = {
                            logScannedDish()
                            selectedDate = SampleData.today
                            tab = 2
                            scanned = false
                        },
                        onEdit = {
                            // Prefill from whatever is on screen, so correcting a
                            // real estimate starts from the model's numbers.
                            val dish = DishView.current()
                            fName = dish.name
                            fCarbs = dish.carbG.toString()
                            fCal = dish.kcal.toString()
                            fProtein = dish.proteinG.toString()
                            fFat = dish.fatG.toString()
                            fTime = if (dish.live) ScanStore.nowLabel() else "12:42"
                            addError = ""
                            scanned = false
                            tab = 3
                        }
                    )
                }
                1 -> FGlucoseScreen()
                2 -> FLogScreen(
                    selected = selectedDate,
                    onSelect = { selectedDate = it },
                    showCal = showCal,
                    onToggleCal = { showCal = it }
                )
                else -> FManualScreen(
                    name = fName, onName = { fName = it },
                    carbs = fCarbs, onCarbs = { fCarbs = it },
                    cal = fCal, onCal = { fCal = it },
                    protein = fProtein, onProtein = { fProtein = it },
                    fat = fFat, onFat = { fFat = it },
                    time = fTime, onTime = { fTime = it },
                    error = addError,
                    onAdd = { tryAddMeal() }
                )
            }
            FusionNav(
                tab = tab,
                onTab = {
                    if (it == 0 && tab != 0) {
                        scanned = false // re-entering Scan starts at the camera
                        showSettings = false
                    }
                    tab = it
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// ---------------- TAB 0 : SCAN (camera -> result, with profile settings) ----------------

@Composable
private fun FCameraScreen(
    onShutter: () -> Unit,
    onPick: () -> Unit,
    onManual: () -> Unit,
    onSettings: () -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 4.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(end = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f)) {
                FHead(eyebrow = "SCAN A MEAL", title = "Point straight down")
            }
            Box(
                Modifier
                    .size(42.dp)
                    .shadow(4.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onSettings),
                contentAlignment = Alignment.Center
            ) {
                Text("\u2699\uFE0F", fontSize = 18.sp)
            }
        }

        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            shape = RoundedCornerShape(26.dp),
            color = FCamDark,
            shadowElevation = 8.dp
        ) {
            Box {
                Canvas(Modifier.fillMaxSize()) {
                    val m = 18.dp.toPx()
                    val len = 26.dp.toPx()
                    val sw = 3.dp.toPx()
                    val w = size.width
                    val h = size.height
                    listOf(
                        listOf(Offset(m, m + len), Offset(m, m), Offset(m + len, m)),
                        listOf(Offset(w - m - len, m), Offset(w - m, m), Offset(w - m, m + len)),
                        listOf(Offset(m, h - m - len), Offset(m, h - m), Offset(m + len, h - m)),
                        listOf(Offset(w - m - len, h - m), Offset(w - m, h - m), Offset(w - m, h - m - len))
                    ).forEach { pts ->
                        drawLine(FBlue, pts[0], pts[1], strokeWidth = sw, cap = StrokeCap.Round)
                        drawLine(FBlue, pts[1], pts[2], strokeWidth = sw, cap = StrokeCap.Round)
                    }
                    drawCircle(
                        color = Color.White.copy(alpha = 0.30f),
                        radius = size.minDimension * 0.28f,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f), 0f)
                        )
                    )
                }
                Text(
                    "Hold the phone flat over the plate",
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp),
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            Modifier.fillMaxWidth().padding(bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ShutterButton(ringColor = FBlue, gapColor = FBg, onClick = onShutter)
            Text(
                if (ScanStore.modelAvailable) "Tap the shutter \u2014 runs on this phone, offline"
                else "Tap the shutter \u2014 no model installed, showing a sample dish",
                modifier = Modifier.padding(top = 8.dp),
                color = FMute,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "or choose a photo",
                modifier = Modifier.padding(top = 6.dp).clickable(onClick = onPick),
                color = FBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Not picking it up? Enter it manually",
                modifier = Modifier.padding(top = 6.dp).clickable(onClick = onManual),
                color = FBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun FSettingsScreen(onDone: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .imePadding()
            .padding(top = 4.dp)
    ) {
        FHead(eyebrow = "YOUR PROFILE", title = "Tune the estimates")

        Surface(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp),
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Gap12) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Gap12) {
                    OutlinedTextField(
                        value = SampleData.profileWeight,
                        onValueChange = { SampleData.profileWeight = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Weight (lb)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = SampleData.profileHeight,
                        onValueChange = { SampleData.profileHeight = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Height") },
                        singleLine = true
                    )
                }
                Text(
                    "Activity level",
                    color = FSlate,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Gap10) {
                    ActivityChip("Sedentary", Modifier.weight(1f))
                    ActivityChip("Light", Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Gap10) {
                    ActivityChip("Active", Modifier.weight(1f))
                    ActivityChip("Very active", Modifier.weight(1f))
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp)
                .height(54.dp)
                .shadow(10.dp, RoundedCornerShape(18.dp), clip = false)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(FBlue, FBlueDeep)))
                .clickable(onClick = onDone),
            contentAlignment = Alignment.Center
        ) {
            Text("Save profile", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Text(
            "Used to tune portion and carb estimates. Stays on this phone.",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 10.dp),
            color = FMute,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun ActivityChip(label: String, modifier: Modifier = Modifier) {
    val selected = SampleData.profileActivity == label
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) FBlue else Color.White)
            .border(1.5.dp, if (selected) FBlue else FChipEdge, RoundedCornerShape(12.dp))
            .clickable { SampleData.profileActivity = if (selected) "" else label },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) Color.White else FSlate,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FScanResultScreen(onLog: () -> Unit, onEdit: () -> Unit) {
    val dish = DishView.current()
    val running = ScanStore.state is ScanStore.State.Running
    val failed = ScanStore.state as? ScanStore.State.Failed

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(top = 4.dp)
    ) {
        FHead(
            eyebrow = when {
                running -> "RUNNING ON THIS PHONE\u2026"
                failed != null -> "SCAN FAILED \u00B7 ${failed.message.uppercase(Locale.ENGLISH)}"
                dish.live -> "SCANNED JUST NOW \u00B7 ${dish.detail.uppercase(Locale.ENGLISH)}"
                else -> "SAMPLE DISH \u00B7 NO MODEL INSTALLED"
            },
            title = "Here's the read"
        )

        // Carb hero
        Surface(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp),
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "${dish.name.uppercase(Locale.ENGLISH)} \u00B7 EST. ${dish.massG} G",
                    color = FSlate, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.4.sp
                )
                Text(
                    dish.carbG.toString(),
                    modifier = Modifier.padding(top = 4.dp),
                    color = FAmber, fontSize = 64.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FSerif, lineHeight = 66.sp
                )
                Text(
                    "GRAMS OF CARBS",
                    color = FMute, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp
                )
                // The per-ingredient split and "tuned to you" belong to the mockup.
                // The model has one head over the whole plate and never sees the
                // profile, so for a live estimate both would be false; show the
                // model's measured error instead.
                Text(
                    if (dish.live) dish.errorNote ?: "One photo, one estimate"
                    else "\u2248 38 g from rice \u00B7 \u2248 8 g veggies & sauce",
                    modifier = Modifier.padding(top = 10.dp),
                    color = FSlate, fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
                val bits = if (dish.live) emptyList() else listOf(
                    SampleData.profileWeight.trim().takeIf { it.isNotEmpty() }?.let { "$it lb" },
                    SampleData.profileActivity.trim().takeIf { it.isNotEmpty() }
                ).filterNotNull()
                if (dish.live) {
                    // no profile line: the estimate does not depend on it
                } else if (bits.isNotEmpty()) {
                    Text(
                        "Tuned to you \u00B7 " + bits.joinToString(" \u00B7 "),
                        modifier = Modifier.padding(top = 6.dp),
                        color = FGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold
                    )
                } else {
                    Text(
                        "Tip: set your profile with \u2699 on Scan for tuned estimates",
                        modifier = Modifier.padding(top = 6.dp),
                        color = FMute, fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Hidden-sugar warning
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp)
                .border(1.5.dp, FWarnEdge, RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            color = FWarnBg
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    if (dish.live) "\u26A0 Estimate, not a measurement"
                    else "\u26A0 Heads up: sauce may hide sugar",
                    color = FWarnTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    // v4 has no cooking-cue head, so the mockup's glaze warning cannot
                    // come from a real scan.
                    if (dish.live)
                        "Read from one overhead photo with no depth. Sauces, oils and " +
                            "anything under the top layer are inferred, not seen. Use " +
                            "Edit details to correct it before logging."
                    else
                        "The model's cooking-cue read suggests a glazed sauce. Glazes can add fast-acting carbs \u2014 if you're unsure, log it and watch your 2-hour trend.",
                    modifier = Modifier.padding(top = 5.dp),
                    color = FWarnBody, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 19.sp
                )
            }
        }

        // Full macro read, 2 x 2
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Gap10
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Gap10) {
                FChip("CALORIES", dish.kcal.toString(), FBlue, Modifier.weight(1f))
                FChip("PROTEIN", "${dish.proteinG} g", FGreen, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Gap10) {
                FChip("FAT", "${dish.fatG} g", FRust, Modifier.weight(1f))
                FChip("MASS", "${dish.massG} g", FMute, Modifier.weight(1f))
            }
        }

        // Buttons
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp),
            horizontalArrangement = Gap12
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(2.dp, FBlue, RoundedCornerShape(18.dp))
                    .clickable(onClick = onEdit),
                contentAlignment = Alignment.Center
            ) {
                Text("Edit details", color = FBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(FInk)
                    .clickable(onClick = onLog),
                contentAlignment = Alignment.Center
            ) {
                Text("Log meal", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun FChip(label: String, value: String, dot: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 3.dp) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
                Text(
                    label,
                    modifier = Modifier.padding(start = 7.dp),
                    color = FSlate,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.1.sp
                )
            }
            Text(
                value,
                modifier = Modifier.padding(top = 3.dp),
                color = FInk,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FSerif
            )
        }
    }
}

// ---------------- TAB 1 : GLUCOSE (its own page) ----------------

@Composable
private fun FGlucoseScreen() {
    val dayName = SampleData.today.dayOfWeek
        .getDisplayName(JTextStyle.FULL, Locale.ENGLISH).uppercase(Locale.ENGLISH)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(top = 4.dp)
    ) {
        FHead(eyebrow = "TODAY \u00B7 $dayName", title = "Glucose")

        // Right-now hero
        Surface(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp),
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("RIGHT NOW", color = FSlate, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.9.sp)
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp)) {
                        Text(
                            SampleData.GLUCOSE_NOW.toString(),
                            color = FInk,
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FSerif,
                            lineHeight = 56.sp
                        )
                        Text(
                            " mg/dL",
                            modifier = Modifier.padding(bottom = 9.dp),
                            color = FMute,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text("\u2192 steady", color = FGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 2.dp))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Surface(shape = RoundedCornerShape(50), color = FOkBg) {
                        Text(
                            SampleData.TIME_IN_RANGE,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = FOkTx,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        "updated just now",
                        modifier = Modifier.padding(top = 6.dp),
                        color = FMute, fontSize = 10.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Curve card
        Surface(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp),
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)) {
                Text("Last 12 h", color = FInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                FCurve(Modifier.padding(top = 6.dp))
                Row(
                    Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("6 am", "\u25CF breakfast", "\u25CF lunch", "now").forEach {
                        Text(it, color = FMute, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Day stats
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp),
            horizontalArrangement = Gap10
        ) {
            FMini("AVG", SampleData.GLUCOSE_AVG.toString(), Modifier.weight(1f))
            FMini("PEAK", SampleData.GLUCOSE_PEAK.toString(), Modifier.weight(1f))
            FMini("LOW", SampleData.GLUCOSE_LOW.toString(), Modifier.weight(1f))
        }

        // Carbs today
        Surface(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            shadowElevation = 5.dp
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "CARBS TODAY",
                        modifier = Modifier.weight(1f),
                        color = FSlate, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.9.sp
                    )
                    Text("62%", color = FMute, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp)) {
                    Text(
                        SampleData.carbsToday(SampleData.fusionMeals).toString(),
                        color = FAmber,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FSerif
                    )
                    Text(
                        " / ${SampleData.CARB_BUDGET} g",
                        modifier = Modifier.padding(bottom = 4.dp),
                        color = FMute,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(FTileNeutral)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth((SampleData.carbsToday(SampleData.fusionMeals) / SampleData.CARB_BUDGET.toFloat()).coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(Brush.horizontalGradient(listOf(FAmberDot, FAmber)))
                    )
                }
            }
        }

        Text(
            "Not fully reliable \u2014 always seek medical advice before treatment decisions.",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 10.dp),
            color = FMute,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun FMini(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 4.dp) {
        Column(Modifier.padding(horizontal = 13.dp, vertical = 12.dp)) {
            Text(label, color = FSlate, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp)
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp)) {
                Text(value, color = FInk, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FSerif)
                Text(
                    " mg/dL",
                    modifier = Modifier.padding(bottom = 2.dp),
                    color = FMute, fontSize = 9.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FCurve(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val h = maxWidth * (150f / 330f)
        Box(Modifier.fillMaxWidth().height(h)) {
            Canvas(Modifier.fillMaxSize()) {
                fun px(v: Float) = v / 330f * size.width
                fun py(v: Float) = v / 150f * size.height

                drawRoundRect(
                    color = FBand,
                    topLeft = Offset(0f, py(38f)),
                    size = Size(size.width, py(102f) - py(38f)),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
                val p = Path().apply {
                    moveTo(px(0f), py(88f))
                    cubicTo(px(25f), py(84f), px(40f), py(70f), px(60f), py(66f))
                    cubicTo(px(80f), py(62f), px(92f), py(50f), px(108f), py(54f))
                    cubicTo(px(124f), py(58f), px(136f), py(86f), px(158f), py(90f))
                    cubicTo(px(180f), py(94f), px(192f), py(74f), px(210f), py(60f))
                    cubicTo(px(228f), py(46f), px(240f), py(44f), px(258f), py(52f))
                    cubicTo(px(276f), py(60f), px(296f), py(84f), px(330f), py(80f))
                }
                drawPath(p, FBlue, style = Stroke(3.5.dp.toPx(), cap = StrokeCap.Round))
                listOf(60f to 66f, 158f to 90f, 258f to 52f).forEach { (x, y) ->
                    drawCircle(FAmberDot, 6.dp.toPx(), Offset(px(x), py(y)))
                    drawCircle(Color.White, 6.dp.toPx(), Offset(px(x), py(y)), style = Stroke(2.5.dp.toPx()))
                }
                drawCircle(FBlue, 5.dp.toPx(), Offset(px(330f) - 5.dp.toPx(), py(80f)))
            }
            Text(
                "180",
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 6.dp).offset(y = h * (38f / 150f) - 6.dp),
                color = FMute, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold
            )
            Text(
                "70",
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 6.dp).offset(y = h * (92f / 150f) - 6.dp),
                color = FMute, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

// ---------------- TAB 2 : LOG ----------------

@Composable
private fun FLogScreen(
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    showCal: Boolean,
    onToggleCal: (Boolean) -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(top = 4.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(end = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f)) {
                FHead(eyebrow = "YOUR HISTORY", title = "The food log")
            }
            Box(
                Modifier
                    .size(42.dp)
                    .shadow(4.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(if (showCal) FBlue else Color.White)
                    .clickable { onToggleCal(!showCal) },
                contentAlignment = Alignment.Center
            ) {
                Text("\uD83D\uDCC5", fontSize = 18.sp)
            }
        }

        // Day tabs Mon-Sun of the current week
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp),
            horizontalArrangement = Gap8
        ) {
            (0..6).forEach { i ->
                val d = SampleData.weekStart.plusDays(i.toLong())
                val on = d == selected
                Box(
                    Modifier
                        .weight(1f)
                        .shadow(2.dp, RoundedCornerShape(14.dp), clip = false)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (on) FInk else Color.White)
                        .clickable { onSelect(d) }
                ) {
                    Text(
                        d.dayOfWeek.getDisplayName(JTextStyle.SHORT, Locale.ENGLISH).take(2),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        color = if (on) Color.White else FMute,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (showCal) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 6.dp
            ) {
                Box(Modifier.padding(14.dp)) {
                    MonthCalendar(
                        selected = selected,
                        onSelect = onSelect,
                        accent = FBlue,
                        weekHighlight = FSoftCard,
                        textColor = FInk,
                        mutedColor = FMute
                    )
                }
            }
        }

        val dayLabel = if (selected == SampleData.today) "Today"
        else selected.dayOfWeek.getDisplayName(JTextStyle.FULL, Locale.ENGLISH)
        val meals = SampleData.fusionMeals.filter { it.date == selected }

        Surface(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Column {
                Text(
                    "$dayLabel's meals",
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 13.dp, bottom = 4.dp),
                    color = FInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                )
                if (meals.isEmpty()) {
                    EmptyDayNote("No meals logged \u2014 scan one or add one to start this day.", FMute)
                } else {
                    meals.forEach { m -> FLogRow(m) }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        // Steadier swaps
        Surface(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp),
            shape = RoundedCornerShape(24.dp),
            color = FSoftCard
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
                Text(
                    "Steadier swaps, based on your week",
                    color = Color(0xFF1D5A8A), fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
                SampleData.steadySwaps.forEachIndexed { i, s ->
                    if (i > 0) {
                        Canvas(Modifier.fillMaxWidth().height(1.dp)) {
                            drawLine(
                                Color(0xFFC3D6E8), Offset(0f, 0f), Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 6f), 0f)
                            )
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(s.emoji, fontSize = 15.sp)
                        Text(
                            s.text,
                            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                            color = Color(0xFF3A5570), fontSize = 13.sp, fontWeight = FontWeight.Bold
                        )
                        Text(s.tag, color = FGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun FLogRow(m: GlucoseMeal) {
    Column {
        Canvas(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp)) {
            drawLine(FLine, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(m.tint),
                contentAlignment = Alignment.Center
            ) {
                Text(m.emoji, fontSize = 17.sp)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    m.name, color = FInk, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text("${m.time} \u00B7 ${m.source}", color = FMute, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(end = 8.dp)) {
                Text(m.carbs.toString(), color = FAmber, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FSerif)
                Text(" g", modifier = Modifier.padding(bottom = 1.dp), color = FMute, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Surface(shape = RoundedCornerShape(50), color = if (m.spikeBad) FHiBg else FOkBg) {
                Text(
                    m.spike,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = if (m.spikeBad) FHiTx else FOkTx,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

// ---------------- TAB 3 : MANUAL (log a meal by hand) ----------------

@Composable
private fun FManualScreen(
    name: String, onName: (String) -> Unit,
    carbs: String, onCarbs: (String) -> Unit,
    cal: String, onCal: (String) -> Unit,
    protein: String, onProtein: (String) -> Unit,
    fat: String, onFat: (String) -> Unit,
    time: String, onTime: (String) -> Unit,
    error: String,
    onAdd: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .imePadding()
            .padding(top = 4.dp)
    ) {
        FHead(eyebrow = "MANUAL ENTRY", title = "Log a meal by hand")

        Surface(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp),
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Gap12) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onName,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Meal name") },
                    singleLine = true
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Gap12) {
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = onCarbs,
                        modifier = Modifier.weight(1f),
                        label = { Text("Carbs (g)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = cal,
                        onValueChange = onCal,
                        modifier = Modifier.weight(1f),
                        label = { Text("Calories") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Gap12) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = onProtein,
                        modifier = Modifier.weight(1f),
                        label = { Text("Protein (g)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = fat,
                        onValueChange = onFat,
                        modifier = Modifier.weight(1f),
                        label = { Text("Fat (g)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                OutlinedTextField(
                    value = time,
                    onValueChange = onTime,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Time (optional, e.g. 12:42)") },
                    singleLine = true
                )
            }
        }

        if (error.isNotEmpty()) {
            Text(
                error,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp),
                color = FBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp)
                .height(54.dp)
                .shadow(10.dp, RoundedCornerShape(18.dp), clip = false)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(FBlue, FBlueDeep)))
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center
        ) {
            Text("Add to today's log", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Text(
            "New meals show up in Log under today.",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 10.dp),
            color = FMute,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(100.dp))
    }
}

// ---------------- Shared header + nav ----------------

@Composable
private fun FHead(eyebrow: String, title: String) {
    Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 2.dp)) {
        Text(
            eyebrow,
            color = FBlue,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
        Text(
            title,
            modifier = Modifier.padding(top = 4.dp),
            color = FInk,
            fontSize = 30.sp,
            lineHeight = 33.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FSerif
        )
    }
}

@Composable
private fun FusionNav(tab: Int, onTab: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.95f))
            .navigationBarsPadding()
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(FLine))
        Row(Modifier.fillMaxWidth().height(72.dp)) {
            val items = listOf(
                Triple("\uD83D\uDCF7", "Scan", 0),
                Triple("\uD83E\uDE78", "Glucose", 1),
                Triple("\uD83D\uDCCB", "Log", 2),
                Triple("\uD83D\uDCDD", "Manual", 3)
            )
            items.forEach { (icon, label, i) ->
                val on = tab == i
                Column(
                    Modifier.weight(1f).fillMaxHeight().clickable { onTab(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(icon, fontSize = 20.sp)
                    Text(
                        label,
                        modifier = Modifier.padding(top = 3.dp),
                        color = if (on) FBlue else FMute,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
