package com.rpeters.jellyfin.utils

import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for logging privacy-safe analytics events to Firebase.
 * All media titles are scrubbed to protect user privacy.
 */
@Singleton
class AnalyticsHelper @Inject constructor() {

    private val firebaseAnalytics: FirebaseAnalytics = Firebase.analytics

    /**
     * Logs an AI-related event.
     * @param feature The feature name (e.g., "summary", "chat", "search")
     * @param success Whether the AI request was successful
     * @param model The model used (e.g., "nano", "cloud")
     */
    fun logAiEvent(feature: String, success: Boolean, model: String) {
        val bundle = Bundle().apply {
            putString("feature", feature)
            putBoolean("success", success)
            putString("model", model)
        }
        firebaseAnalytics.logEvent("ai_usage", bundle)
    }

    /**
     * Logs a playback event.
     * @param method The playback method (e.g., "Direct Play", "Transcoding")
     * @param container The container format (e.g., "mp4", "mkv")
     * @param resolution The video resolution (e.g., "1080p", "4K")
     */
    fun logPlaybackEvent(method: String, container: String, resolution: String) {
        val bundle = Bundle().apply {
            putString("method", method)
            putString("container", container)
            putString("resolution", resolution)
        }
        firebaseAnalytics.logEvent("playback_start", bundle)
    }

    /**
     * Logs a general UI interaction event.
     * @param screen The screen name
     * @param action The action performed
     */
    fun logUiEvent(screen: String, action: String) {
        val bundle = Bundle().apply {
            putString("screen", screen)
            putString("action", action)
        }
        firebaseAnalytics.logEvent("ui_interaction", bundle)
    }

    /**
     * Logs a cast session event.
     * @param deviceType The type of cast device
     */
    fun logCastEvent(deviceType: String) {
        val bundle = Bundle().apply {
            putString("device_type", deviceType)
        }
        firebaseAnalytics.logEvent("cast_session_start", bundle)
    }
}
