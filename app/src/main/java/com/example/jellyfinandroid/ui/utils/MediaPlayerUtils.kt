package com.example.jellyfinandroid.ui.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import org.jellyfin.sdk.model.api.BaseItemDto

object MediaPlayerUtils {
    
    /**
     * Launches an external media player with the given stream URL
     */
    fun playMedia(context: Context, streamUrl: String, item: BaseItemDto) {
        try {
            Log.d("MediaPlayerUtils", "Attempting to play: ${item.name} with URL: $streamUrl")
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(streamUrl), "video/*")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                
                // Add extra information for media players
                putExtra("title", item.name ?: "Unknown Title")
                putExtra("artist", item.albumArtist ?: item.artists?.joinToString(", ") ?: "")
                putExtra("duration", item.runTimeTicks?.div(10_000) ?: 0L) // Convert to milliseconds
            }
            
            // Try to start the intent
            context.startActivity(intent)
            Log.d("MediaPlayerUtils", "Successfully launched external media player")
            
        } catch (e: Exception) {
            Log.e("MediaPlayerUtils", "Failed to launch media player: ${e.message}", e)
            
            // Fallback: try with generic intent
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(streamUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
                Log.d("MediaPlayerUtils", "Successfully launched fallback intent")
            } catch (fallbackException: Exception) {
                Log.e("MediaPlayerUtils", "Fallback intent also failed: ${fallbackException.message}", fallbackException)
                throw MediaPlayerException("Unable to play media. No compatible media player found.")
            }
        }
    }
    
    /**
     * Checks if there's a media player available to handle the stream
     */
    fun isMediaPlayerAvailable(context: Context): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse("http://example.com/test.mp4"), "video/*")
            }
            val activities = context.packageManager.queryIntentActivities(intent, 0)
            activities.isNotEmpty()
        } catch (e: Exception) {
            Log.w("MediaPlayerUtils", "Error checking for media player availability: ${e.message}")
            false
        }
    }
}

class MediaPlayerException(message: String, cause: Throwable? = null) : Exception(message, cause)