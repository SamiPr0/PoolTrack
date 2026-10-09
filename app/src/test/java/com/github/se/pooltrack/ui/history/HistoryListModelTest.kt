package com.github.se.pooltrack.ui.history

import com.github.se.pooltrack.model.entry.Entry
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryListModelTest {

  private val zone = ZoneId.of("UTC")
  private val monday = DayOfWeek.MONDAY

  private fun entry(day: LocalDate, hour: Int = 12, meters: Int? = null) =
      Entry(
          timestampEpochMilli = day.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli(),
          swimDistanceMeters = meters,
      )

  // 2026-10-05 is a Monday.
  private val thisWeekMon = LocalDate.of(2026, 10, 5)
  private val thisWeekWed = LocalDate.of(2026, 10, 7)
  private val lastWeekSun = LocalDate.of(2026, 10, 4)

  private fun rows(vararg entries: Entry) = buildHistoryRows(entries.toList(), monday, zone)

  @Test
  fun buildHistoryRows_isEmpty_withoutEntries() {
    assertEquals(emptyList<HistoryRow>(), rows())
  }

  @Test
  fun buildHistoryRows_putsAWeekHeaderBeforeItsEntries_mostRecentFirst() {
    val monday9 = entry(thisWeekMon, hour = 9)
    val wednesday = entry(thisWeekWed)
    val sunday = entry(lastWeekSun)

    val rows = rows(sunday, monday9, wednesday)

    assertEquals(
        listOf(
            HistoryRow.Week(thisWeekMon, visits = 2, meters = 0, loggedVisits = 0),
            HistoryRow.Item(thisWeekWed, wednesday),
            HistoryRow.Item(thisWeekMon, monday9),
            HistoryRow.Week(LocalDate.of(2026, 9, 28), visits = 1, meters = 0, loggedVisits = 0),
            HistoryRow.Item(lastWeekSun, sunday),
        ),
        rows,
    )
  }

  @Test
  fun buildHistoryRows_sumsTheLoggedDistanceOfTheWeek() {
    val rows =
        rows(
            entry(thisWeekMon, meters = 1000),
            entry(thisWeekWed, meters = 500),
            entry(thisWeekWed, hour = 8),
        )

    assertEquals(1500, (rows.first() as HistoryRow.Week).meters)
    assertEquals(2, (rows.first() as HistoryRow.Week).loggedVisits)
  }

  @Test
  fun buildHistoryRows_startsWeeksOnTheGivenFirstDayOfWeek() {
    val rows = buildHistoryRows(listOf(entry(lastWeekSun)), DayOfWeek.SUNDAY, zone)

    assertEquals(lastWeekSun, (rows.first() as HistoryRow.Week).start)
  }

  @Test
  fun buildHistoryRows_resolvesDaysInTheGivenZone() {
    val lateEvening =
        Entry(LocalDate.of(2026, 10, 7).atTime(23, 30).atZone(zone).toInstant().toEpochMilli())

    val rows = buildHistoryRows(listOf(lateEvening), monday, ZoneId.of("Asia/Tokyo"))

    // 23:30 UTC is already the next morning in Tokyo.
    assertEquals(LocalDate.of(2026, 10, 8), (rows[1] as HistoryRow.Item).day)
  }

  @Test
  fun indexOfDay_findsTheEntryOfThatDay() {
    val rows = rows(entry(thisWeekMon), entry(thisWeekWed), entry(lastWeekSun))

    assertEquals(1, rows.indexOfDay(thisWeekWed))
    assertEquals(2, rows.indexOfDay(thisWeekMon))
    assertEquals(4, rows.indexOfDay(lastWeekSun))
  }

  @Test
  fun indexOfDay_isNull_forADayWithoutEntry() {
    assertNull(rows(entry(thisWeekMon)).indexOfDay(LocalDate.of(2026, 10, 6)))
  }

  @Test
  fun entryRowTexts_leadsWithTheDistance_andKeepsTimeAndDurationBelow() {
    val e = Entry(0, swimDistanceMeters = 1200, swimDurationMillis = 3_120_000L)

    assertEquals(EntryRowTexts("1200 m", "6:42 PM · 52min"), entryRowTexts(e, "6:42 PM"))
  }

  @Test
  fun entryRowTexts_showsOnlyTheTimeBelowADistanceWithoutDuration() {
    assertEquals(
        EntryRowTexts("800 m", "6:42 PM"),
        entryRowTexts(Entry(0, swimDistanceMeters = 800), "6:42 PM"),
    )
  }

  @Test
  fun entryRowTexts_leadsWithTheTime_whenNoDistanceWasLogged() {
    assertEquals(
        EntryRowTexts("6:42 PM", "52min"),
        entryRowTexts(Entry(0, swimDurationMillis = 3_120_000L), "6:42 PM"),
    )
    assertEquals(EntryRowTexts("6:42 PM", null), entryRowTexts(Entry(0), "6:42 PM"))
  }

  @Test
  fun formatTotalDistance_usesMetresUnderAKilometre_andKilometresAbove() {
    assertEquals("0 m", formatTotalDistance(0, Locale.ENGLISH))
    assertEquals("850 m", formatTotalDistance(850, Locale.ENGLISH))
    assertEquals("1.0 km", formatTotalDistance(1000, Locale.ENGLISH))
    assertEquals("4.2 km", formatTotalDistance(4200, Locale.ENGLISH))
  }

  @Test
  fun weekTotals_listsVisitsAndDistance_whenEveryVisitHasOne() {
    assertEquals("3 visits · 4.2 km", weekTotals(3, 4200, loggedVisits = 3))
    assertEquals("1 visit · 600 m", weekTotals(1, 600, loggedVisits = 1))
  }

  @Test
  fun weekTotals_saysLogged_whenOnlySomeVisitsHaveADistance() {
    assertEquals("4 visits · 2.6 km logged", weekTotals(4, 2600, loggedVisits = 3))
  }

  @Test
  fun weekTotals_leavesOutTheDistance_whenNoneWasLogged() {
    assertEquals("2 visits", weekTotals(2, 0, loggedVisits = 0))
  }
}
