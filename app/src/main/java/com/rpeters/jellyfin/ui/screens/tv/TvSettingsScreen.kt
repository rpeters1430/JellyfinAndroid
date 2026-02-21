package com.rpeters.jellyfin.ui.screens.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rpeters.jellyfin.BuildConfig
import com.rpeters.jellyfin.ui.components.tv.TvImmersiveBackground
import com.rpeters.jellyfin.ui.viewmodel.ServerConnectionViewModel
import androidx.tv.material3.Button as TvButton
import androidx.tv.material3.ButtonDefaults as TvButtonDefaults
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.Text as TvText

@Composable
fun TvSettingsScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    connectionViewModel: ServerConnectionViewModel = hiltViewModel(),
) {
    val connectionState by connectionViewModel.connectionState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        TvImmersiveBackground(backdropUrl = null)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 56.dp)
                .padding(top = 48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            TvText(
                text = "Settings",
                style = TvMaterialTheme.typography.displaySmall,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsRow(label = "Server", value = connectionState.savedServerUrl.ifBlank { "Not connected" })
            SettingsRow(label = "Signed in as", value = connectionState.savedUsername.ifBlank { "Unknown" })
            SettingsRow(label = "App version", value = BuildConfig.VERSION_NAME)

            Spacer(modifier = Modifier.height(32.dp))

            TvButton(
                onClick = {
                    connectionViewModel.logout()
                    onSignOut()
                },
                colors = TvButtonDefaults.colors(
                    containerColor = TvMaterialTheme.colorScheme.error,
                    contentColor = TvMaterialTheme.colorScheme.onError,
                ),
            ) {
                TvText(text = "Sign Out")
            }
        }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TvText(
            text = label,
            style = TvMaterialTheme.typography.labelMedium,
            color = TvMaterialTheme.colorScheme.onSurfaceVariant,
        )
        TvText(
            text = value,
            style = TvMaterialTheme.typography.bodyLarge,
            color = Color.White,
        )
    }
}
