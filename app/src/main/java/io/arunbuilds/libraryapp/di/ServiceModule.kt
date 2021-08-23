package io.arunbuilds.libraryapp.di

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import io.arunbuilds.libraryapp.Constants
import io.arunbuilds.libraryapp.R
import io.arunbuilds.libraryapp.ui.main.HomeActivity

@Module
@InstallIn(ServiceComponent::class)
object ServiceModule {

    @ServiceScoped
    @Provides
    fun provideHomeActivityPendingIntent(
        @ApplicationContext app: Context
    ): PendingIntent {
        val homeActivityIntent = Intent(app.applicationContext, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(app, 0, homeActivityIntent, 0)
    }

    @ServiceScoped
    @Provides
    fun provideBaseNotificationBuilder(
        @ApplicationContext app: Context,
        pendingIntent: PendingIntent
    ): NotificationCompat.Builder {

        return NotificationCompat.Builder(app, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Timer")
            .setContentText(app.getText(R.string.time))
            .setSmallIcon(R.drawable.ic_baseline_timer_24)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setColor(Color.BLUE)
            .setColorized(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setTicker(app.getText(R.string.time))
            .setAutoCancel(true)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
    }
}
