package com.rpeters.jellyfin.ui.player.dlna

import androidx.media3.common.util.UnstableApi

@UnstableApi
data class DlnaDevice(
    val friendlyName: String,
    val udn: String,
    val locationUrl: String,
    val avTransportControlUrl: String,
)

