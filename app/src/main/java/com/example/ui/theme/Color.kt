package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// High Density Design Theme Colors
val HighDensityPrimary = Color(0xFF6750A4)
val HighDensityOnPrimary = Color(0xFFFFFFFF)
val HighDensityPrimaryContainer = Color(0xFFEADDFF)
val HighDensityOnPrimaryContainer = Color(0xFF21005D)

val HighDensitySecondary = Color(0xFF625B71)
val HighDensityOnSecondary = Color(0xFFFFFFFF)
val HighDensitySecondaryContainer = Color(0xFFF3EDF7)
val HighDensityOnSecondaryContainer = Color(0xFF1C1B1F)

val HighDensityBackground = Color(0xFFFDF8F6)
val HighDensityOnBackground = Color(0xFF1C1B1F)

val HighDensitySurface = Color(0xFFFFFFFF)
val HighDensityOnSurface = Color(0xFF1C1B1F)
val HighDensitySurfaceVariant = Color(0xFFF7F2FA)
val HighDensityOnSurfaceVariant = Color(0xFF49454F)

val HighDensityOutline = Color(0xFFE6E1E5)

// Aliases for compatibility with CPDMSScreens.kt and other views
val ConstructionBlue = HighDensityPrimary
val SafetyOrange = HighDensityOnPrimaryContainer // Deep contrast purple #21005D
val SafetyYellow = HighDensityPrimaryContainer    // Soft container purple #EADDFF

val SlateLightBg = HighDensityBackground
val SlateDarkBg = Color(0xFF141218) // M3 dark theme default background

val CardLightBg = HighDensitySurface
val CardDarkBg = Color(0xFF1D1B20)

val NeutralDark = HighDensityOnBackground
val NeutralLight = HighDensitySurfaceVariant

// Status Colors
val StateApproved = Color(0xFF10B981)         // Success green
val StateRejected = Color(0xFFEF4444)         // Error red
val StatePending = Color(0xFFF59E0B)          // Warning orange
val StateProgress = Color(0xFF3B82F6)         // Info blue
