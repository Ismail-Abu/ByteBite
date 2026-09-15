package com.example.guione

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ByteBiteApp()
        }
    }
}

@Composable
fun ByteBiteApp() {
    val nav = rememberNavController()
    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Color(0xFFF6F4EE)) {
            NavHost(navController = nav, startDestination = "chooser") {
                composable("chooser") {
                    ChooserScreen(
                        onFusion = { nav.navigate("fusion") },
                        onKitchen = { nav.navigate("kitchen") },
                        onGlucose = { nav.navigate("glucose") }
                    )
                }
                composable("fusion") { FusionExperience() }
                composable("kitchen") { KitchenExperience() }
                composable("glucose") { GlucoseExperience() }
            }
        }
    }
}

@Composable
private fun ChooserScreen(onFusion: () -> Unit, onKitchen: () -> Unit, onGlucose: () -> Unit) {
    // Load the model as soon as the app opens, so the footer can say whether
    // this build is running the real model before anyone picks an experience.
    val context = LocalContext.current
    LaunchedEffect(Unit) { ScanStore.warmUp(context) }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            "ByteBite",
            color = Color(0xFF1F2617),
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        )
        Text(
            "Nutrition from a photo \u2014 estimated on your phone",
            modifier = Modifier.padding(top = 6.dp),
            color = Color(0xFF7A7F6E),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(30.dp))
        ExperienceCard(
            bg = Color(0xFFFDF0EA),
            dot = Color(0xFFE4674E),
            title = "ByteBite Combined",
            sub = "scan \u00B7 glucose \u00B7 log \u00B7 manual",
            onClick = onFusion
        )
        Spacer(Modifier.height(14.dp))
        ExperienceCard(
            bg = Color(0xFFF1F5EA),
            dot = Color(0xFF7BA05B),
            title = "Kitchen Journal",
            sub = "warm food-diary look",
            onClick = onKitchen
        )
        Spacer(Modifier.height(14.dp))
        ExperienceCard(
            bg = Color(0xFFEDF2FB),
            dot = Color(0xFF1E2A44),
            title = "Glucose",
            sub = "carb-first diabetes view",
            onClick = onGlucose
        )
        Spacer(Modifier.weight(1f))
        Text(
            if (ScanStore.modelAvailable) "On-device model \u00B7 ${ScanStore.modelLabel}"
            else "No model installed \u00B7 sample data only",
            modifier = Modifier.padding(bottom = 26.dp),
            color = Color(0xFFA0A594),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ExperienceCard(
    bg: Color,
    dot: Color,
    title: String,
    sub: String,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(26.dp), clip = false)
            .clip(RoundedCornerShape(26.dp))
            .background(bg)
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(dot))
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(title, color = Color(0xFF20261B), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                Text(sub, color = Color(0xFF83887A), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("\u203A", color = Color(0xFF9AA08E), fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
    }
}
