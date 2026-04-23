package com.rpeters.jellyfin.data.repository

import android.util.Log
import com.rpeters.jellyfin.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.measureTimeMillis

@Singleton
class JellyfinAuthRefreshManager @Inject constructor(
    private val authRepository: IJellyfinAuthRepository,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : IJellyfinAuthRefreshManager {

    private val singleFlightMutex = Mutex()
    @Volatile
    private var inFlightRefresh: Deferred<RefreshResult>? = null

    override fun currentAccessToken(): String? {
        return authRepository.getCurrentServer()?.accessToken
    }

    override fun scheduleProactiveRefreshIfNeeded() {
        if (!authRepository.isUserAuthenticated() || !authRepository.shouldRefreshToken()) {
            return
        }

        applicationScope.launch {
            runSingleFlightRefresh(trigger = REFRESH_TRIGGER_PROACTIVE)
        }
    }

    override fun refreshAfterUnauthorized(attempt: Int): String? {
        val trigger = "$REFRESH_TRIGGER_401-$attempt"
        val result = runBlocking {
            withTimeoutOrNull(REFRESH_TIMEOUT_MS) {
                runSingleFlightRefresh(trigger = trigger)
            }
        } ?: run {
            logTelemetry("failure", trigger, attempt, "timeout")
            RefreshResult.Failure("timeout")
        }

        return when (result) {
            is RefreshResult.Success -> result.token
            is RefreshResult.Failure -> {
                logTelemetry("failure", trigger, attempt, result.reason)
                null
            }
        }
    }

    private suspend fun runSingleFlightRefresh(trigger: String): RefreshResult {
        val deferred = singleFlightMutex.withLock {
            inFlightRefresh?.let { return@withLock it }
            val created = applicationScope.async {
                executeRefresh(trigger)
            }
            inFlightRefresh = created
            created.invokeOnCompletion {
                applicationScope.launch {
                    singleFlightMutex.withLock {
                        if (inFlightRefresh === created) {
                            inFlightRefresh = null
                        }
                    }
                }
            }
            created
        }

        return deferred.await()
    }

    private suspend fun executeRefresh(trigger: String): RefreshResult {
        val elapsed = measureTimeMillis {
            repeat(MAX_REFRESH_RETRIES) { retryIndex ->
                val delayMs = RETRY_BACKOFF_MS[retryIndex]
                if (delayMs > 0L) {
                    delay(delayMs)
                }

                val refreshed = authRepository.forceReAuthenticate()
                if (refreshed) {
                    val token = authRepository.getCurrentServer()?.accessToken
                    if (!token.isNullOrBlank()) {
                        logTelemetry("success", trigger, retryIndex + 1, null)
                        return RefreshResult.Success(token)
                    }
                    logTelemetry("failure", trigger, retryIndex + 1, "missing_token")
                } else {
                    logTelemetry("failure", trigger, retryIndex + 1, "reauth_failed")
                }
            }
        }

        Log.w(TAG, "Auth refresh exhausted retries for trigger=$trigger durationMs=$elapsed")
        return RefreshResult.Failure("retry_exhausted")
    }

    private fun logTelemetry(
        outcome: String,
        trigger: String,
        attempt: Int,
        reason: String?,
    ) {
        val reasonPart = reason?.let { " reason=$it" } ?: ""
        Log.i(TAG, "AuthRefreshEvent outcome=$outcome trigger=$trigger attempt=$attempt$reasonPart")
    }

    private sealed class RefreshResult {
        data class Success(val token: String) : RefreshResult()
        data class Failure(val reason: String) : RefreshResult()
    }

    companion object {
        private const val TAG = "JellyfinAuthRefreshManager"
        private const val REFRESH_TRIGGER_PROACTIVE = "proactive"
        private const val REFRESH_TRIGGER_401 = "http_401"
        private const val REFRESH_TIMEOUT_MS = 10_000L
        private const val MAX_REFRESH_RETRIES = 3
        private val RETRY_BACKOFF_MS = longArrayOf(0L, 100L, 500L)
    }
}
