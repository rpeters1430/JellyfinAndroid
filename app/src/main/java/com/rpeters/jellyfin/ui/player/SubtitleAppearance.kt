package com.rpeters.jellyfin.ui.player

import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.rpeters.jellyfin.data.preferences.SubtitleAppearancePreferences
import com.rpeters.jellyfin.data.preferences.SubtitleBackground
import com.rpeters.jellyfin.data.preferences.SubtitleFont

internal fun applySubtitleAppearance(
    playerView: PlayerView,
    preferences: SubtitleAppearancePreferences,
) {
    val subtitleView = playerView.subtitleView ?: return
    val backgroundColor = when (preferences.background) {
        SubtitleBackground.NONE -> Color.TRANSPARENT
        SubtitleBackground.BLACK -> Color.BLACK
        SubtitleBackground.SEMI_TRANSPARENT -> Color.parseColor("#80000000")
    }
    val typeface = when (preferences.font) {
        SubtitleFont.DEFAULT -> Typeface.DEFAULT
        SubtitleFont.SANS_SERIF -> Typeface.SANS_SERIF
        SubtitleFont.SERIF -> Typeface.SERIF
        SubtitleFont.MONOSPACE -> Typeface.MONOSPACE
    }
    val style = CaptionStyleCompat(
        Color.WHITE,
        backgroundColor,
        Color.TRANSPARENT,
        CaptionStyleCompat.EDGE_TYPE_NONE,
        Color.BLACK,
        typeface,
    )
    subtitleView.setStyle(style)
    subtitleView.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, preferences.textSize.sizeSp)
}
