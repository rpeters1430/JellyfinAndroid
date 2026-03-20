package com.rpeters.jellyfin.ui.screens.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rpeters.jellyfin.OptInAppExperimentalApis
import com.rpeters.jellyfin.ui.components.ExpressiveBackNavigationIcon
import com.rpeters.jellyfin.ui.components.ExpressiveMediaListItem
import com.rpeters.jellyfin.ui.components.ExpressiveTopAppBar

@OptInAppExperimentalApis
@Composable
fun SettingsSectionScreen(
    @StringRes titleRes: Int,
    @StringRes descriptionRes: Int,
    optionRes: List<Int>,
    onNavigateBack: () -> Unit,
    onOptionClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            ExpressiveTopAppBar(
                title = stringResource(titleRes),
                navigationIcon = {
                    ExpressiveBackNavigationIcon(onClick = onNavigateBack)
                }
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(descriptionRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                optionRes.forEach { option ->
                    ExpressiveMediaListItem(
                        title = stringResource(option),
                        leadingIcon = Icons.Default.Settings,
                        onClick = { onOptionClick(option) },
                    )
                }
            }
        }
    }
}
