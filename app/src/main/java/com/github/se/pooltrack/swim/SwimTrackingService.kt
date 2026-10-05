package com.github.se.pooltrack.swim

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.github.se.pooltrack.model.entry.EntryRepositoryProvider
import com.github.se.pooltrack.model.swim.SwimReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Waits, while the user swims, for them to unlock their phone again, then tells them how long they
 * swam. A foreground service because `ACTION_USER_PRESENT` can only be received by a receiver
 * registered at runtime. Stops itself as soon as there is no swim left to report.
 */
class SwimTrackingService : Service() {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private lateinit var reporter: SwimReporter

  private val unlockReceiver =
      object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
          scope.launch {
            reporter.onPhoneUnlocked()
            stopIfNothingPending()
          }
        }
      }

  override fun onCreate() {
    super.onCreate()
    EntryRepositoryProvider.init(this)
    reporter =
        SwimReporter(EntryRepositoryProvider.repository) { SwimNotifications.postReport(this, it) }
    SwimNotifications.createChannels(this)
    val notification = SwimNotifications.trackingNotification(this)
    // The special-use type only exists (and is only required) from Android 14.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      startForeground(
          SwimNotifications.TRACKING_NOTIFICATION_ID,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
      )
    } else {
      startForeground(SwimNotifications.TRACKING_NOTIFICATION_ID, notification)
    }
    ContextCompat.registerReceiver(
        this,
        unlockReceiver,
        IntentFilter(Intent.ACTION_USER_PRESENT),
        ContextCompat.RECEIVER_NOT_EXPORTED,
    )
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    scope.launch { stopIfNothingPending() }
    return START_STICKY
  }

  override fun onDestroy() {
    unregisterReceiver(unlockReceiver)
    scope.cancel()
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private suspend fun stopIfNothingPending() {
    if (!reporter.hasPendingSwim()) stopSelf()
  }

  companion object {
    /** Starts tracking. Must be called while the app is in the foreground. */
    fun start(context: Context) {
      context.startForegroundService(Intent(context, SwimTrackingService::class.java))
    }
  }
}
