package sergirex.portadasperiodicos

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar
import java.util.Date

class AlarmBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        showNotification(context)
        startAlarmBroadcastReceiver(context, forceScheduleNextDay = true)
    }

    fun startAlarmBroadcastReceiver(context: Context, forceScheduleNextDay: Boolean) {
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, Intent(context, AlarmBroadcastReceiver::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
        }
        if (checkIfTheTimeHasPassed(calendar.timeInMillis) || forceScheduleNextDay) {
            calendar.add(Calendar.DATE, 1)
        }
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }

    private fun checkIfTheTimeHasPassed(timeInMillis: Long): Boolean = Date().time > timeInMillis

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
            .setChannelId(channelId)
            .setContentTitle(context.getString(R.string.notification_name))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setContentText(context.getString(R.string.notification_description))
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
