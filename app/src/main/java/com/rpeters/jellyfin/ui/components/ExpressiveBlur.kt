package com.rpeters.jellyfin.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * A surface that provides a semi-transparent frosted appearance.
 * On Android 12+ a subtle blur was previously used here, but it caused rendering
 * artefacts (glow/bleed) on adjacent composables. The blur has been replaced with a
 * plain translucent surface colour which achieves a similar look without side effects.
 *
 * @param modifier The modifier to be applied to the surface.
 * @param shape The shape of the surface.
 * @param color The background color (should be translucent for best effect).
 * @param content The content to be displayed on the surface.
 */
@Composable
fun ExpressiveBlurSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    color: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier.clip(shape),
        shape = shape,
        color = color,
    ) {
        Box(content = content)
    }
}

