package com.rpeters.jellyfin.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun ExpressiveFloatingToolbar(
    isVisible: Boolean,
    onPlayClick: () -> Unit,
    onQueueClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onCastClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onShareClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryAction: ToolbarAction = ToolbarAction.PLAY,
    showQueueAction: Boolean = true,
    showCastAction: Boolean = true,
    showShareAction: Boolean = true,
    showMoreAction: Boolean = true,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 6.dp,
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FloatingActionButton(
                    onClick = when (primaryAction) {
                        ToolbarAction.PLAY -> onPlayClick
                        ToolbarAction.DOWNLOAD -> onDownloadClick
                        ToolbarAction.FAVORITE -> onFavoriteClick
                    },
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        imageVector = when (primaryAction) {
                            ToolbarAction.PLAY -> Icons.Default.PlayArrow
                            ToolbarAction.DOWNLOAD -> Icons.Default.Download
                            ToolbarAction.FAVORITE -> Icons.Default.Favorite
                        },
                        contentDescription = null,
                    )
                }

                if (showQueueAction) {
                    ExpressiveToolbarButton(
                        icon = Icons.Default.Queue,
                        contentDescription = "Add to Queue",
                        onClick = onQueueClick,
                    )
                }
                if (showCastAction) {
                    ExpressiveToolbarButton(
                        icon = Icons.Default.Cast,
                        contentDescription = "Cast",
                        onClick = onCastClick,
                    )
                }
                if (showShareAction) {
                    ExpressiveToolbarButton(
                        icon = Icons.Default.Share,
                        contentDescription = "Share",
                        onClick = onShareClick,
                    )
                }
                if (showMoreAction) {
                    ExpressiveToolbarButton(
                        icon = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        onClick = onMoreClick,
                    )
                }
            }
        }
    }
}

@Composable
fun ExpressiveCompactToolbar(
    isVisible: Boolean,
    actions: List<ToolbarActionItem>,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 6.dp,
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actions.forEach { action ->
                    ExpressiveToolbarButton(
                        icon = action.icon,
                        contentDescription = action.contentDescription,
                        onClick = action.onClick,
                        isActive = action.isActive,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpressiveToolbarButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(40.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun ExpressiveVideoToolbar(
    isVisible: Boolean,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onSkipPreviousClick: () -> Unit,
    onSkipNextClick: () -> Unit,
    onCastClick: () -> Unit,
    onPictureInPictureClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.8f),
                        ),
                    ),
                ),
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .shadow(8.dp, MaterialTheme.shapes.extraLarge),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SmallFloatingActionButton(onClick = onSkipPreviousClick) {
                        Icon(Icons.Default.Queue, contentDescription = "Skip Previous")
                    }
                    FloatingActionButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier.size(64.dp),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    SmallFloatingActionButton(onClick = onSkipNextClick) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Skip Next")
                    }
                    IconButton(onClick = onCastClick) {
                        Icon(Icons.Default.Cast, contentDescription = "Cast")
                    }
                    IconButton(onClick = onPictureInPictureClick) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Picture in picture")
                    }
                }
            }
        }
    }
}

enum class ToolbarAction {
    PLAY, DOWNLOAD, FAVORITE
}

data class ToolbarActionItem(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
    val isActive: Boolean = false,
)
