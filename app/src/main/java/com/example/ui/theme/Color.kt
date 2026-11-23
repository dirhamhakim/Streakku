package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Exact Light Mode tokens from user UI/UX mockups
val GreenPrimaryLight = Color(0xFF2E9E5B)
val GreenOnPrimaryLight = Color(0xFFFFFFFF)
val GreenContainerLight = Color(0xFFE3F3EA)
val GreenOnContainerLight = Color(0xFF2E9E5B)

val BackgroundLight = Color(0xFFF6F7F4)
val OnBackgroundLight = Color(0xFF1B1F23)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF1B1F23)
val SurfaceVariantLight = Color(0xFFEEEFE9)
val OnSurfaceVariantLight = Color(0xFF6B7280)
val OutlineLight = Color(0xFFC9CDD2)
val OutlineVariantLight = Color(0xFFE6E8EB)

val ErrorLight = Color(0xFFE5484D)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFDECEC)
val OnErrorContainerLight = Color(0xFFB42328)

// Exact Dark Mode tokens from user UI/UX mockups
val GreenPrimaryDark = Color(0xFF3DBE76)
val GreenOnPrimaryDark = Color(0xFFFFFFFF)
val GreenContainerDark = Color(0xFF1F3A2B)
val GreenOnContainerDark = Color(0xFF3DBE76)

val BackgroundDark = Color(0xFF111412)
val OnBackgroundDark = Color(0xFFF1F3F2)
val SurfaceDark = Color(0xFF1C201D)
val OnSurfaceDark = Color(0xFFF1F3F2)
val SurfaceVariantDark = Color(0xFF252B27)
val OnSurfaceVariantDark = Color(0xFF9AA3A0)
val OutlineDark = Color(0xFF4A524E)
val OutlineVariantDark = Color(0xFF2B312D)

val ErrorDark = Color(0xFFF2686C)
val OnErrorDark = Color(0xFFFFFFFF)
val ErrorContainerDark = Color(0xFF3D1D1F)
val OnErrorContainerDark = Color(0xFFFCA5A5)

data class StatusColors(
    val done: Color,
    val onDone: Color,
    val doneContainer: Color,
    val onDoneContainer: Color,
    val doneStreakLine: Color,
    val bannerSubtitle: Color,
    val bannerProgressTrack: Color,
    val failed: Color,
    val onFailed: Color,
    val failedContainer: Color,
    val onFailedContainer: Color,
    val empty: Color,
    val emptyRing: Color,
    val onEmpty: Color,
    val emptyContainer: Color,
    val onEmptyContainer: Color,
    val inactiveText: Color,
    val streakFlame: Color
)

val LightStatusColors = StatusColors(
    done = Color(0xFF2E9E5B),
    onDone = Color(0xFFFFFFFF),
    doneContainer = Color(0xFFE3F3EA),
    onDoneContainer = Color(0xFF2E9E5B),
    doneStreakLine = Color(0xFF2E9E5B),
    bannerSubtitle = Color(0xFFDFF3E7),
    bannerProgressTrack = Color(0xFF5DBB84),
    failed = Color(0xFFE5484D),
    onFailed = Color(0xFFFFFFFF),
    failedContainer = Color(0xFFFDECEC),
    onFailedContainer = Color(0xFFB42328),
    empty = Color(0xFFC9CDD2),
    emptyRing = Color(0xFFC9CDD2),
    onEmpty = Color(0xFFFFFFFF),
    emptyContainer = Color(0xFFF1F3F5),
    onEmptyContainer = Color(0xFF6B7280),
    inactiveText = Color(0xFFB8BDC4),
    streakFlame = Color(0xFF2E9E5B)
)

val DarkStatusColors = StatusColors(
    done = Color(0xFF3DBE76),
    onDone = Color(0xFFFFFFFF),
    doneContainer = Color(0xFF1F3A2B),
    onDoneContainer = Color(0xFF3DBE76),
    doneStreakLine = Color(0xFF3DBE76),
    bannerSubtitle = Color(0xFFD6F0E2),
    bannerProgressTrack = Color(0xFF2F8F58),
    failed = Color(0xFFF2686C),
    onFailed = Color(0xFFFFFFFF),
    failedContainer = Color(0xFF3D1D1F),
    onFailedContainer = Color(0xFFF2686C),
    empty = Color(0xFF4A524E),
    emptyRing = Color(0xFF4A524E),
    onEmpty = Color(0xFFFFFFFF),
    emptyContainer = Color(0xFF252B27),
    onEmptyContainer = Color(0xFF9AA3A0),
    inactiveText = Color(0xFF4A524E),
    streakFlame = Color(0xFF3DBE76)
)

// Exact 6 habit accent colors from Form Kegiatan mockups
val HabitColorPalette: List<String> = listOf(
    "#2E9E5B", // Hijau
    "#3B82F6", // Biru
    "#F59E0B", // Oranye
    "#8B5CF6", // Ungu
    "#06B6D4", // Biru muda / Cyan
    "#EC4899"  // Merah muda
)

fun parseHexColor(hex: String, fallback: Color = GreenPrimaryLight): Color {
    return try {
        val cleaned = hex.trim().removePrefix("#")
        val value = cleaned.toLong(16)
        when (cleaned.length) {
            6 -> Color(0xFF000000L or value)
            8 -> Color(value)
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}
