package com.rpeters.jellyfin.ui.screens.details.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Sd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rpeters.jellyfin.R
import com.rpeters.jellyfin.ui.theme.Quality1440
import com.rpeters.jellyfin.ui.theme.Quality4K
import com.rpeters.jellyfin.ui.theme.QualityHD
import com.rpeters.jellyfin.ui.theme.QualitySD

@Composable
fun DetailVideoInfoRow(
    label: String,
    codec: String?,
    icon: ImageVector,
    resolutionBadge: Triple<ImageVector, String, Color>? = null,
    is3D: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(8.dp),
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                codec?.let { codecText ->
                    Text(
                        text = codecText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                } ?: run {
                    Text(
                        text = stringResource(id = R.string.unknown),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                // 3D Badge
                if (is3D) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier,
                    ) {
                        Text(
                            text = "3D",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        )
                    }
                }

                // Quality badge (4K, FHD, HD, SD)
                resolutionBadge?.let { (icon, label, color) ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = color,
                        modifier = Modifier,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "$label quality",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailSubtitleRow(
    subtitles: List<org.jellyfin.sdk.model.api.MediaStream>,
    selectedSubtitleIndex: Int?,
    onSubtitleSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentSubtitle = if (selectedSubtitleIndex != null) {
        subtitles.find { it.index == selectedSubtitleIndex }
    } else {
        null
    }

    val labelText = currentSubtitle?.let {
        val lang = it.language ?: "UNK"
        val title = it.title ?: it.displayTitle
        if (title != null && title != lang) "$lang - $title" else lang
    } ?: "None"

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.ClosedCaption,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(8.dp),
            )
        }

        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable { expanded = true },
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "Subtitles",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = labelText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.none)) },
                    onClick = {
                        onSubtitleSelect(null)
                        expanded = false
                    },
                )

                subtitles.forEach { stream ->
                    DropdownMenuItem(
                        text = {
                            val lang = stream.language ?: "UNK"
                            val title = stream.title ?: stream.displayTitle
                            Text(if (title != null && title != lang) "$lang - $title" else lang)
                        },
                        onClick = {
                            onSubtitleSelect(stream.index)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

fun getResolutionIcon(width: Int?, height: Int?): ImageVector {
    return Icons.Rounded.Movie
}

fun getResolutionBadge(width: Int?, height: Int?): Triple<ImageVector, String, Color>? {
    val w = width ?: 0
    val h = height ?: 0

    return when {
        h >= 4320 || w >= 7680 -> Triple(Icons.Rounded.HighQuality, "8K", Quality4K)
        h >= 2160 || w >= 3840 -> Triple(Icons.Rounded.HighQuality, "4K", Quality4K)
        h >= 1440 || w >= 2560 -> Triple(Icons.Rounded.HighQuality, "1440p", Quality1440)
        h >= 1080 || w >= 1920 -> Triple(Icons.Rounded.Hd, "FHD", QualityHD)
        h >= 720 || w >= 1280 -> Triple(Icons.Rounded.Hd, "HD", QualityHD)
        h > 0 -> Triple(Icons.Rounded.Sd, "SD", QualitySD)
        else -> null
    }
}
