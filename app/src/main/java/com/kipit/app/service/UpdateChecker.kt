package com.kipit.app.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateCheckResult {
    data object Idle : UpdateCheckResult
    data object Checking : UpdateCheckResult
    data class UpToDate(
        val currentVersion: String,
        val checkedAtMs: Long = System.currentTimeMillis()
    ) : UpdateCheckResult
    data class Available(
        val currentVersion: String,
        val newVersion: String,
        val title: String,
        val releaseNotes: String,
        val releaseUrl: String,
        val downloadUrl: String?
    ) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

data class GitHubReleaseInfo(
    val tagName: String,
    val name: String,
    val body: String,
    val htmlUrl: String,
    val publishedAt: String,
    val apkDownloadUrl: String?
)

object AppUpdateChecker {

    fun isNewerVersion(current: String, candidate: String): Boolean {
        val currParts = parseVersionNumbers(current)
        val candParts = parseVersionNumbers(candidate)

        val maxLen = maxOf(currParts.size, candParts.size)
        for (i in 0 until maxLen) {
            val currNum = currParts.getOrElse(i) { 0 }
            val candNum = candParts.getOrElse(i) { 0 }
            if (candNum > currNum) return true
            if (candNum < currNum) return false
        }
        return false
    }

    private fun parseVersionNumbers(v: String): List<Int> {
        val clean = v.trim().removePrefix("v").removePrefix("V")
        val mainVersion = clean.substringBefore("-").substringBefore("+")
        return mainVersion.split(".").mapNotNull { it.trim().toIntOrNull() }
    }

    private fun extractJsonString(json: String, key: String): String {
        val pattern = Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""")
        val match = pattern.find(json) ?: return ""
        return unescapeJson(match.groupValues[1])
    }

    private fun extractApkDownloadUrl(json: String): String? {
        val pattern = Regex(""""browser_download_url"\s*:\s*"((?:\\.|[^"\\])*?\.apk)"""", RegexOption.IGNORE_CASE)
        val match = pattern.find(json) ?: return null
        return unescapeJson(match.groupValues[1])
    }

    private fun unescapeJson(str: String): String {
        return str.replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\/", "/")
    }

    fun parseReleaseJson(jsonString: String): GitHubReleaseInfo? {
        val tagName = extractJsonString(jsonString, "tag_name")
        if (tagName.isBlank()) return null

        val name = extractJsonString(jsonString, "name").ifBlank { tagName }
        val body = extractJsonString(jsonString, "body")
        val htmlUrl = extractJsonString(jsonString, "html_url")
        val publishedAt = extractJsonString(jsonString, "published_at")
        val apkUrl = extractApkDownloadUrl(jsonString)

        return GitHubReleaseInfo(
            tagName = tagName,
            name = name,
            body = body,
            htmlUrl = htmlUrl,
            publishedAt = publishedAt,
            apkDownloadUrl = apkUrl
        )
    }

    fun evaluateCandidate(
        currentVersion: String,
        tagName: String,
        title: String,
        body: String,
        htmlUrl: String,
        apkUrl: String?
    ): UpdateCheckResult {
        return if (isNewerVersion(current = currentVersion, candidate = tagName)) {
            UpdateCheckResult.Available(
                currentVersion = currentVersion,
                newVersion = tagName,
                title = title.ifBlank { tagName },
                releaseNotes = body,
                releaseUrl = htmlUrl,
                downloadUrl = apkUrl
            )
        } else {
            UpdateCheckResult.UpToDate(currentVersion = currentVersion)
        }
    }

    suspend fun checkForUpdates(
        currentVersion: String,
        repoOwner: String = "Patkik",
        repoName: String = "Kipit"
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL("https://api.github.com/repos/$repoOwner/$repoName/releases/latest")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Kipit-Android-App")
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                // No releases published yet on the repository
                return@withContext UpdateCheckResult.UpToDate(currentVersion = currentVersion)
            }
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext UpdateCheckResult.Error("GitHub update check failed (HTTP $responseCode)")
            }

            val responseBody = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val release = parseReleaseJson(responseBody)
                ?: return@withContext UpdateCheckResult.Error("Could not parse release information")

            evaluateCandidate(
                currentVersion = currentVersion,
                tagName = release.tagName,
                title = release.name,
                body = release.body,
                htmlUrl = release.htmlUrl,
                apkUrl = release.apkDownloadUrl
            )
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Network error while checking for updates")
        } finally {
            conn?.disconnect()
        }
    }
}
