package com.kipit.app
import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.kipit.app.service.Schedules
@HiltAndroidApp class KipitApplication:Application(){override fun onCreate(){super.onCreate();Schedules.install(this)}}
