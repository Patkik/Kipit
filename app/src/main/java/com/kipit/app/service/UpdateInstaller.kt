package com.kipit.app.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

object UpdateInstaller {

    private const val TAG = "UpdateInstaller"

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun getManageUnknownAppsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    fun installApk(context: Context, apkFile: File): Result<Unit> {
        return try {
            Log.i(TAG, "installApk requested for file: ${apkFile.absolutePath}, exists=${apkFile.exists()}, size=${apkFile.length()}")
            if (!apkFile.exists() || apkFile.length() == 0L) {
                val err = IllegalStateException("APK file does not exist or is empty: ${apkFile.absolutePath}")
                Log.e(TAG, "Validation failed", err)
                return Result.failure(err)
            }

            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                authority,
                apkFile
            )
            Log.i(TAG, "Generated FileProvider URI: $apkUri")

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            }

            // Grant read permission to package installer explicitly
            val resolveInfos = context.packageManager.queryIntentActivities(intent, 0)
            Log.i(TAG, "Found ${resolveInfos.size} activities handling package archive intent")
            for (info in resolveInfos) {
                val pkgName = info.activityInfo.packageName
                Log.i(TAG, "Granting URI read permission to $pkgName")
                context.grantUriPermission(pkgName, apkUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(intent)
            Log.i(TAG, "startActivity successfully invoked")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer", e)
            Result.failure(e)
        }
    }
}
