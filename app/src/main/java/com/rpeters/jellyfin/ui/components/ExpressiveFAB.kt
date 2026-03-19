package com.rpeters.jellyfin.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.rpeters.jellyfin.ui.theme.MotionTokens

@Composable
fun ExpressiveFABMenu(
    modifier: Modifier = Modifier,
    onPlayClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
    onQueueClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    isVisible: Boolean = true,
) {
    var isExpanded by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(MotionTokens.fabMenuExpand) + fadeIn(),
        exit = scaleOut(MotionTokens.fabMenuCollapse) + fadeOut(),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AnimatedVisibility(visible = isExpanded, enter = fadeIn(), exit = fadeOut()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExpressiveFabAction(
                        icon = Icons.Default.PlayArrow,
                        label = "Play Now",
                        onClick = {
                            onPlayClick()
                            isExpanded = false
                        },
                    )
                    ExpressiveFabAction(
                        icon = Icons.Default.Queue,
                        label = "Add to Queue",
                        onClick = {
                            onQueueClick()
                            isExpanded = false
                        },
                    )
                    ExpressiveFabAction(
                        icon = Icons.Default.Download,
                        label = "Download",
                        onClick = {
                            onDownloadClick()
                            isExpanded = false
                        },
                    )
                    ExpressiveFabAction(
                        icon = Icons.Default.Favorite,
                        label = "Add to Favorites",
                        onClick = {
                            onFavoriteClick()
                            isExpanded = false
                        },
                    )
                }
            }

            FloatingActionButton(onClick = { isExpanded = !isExpanded }) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = if (isExpanded) "Close menu" else "Open menu",
                )
            }
        }
    }
}

@Composable
private fun ExpressiveFabAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    SmallFloatingActionButton(onClick = onClick) {
        Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun ExpressiveExtendedFAB(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        expanded = expanded,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        },
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
            )
        },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier,
    )
}
