package sergirex.portadasperiodicos.domain.model

import java.util.Calendar

/**
 * When the daily reminder fires: a time of day and the days of the week it is allowed on. Days use
 * [Calendar]'s constants ([Calendar.SUNDAY] = 1 … [Calendar.SATURDAY] = 7) so they follow the device calendar.
 */
object ReminderSchedule {
    const val DEFAULT_MINUTES = 9 * 60

    /** Monday to Sunday, the order the days are listed in. */
    val DAYS_IN_ORDER = listOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
        Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    )
    val ALL_DAYS: Set<Int> = DAYS_IN_ORDER.toSet()
    val WEEKDAYS: Set<Int> = setOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY
    )

    /**
     * The first moment strictly after [now] at [minutesOfDay] on one of [days], or null if no day is selected.
     * With [afterFiring] today is skipped (the alarm just went off, possibly a little early).
     */
    fun nextTrigger(
        now: Long,
        minutesOfDay: Int,
        days: Set<Int>,
        afterFiring: Boolean,
        calendar: Calendar = Calendar.getInstance()
    ): Long? {
        if (days.isEmpty()) return null
        for (offset in 0..7) {
            val candidate = (calendar.clone() as Calendar).apply {
                timeInMillis = now
                add(Calendar.DATE, offset)
                set(Calendar.HOUR_OF_DAY, minutesOfDay / 60)
                set(Calendar.MINUTE, minutesOfDay % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (candidate.get(Calendar.DAY_OF_WEEK) !in days) continue
            if (candidate.timeInMillis <= now) continue
            if (afterFiring && offset == 0) continue
            return candidate.timeInMillis
        }
        return null
    }

    /** Whether the reminder is allowed on the day [now] falls on. */
    fun isAllowedOn(now: Long, days: Set<Int>, calendar: Calendar = Calendar.getInstance()): Boolean {
        val day = (calendar.clone() as Calendar).apply { timeInMillis = now }.get(Calendar.DAY_OF_WEEK)
        return day in days
    }
}
