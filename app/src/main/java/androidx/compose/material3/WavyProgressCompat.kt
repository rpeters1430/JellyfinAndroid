package androidx.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CircularWavyProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.circularColor,
    trackColor: Color = ProgressIndicatorDefaults.circularIndeterminateTrackColor,
    amplitude: Float = 0f,
    wavelength: Dp = 0.dp,
    waveSpeed: Dp = 0.dp,
) {
    CircularProgressIndicator(
        modifier = modifier,
        color = color,
        trackColor = trackColor,
    )
}

@Composable
fun CircularWavyProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.circularColor,
    trackColor: Color = ProgressIndicatorDefaults.circularDeterminateTrackColor,
    amplitude: (Float) -> Float = { 0f },
    wavelength: Dp = 0.dp,
    waveSpeed: Dp = 0.dp,
) {
    CircularProgressIndicator(
        progress = progress,
        modifier = modifier,
        color = color,
        trackColor = trackColor,
    )
}

@Composable
fun LinearWavyProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
    amplitude: Float = 0f,
    wavelength: Dp = 0.dp,
    waveSpeed: Dp = 0.dp,
) {
    LinearProgressIndicator(
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        strokeCap = StrokeCap.Round,
    )
}

@Composable
fun LinearWavyProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
    amplitude: (Float) -> Float = { 0f },
    wavelength: Dp = 0.dp,
    waveSpeed: Dp = 0.dp,
) {
    LinearProgressIndicator(
        progress = progress,
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        strokeCap = StrokeCap.Round,
    )
}
