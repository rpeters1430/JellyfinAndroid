package com.rpeters.jellyfin.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rpeters.jellyfin.OptInAppExperimentalApis
import com.rpeters.jellyfin.R
import com.rpeters.jellyfin.data.preferences.SubtitleBackground
import com.rpeters.jellyfin.data.preferences.SubtitleFont
import com.rpeters.jellyfin.data.preferences.SubtitleTextSize
import com.rpeters.jellyfin.ui.components.ExpressiveRadioListItem
import com.rpeters.jellyfin.ui.viewmodel.SubtitleAppearancePreferencesViewModel

@OptInAppExperimentalApis
@Composable
fun SubtitleSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SubtitleAppearancePreferencesViewModel = hiltViewModel(),
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_subtitles_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SettingsSection(
                title = stringResource(R.string.settings_subtitles_text_size),
                icon = Icons.Default.FormatSize,
            ) {
                SubtitleTextSize.entries.forEach { size ->
                    ExpressiveRadioListItem(
                        title = subtitleTextSizeLabel(size),
                        selected = preferences.textSize == size,
                        onSelect = { viewModel.setTextSize(size) },
                    )
                }
            }

            SettingsSection(
                title = stringResource(R.string.settings_subtitles_font),
                icon = Icons.Default.FontDownload,
            ) {
                SubtitleFont.entries.forEach { font ->
                    ExpressiveRadioListItem(
                        title = subtitleFontLabel(font),
                        selected = preferences.font == font,
                        onSelect = { viewModel.setFont(font) },
                    )
                }
            }

            SettingsSection(
                title = stringResource(R.string.settings_subtitles_background),
                icon = Icons.Default.Layers,
            ) {
                SubtitleBackground.entries.forEach { background ->
                    ExpressiveRadioListItem(
                        title = subtitleBackgroundLabel(background),
                        selected = preferences.background == background,
                        onSelect = { viewModel.setBackground(background) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            content()
        }
    }
}

@Composable
private fun subtitleTextSizeLabel(size: SubtitleTextSize): String {
    return when (size) {
        SubtitleTextSize.SMALL -> stringResource(R.string.settings_subtitles_text_size_small)
        SubtitleTextSize.MEDIUM -> stringResource(R.string.settings_subtitles_text_size_medium)
        SubtitleTextSize.LARGE -> stringResource(R.string.settings_subtitles_text_size_large)
        SubtitleTextSize.EXTRA_LARGE -> stringResource(R.string.settings_subtitles_text_size_extra_large)
    }
}

@Composable
private fun subtitleFontLabel(font: SubtitleFont): String {
    return when (font) {
        SubtitleFont.DEFAULT -> stringResource(R.string.settings_subtitles_font_default)
        SubtitleFont.SANS_SERIF -> stringResource(R.string.settings_subtitles_font_sans_serif)
        SubtitleFont.SERIF -> stringResource(R.string.settings_subtitles_font_serif)
        SubtitleFont.MONOSPACE -> stringResource(R.string.settings_subtitles_font_monospace)
        SubtitleFont.ROBOTO -> stringResource(R.string.settings_subtitles_font_roboto)
        SubtitleFont.ROBOTO_FLEX -> stringResource(R.string.settings_subtitles_font_roboto_flex)
        SubtitleFont.ROBOTO_SERIF -> stringResource(R.string.settings_subtitles_font_roboto_serif)
        SubtitleFont.ROBOTO_MONO -> stringResource(R.string.settings_subtitles_font_roboto_mono)
    }
}

@Composable
private fun subtitleBackgroundLabel(background: SubtitleBackground): String {
    return when (background) {
        SubtitleBackground.NONE -> stringResource(R.string.settings_subtitles_background_none)
        SubtitleBackground.BLACK -> stringResource(R.string.settings_subtitles_background_black)
        SubtitleBackground.SEMI_TRANSPARENT -> stringResource(R.string.settings_subtitles_background_semi)
    }
}
