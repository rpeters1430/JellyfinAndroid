package com.rpeters.jellyfin.data.repository

interface IJellyfinAuthRefreshManager {
    fun currentAccessToken(): String?
    fun scheduleProactiveRefreshIfNeeded()
    fun refreshAfterUnauthorized(attempt: Int): String?
}
