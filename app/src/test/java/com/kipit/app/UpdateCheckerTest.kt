package com.kipit.app

import com.kipit.app.service.AppUpdateChecker
import com.kipit.app.service.UpdateCheckResult
import org.junit.Assert.*
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun isNewerVersion_correctlyComparesSemver() {
        assertTrue(AppUpdateChecker.isNewerVersion(current = "0.1.0", candidate = "0.2.0"))
        assertTrue(AppUpdateChecker.isNewerVersion(current = "0.1.0", candidate = "v0.1.1"))
        assertTrue(AppUpdateChecker.isNewerVersion(current = "0.9.0", candidate = "0.10.0"))
        assertTrue(AppUpdateChecker.isNewerVersion(current = "0.1.0", candidate = "1.0.0"))

        assertFalse(AppUpdateChecker.isNewerVersion(current = "0.2.0", candidate = "0.1.0"))
        assertFalse(AppUpdateChecker.isNewerVersion(current = "0.1.0", candidate = "0.1.0"))
        assertFalse(AppUpdateChecker.isNewerVersion(current = "v0.1.0", candidate = "0.1.0"))
        assertFalse(AppUpdateChecker.isNewerVersion(current = "1.0.0", candidate = "0.9.9"))
    }

    @Test
    fun formatReleaseDate_formatsIsoDateCorrectly() {
        assertEquals("Oct 29, 2026", AppUpdateChecker.formatReleaseDate("2026-10-29T10:00:00Z"))
        assertEquals("Jan 05, 2026", AppUpdateChecker.formatReleaseDate("2026-01-05T08:30:00Z"))
        assertEquals("Unknown date", AppUpdateChecker.formatReleaseDate(""))
    }

    @Test
    fun parseReleaseJson_extractsMetadataAndApkAsset() {
        val jsonPayload = """
            {
              "tag_name": "v0.2.0",
              "name": "Kipit v0.2.0 - Performance & Updates",
              "body": "### Changes\n- Added in-app update checker\n- Improved animations",
              "html_url": "https://github.com/Patkik/Kipit/releases/tag/v0.2.0",
              "published_at": "2026-10-29T10:00:00Z",
              "assets": [
                {
                  "name": "checksums.txt",
                  "browser_download_url": "https://github.com/Patkik/Kipit/releases/download/v0.2.0/checksums.txt"
                },
                {
                  "name": "app-debug.apk",
                  "browser_download_url": "https://github.com/Patkik/Kipit/releases/download/v0.2.0/app-debug.apk"
                }
              ]
            }
        """.trimIndent()

        val release = AppUpdateChecker.parseReleaseJson(jsonPayload)
        assertNotNull(release)
        assertEquals("v0.2.0", release?.tagName)
        assertEquals("Kipit v0.2.0 - Performance & Updates", release?.name)
        assertEquals("https://github.com/Patkik/Kipit/releases/tag/v0.2.0", release?.htmlUrl)
        assertEquals("https://github.com/Patkik/Kipit/releases/download/v0.2.0/app-debug.apk", release?.apkDownloadUrl)
        assertEquals("2026-10-29T10:00:00Z", release?.publishedAt)
        assertTrue(release?.body?.contains("Added in-app update checker") == true)
    }

    @Test
    fun evaluateUpdate_returnsAvailableWithFormattedDateWhenCandidateIsNewer() {
        val result = AppUpdateChecker.evaluateCandidate(
            currentVersion = "0.1.0",
            tagName = "v0.2.0",
            title = "Kipit 0.2.0",
            body = "Release notes",
            htmlUrl = "https://github.com/Patkik/Kipit/releases/v0.2.0",
            publishedAt = "2026-10-29T10:00:00Z",
            apkUrl = "https://example.com/app.apk"
        )

        assertTrue(result is UpdateCheckResult.Available)
        val available = result as UpdateCheckResult.Available
        assertEquals("v0.2.0", available.newVersion)
        assertEquals("Oct 29, 2026", available.releaseDate)
        assertEquals("https://example.com/app.apk", available.downloadUrl)
    }

    @Test
    fun evaluateUpdate_returnsUpToDateWithFormattedDateWhenCurrentIsLatest() {
        val result = AppUpdateChecker.evaluateCandidate(
            currentVersion = "0.2.0",
            tagName = "v0.2.0",
            title = "Kipit 0.2.0",
            body = "Release notes",
            htmlUrl = "https://github.com/Patkik/Kipit/releases/v0.2.0",
            publishedAt = "2026-10-29T10:00:00Z",
            apkUrl = null
        )

        assertTrue(result is UpdateCheckResult.UpToDate)
        val upToDate = result as UpdateCheckResult.UpToDate
        assertEquals("0.2.0", upToDate.currentVersion)
        assertEquals("Oct 29, 2026", upToDate.releaseDate)
    }
}
