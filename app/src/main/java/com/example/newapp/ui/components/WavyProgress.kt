package com.example.newapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun LinearWavyProgressIndicator(
    progress: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    wavelength: Dp = 20.dp,
    amplitude: Dp = 4.dp, // Height of the wave
    strokeWidth: Dp = 4.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val midHeight = height / 2

        // Draw Track (Still Line)
        drawLine(
            color = trackColor,
            start = Offset(0f, midHeight),
            end = Offset(width, midHeight),
            strokeWidth = strokeWidth.toPx(),
            cap = StrokeCap.Round
        )

        // Draw Progress (Wavy Line)
        if (progress > 0) {
            val path = Path()
            val waveLenPx = wavelength.toPx()
            val ampPx = amplitude.toPx()
            val progressWidth = width * progress

            path.moveTo(0f, midHeight + ampPx * sin(phase))

            var x = 0f
            while (x <= progressWidth) {
                val y = midHeight + ampPx * sin((2 * Math.PI * x / waveLenPx) + phase).toFloat()
                path.lineTo(x, y)
                x += 1f // Step size
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}
