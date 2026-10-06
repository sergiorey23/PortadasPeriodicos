package sergirex.portadasperiodicos.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import sergirex.portadasperiodicos.AlarmBroadcastReceiver
import sergirex.portadasperiodicos.domain.repository.SettingsRepository
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the daily 09:00 reminder alarm in line with the setting and the system permission:
 * scheduled only when the user wants it AND notifications can actually be shown, cancelled
 * otherwise. Call [sync] whenever either could have changed (app start, setting toggled,
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

    /** @param afterFiring true when called from the alarm itself, so the next one is tomorrow's. */
    suspend fun sync(afterFiring: Boolean = false) {
        val wanted = settings.dailyNotificationEnabled.first() &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (wanted) schedule(afterFiring) else alarmManager.cancel(alarmIntent)
    }

    private fun schedule(afterFiring: Boolean) {
        val now = System.currentTimeMillis()
        val trigger = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (afterFiring || timeInMillis <= now) add(Calendar.DATE, 1)
        }.timeInMillis
        // Inexact on purpose (no exact-alarm permission needed); allowed in Doze so it isn't held back all day.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, alarmIntent)
    }

    private companion object {
        const val REQUEST_CODE = 0
        const val HOUR = 9
    }
}
