package com.kipit.app.service

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val percent: Int
    ) : DownloadState
    data class Downloaded(
        val apkFile: File,
        val version: String
    ) : DownloadState
    data class Error(val message: String) : DownloadState
}

object AppUpdateDownloader {

    fun getUpdatesDir(context: Context): File {
        val dir = File(context.cacheDir, "apk_updates")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun cleanUpDownloadedApk(context: Context) {
        try {
            val dir = getUpdatesDir(context)
            dir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".apk") || file.name.endsWith(".tmp")) {
                    file.delete()
                }
            }
        } catch (_: Exception) {
            // Ignore clean up errors
        }
    }

    suspend fun download(
        context: Context,
        downloadUrl: String,
        version: String,
        onProgress: (DownloadState) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        val updatesDir = getUpdatesDir(context)
        val cleanVersion = version.trim().removePrefix("v")
        val targetFile = File(updatesDir, "kipit-$cleanVersion.apk")
        val tempFile = File(updatesDir, "kipit-$cleanVersion.apk.tmp")

        try {
            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (tempFile.exists()) {
                tempFile.delete()
            }

            onProgress(DownloadState.Downloading(0L, -1L, 0))

            var currentUrl = downloadUrl
            var redirects = 0
            while (redirects < 5) {
                val url = URL(currentUrl)
                conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Kipit-Android-App")
                    setRequestProperty("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
                }
                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_TEMP ||
                    code == HttpURLConnection.HTTP_MOVED_PERM ||
                    code == 307 ||
                    code == 308
                ) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (location.isNullOrBlank()) break
                    currentUrl = location
                    redirects++
                } else {
                    break
                }
            }

            val responseCode = conn?.responseCode ?: -1
            if (responseCode != HttpURLConnection.HTTP_OK) {
                onProgress(DownloadState.Error("Download failed with HTTP $responseCode"))
                return@withContext null
            }

            val contentLength = conn?.contentLengthLong ?: -1L
            inputStream = conn?.inputStream ?: run {
                onProgress(DownloadState.Error("Unable to open download stream"))
                return@withContext null
            }
            outputStream = FileOutputStream(tempFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L
            var lastPercent = -1

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                val percent = if (contentLength > 0) {
                    ((totalBytesRead * 100) / contentLength).toInt().coerceIn(0, 100)
                } else {
                    -1
                }
                if (percent != lastPercent || totalBytesRead % (128 * 1024) == 0L) {
                    lastPercent = percent
                    onProgress(DownloadState.Downloading(totalBytesRead, contentLength, percent))
                }
            }
            outputStream.flush()
            outputStream.close()
            outputStream = null

            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            onProgress(DownloadState.Downloaded(targetFile, version))
            targetFile
        } catch (e: Exception) {
            tempFile.delete()
            onProgress(DownloadState.Error(e.message ?: "Failed to download update"))
            null
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
            try { outputStream?.close() } catch (_: Exception) {}
            conn?.disconnect()
        }
    }
}
