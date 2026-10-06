package sergirex.portadasperiodicos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import sergirex.portadasperiodicos.notifications.launchAsync
import sergirex.portadasperiodicos.notifications.notificationScheduler

class AlarmBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        showNotification(context)
        val scheduler = context.notificationScheduler()
        launchAsync { scheduler.sync(afterFiring = true) }
    }

    private fun showNotification(context: Context) {
        val channelId = "26081995"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, context.getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(context, Portadas::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.news_icon)
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
