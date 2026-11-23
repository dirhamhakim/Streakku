package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Uses locally bundled DM Sans as the closest geometric-humanist sans-serif match to Bit Sans
// (Bit Sans / Bitsans is not hosted on Google Fonts; drop bitsans.ttf in res/font to swap directly).
val BitSansFallbackFamily = FontFamily(
    Font(R.font.dm_sans, FontWeight.Normal),
    Font(R.font.dm_sans, FontWeight.Medium),
    Font(R.font.dm_sans, FontWeight.SemiBold),
    Font(R.font.dm_sans, FontWeight.Bold)
)

val StreakkuTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = BitSansFallbackFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = BitSansFallbackFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.2).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = BitSansFallbackFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = BitSansFallbackFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = BitSansFallbackFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = BitSansFallbackFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = BitSansFallbackFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = BitSansFallbackFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = BitSansFallbackFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = BitSansFallbackFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = BitSansFallbackFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = BitSansFallbackFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)
