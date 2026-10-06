package com.example.guione.meal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.guione.ScanStore
import com.example.guione.rememberDishCapture
import com.example.guione.ui.components.EmptyState
import com.example.guione.ui.theme.Spacing
import androidx.compose.material.icons.outlined.NoPhotography

/**
 * Photo capture → on-device nutrition estimate. Takes or picks a meal photo,
 * runs the model through [ScanStore], and on a ready estimate navigates to the
 * review screen. Honest about its limits: there is no food-check gate yet, so it
 * only makes sense on a clear overhead photo of one real meal, and the result is
 * an approximate estimate.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    onBack: () -> Unit,
    onReviewReady: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        ScanStore.reset()
        ScanStore.warmUp(context)
    }
    val capture = rememberDishCapture(onCaptured = { bmp -> ScanStore.scan(context, bmp) })
    val state = ScanStore.state

    LaunchedEffect(state) {
        if (state is ScanStore.State.Ready) onReviewReady()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan a meal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            is ScanStore.State.NoModel -> EmptyState(
                icon = Icons.Outlined.NoPhotography,
                title = "Photo estimation unavailable",
                message = "The nutrition model isn't installed in this build, so photos can't be estimated. You can still log meals manually.",
                actionLabel = "Go back",
                onAction = onBack,
                modifier = Modifier.padding(padding),
            )

            is ScanStore.State.Running -> Column(
                Modifier.fillMaxSize().padding(padding).padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(Spacing.lg))
                Text("Analyzing your photo…", style = MaterialTheme.typography.titleMedium)
            }

            is ScanStore.State.Failed -> EmptyState(
                icon = Icons.Outlined.NoPhotography,
                title = "Couldn't read that photo",
                message = state.message,
                actionLabel = "Try again",
                onAction = { ScanStore.reset() },
                modifier = Modifier.padding(padding),
            )

            else -> Column(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(Spacing.xl))
                Icon(
                    Icons.Outlined.PhotoCamera,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(Modifier.height(Spacing.lg))
                Text(
                    "Hold your phone flat, directly above one plate, so the whole meal is in frame.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "Experimental: there's no food check yet, so only scan a clear photo of a real meal. The result is an estimate with a typical error range.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.xl))
                Button(onClick = { capture.fromCamera() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Take photo")
                }
                Spacer(Modifier.height(Spacing.md))
                OutlinedButton(onClick = { capture.fromGallery() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Choose from gallery")
                }
            }
        }
    }
}
