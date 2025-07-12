package com.example.jellyfinandroid.utils

import org.jellyfin.sdk.model.api.BaseItemDto

/**
 * Extension functions for common operations across the app
 */

/**
 * Safely converts community rating to Double with fallback to 0.0
 * Eliminates code duplication across multiple screen files
 */
fun BaseItemDto.getRatingAsDouble(): Double = 
    (communityRating as? Number)?.toDouble() ?: 0.0

/**
 * Checks if an item has a high rating (>= 7.0)
 */
fun BaseItemDto.hasHighRating(): Boolean = getRatingAsDouble() >= 7.0

/**
 * Constants for rating thresholds
 */
object RatingConstants {
    const val HIGH_RATING_THRESHOLD = 7.0
    const val EXCELLENT_RATING_THRESHOLD = 8.5
}
