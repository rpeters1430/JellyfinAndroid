package com.rpeters.jellyfin.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rpeters.jellyfin.ui.components.immersive.HdrBadge
import com.rpeters.jellyfin.ui.components.immersive.HdrType
import com.rpeters.jellyfin.ui.components.immersive.QualityBadge
import com.rpeters.jellyfin.ui.components.immersive.ResolutionQuality
import com.rpeters.jellyfin.ui.utils.findDefaultVideoStream
import org.jellyfin.sdk.model.api.BaseItemDto

@Composable
fun MediaInfoIcons(
    item: BaseItemDto?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 20.dp,
) {
    if (item == null) return

    val videoStream = item.mediaSources?.firstOrNull()?.mediaStreams?.findDefaultVideoStream()
    val width = videoStream?.width
    val height = videoStream?.height
    val resolution = ResolutionQuality.fromResolution(width, height)
    
    val videoRange = videoStream?.videoRange?.toString()
    val videoRangeType = videoStream?.videoRangeType?.toString()
    val hdrType = HdrType.detect(videoRange, videoRangeType)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QualityBadge(
            resolution = resolution,
            showIcon = true,
            animate = false
        )
        
        if (hdrType != null) {
            HdrBadge(hdrType = hdrType)
        }
    }
}
