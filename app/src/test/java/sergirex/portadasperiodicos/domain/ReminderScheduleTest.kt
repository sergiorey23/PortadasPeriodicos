package sergirex.portadasperiodicos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import sergirex.portadasperiodicos.domain.model.ReminderSchedule
import java.util.Calendar
import java.util.TimeZone

class ReminderScheduleTest {

    private val utc = TimeZone.getTimeZone("UTC")

    private fun calendar() = Calendar.getInstance(utc)

    /** Epoch millis of the given UTC date and time (month is 1-based). */
    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        calendar().apply { clear(); set(year, month - 1, day, hour, minute) }.timeInMillis

    private fun trigger(now: Long, minutes: Int, days: Set<Int>, afterFiring: Boolean = false) =
        ReminderSchedule.nextTrigger(now, minutes, days, afterFiring, calendar())

    // 2026-10-19 is a Monday.
    private val mondayMorning = at(2026, 10, 19, 7)

    @Test
    fun `fires later today when the time hasn't passed`() {
        assertEquals(at(2026, 10, 19, 9), trigger(mondayMorning, 9 * 60, ReminderSchedule.ALL_DAYS))
    }

    @Test
    fun `fires tomorrow when today's time has passed`() {
        assertEquals(at(2026, 10, 20, 9), trigger(at(2026, 10, 19, 9, 30), 9 * 60, ReminderSchedule.ALL_DAYS))
    }

    @Test
    fun `a custom time is respected`() {
        assertEquals(at(2026, 10, 19, 18, 45), trigger(mondayMorning, 18 * 60 + 45, ReminderSchedule.ALL_DAYS))
    }

    @Test
    fun `after firing it moves to the next day even if the time hasn't passed`() {
        // Inexact alarms may fire a little early: the next one must still be a later day.
        assertEquals(at(2026, 10, 20, 9), trigger(at(2026, 10, 19, 8, 59), 9 * 60, ReminderSchedule.ALL_DAYS, afterFiring = true))
    }

    @Test
    fun `weekdays only skips the weekend`() {
        val fridayAfterTime = at(2026, 10, 23, 10)
        assertEquals(at(2026, 10, 26, 9), trigger(fridayAfterTime, 9 * 60, ReminderSchedule.WEEKDAYS))
    }

    @Test
    fun `a single chosen day waits for that day`() {
        assertEquals(at(2026, 10, 24, 9), trigger(mondayMorning, 9 * 60, setOf(Calendar.SATURDAY)))
    }

    @Test
    fun `the same weekday a week later is found when today has passed`() {
        assertEquals(at(2026, 10, 26, 9), trigger(at(2026, 10, 19, 12), 9 * 60, setOf(Calendar.MONDAY)))
    }

    @Test
    fun `no days means no reminder`() {
        assertNull(trigger(mondayMorning, 9 * 60, emptySet()))
    }

    @Test
    fun `isAllowedOn checks the day of the week`() {
        assertTrue(ReminderSchedule.isAllowedOn(mondayMorning, ReminderSchedule.WEEKDAYS, calendar()))
        assertFalse(ReminderSchedule.isAllowedOn(at(2026, 10, 24, 9), ReminderSchedule.WEEKDAYS, calendar()))
    }
}
