@file:OptInAppExperimentalApis

package com.rpeters.jellyfin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import com.rpeters.jellyfin.OptInAppExperimentalApis
import com.rpeters.jellyfin.ui.components.ExpressiveWavyLinearLoading

@Composable
fun AiSummaryCard(
    summary: String?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            if (isLoading) {
                ExpressiveWavyLinearLoading(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (summary != null) {
                Text(
                    text = summary.sanitizedAiSummary(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}

fun String.sanitizedAiSummary(): String =
    trim()
        .replace(Regex("\\n{3,}"), "\n\n")
