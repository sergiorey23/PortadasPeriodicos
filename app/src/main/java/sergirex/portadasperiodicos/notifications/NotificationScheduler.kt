package sergirex.portadasperiodicos.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import sergirex.portadasperiodicos.AlarmBroadcastReceiver
import sergirex.portadasperiodicos.domain.model.ReminderSchedule
import sergirex.portadasperiodicos.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the daily reminder alarm in line with the settings (time of day, days of the week) and the
 * system permission: scheduled only when the user wants it AND notifications can actually be shown,
 * cancelled otherwise. Call [sync] whenever either could have changed (app start, setting toggled,
 * permission result, device reboot or app update — alarms don't survive those).
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository
) {
    private val alarmManager get() = context.getSystemService(AlarmManager::class.java)

    private val alarmIntent: PendingIntent
        get() = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, AlarmBroadcastReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    /** @param afterFiring true when called from the alarm itself, so the next one is a later day's. */
    suspend fun sync(afterFiring: Boolean = false) {
        val days = settings.dailyNotificationDays.first()
        val wanted = settings.dailyNotificationEnabled.first() && days.isNotEmpty() &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        val trigger = if (wanted) {
            ReminderSchedule.nextTrigger(
                now = System.currentTimeMillis(),
                minutesOfDay = settings.dailyNotificationMinutes.first(),
                days = days,
                afterFiring = afterFiring
            )
        } else {
            null
        }
        // Inexact on purpose (no exact-alarm permission needed); allowed in Doze so it isn't held back all day.
        if (trigger != null) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, alarmIntent)
        } else {
            alarmManager.cancel(alarmIntent)
        }
    }

    /** False if the user doesn't want the reminder on today's day of the week (e.g. weekdays only). */
    suspend fun isAllowedToday(): Boolean =
        ReminderSchedule.isAllowedOn(System.currentTimeMillis(), settings.dailyNotificationDays.first())

    private companion object {
        const val REQUEST_CODE = 0
    }
}
