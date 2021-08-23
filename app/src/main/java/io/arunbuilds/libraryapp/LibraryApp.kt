package io.arunbuilds.libraryapp

import android.app.Application
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp
import io.arunbuilds.libraryapp.utils.NotificationUtils
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class LibraryApp : Application() {
    @Inject
    lateinit var notificationUtils: NotificationUtils

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.i("Library App Created")
        }
        createNotificationChannels()
        disableDarkMode()
    }

    private fun disableDarkMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Timber.i("Creating Notification Channel ${Constants.NOTIFICATION_CHANNEL_NAME} for Oreo+ Device")
            notificationUtils.createNotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME
            )
        }
    }
}