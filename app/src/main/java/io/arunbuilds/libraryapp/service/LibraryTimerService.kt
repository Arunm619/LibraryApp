package io.arunbuilds.libraryapp.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dagger.hilt.android.AndroidEntryPoint
import io.arunbuilds.libraryapp.Constants
import io.arunbuilds.libraryapp.utils.NotificationUtils
import io.arunbuilds.libraryapp.utils.TimeUtils
import io.arunbuilds.libraryapp.utils.rx.SchedulerProvider
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class LibraryTimerService : Service() {
    private val mBinder: IBinder = TimerBinder()
    private var isBound = false

    @Inject
    lateinit var notificationBuilder: NotificationCompat.Builder

    @Inject
    lateinit var notificationUtils: NotificationUtils

    @Inject
    lateinit var appSchedulers: SchedulerProvider

    private var isWorkRunning: AtomicBoolean = AtomicBoolean(false)
    private val disposableBag = CompositeDisposable()

    override fun onCreate() {
        super.onCreate()
        Timber.d("onCreate Launched")
        startForegroundService()
        isBound = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("OnStartCommand Launched")
        if (!isWorkRunning.get()) {
            isWorkRunning.set(true)
            val disposable: Disposable = Observable.interval(ONE, TimeUnit.SECONDS)
                .timeInterval()
                .subscribeOn(appSchedulers.io())
                .subscribe { data ->
                    val secondsElapsed: Long = data.value()
                    with(TimeUtils.getTimeStampFromElapsedSeconds(secondsElapsed)) {
                        updateNotification(this)
                        sendBroadcastEvent(this)
                        Timber.i("Time Elapsed - $this")
                    }
                }
            disposableBag.add(disposable)
        } else {
            Timber.i("Work is already running no need to redo")
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        Timber.d("LibraryTimerService Bound")
        isBound = true
        return mBinder
    }

    override fun onDestroy() {
        super.onDestroy()
        removeNotification()
        Timber.i("Stopping Service inside OnDestroy")
        destroyService()
    }

    /**
     * Create and surface the service with on-going notification
     * */
    private fun startForegroundService() {
        val notification: Notification = getNotification()
        Timber.i("Starting Foreground Service")
        startForeground(Constants.NOTIFICATION_ID, notification)
    }

    /**
     * Destroy the service removing the notification.
     * */
    private fun destroyService() {
        isBound = false
        isWorkRunning.set(false)
        disposableBag.dispose()
        stopForeground(true)
        stopSelf()
    }

    /**
     * Send the time stamp data as broadcast to the activity every second.
     * */
    private fun sendBroadcastEvent(time: String) {
        val timerIntent = Intent(Constants.ACTION_LIBRARY_APP_KEY_TIME).apply {
            putExtra(Constants.ACTION_LIBRARY_APP_KEY_DATA, time)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(timerIntent)
    }

    /**
     * Get the Library Timer notification for displaying
     * */
    private fun getNotification(): Notification {
        return notificationUtils.getLibraryTimerServiceNotification(notificationBuilder)
    }

    /**
     * updates the content with the current time stamp in the Library Timer Notification
     */
    private fun updateNotification(timestampContent: String) {
        notificationUtils.updateNotification(timestampContent, notificationBuilder)
    }

    private fun removeNotification() {
        Timber.d("Removing the notification ${Constants.NOTIFICATION_ID}")
        NotificationManagerCompat.from(this).cancel(Constants.NOTIFICATION_ID)
    }

    inner class TimerBinder : Binder() {
        val service: LibraryTimerService
            get() = this@LibraryTimerService
    }

    companion object {
        const val ONE = 1L
    }
}
