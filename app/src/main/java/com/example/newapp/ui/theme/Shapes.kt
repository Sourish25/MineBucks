package com.example.newapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp), // More playful rounding
    large = RoundedCornerShape(32.dp),   // Very round for cards
    extraLarge = RoundedCornerShape(48.dp)
)
