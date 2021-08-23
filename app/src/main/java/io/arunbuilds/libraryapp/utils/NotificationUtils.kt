package io.arunbuilds.libraryapp.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.arunbuilds.libraryapp.Constants
import javax.inject.Inject

class NotificationUtils @Inject constructor(
    @ApplicationContext private val appContext: Context
) {

    /**
     * Creates Notification channel with defined configuration.
     * */
    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannel(channelId: String, channelName: String) {
        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        channel.lightColor = Color.BLUE
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        val service =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
    }

    /**
     * updates the Notification content by [Constants.NOTIFICATION_ID]
     */
    fun updateNotification(content: String, notificationBuilder: NotificationCompat.Builder) {
        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = notificationBuilder
            .setContentText(content)
            .build()
        notificationManager.notify(Constants.NOTIFICATION_ID, notification)
    }

    /**
     * Builds the notification required to be surfaced during the timer service.
     * */
    fun getLibraryTimerServiceNotification(notificationBuilder: NotificationCompat.Builder): Notification {
        return notificationBuilder.build()
    }
}
