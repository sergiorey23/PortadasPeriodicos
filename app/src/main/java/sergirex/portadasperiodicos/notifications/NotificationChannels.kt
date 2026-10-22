package sergirex.portadasperiodicos.notifications

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import sergirex.portadasperiodicos.R

/** The daily-reminder channel: where the user controls sound and vibration (Android 8+). */
object NotificationChannels {
    const val DAILY = "26081995"

    /** Safe to call repeatedly; an existing channel keeps the user's own sound and vibration choices. */
    fun ensureDaily(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannelCompat.Builder(DAILY, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.notification_channel_daily))
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }
}
