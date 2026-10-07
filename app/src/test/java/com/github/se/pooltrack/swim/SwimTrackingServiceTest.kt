package com.github.se.pooltrack.swim

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.entry.EntryRepositoryProvider
import com.github.se.pooltrack.utils.FakeEntryRepository
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController

@RunWith(RobolectricTestRunner::class)
class SwimTrackingServiceTest {

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val notificationManager = context.getSystemService(NotificationManager::class.java)

  private fun entryEnteredAgo(duration: Duration, awaitingDistance: Boolean = true) =
      Entry(
          timestampEpochMilli = Instant.now().minus(duration).toEpochMilli(),
          awaitingDistance = awaitingDistance,
      )

  private fun startService(
      vararg entries: Entry
  ): Pair<ServiceController<SwimTrackingService>, FakeEntryRepository> {
    val repository = FakeEntryRepository(entries.toList())
    EntryRepositoryProvider.repository = repository
    val controller = Robolectric.buildService(SwimTrackingService::class.java).create()
    controller.startCommand(0, 0)
    shadowOf(android.os.Looper.getMainLooper()).idle()
    return controller to repository
  }

  private fun unlockPhone() {
    context.sendBroadcast(Intent(Intent.ACTION_USER_PRESENT))
    shadowOf(android.os.Looper.getMainLooper()).idle()
  }

  @Test
  fun onCreate_showsTheOngoingNotification() {
    val (controller, _) = startService(entryEnteredAgo(Duration.ofMinutes(1)))

    assertNotNull(shadowOf(controller.get()).lastForegroundNotification)
    assertEquals(
        SwimNotifications.TRACKING_NOTIFICATION_ID,
        shadowOf(controller.get()).lastForegroundNotificationId,
    )
  }

  @Test
  fun unlock_afterTheCancelWindow_postsTheLogReminderOnceAndStops() {
    val (controller, repository) = startService(entryEnteredAgo(Duration.ofMinutes(72)))

    unlockPhone()

    val reminder =
        shadowOf(notificationManager).getNotification(SwimNotifications.REPORT_NOTIFICATION_ID)
    assertNotNull(reminder)
    assertEquals(
        "Log how far you swam",
        reminder.extras.getCharSequence(Notification.EXTRA_TITLE).toString(),
    )
    assertNotNull(reminder.contentIntent)
    // The swim is no longer timed: nothing but the reminder happens.
    assertNull(repository.storedEntries.single().swimDurationMillis)
    assertTrue(repository.storedEntries.single().awaitingDistance)
    assertTrue(shadowOf(controller.get()).isStoppedBySelf)
  }

  @Test
  fun unlock_insideTheCancelWindow_staysSilentAndKeepsRunning() {
    val (controller, _) = startService(entryEnteredAgo(Duration.ofMinutes(2)))

    unlockPhone()

    assertNull(
        shadowOf(notificationManager).getNotification(SwimNotifications.REPORT_NOTIFICATION_ID)
    )
    assertTrue(!shadowOf(controller.get()).isStoppedBySelf)
  }

  @Test
  fun start_stopsAtOnce_whenThereIsNothingToLog() {
    val (controller, _) =
        startService(entryEnteredAgo(Duration.ofMinutes(30), awaitingDistance = false))

    assertTrue(shadowOf(controller.get()).isStoppedBySelf)
  }

  @Test
  fun destroy_unregistersTheReceiver() {
    val (controller, _) = startService(entryEnteredAgo(Duration.ofMinutes(72)))

    controller.destroy()
    unlockPhone()

    assertNull(
        shadowOf(notificationManager).getNotification(SwimNotifications.REPORT_NOTIFICATION_ID)
    )
  }

  @Test
  fun loggingTheDistance_stopsTheServiceAtOnce() {
    val entry = entryEnteredAgo(Duration.ofMinutes(2))
    val (controller, repository) = startService(entry)

    runBlocking { repository.recordSwimDistance(entry, 900) }
    shadowOf(android.os.Looper.getMainLooper()).idle()

    assertTrue(shadowOf(controller.get()).isStoppedBySelf)
  }

  @Test
  fun removingTheEntry_stopsTheServiceAtOnce() {
    val entry = entryEnteredAgo(Duration.ofMinutes(2))
    val (controller, repository) = startService(entry)
    assertTrue(!shadowOf(controller.get()).isStoppedBySelf)

    runBlocking { repository.deleteEntry(entry) }
    shadowOf(android.os.Looper.getMainLooper()).idle()

    assertTrue(shadowOf(controller.get()).isStoppedBySelf)
  }

  @Test
  fun unlock_afterTheEntryWasRemoved_doesNotReportAnOlderEntry() {
    val older = entryEnteredAgo(Duration.ofMinutes(90)).copy(subscriptionId = "old")
    val newest = entryEnteredAgo(Duration.ofMinutes(20))
    val (_, repository) = startService(older, newest)

    runBlocking { repository.deleteEntry(newest) }
    shadowOf(android.os.Looper.getMainLooper()).idle()
    unlockPhone()

    assertNull(
        shadowOf(notificationManager).getNotification(SwimNotifications.REPORT_NOTIFICATION_ID)
    )
    assertEquals(listOf(older), repository.storedEntries)
  }
}
