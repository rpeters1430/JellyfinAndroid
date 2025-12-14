package com.rpeters.jellyfin.ui.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.junit.Assert.assertFalse
import org.junit.Test

class MediaDownloadManagerTest {

    @Test
    fun isDownloaded_returnsFalse_whenPermissionDenied() {
        val context = mockk<Context>(relaxed = true)

        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(any(), any()) } returns PackageManager.PERMISSION_DENIED

        val item = BaseItemDto(id = java.util.UUID.randomUUID(), name = "Video", type = BaseItemKind.MOVIE)

        try {
            assertFalse(MediaDownloadManager.isDownloaded(context, item, sdkInt = android.os.Build.VERSION_CODES.P))
        } finally {
            unmockkAll()
        }
    }
}
