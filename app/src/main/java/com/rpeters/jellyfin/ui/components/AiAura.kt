package com.rpeters.jellyfin.ui.components

import androidx.compose.animation.core.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale

/**
 * A pulsing glow effect for AI-powered components.
 * Kept subtle to avoid visual bleed into adjacent content.
 */
@Composable
fun Modifier.aiAura(
    enabled: Boolean = true,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.tertiary,
): Modifier {
    if (!enabled) return this

    val infiniteTransition = rememberInfiniteTransition(label = "ai_aura")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    return this.drawBehind {
        val radius = size.minDimension / 2 * pulseScale
        val brush = Brush.sweepGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.2f),
                secondaryColor.copy(alpha = 0.2f),
                primaryColor.copy(alpha = 0.2f),
            )
        )
        
        scale(pulseScale) {
            drawCircle(
                brush = brush,
                radius = radius,
                center = center,
                alpha = 0.15f
            )
        }
    }
}
