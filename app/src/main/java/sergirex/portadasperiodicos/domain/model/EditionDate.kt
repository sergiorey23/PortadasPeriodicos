package sergirex.portadasperiodicos.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Edition dates are `yyyy/MM/dd` strings (they double as URL path segments).
 * "Today" is the edition of the current day, except before 06:00, when most papers haven't
 * published yet and the previous day's edition is still the latest.
 */
object EditionDate {
    private const val PATTERN = "yyyy/MM/dd"
    private const val FIRST_EDITION_HOUR = 6

    // (today, epoch millis until which it stays valid) — "today" only changes at 06:00, so
    // callers on hot paths (every image response) don't rebuild a Calendar/SimpleDateFormat.
    @Volatile
    private var cachedToday: Pair<String, Long>? = null

    fun today(): String {
        val now = System.currentTimeMillis()
        cachedToday?.takeIf { now < it.second }?.let { return it.first }

        val day = Calendar.getInstance().apply { timeInMillis = now }
        val boundary = (day.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, FIRST_EDITION_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (now < boundary.timeInMillis) {
            day.add(Calendar.DATE, -1)
        } else {
            boundary.add(Calendar.DATE, 1)
        }
        return format(day.time).also { cachedToday = it to boundary.timeInMillis }
    }

    fun isPast(date: String): Boolean = date < today()

    fun format(date: Date): String = SimpleDateFormat(PATTERN, Locale.FRANCE).format(date)

    /** [start] followed by the [count] - 1 days before it, newest first. */
    fun lookback(start: String, count: Int): List<String> {
        val formatter = SimpleDateFormat(PATTERN, Locale.FRANCE)
        val calendar = Calendar.getInstance().apply { time = runCatching { formatter.parse(start) }.getOrNull() ?: Date() }
        return List(count) { formatter.format(calendar.time).also { calendar.add(Calendar.DATE, -1) } }
    }

    /** `2026/10/06` -> `06/10/2026`. */
    fun toDisplay(date: String): String = date.split("/").asReversed().joinToString("/")
}
