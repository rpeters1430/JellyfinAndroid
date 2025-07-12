package com.example.jellyfinandroid.data.model

/**
 * ✅ IMPROVEMENT: QuickConnect models extracted for better organization
 * These were previously in the large JellyfinRepository.kt file
 */

data class QuickConnectResult(
    val code: String,
    val secret: String
)

data class QuickConnectState(
    val state: String // "Pending", "Approved", "Denied", "Expired"
) {
    val isPending: Boolean get() = state == "Pending"
    val isApproved: Boolean get() = state == "Approved"
    val isDenied: Boolean get() = state == "Denied"
    val isExpired: Boolean get() = state == "Expired"
    val isCompleted: Boolean get() = isApproved || isDenied || isExpired
}

/**
 * ✅ IMPROVEMENT: Constants for QuickConnect configuration
 */
object QuickConnectConstants {
    const val CODE_LENGTH = 6
    const val SECRET_LENGTH = 32
    const val MAX_POLL_ATTEMPTS = 60 // 5 minutes at 5-second intervals
    const val POLL_INTERVAL_MS = 5000L
    
    // Valid characters for QuickConnect codes
    const val CODE_CHARACTERS = "0123456789"
}
