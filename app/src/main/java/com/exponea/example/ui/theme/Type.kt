package com.exponea.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val BloomreachTypography = Typography(
    titleMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = BrBlack
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        color = BrBlack
    ),
    labelMedium = TextStyle(
        fontSize = 13.sp,
        color = BrGrey600
    ),
    labelSmall = TextStyle(
        fontSize = 12.sp,
        color = BrGrey600
    )
)

val MonospaceCodeStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 12.sp,
    color = BrBlack
)
