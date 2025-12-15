package com.Sourish25.MineBucks.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

object ExpressiveMotion {
    // Spatial Springs (for movement)
    val EmphasizedSpring = spring<Float>(
        dampingRatio = 0.7f,
        stiffness = 300f // Somewhat snappy
    )
    
    val StandardSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    // Effects Springs (for bounces/scales)
    val BouncySpring = spring<Float>(
        dampingRatio = 0.4f, // Very bouncy
        stiffness = 600f
    )

    // Easing Curves (Web compatibility / fallback)
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
}
