package com.example.guione

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

// ---------- Palette (from bytebite_gui_2_kitchen.html) ----------
private val KInk = Color(0xFF24331F)
private val KBg = Color(0xFFF1F5EA)
private val KGreen = Color(0xFF7BA05B)
private val KGold = Color(0xFFE9C46A)
private val KRust = Color(0xFF9C5B4B)
private val KSage = Color(0xFFB8C6A4)
private val KMuted = Color(0xFF8B9A7B)
private val KLabel = Color(0xFF6C7C5C)
private val KHint = Color(0xFF4E6140)
private val KNoteText = Color(0xFF3E5231)
private val KNoteBg = Color(0xFFE4EDD6)
private val KOrange = Color(0xFFC9793A)
private val KNavIdle = Color(0xFFAAB99B)
private val KStripeA = Color(0xFFE7EEDC)
private val KStripeB = Color(0xFFEDF3E4)
private val KDash = Color(0xFFC6D3B4)
private val KTrack = Color(0xFFEDF2E3)

// Serif stands in for Fraunces, default sans for Nunito.
private val KSerif = FontFamily.Serif

@Composable
fun KitchenExperience() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selectedDate by remember { mutableStateOf(SampleData.today) }
    var showCal by remember { mutableStateOf(false) }

    val context = LocalContext.current
    // Load the interpreter while the user is still framing the shot, so the
    // shutter tap is not charged for it.
    LaunchedEffect(Unit) { ScanStore.warmUp(context) }
    val capture = rememberDishCapture { bitmap ->
        ScanStore.scan(context, bitmap)
        tab = 1                       // jump to the plate; it renders the running state
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = KGreen,
            background = KBg,
            surface = Color.White,
            onBackground = KInk,
            onSurface = KInk
        )
    ) {
        Box(Modifier.fillMaxSize().background(KBg)) {
            when (tab) {
                0 -> SnapScreen(
                    onSnap = { capture.fromCamera() },
                    onPick = { capture.fromGallery() }
                )
                1 -> PlateScreen()
                else -> WeekScreen(
                    selected = selectedDate,
                    onSelect = { selectedDate = it },
                    showCal = showCal,
                    onToggleCal = { showCal = it }
                )
            }
            KitchenNav(
                tab = tab,
                onTab = { tab = it },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// ---------------- PAGE 1 : SNAP ----------------

@Composable
private fun SnapScreen(onSnap: () -> Unit, onPick: () -> Unit) {
    val dayName = SampleData.today.dayOfWeek
        .getDisplayName(JTextStyle.FULL, Locale.ENGLISH).uppercase(Locale.ENGLISH)

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 10.dp)) {
        KitchenHead(eyebrow = "BYTEBITE \u00B7 $dayName", title = "What's on the plate?")

        // Tablecloth frame with illustrated plate
        Box(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(32.dp))
                    .drawBehind {
                        rotate(45f) {
                            val stripe = 22.dp.toPx()
                            val reach = size.maxDimension * 1.5f
                            var x = -reach
                            var i = 0
                            while (x < reach) {
                                drawRect(
                                    color = if (i % 2 == 0) KStripeA else KStripeB,
                                    topLeft = Offset(x, -reach),
                                    size = Size(stripe, reach * 2f)
                                )
                                x += stripe
                                i++
                            }
                        }
                    }
            )
            Canvas(Modifier.matchParentSize()) {
                drawRoundRect(
                    color = KDash,
                    cornerRadius = CornerRadius(32.dp.toPx()),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    )
                )
            }
            Column(
                Modifier.matchParentSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                FoodPlate()
                Surface(
                    modifier = Modifier.padding(top = 22.dp),
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        buildAnnotatedString {
                            append("Hold phone ")
                            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = KGreen)) {
                                append("flat overhead")
                            }
                            append(" \u2014 looks great!")
                        },
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        color = KHint,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Snap bar
        Row(
            Modifier.fillMaxWidth().padding(bottom = 108.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIcon("\uD83D\uDDBC")
            ShutterButton(
                ringColor = KGreen,
                gapColor = KBg,
                onClick = onSnap,
                modifier = Modifier.padding(horizontal = 34.dp)
            )
            RoundIcon("\u27F3")
        }
    }
}

@Composable
private fun FoodPlate() {
    Box(Modifier.size(264.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(264.dp)
                .shadow(18.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(Color.White)
                .drawBehind {
                    val r = size.minDimension / 2f
                    drawCircle(Color(0xFFF4F7EF), radius = r - 9.dp.toPx(), style = Stroke(18.dp.toPx()))
                    drawCircle(Color(0xFFDCE6CE), radius = r - 19.dp.toPx(), style = Stroke(2.dp.toPx()))
                }
        )
        Box(
            Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(Color(0xFFEFE7D6))
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    drawCircle(Color(0xFFB7CE8F), radius = 0.30f * w, center = Offset(0.34f * w, 0.30f * h))
                    drawCircle(KGold, radius = 0.27f * w, center = Offset(0.68f * w, 0.42f * h))
                    drawCircle(KRust, radius = 0.29f * w, center = Offset(0.48f * w, 0.68f * h))
                }
        )
    }
}

@Composable
private fun RoundIcon(glyph: String, onClick: (() -> Unit)? = null) {
    Surface(
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier
            .size(48.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(glyph, fontSize = 19.sp)
        }
    }
}

/** Ring shutter shared by both experiences. */
@Composable
fun ShutterButton(
    ringColor: Color,
    gapColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Int = 82
) {
    Box(
        modifier
            .size(diameter.dp)
            .shadow(10.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(Color.White)
            .border(6.dp, gapColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .matchParentSize()
                .padding(6.dp)
                .border(8.dp, ringColor, CircleShape)
        )
    }
}

// ---------------- PAGE 2 : PLATE ----------------

@Composable
private fun PlateScreen() {
    val dish = DishView.current()
    val running = ScanStore.state is ScanStore.State.Running
    val failed = ScanStore.state as? ScanStore.State.Failed

    // The diary has no "log" button: a snap is the entry. Keyed on the scan so
    // it fires once per new plate, not on every recomposition.
    LaunchedEffect(dish.scanId) {
        if (dish.live && ScanStore.claimForLog(dish, "kitchen")) {
            SampleData.kitchenMeals.add(
                0,
                KitchenMeal(dish.name, dish.kcal, SampleData.slotNow(), KSage, SampleData.today)
            )
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(top = 10.dp)
    ) {
        KitchenHead(
            eyebrow = when {
                running -> "READING THE PLATE\u2026"
                dish.live -> "JUST SNAPPED \u00B7 ${dish.detail.uppercase(Locale.ENGLISH)}"
                else -> "SAMPLE DISH \u00B7 NO MODEL INSTALLED"
            },
            title = dish.name
        )

        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("\uD83C\uDF74", fontSize = 26.sp, color = KSage, modifier = Modifier.padding(end = 12.dp))
            CalorieDonut(kcal = dish.kcal)
            Text("\uD83E\uDD44", fontSize = 26.sp, color = KSage, modifier = Modifier.padding(start = 12.dp))
        }

        // Macro chips, 2 x 2
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Gap10
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Gap10) {
                MacroChip("CARBS", "${dish.carbG}g", KGold, Modifier.weight(1f))
                MacroChip("PROTEIN", "${dish.proteinG}g", KGreen, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Gap10) {
                MacroChip("FAT", "${dish.fatG}g", KRust, Modifier.weight(1f))
                MacroChip("MASS", "${dish.massG}g", KSage, Modifier.weight(1f))
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp),
            shape = RoundedCornerShape(16.dp),
            color = KNoteBg
        ) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = KInk)) {
                        append(
                            when {
                                failed != null -> "Couldn't read that photo. "
                                dish.live -> "Estimated from one overhead photo. "
                                else -> "Sample dish. "
                            }
                        )
                    }
                    // The mockup's coaching line ("protein covers 31% of today's
                    // goal") was illustrative. The model returns five totals and
                    // nothing else, so for a real estimate the honest thing to
                    // show beside them is their measured error, not invented advice.
                    append(
                        when {
                            failed != null -> failed.message
                            dish.live -> dish.errorNote
                                ?: "Estimate only \u2014 check it against a carb count you trust."
                            else -> "Run notebooks/bytebite_android_export.ipynb to install the " +
                                "model, then these numbers come from the photo."
                        }
                    )
                },
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                color = KNoteText,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun CalorieDonut(kcal: Int) {
    Surface(shape = CircleShape, color = Color.White, shadowElevation = 12.dp, modifier = Modifier.size(252.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 30.dp.toPx()
                val radius = (size.minDimension - stroke) / 2f
                val arcSize = Size(size.minDimension - stroke, size.minDimension - stroke)
                val topLeft = Offset((size.width - arcSize.width) / 2f, (size.height - arcSize.height) / 2f)
                drawCircle(KTrack, radius = radius, style = Stroke(stroke))
                var start = -90f
                // carbs 43% / protein 40% / fat 17% of macro grams (from mockup)
                listOf(0.43f to KGold, 0.40f to KGreen, 0.17f to KRust).forEach { (f, c) ->
                    val sweep = f * 360f
                    drawArc(
                        color = c,
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Butt)
                    )
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    kcal.toString(),
                    color = KInk,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = KSerif
                )
                Text(
                    "CALORIES",
                    color = KMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.4.sp
                )
            }
        }
    }
}

@Composable
private fun MacroChip(label: String, value: String, dot: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 3.dp) {
        Column(Modifier.padding(horizontal = 15.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
                Text(
                    label,
                    modifier = Modifier.padding(start = 7.dp),
                    color = KLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.1.sp
                )
            }
            Text(
                value,
                modifier = Modifier.padding(top = 4.dp),
                color = KInk,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = KSerif
            )
        }
    }
}

// ---------------- PAGE 3 : WEEK ----------------

@Composable
private fun WeekScreen(
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
                KitchenHead(eyebrow = "YOUR WEEK", title = "The food journal")
            }
            Box(
                Modifier
                    .size(42.dp)
                    .shadow(4.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(if (showCal) KGreen else Color.White)
                    .clickable { onToggleCal(!showCal) },
                contentAlignment = Alignment.Center
            ) {
                Text("\uD83D\uDCC5", fontSize = 18.sp)
            }
        }

        if (showCal) {
            KitchenCard(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                Box(Modifier.padding(16.dp)) {
                    MonthCalendar(
                        selected = selected,
                        onSelect = onSelect,
                        accent = KGreen,
                        weekHighlight = KNoteBg,
                        textColor = KInk,
                        mutedColor = KMuted
                    )
                }
            }

            // Selected-day view
            val dayMeals = SampleData.kitchenMeals.filter { it.date == selected }
            val title = if (selected == SampleData.today) "Today"
            else selected.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH))
            KitchenCard(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                Column {
                    CardHead(title, "${dayMeals.size} logged")
                    if (dayMeals.isEmpty()) {
                        EmptyDayNote("No meals logged this day \u2014 snap a plate to start it.", KMuted)
                    } else {
                        dayMeals.forEachIndexed { i, m -> KitchenMealRow(m, topLine = i > 0) }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        } else {
            // Default: this week's meals
            KitchenCard(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                Column {
                    CardHead("Meals this week", "11 logged")
                    SampleData.kitchenMeals.forEachIndexed { i, m -> KitchenMealRow(m, topLine = i > 0) }
                    Spacer(Modifier.height(12.dp))
                }
            }
            // Try next
            KitchenCard(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                Column {
                    CardHead("Try next", "picked for you")
                    SampleData.tryNext.forEach { t ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BiteDot(t.dot)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(
                                    t.name, color = KInk, fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Text(t.sub, color = KMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Surface(shape = RoundedCornerShape(50), color = if (t.alt) KOrange else KGreen) {
                                Text(
                                    t.tag,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun KitchenMealRow(m: KitchenMeal, topLine: Boolean) {
    Column(Modifier.padding(horizontal = 18.dp)) {
        if (topLine) {
            Canvas(Modifier.fillMaxWidth().height(1.dp)) {
                drawLine(
                    color = Color(0xFFE1E9D4),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BiteDot(m.dot)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    m.name, color = KInk, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                val dow = m.date.dayOfWeek.getDisplayName(JTextStyle.SHORT, Locale.ENGLISH)
                Text("$dow ${m.slot}", color = KMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                m.kcal.toString(),
                color = KHint,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = KSerif
            )
        }
    }
}

@Composable
private fun BiteDot(color: Color) {
    Box(
        Modifier
            .size(40.dp)
            .shadow(2.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(color)
            .border(4.dp, Color.White, CircleShape)
    )
}

@Composable
private fun KitchenCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 5.dp
    ) { content() }
}

@Composable
private fun CardHead(title: String, tag: String) {
    Row(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 15.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            color = KInk,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = KSerif
        )
        Text(tag, color = KGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)
    }
}

@Composable
private fun KitchenHead(eyebrow: String, title: String) {
    Column(Modifier.padding(start = 26.dp, end = 26.dp, bottom = 6.dp)) {
        Text(
            eyebrow,
            color = KGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
        Text(
            title,
            modifier = Modifier.padding(top = 4.dp),
            color = KInk,
            fontSize = 30.sp,
            lineHeight = 33.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = KSerif
        )
    }
}

// ---------------- NAV ----------------

@Composable
private fun KitchenNav(tab: Int, onTab: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, bottom = 18.dp)
            .fillMaxWidth()
            .height(66.dp)
            .shadow(16.dp, RoundedCornerShape(50), clip = false)
            .clip(RoundedCornerShape(50))
            .background(KInk)
            .padding(6.dp)
    ) {
        listOf("Snap", "Plate", "Week").forEachIndexed { i, label ->
            val on = tab == i
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(if (on) KGreen else Color.Transparent)
                    .clickable { onTab(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (on) Color.White else KNavIdle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
