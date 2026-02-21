package com.rpeters.jellyfin.ui.components.tv

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.rpeters.jellyfin.ui.image.JellyfinAsyncImage
import com.rpeters.jellyfin.ui.image.rememberScreenWidthHeight

/**
 * A background layer for TV screens that provides an immersive experience by
 * displaying a blurred, dimmed version of the currently focused item's backdrop.
 */
@Composable
fun TvImmersiveBackground(
    backdropUrl: String?,
    modifier: Modifier = Modifier,
    blurRadius: Int = 20,
    dimAmount: Float = 0.6f,
) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Crossfade(
            targetState = backdropUrl,
            animationSpec = tween(durationMillis = 1000),
            label = "background_crossfade"
        ) { url ->
            if (url != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    JellyfinAsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(blurRadius.dp),
                        requestSize = rememberScreenWidthHeight(200.dp) // Lower res for blur
                    )
                    
                    // Dimming overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = dimAmount))
                    )
                    
                    // Optional: Vignette or Gradient for better text readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.4f)
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}
