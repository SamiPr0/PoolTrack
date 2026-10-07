package com.github.se.pooltrack.model.subscription

import android.content.ContentResolver
import android.content.Context
import com.github.se.pooltrack.model.ResolverContext
import com.github.se.pooltrack.model.backup.FirebaseMocks
import com.github.se.pooltrack.model.clearPreferencesDataStore
import com.github.se.pooltrack.model.entry.Entry
import com.github.se.pooltrack.model.entry.EntryRepositoryLocal
import com.github.se.pooltrack.model.entry.timestamp
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Pins down what happens to the entries of a subscription once it is deleted: they stay, still
 * tagged with the deleted subscription's id (see issue #78).
 */
@RunWith(RobolectricTestRunner::class)
class DeletingSubscriptionKeepsEntriesTest {

  private val currentUid = MutableStateFlow<String?>("user-1")

  private lateinit var firebase: FirebaseMocks
  private lateinit var subscriptions: SubscriptionRepositoryLocal
  private lateinit var entries: EntryRepositoryLocal

  @Before
  fun setUp() = runTest {
    val appContext: Context = RuntimeEnvironment.getApplication()
    clearPreferencesDataStore(appContext, SUBSCRIPTION_FILE_CLASS, "subscriptionDataStore")
    clearPreferencesDataStore(appContext, ENTRY_FILE_CLASS, "entryDataStore")
    firebase = FirebaseMocks(uid = "user-1")
    val resolver = mockk<ContentResolver>(relaxed = true)
    subscriptions = SubscriptionRepositoryLocal(ResolverContext(appContext, resolver), currentUid)
    entries = EntryRepositoryLocal(appContext, currentUid)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun deleteSubscription_keepsItsEntries_stillTaggedWithTheDeletedId() = runTest {
    val deleted = subscriptions.addSubscription(uri = "content://docs/a.pdf")
    val kept = subscriptions.addSubscription(uri = "content://docs/b.pdf")
    val first = Entry(timestampEpochMilli = 1_000L, subscriptionId = deleted.id)
    val second = Entry(timestampEpochMilli = 2_000L, subscriptionId = kept.id)
    entries.addEntry(first)
    entries.addEntry(second)

    subscriptions.deleteSubscription(deleted.id)

    assertEquals(listOf(kept), subscriptions.getSubscriptions().first())
    assertEquals(listOf(second, first), entries.getEntries().first())
    assertEquals(second.timestamp, entries.getLastEntryTimestamp().first())
  }

  private companion object {
    const val SUBSCRIPTION_FILE_CLASS =
        "com.github.se.pooltrack.model.subscription.SubscriptionRepositoryLocalKt"
    const val ENTRY_FILE_CLASS = "com.github.se.pooltrack.model.entry.EntryRepositoryLocalKt"
  }
}
