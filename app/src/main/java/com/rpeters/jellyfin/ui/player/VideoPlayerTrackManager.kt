package com.rpeters.jellyfin.ui.player

import androidx.media3.common.Format
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.rpeters.jellyfin.utils.SecureLogger
import java.util.Locale
import javax.inject.Inject

/**
 * Manages audio tracks, subtitle tracks, and video quality selection.
 */
@UnstableApi
class VideoPlayerTrackManager @Inject constructor(
    private val stateManager: VideoPlayerStateManager
) {
    fun updateTracks(tracks: Tracks) {
        val audio = mutableListOf<TrackInfo>()
        val text = mutableListOf<TrackInfo>()
        var isHdr = false

        tracks.groups.forEachIndexed { groupIndex, group ->
            val trackType = group.type
            val mediaGroup = group.mediaTrackGroup
            for (i in 0 until mediaGroup.length) {
                val format = mediaGroup.getFormat(i)
                val isSelected = group.isTrackSelected(i)
                val display = buildTrackDisplayName(format, i, trackType)

                val info = TrackInfo(groupIndex, i, format, isSelected, display)

                when (trackType) {
                    androidx.media3.common.C.TRACK_TYPE_AUDIO -> audio += info
                    androidx.media3.common.C.TRACK_TYPE_TEXT -> text += info
                    androidx.media3.common.C.TRACK_TYPE_VIDEO -> {
                        if (isSelected && isHdrFormat(format)) isHdr = true
                    }
                }
            }
        }

        stateManager.updateState { it.copy(
            availableAudioTracks = audio,
            selectedAudioTrack = audio.firstOrNull { it.isSelected },
            availableSubtitleTracks = text,
            selectedSubtitleTrack = text.firstOrNull { it.isSelected },
            isHdrContent = isHdr
        ) }

        updateAvailableQualities(tracks)
    }

    private fun updateAvailableQualities(tracks: Tracks) {
        val qualities = mutableListOf<VideoQuality>()
        for (trackGroup in tracks.groups) {
            if (trackGroup.type == androidx.media3.common.C.TRACK_TYPE_VIDEO) {
                for (i in 0 until trackGroup.length) {
                    val format = trackGroup.getTrackFormat(i)
                    val width = format.width
                    val height = format.height
                    val bitrate = format.bitrate

                    if (width > 0 && height > 0) {
                        val label = when {
                            height >= 2160 -> "4K (${width}x$height)"
                            height >= 1440 -> "1440p"
                            height >= 1080 -> "1080p"
                            height >= 720 -> "720p"
                            height >= 480 -> "480p"
                            height >= 360 -> "360p"
                            else -> "${height}p"
                        }
                        val quality = VideoQuality("${width}x$height", label, if (bitrate > 0) bitrate else 0, width, height)
                        if (qualities.none { it.width == width && it.height == height }) {
                            qualities.add(quality)
                        }
                    }
                }
            }
        }
        qualities.sortByDescending { it.height }
        stateManager.updateState { it.copy(availableQualities = qualities) }
    }

    fun buildTrackDisplayName(format: Format, index: Int, trackType: Int): String {
        val label = format.label?.trim().orEmpty()
        val languageName = format.language?.toDisplayLanguage()
        val primary = when {
            label.isNotBlank() && label.length > 3 -> label
            languageName != null -> languageName
            label.isNotBlank() -> label.uppercase()
            else -> null
        } ?: "Track ${index + 1}"

        val descriptor = label.takeIf {
            it.isNotBlank() && !it.equals(primary, true) && !it.equals(format.language, true)
        }

        return buildString {
            append(primary)
            if (descriptor != null) append(" ($descriptor)")
            if (trackType == androidx.media3.common.C.TRACK_TYPE_AUDIO) {
                if (format.channelCount != Format.NO_VALUE && format.channelCount > 0) {
                    append(" • ${format.channelCount}ch")
                }
                if (format.sampleRate != Format.NO_VALUE && format.sampleRate > 0) {
                    append(" • ${format.sampleRate}Hz")
                }
            }
        }.ifBlank { "Track ${index + 1}" }
    }

    private fun isHdrFormat(format: Format): Boolean {
        val colorInfo = format.colorInfo ?: return false
        return when (colorInfo.colorTransfer) {
            androidx.media3.common.C.COLOR_TRANSFER_ST2084,
            androidx.media3.common.C.COLOR_TRANSFER_HLG -> true
            else -> false
        }
    }

    private fun String.toDisplayLanguage(): String? {
        if (isBlank() || equals("und", true)) return null
        val locale = Locale.forLanguageTag(this)
        return locale.getDisplayName(Locale.getDefault()).trim().takeIf { it.isNotBlank() }
    }

    fun changeQuality(quality: VideoQuality?, trackSelector: DefaultTrackSelector?, exoPlayer: ExoPlayer?) {
        val selector = trackSelector ?: return
        val player = exoPlayer ?: return

        if (quality == null) {
            selector.setParameters(selector.buildUponParameters().clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_VIDEO).build())
            stateManager.updateState { it.copy(selectedQuality = null) }
        } else {
            val currentTracks = player.currentTracks
            var trackSelected = false
            for (trackGroup in currentTracks.groups) {
                if (trackGroup.type == androidx.media3.common.C.TRACK_TYPE_VIDEO) {
                    for (i in 0 until trackGroup.length) {
                        val format = trackGroup.getTrackFormat(i)
                        if (format.width == quality.width && format.height == quality.height) {
                            val override = androidx.media3.common.TrackSelectionOverride(trackGroup.mediaTrackGroup, listOf(i))
                            selector.setParameters(selector.buildUponParameters().clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_VIDEO).addOverride(override).build())
                            stateManager.updateState { it.copy(selectedQuality = quality) }
                            trackSelected = true
                            break
                        }
                    }
                }
                if (trackSelected) break
            }
        }
    }
}
