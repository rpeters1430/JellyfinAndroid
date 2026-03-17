package com.rpeters.jellyfin.ui.player

import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi

/**
 * Factory for creating MediaItem instances with support for side-loaded subtitles
 * and proper MIME type hints for HLS/DASH content.
 */
@UnstableApi
object MediaItemFactory {

    /**
     * Build a MediaItem with optional side-loaded subtitle tracks and MIME type hint
     */
    fun build(
        videoUrl: String,
        title: String?,
        sideLoadedSubs: List<SubtitleSpec> = emptyList(),
        mimeTypeHint: String? = null,
    ): MediaItem {
        val builder = MediaItem.Builder()
            .setUri(videoUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .build(),
            )

        // If you know it's HLS or DASH, set the correct MIME type
        mimeTypeHint?.let { builder.setMimeType(it) }

        // Add side-loaded subtitle configurations
        if (sideLoadedSubs.isNotEmpty()) {
            val subtitleConfigurations = sideLoadedSubs.map { sub ->
                MediaItem.SubtitleConfiguration.Builder(sub.url.toUri())
                    .setMimeType(sub.mimeType)
                    .setLanguage(sub.language)
                    .setLabel(sub.label ?: sub.language?.uppercase() ?: "Subtitles")
                    .setSelectionFlags(if (sub.isForced) C.SELECTION_FLAG_FORCED else 0)
                    .build()
            }
            builder.setSubtitleConfigurations(subtitleConfigurations)
        }

        return builder.build()
    }

    /**
     * Infer MIME type from URL for proper Media3 pipeline selection
     */
    fun inferMimeType(url: String): String = when {
        url.endsWith(".m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
        url.endsWith(".mpd", ignoreCase = true) -> MimeTypes.APPLICATION_MPD
        else -> MimeTypes.VIDEO_MP4 // safe default
    }

    /**
     * Get MIME type from Jellyfin container format for accurate Media3 pipeline selection
     */
    fun mimeTypeFromContainer(container: String?): String = when (container?.lowercase()) {
        "mkv", "matroska" -> MimeTypes.VIDEO_MATROSKA
        "mp4" -> MimeTypes.VIDEO_MP4
        "avi" -> MimeTypes.VIDEO_UNKNOWN // Media3 doesn't have specific AVI support, let it auto-detect
        "mov" -> MimeTypes.VIDEO_MP4 // MOV is similar to MP4
        "webm" -> MimeTypes.VIDEO_WEBM
        "m4v" -> MimeTypes.VIDEO_MP4
        "3gp" -> MimeTypes.VIDEO_UNKNOWN
        "flv" -> MimeTypes.VIDEO_UNKNOWN
        "wmv" -> MimeTypes.VIDEO_UNKNOWN
        "ts", "m2ts" -> MimeTypes.VIDEO_UNKNOWN // Let ExoPlayer auto-detect TS streams
        "m3u8" -> MimeTypes.APPLICATION_M3U8
        "mpd" -> MimeTypes.APPLICATION_MPD
        else -> MimeTypes.VIDEO_UNKNOWN // Let ExoPlayer auto-detect unknown formats
    }
}
