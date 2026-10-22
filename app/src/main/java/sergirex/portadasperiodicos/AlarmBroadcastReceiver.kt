package sergirex.portadasperiodicos

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import sergirex.portadasperiodicos.notifications.NotificationChannels
import sergirex.portadasperiodicos.notifications.launchAsync
import sergirex.portadasperiodicos.notifications.notificationScheduler

class AlarmBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduler = context.notificationScheduler()
        launchAsync {
            // The alarm is inexact and the settings may have changed since it was set: check today is still a reminder day.
            if (scheduler.isAllowedToday()) showNotification(context)
            scheduler.sync(afterFiring = true)
        }
    }

    private fun showNotification(context: Context) {
        val channelId = NotificationChannels.DAILY
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        NotificationChannels.ensureDaily(context)

        val notificationIntent = Intent(context, Portadas::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_news)
            .setColor(ContextCompat.getColor(context, R.color.primary))
            .setLights(Color.CYAN, 300, 300)
            .setContentTitle(context.getString(R.string.notification_name))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setContentText(context.getString(R.string.notification_description))
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
