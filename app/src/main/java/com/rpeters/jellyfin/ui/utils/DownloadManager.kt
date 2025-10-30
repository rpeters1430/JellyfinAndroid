package com.rpeters.jellyfin.ui.utils

import android.app.DownloadManager
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import com.rpeters.jellyfin.BuildConfig
import com.rpeters.jellyfin.data.storage.MediaStoreSaver
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.io.File
import java.io.IOException

/**
 * Utility class for handling media downloads from Jellyfin server.
 *
 * Manages downloading media files to local storage with proper file organization,
 * progress tracking, and storage management.
 */
object MediaDownloadManager {

    private const val TAG = "MediaDownloadManager"
    private const val DOWNLOADS_FOLDER = "JellyfinAndroid"
    private const val DEFAULT_MIME_TYPE = "application/octet-stream"

    /**
     * Downloads a media item to local storage using Android's DownloadManager.
     *
     * @param context The application context
     * @param item The Jellyfin media item to download
     * @param streamUrl The stream URL for the media item
     * @return The download ID for tracking, or null if download failed to start
     */
    fun downloadMedia(context: Context, item: BaseItemDto, streamUrl: String): Long? {
        return try {
            ensureMediaPermission(context, item)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            // Create download request
            val request = DownloadManager.Request(streamUrl.toUri()).apply {
                setTitle("${item.name ?: "Unknown"}")
                setDescription("Downloading from Jellyfin")

                // Set destination in Downloads directory with organized folder structure
                val fileName = generateFileName(item)
                val subPath = generateSubPath(item)
                val mimeType = getMimeType(item) ?: DEFAULT_MIME_TYPE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    queryDownloadEntry(context, fileName, subPath)?.uri?.let { existingUri ->
                        context.contentResolver.delete(existingUri, null, null)
                    }
                    val destinationUri = createDownloadUri(context, fileName, subPath, mimeType)
                    if (destinationUri == null) {
                        Log.w(TAG, "Unable to create MediaStore destination for ${item.name}")
                        return null
                    }
                    setDestinationUri(destinationUri)
                } else {
                    val subFolder = "$DOWNLOADS_FOLDER/$subPath/$fileName"
                    File(getDownloadBaseDir(context), subFolder).takeIf { it.exists() }?.let { existing ->
                        if (!existing.delete() && BuildConfig.DEBUG) {
                            Log.w(TAG, "Failed to delete existing legacy download file for ${item.name}")
                        }
                    }
                    setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, subFolder)
                }

                // Allow download only over WiFi by default for large media files
                setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)

                // Show download notification
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                // Set MIME type based on media type
                setMimeType(mimeType)
            }

            // Enqueue the download
            val downloadId = downloadManager.enqueue(request)

            if (BuildConfig.DEBUG) {
                Log.i(TAG, "Started download for ${item.name} with ID: $downloadId")
            }
            downloadId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start download for ${item.name}: ${e.message}", e)
            null
        }
    }

    /**
     * Checks if a media item is already downloaded.
     *
     * @param context The application context
     * @param item The media item to check
     * @return True if the item is already downloaded
     */
    fun isDownloaded(context: Context, item: BaseItemDto, sdkInt: Int = Build.VERSION.SDK_INT): Boolean {
        return try {
            ensureMediaPermission(context, item)
            val fileName = generateFileName(item)
            val subPath = generateSubPath(item)

            if (sdkInt >= Build.VERSION_CODES.Q) {
                queryDownloadEntry(context, fileName, subPath) != null
            } else {
                val file = File(
                    getDownloadBaseDir(context),
                    "$DOWNLOADS_FOLDER/$subPath/$fileName",
                )

                file.exists() && file.length() > 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking download status for ${item.name}: ${e.message}", e)
            false
        }
    }

    /**
     * Gets the local file path for a downloaded media item.
     *
     * @param context The application context
     * @param item The media item
     * @return The local file path, or null if not downloaded
     */
    fun getLocalFilePath(context: Context, item: BaseItemDto): String? {
        return try {
            ensureMediaPermission(context, item)
            val fileName = generateFileName(item)
            val subPath = generateSubPath(item)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                queryDownloadEntry(context, fileName, subPath)?.uri?.toString()
            } else {
                val file = File(
                    getDownloadBaseDir(context),
                    "$DOWNLOADS_FOLDER/$subPath/$fileName",
                )

                if (file.exists() && file.length() > 0) {
                    file.absolutePath
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local file path for ${item.name}: ${e.message}", e)
            null
        }
    }

    /**
     * Deletes a downloaded media file.
     *
     * @param context The application context
     * @param item The media item to delete
     * @return True if the file was successfully deleted
     */
    fun deleteDownload(context: Context, item: BaseItemDto): Boolean {
        return try {
            ensureMediaPermission(context, item)
            val fileName = generateFileName(item)
            val subPath = generateSubPath(item)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val entry = queryDownloadEntry(context, fileName, subPath)
                if (entry?.uri != null) {
                    val deleted = context.contentResolver.delete(entry.uri, null, null) > 0
                    if (!deleted) {
                        Log.w(TAG, "Failed to delete MediaStore entry for ${item.name}")
                    }
                    deleted
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "MediaStore entry does not exist for ${item.name}")
                    }
                    true
                }
            } else {
                val file = File(
                    getDownloadBaseDir(context),
                    "$DOWNLOADS_FOLDER/$subPath/$fileName",
                )

                if (file.exists()) {
                    val deleted = file.delete()
                    if (deleted) {
                        if (BuildConfig.DEBUG) {
                            Log.i(TAG, "Deleted download for ${item.name}")
                        }
                    } else {
                        Log.w(TAG, "Failed to delete download for ${item.name}")
                    }
                    deleted
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Download file does not exist for ${item.name}")
                    }
                    true // Consider it deleted if it doesn't exist
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting download for ${item.name}: ${e.message}", e)
            false
        }
    }

    /**
     * Gets the download status for a given download ID.
     *
     * @param context The application context
     * @param downloadId The download ID
     * @return A DownloadStatus object with progress and status information
     */
    fun getDownloadStatus(context: Context, downloadId: Long): DownloadStatus? {
        return try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))

            if (cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                val totalSizeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val downloadedSizeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)

                val status = cursor.getInt(statusIndex)
                val reason = cursor.getInt(reasonIndex)
                val totalSize = cursor.getLong(totalSizeIndex)
                val downloadedSize = cursor.getLong(downloadedSizeIndex)

                cursor.close()

                DownloadStatus(
                    status = status,
                    reason = reason,
                    totalSize = totalSize,
                    downloadedSize = downloadedSize,
                    progress = if (totalSize > 0) (downloadedSize.toFloat() / totalSize.toFloat()) else 0f,
                )
            } else {
                cursor.close()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting download status for ID $downloadId: ${e.message}", e)
            null
        }
    }

    /**
     * Calculates total storage used by downloads.
     *
     * @param context The application context
     * @return The total size in bytes used by downloads
     */
    fun getTotalDownloadSize(context: Context): Long {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                sumMediaStoreDownloads(context)
            } else {
                val downloadsDir = File(
                    getDownloadBaseDir(context),
                    DOWNLOADS_FOLDER,
                )

                if (downloadsDir.exists() && downloadsDir.isDirectory) {
                    calculateDirectorySize(downloadsDir)
                } else {
                    0L
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating total download size: ${e.message}", e)
            0L
        }
    }

    /**
     * Clears all downloads to free up storage space.
     *
     * @param context The application context
     * @return True if all downloads were successfully cleared
     */
    fun clearAllDownloads(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                deleteMediaStoreDownloads(context)
            } else {
                val downloadsDir = File(
                    getDownloadBaseDir(context),
                    DOWNLOADS_FOLDER,
                )

                if (downloadsDir.exists() && downloadsDir.isDirectory) {
                    downloadsDir.deleteRecursively()
                } else {
                    true // Consider it cleared if directory doesn't exist
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all downloads: ${e.message}", e)
            false
        }
    }

    // Helper methods

    private fun sumMediaStoreDownloads(context: Context): Long {
        val resolver = context.contentResolver
        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.MediaColumns.SIZE,
            MediaStore.Downloads.RELATIVE_PATH,
        )
        val selection = "${MediaStore.Downloads.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("${buildRelativePath("")}%")

        resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            var total = 0L
            val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            while (cursor.moveToNext()) {
                if (sizeIndex != -1) {
                    total += cursor.getLong(sizeIndex)
                }
            }
            return total
        }
        return 0L
    }

    private fun deleteMediaStoreDownloads(context: Context): Boolean {
        val resolver = context.contentResolver
        val selection = "${MediaStore.Downloads.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("${buildRelativePath("")}%")

        return resolver.delete(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            selection,
            selectionArgs,
        ) >= 0
    }

    internal fun getDownloadBaseDir(context: Context): File {
        return context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
    }

    private fun ensureMediaPermission(context: Context, item: BaseItemDto) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val permission = when (item.type) {
            BaseItemKind.AUDIO -> android.Manifest.permission.READ_MEDIA_AUDIO
            BaseItemKind.MOVIE, BaseItemKind.EPISODE, BaseItemKind.MUSIC_VIDEO -> android.Manifest.permission.READ_MEDIA_VIDEO
            else -> null
        }

        if (permission != null &&
            androidx.core.content.ContextCompat.checkSelfPermission(context, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("Missing required media permission: $permission")
        }
    }

    private fun generateFileName(item: BaseItemDto): String {
        val baseName = (item.name ?: "Unknown").replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val extension = getFileExtension(item)
        return "$baseName.$extension"
    }

    private fun generateSubPath(item: BaseItemDto): String {
        return when (item.type) {
            BaseItemKind.MOVIE -> "Movies"
            BaseItemKind.EPISODE -> "TV Shows/${item.seriesName ?: "Unknown Series"}"
            BaseItemKind.AUDIO -> "Music/${item.albumArtist ?: "Unknown Artist"}"
            BaseItemKind.MUSIC_VIDEO -> "Music Videos"
            else -> "Other"
        }.replace(Regex("[^a-zA-Z0-9._/ -]"), "_")
    }

    private fun getFileExtension(item: BaseItemDto): String {
        // Default extensions based on media type
        return when (item.type) {
            BaseItemKind.MOVIE, BaseItemKind.EPISODE -> "mp4"
            BaseItemKind.AUDIO -> "mp3"
            BaseItemKind.MUSIC_VIDEO -> "mp4"
            else -> "bin"
        }
    }

    private fun getMimeType(item: BaseItemDto): String? {
        return when (item.type) {
            BaseItemKind.MOVIE, BaseItemKind.EPISODE, BaseItemKind.MUSIC_VIDEO -> "video/mp4"
            BaseItemKind.AUDIO -> "audio/mpeg"
            else -> null
        }
    }

    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        if (directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    calculateDirectorySize(file)
                } else {
                    file.length()
                }
            }
        } else {
            size = directory.length()
        }
        return size
    }

    private fun queryDownloadEntry(context: Context, fileName: String, subPath: String): DownloadEntry? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null

        val resolver = context.contentResolver
        val relativePath = buildRelativePath(subPath)
        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.MediaColumns.SIZE,
            MediaStore.Downloads.RELATIVE_PATH,
        )
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ? AND ${MediaStore.Downloads.RELATIVE_PATH} = ?"
        val selectionArgs = arrayOf(fileName, relativePath)

        resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val idIndex = cursor.getColumnIndex(MediaStore.Downloads._ID)
                if (sizeIndex != -1 && idIndex != -1) {
                    val size = cursor.getLong(sizeIndex)
                    if (size > 0) {
                        val id = cursor.getLong(idIndex)
                        val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                        return DownloadEntry(uri = uri)
                    }
                }
            }
        }
        return null
    }

    private fun createDownloadUri(
        context: Context,
        fileName: String,
        subPath: String,
        mimeType: String,
    ): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null

        val relativePath = buildRelativePath(subPath)
        return try {
            MediaStoreSaver.prepareDownload(
                context = context,
                fileName = fileName,
                relativePath = relativePath,
                mimeType = mimeType,
            )
        } catch (ioe: IOException) {
            Log.w(TAG, "Unable to create MediaStore destination for $fileName", ioe)
            null
        }
    }

    private fun buildRelativePath(subPath: String): String {
        val basePath = "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOADS_FOLDER"
        val normalized = subPath.trim('/').takeIf { it.isNotEmpty() }
        return if (normalized != null) {
            "$basePath/$normalized/"
        } else {
            "$basePath/"
        }
    }

    private data class DownloadEntry(
        val uri: Uri,
    )
}

/**
 * Data class representing the status of a download.
 */
data class DownloadStatus(
    val status: Int,
    val reason: Int,
    val totalSize: Long,
    val downloadedSize: Long,
    val progress: Float,
) {
    val isCompleted: Boolean
        get() = status == DownloadManager.STATUS_SUCCESSFUL

    val isFailed: Boolean
        get() = status == DownloadManager.STATUS_FAILED

    val isRunning: Boolean
        get() = status == DownloadManager.STATUS_RUNNING

    val isPaused: Boolean
        get() = status == DownloadManager.STATUS_PAUSED

    val isPending: Boolean
        get() = status == DownloadManager.STATUS_PENDING
}
