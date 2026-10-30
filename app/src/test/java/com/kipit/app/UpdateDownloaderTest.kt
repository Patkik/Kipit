package com.kipit.app

import com.kipit.app.service.DownloadState
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class UpdateDownloaderTest {

    @Test
    fun testDownloadStateTransitions() {
        val idle: DownloadState = DownloadState.Idle
        assertTrue(idle is DownloadState.Idle)

        val downloading: DownloadState = DownloadState.Downloading(
            bytesDownloaded = 5_000_000L,
            totalBytes = 10_000_000L,
            percent = 50
        )
        assertTrue(downloading is DownloadState.Downloading)
        assertEquals(50, (downloading as DownloadState.Downloading).percent)

        val tempFile = File.createTempFile("test_apk", ".apk")
        val downloaded: DownloadState = DownloadState.Downloaded(tempFile, "0.1.2")
        assertTrue(downloaded is DownloadState.Downloaded)
        assertEquals("0.1.2", (downloaded as DownloadState.Downloaded).version)
        tempFile.delete()

        val error: DownloadState = DownloadState.Error("Network failure")
        assertTrue(error is DownloadState.Error)
        assertEquals("Network failure", (error as DownloadState.Error).message)
    }

    @Test
    fun testFileCleanupLogic() {
        val testDir = File(System.getProperty("java.io.tmpdir"), "test_updates_" + System.currentTimeMillis())
        testDir.mkdirs()

        val apkFile = File(testDir, "kipit-0.1.2.apk").apply { writeText("dummy apk content") }
        val tmpFile = File(testDir, "kipit-0.1.2.apk.tmp").apply { writeText("dummy tmp content") }
        val otherFile = File(testDir, "keep_me.txt").apply { writeText("keep") }

        assertTrue(apkFile.exists())
        assertTrue(tmpFile.exists())
        assertTrue(otherFile.exists())

        testDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".apk") || file.name.endsWith(".tmp")) {
                file.delete()
            }
        }

        assertFalse(apkFile.exists())
        assertFalse(tmpFile.exists())
        assertTrue(otherFile.exists())

        otherFile.delete()
        testDir.delete()
    }
}
