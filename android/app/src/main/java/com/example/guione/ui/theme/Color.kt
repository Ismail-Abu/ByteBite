package com.example.guione.ui.theme

import androidx.compose.ui.graphics.Color

// ByteBite palette: a calm research-health look — near-white neutrals, deep
// slate text, one restrained teal accent, semantic error/warning only. Defined
// once here; screens read them through MaterialTheme.colorScheme, never as
// literals. Dark values are chosen separately for readable contrast, not derived
// by inverting the light ones.

// Teal accent
internal val Teal40 = Color(0xFF006A6B)   // light primary
internal val Teal90 = Color(0xFFCCE8E7)   // light primary container
internal val Teal80 = Color(0xFF4FD8DC)   // dark primary
internal val Teal30 = Color(0xFF004F50)   // dark primary container
internal val OnTeal90 = Color(0xFF00201F)
internal val OnTeal30 = Color(0xFFCCE8E7)
internal val OnTeal80 = Color(0xFF00363A)  // dark onPrimary

// Neutral slates
internal val Slate10 = Color(0xFF171D1E)  // light onSurface
internal val Slate30 = Color(0xFF3F484A)  // light onSurfaceVariant-ish text
internal val SlateVariant = Color(0xFF5B6466) // light onSurfaceVariant
internal val NeutralBg = Color(0xFFF8FAFA) // light background (near white)
internal val NeutralSurface = Color(0xFFFFFFFF)
internal val NeutralSurfaceVariant = Color(0xFFECF2F1) // subtle differentiated surface
internal val Outline = Color(0xFF6F797A)
internal val OutlineVariant = Color(0xFFBEC8C9)

internal val DarkBg = Color(0xFF0E1415)
internal val DarkSurface = Color(0xFF151C1D)
internal val DarkSurfaceVariant = Color(0xFF3F484A)
internal val DarkOnSurface = Color(0xFFDDE4E3)
internal val DarkOnSurfaceVariant = Color(0xFFBEC8C9)
internal val DarkOutline = Color(0xFF899392)
internal val DarkOutlineVariant = Color(0xFF3F484A)

// Semantic
internal val ErrorLight = Color(0xFFBA1A1A)
internal val ErrorContainerLight = Color(0xFFFFDAD6)
internal val OnErrorContainerLight = Color(0xFF410002)
internal val ErrorDark = Color(0xFFFFB4AB)
internal val ErrorContainerDark = Color(0xFF93000A)
internal val OnErrorContainerDark = Color(0xFFFFDAD6)
