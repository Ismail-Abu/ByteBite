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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

// ---------- Palette (from bytebite_gui_5_glucose.html) ----------
private val GInk = Color(0xFF1E2A44)
private val GBlue = Color(0xFF2E7BA6)
private val GTeal = Color(0xFF2E9E8B)
private val GSlate = Color(0xFF5B7FA6)
private val GMute = Color(0xFF8AA3BF)
private val GAmber = Color(0xFFB4691E)
private val GAmberLt = Color(0xFFE8A44C)
private val GBand = Color(0xFFE4F3EA)
private val GLine = Color(0xFFDCE6F2)
private val GTrackBg = Color(0xFFEDF2FB)
private val GOkBg = Color(0xFFDEF3E7)
private val GOkTx = Color(0xFF2E9E6B)
private val GHiBg = Color(0xFFFDE6DE)
private val GHiTx = Color(0xFFC4502E)
private val GSteadyBg = Color(0xFFE7F2F7)
private val GSteadyTitle = Color(0xFF20618A)
private val GSteadyText = Color(0xFF33526B)
private val GCamDark = Color(0xFF10131C)

private val GBgBrush = Brush.verticalGradient(
    0f to Color(0xFFEDF2FB),
    0.6f to Color(0xFFE4EEF2),
    1f to Color(0xFFE9F4EE)
)

@Composable
fun GlucoseExperience() {
    // Opens on the Scan tab, camera stage, per the flow spec.
    var tab by rememberSaveable { mutableIntStateOf(1) }
    var scanned by rememberSaveable { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(SampleData.today) }
    var showCal by remember { mutableStateOf(false) }

    val context = LocalContext.current
    LaunchedEffect(Unit) { ScanStore.warmUp(context) }
    val capture = rememberDishCapture { bitmap ->
        ScanStore.scan(context, bitmap)
        scanned = true                 // the result screen renders the running state
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = GBlue,
            background = Color(0xFFEDF2FB),
            surface = Color.White,
            onBackground = GInk,
            onSurface = GInk
        )
    ) {
        Box(Modifier.fillMaxSize().background(GBgBrush)) {
            when (tab) {
                0 -> GTodayScreen(onScan = { scanned = false; tab = 1 })
                1 -> if (!scanned) {
                    GCameraScreen(
                        onShutter = { capture.fromCamera() },
                        onPick = { capture.fromGallery() }
                    )
                } else {
                    GScanResultScreen(onLog = { tab = 2; scanned = false })
                }
                else -> GLogScreen(
                    selected = selectedDate,
                    onSelect = { selectedDate = it },
                    showCal = showCal,
                    onToggleCal = { showCal = it }
                )
            }
            GlucoseNav(
                tab = tab,
                onTab = {
                    if (it == 1 && tab != 1) scanned = false // re-entering Scan starts at the camera
                    tab = it
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// ---------------- SCREEN 0 (start) : MOCK CAMERA ----------------

@Composable
private fun GCameraScreen(onShutter: () -> Unit, onPick: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 10.dp)) {
        GlucoseHead(hi = "SCAN A MEAL", title = "Point straight down")

        // Mock viewfinder
        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            shape = RoundedCornerShape(26.dp),
            color = GCamDark,
            shadowElevation = 8.dp
        ) {
            Box {
                Canvas(Modifier.matchParentSize()) {
                    val m = 18.dp.toPx()
                    val len = 26.dp.toPx()
                    val sw = 3.dp.toPx()
                    val w = size.width
                    val h = size.height
                    // Corner brackets
                    listOf(
                        listOf(Offset(m, m + len), Offset(m, m), Offset(m + len, m)),
                        listOf(Offset(w - m - len, m), Offset(w - m, m), Offset(w - m, m + len)),
                        listOf(Offset(m, h - m - len), Offset(m, h - m), Offset(m + len, h - m)),
                        listOf(Offset(w - m - len, h - m), Offset(w - m, h - m), Offset(w - m, h - m - len))
                    ).forEach { pts ->
                        drawLine(GTeal, pts[0], pts[1], strokeWidth = sw, cap = StrokeCap.Round)
                        drawLine(GTeal, pts[1], pts[2], strokeWidth = sw, cap = StrokeCap.Round)
                    }
                    // Plate framing guide
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
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            Modifier.fillMaxWidth().padding(bottom = 108.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ShutterButton(ringColor = GTeal, gapColor = Color(0xFFE4EEF2), onClick = onShutter)
            Text(
                if (ScanStore.modelAvailable) "Tap to scan \u2014 runs on this phone, offline"
                else "Tap to scan \u2014 no model installed, showing a sample dish",
                modifier = Modifier.padding(top = 10.dp),
                color = GMute,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "or choose an existing photo",
                modifier = Modifier.padding(top = 8.dp).clickable(onClick = onPick),
                color = GBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ---------------- SCREEN 1 : TODAY ----------------

@Composable
private fun GTodayScreen(onScan: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(top = 10.dp)
    ) {
        GlucoseHead(hi = "Good afternoon, Ismail", title = "Today at a glance")

        // Glucose curve card
        Surface(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp),
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Glucose \u00B7 last 12 h",
                        modifier = Modifier.weight(1f),
                        color = GInk,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(shape = RoundedCornerShape(50), color = GOkBg) {
                        Text(
                            SampleData.TIME_IN_RANGE,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = GOkTx,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                GlucoseCurve(Modifier.padding(top = 8.dp))
                Row(
                    Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("6 am", "\u25CF breakfast", "\u25CF lunch", "now").forEach {
                        Text(it, color = GMute, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Now + carb budget
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp),
            horizontalArrangement = Gap12
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(22.dp),
                color = GInk,
                shadowElevation = 6.dp
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("NOW", color = Color(0xFF9DB3D4), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.9.sp)
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp)) {
                        Text(
                            SampleData.GLUCOSE_NOW.toString(),
                            color = Color.White,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            " mg/dL",
                            modifier = Modifier.padding(bottom = 5.dp),
                            color = Color(0xFF9DB3D4),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text("\u2192 steady", color = Color(0xFF7FD8A8), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 2.dp))
                }
            }
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 6.dp
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("CARBS TODAY", color = GSlate, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.9.sp)
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp)) {
                        Text(
                            SampleData.CARBS_SO_FAR.toString(),
                            color = GAmber,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            " / ${SampleData.CARB_BUDGET} g",
                            modifier = Modifier.padding(bottom = 5.dp),
                            color = GMute,
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
                            .background(GTrackBg)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(SampleData.CARBS_SO_FAR / SampleData.CARB_BUDGET.toFloat())
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(Brush.horizontalGradient(listOf(GAmberLt, GAmber)))
                        )
                    }
                }
            }
        }

        // Scan CTA
        Box(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp)
                .height(64.dp)
                .shadow(10.dp, RoundedCornerShape(22.dp), clip = false)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.horizontalGradient(listOf(GBlue, GTeal)))
                .clickable(onClick = onScan),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "\uD83D\uDCF7  Scan a meal \u2014 count carbs",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            "Estimates from a photo, powered by ByteBite. Not medical advice \u2014 confirm dosing decisions with your care team.",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 12.dp),
            color = GMute,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )

        Spacer(Modifier.height(116.dp))
    }
}

@Composable
private fun GlucoseCurve(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val h = maxWidth * (150f / 330f)
        Box(Modifier.fillMaxWidth().height(h)) {
            Canvas(Modifier.fillMaxSize()) {
                fun px(v: Float) = v / 330f * size.width
                fun py(v: Float) = v / 150f * size.height

                // In-range band (70-180)
                drawRoundRect(
                    color = GBand,
                    topLeft = Offset(0f, py(38f)),
                    size = Size(size.width, py(102f) - py(38f)),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
                // Curve
                val p = Path().apply {
                    moveTo(px(0f), py(88f))
                    cubicTo(px(25f), py(84f), px(40f), py(70f), px(60f), py(66f))
                    cubicTo(px(80f), py(62f), px(92f), py(50f), px(108f), py(54f))
                    cubicTo(px(124f), py(58f), px(136f), py(86f), px(158f), py(90f))
                    cubicTo(px(180f), py(94f), px(192f), py(74f), px(210f), py(60f))
                    cubicTo(px(228f), py(46f), px(240f), py(44f), px(258f), py(52f))
                    cubicTo(px(276f), py(60f), px(296f), py(84f), px(330f), py(80f))
                }
                drawPath(p, GBlue, style = Stroke(3.5.dp.toPx(), cap = StrokeCap.Round))
                // Meal markers
                listOf(60f to 66f, 158f to 90f, 258f to 52f).forEach { (x, y) ->
                    drawCircle(GAmberLt, 6.dp.toPx(), Offset(px(x), py(y)))
                    drawCircle(Color.White, 6.dp.toPx(), Offset(px(x), py(y)), style = Stroke(2.5.dp.toPx()))
                }
                // "Now" endpoint
                drawCircle(GBlue, 5.dp.toPx(), Offset(px(330f) - 5.dp.toPx(), py(80f)))
            }
            Text(
                "180",
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 6.dp).offset(y = h * (38f / 150f) - 6.dp),
                color = GMute, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold
            )
            Text(
                "70",
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 6.dp).offset(y = h * (92f / 150f) - 6.dp),
                color = GMute, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

// ---------------- SCREEN 2 : SCAN RESULT ----------------

@Composable
private fun GScanResultScreen(onLog: () -> Unit) {
    val dish = DishView.current()
    val running = ScanStore.state is ScanStore.State.Running
    val failed = ScanStore.state as? ScanStore.State.Failed

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(top = 10.dp)
    ) {
        GlucoseHead(
            hi = when {
                running -> "Running on device\u2026"
                failed != null -> "Scan failed"
                dish.live -> "Scanned just now \u00B7 ${dish.detail}"
                else -> "Sample dish \u00B7 no model installed"
            },
            title = "Carb count first"
        )

        // Carb hero
        Surface(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp),
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "${dish.name.uppercase(Locale.ENGLISH)} \u00B7 EST. ${dish.massG} G",
                    color = GSlate, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.4.sp
                )
                Text(
                    dish.carbG.toString(),
                    modifier = Modifier.padding(top = 6.dp),
                    color = GAmber, fontSize = 74.sp, fontWeight = FontWeight.Bold, lineHeight = 76.sp
                )
                Text(
                    "GRAMS OF CARBS",
                    modifier = Modifier.padding(top = 2.dp),
                    color = GMute, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp
                )
                // The mockup broke the total into "38 g from rice / 8 g veggies
                // & sauce". v4 has a single Dense(5) head over the whole dish and
                // no per-ingredient output, so that split cannot be produced from
                // a real scan. Shown only in the sample-data state, where it is
                // clearly illustrative; a live estimate gets its error bar instead.
                if (dish.live) {
                    Text(
                        dish.errorNote ?: "One photo, one estimate",
                        modifier = Modifier.padding(top = 14.dp),
                        color = GSlate, fontSize = 13.sp, fontWeight = FontWeight.Bold
                    )
                } else {
                    Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        SplitBit("\u2248 ", "38 g", " from rice")
                        SplitBit("\u2248 ", "8 g", " veggies & sauce")
                    }
                }
            }
        }

        // Advisory card. The mockup attributed this to "the model's cooking-cue
        // read", but v4 dropped the text and cooking-classification heads
        // permanently, so there is no cooking cue to report. A live estimate gets
        // a caveat the model's actual inputs support instead: one overhead RGB
        // frame, no depth, so anything below the top layer is inferred.
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp)
                .border(1.5.dp, Color(0xFFF0D9B4), RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFFFFF7EC)
        ) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                Text(
                    if (dish.live) "\u26A0 Estimate, not a measurement"
                    else "\u26A0 Heads up: sauce may hide sugar",
                    color = Color(0xFF8A5B14), fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    if (dish.live)
                        "Read from a single overhead photo of the plate, with no depth " +
                        "information. Sauces, oils and anything hidden under the top " +
                        "layer are inferred, not seen. Treat it as a starting point for " +
                        "a carb count, not a replacement for one."
                    else
                        "The model's cooking-cue read suggests a glazed sauce. Glazes can add fast-acting carbs \u2014 if you're unsure, log it and watch your 2-hour trend.",
                    modifier = Modifier.padding(top = 6.dp),
                    color = Color(0xFF6E5A34), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp
                )
            }
        }

        // Mini stats
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp),
            horizontalArrangement = Gap12
        ) {
            MiniStat("CALORIES", dish.kcal.toString(), Modifier.weight(1f))
            MiniStat("PROTEIN", "${dish.proteinG} g", Modifier.weight(1f))
            MiniStat("FAT", "${dish.fatG} g", Modifier.weight(1f))
        }

        // Buttons
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp),
            horizontalArrangement = Gap12
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(2.dp, GBlue, RoundedCornerShape(18.dp))
                    .clickable { /* visual only */ },
                contentAlignment = Alignment.Center
            ) {
                Text("Edit portion", color = GBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(GInk)
                    .clickable(onClick = onLog),
                contentAlignment = Alignment.Center
            ) {
                Text("Log meal", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(116.dp))
    }
}

@Composable
private fun SplitBit(pre: String, strong: String, post: String) {
    Text(
        buildAnnotatedString {
            append(pre)
            withStyle(SpanStyle(color = GInk, fontWeight = FontWeight.ExtraBold)) { append(strong) }
            append(post)
        },
        color = GSlate, fontSize = 13.sp, fontWeight = FontWeight.Bold
    )
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 4.dp) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
            Text(label, color = GSlate, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp)
            Text(value, modifier = Modifier.padding(top = 3.dp), color = GInk, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ---------------- SCREEN 3 : LOG ----------------

@Composable
private fun GLogScreen(
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    showCal: Boolean,
    onToggleCal: (Boolean) -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(top = 10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(end = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f)) {
                GlucoseHead(hi = "Your history", title = "Meals & glucose")
            }
            Box(
                Modifier
                    .size(42.dp)
                    .shadow(4.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(if (showCal) GInk else Color.White)
                    .clickable { onToggleCal(!showCal) },
                contentAlignment = Alignment.Center
            ) {
                Text("\uD83D\uDCC5", fontSize = 18.sp)
            }
        }

        // Day tabs: full current week, Mon-Sun
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp),
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
                        .background(if (on) GInk else Color.White)
                        .clickable { onSelect(d) }
                ) {
                    Text(
                        d.dayOfWeek.getDisplayName(JTextStyle.SHORT, Locale.ENGLISH).take(2),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
                        color = if (on) Color.White else GMute,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (showCal) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 6.dp
            ) {
                Box(Modifier.padding(16.dp)) {
                    MonthCalendar(
                        selected = selected,
                        onSelect = onSelect,
                        accent = GBlue,
                        weekHighlight = GSteadyBg,
                        textColor = GInk,
                        mutedColor = GMute
                    )
                }
            }
        }

        // Meals for the selected day
        val dayLabel = if (selected == SampleData.today) "Today"
        else selected.dayOfWeek.getDisplayName(JTextStyle.FULL, Locale.ENGLISH)
        val meals = SampleData.glucoseMeals.filter { it.date == selected }

        Surface(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Column {
                Text(
                    "$dayLabel's meals",
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 15.dp, bottom = 6.dp),
                    color = GInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                )
                if (meals.isEmpty()) {
                    EmptyDayNote("No meals logged \u2014 scan one to start this day.", GMute)
                } else {
                    meals.forEach { m -> LgRow(m) }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // Steadier swaps
        Surface(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = GSteadyBg
        ) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                Text(
                    "Steadier swaps, based on your week",
                    color = GSteadyTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
                SampleData.steadySwaps.forEachIndexed { i, s ->
                    if (i > 0) {
                        Canvas(Modifier.fillMaxWidth().height(1.dp)) {
                            drawLine(
                                Color(0xFFC3DBE8), Offset(0f, 0f), Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 6f), 0f)
                            )
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(s.emoji, fontSize = 15.sp)
                        Text(
                            s.text,
                            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                            color = GSteadyText, fontSize = 13.sp, fontWeight = FontWeight.Bold
                        )
                        Text(s.tag, color = GTeal, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        Spacer(Modifier.height(116.dp))
    }
}

@Composable
private fun LgRow(m: GlucoseMeal) {
    Column {
        Canvas(Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(1.dp)) {
            drawLine(GTrackBg, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 11.dp),
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
                    m.name, color = GInk, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text("${m.time} \u00B7 ${m.source}", color = GMute, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                buildAnnotatedString {
                    append(m.carbs.toString())
                    withStyle(SpanStyle(color = GMute, fontSize = 10.sp)) { append(" g") }
                },
                color = GAmber, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 8.dp)
            )
            Surface(shape = RoundedCornerShape(50), color = if (m.spikeBad) GHiBg else GOkBg) {
                Text(
                    m.spike,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = if (m.spikeBad) GHiTx else GOkTx,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun GlucoseHead(hi: String, title: String) {
    Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 6.dp)) {
        Text(hi, color = GSlate, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.3.sp)
        Text(
            title,
            modifier = Modifier.padding(top = 2.dp),
            color = GInk,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ---------------- NAV ----------------

@Composable
private fun GlucoseNav(tab: Int, onTab: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.95f))
            .navigationBarsPadding()
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(GLine))
        Row(Modifier.fillMaxWidth().height(72.dp)) {
            val items = listOf(
                Triple("\uD83C\uDF1E", "Today", 0),
                Triple("\uD83D\uDCF7", "Scan", 1),
                Triple("\uD83D\uDCCB", "Log", 2)
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
                        color = if (on) GBlue else GMute,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
