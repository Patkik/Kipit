package com.kipit.app

import android.app.Application
import com.kipit.app.service.AppUpdateDownloader
import com.kipit.app.service.Schedules
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KipitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Schedules.install(this)
        AppUpdateDownloader.cleanUpDownloadedApk(this)
    }
}
