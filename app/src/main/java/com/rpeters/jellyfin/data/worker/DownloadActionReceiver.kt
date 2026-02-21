package com.rpeters.jellyfin.data.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.rpeters.jellyfin.data.offline.OfflineDownloadManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DownloadActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var downloadManager: OfflineDownloadManager

    override fun onReceive(context: Context, intent: Intent) {
        val downloadId = intent.getStringExtra(EXTRA_DOWNLOAD_ID) ?: return
        val action = intent.action ?: return

        Log.d(TAG, "Received action: $action for download: $downloadId")

        when (action) {
            ACTION_PAUSE -> {
                downloadManager.pauseDownload(downloadId)
            }
            ACTION_RESUME -> {
                downloadManager.resumeDownload(downloadId)
            }
            ACTION_CANCEL -> {
                downloadManager.cancelDownload(downloadId)
            }
        }
    }

    companion object {
        private const val TAG = "DownloadActionReceiver"
        
        const val ACTION_PAUSE = "com.rpeters.jellyfin.ACTION_PAUSE_DOWNLOAD"
        const val ACTION_RESUME = "com.rpeters.jellyfin.ACTION_RESUME_DOWNLOAD"
        const val ACTION_CANCEL = "com.rpeters.jellyfin.ACTION_CANCEL_DOWNLOAD"
        
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"
    }
}
