package sergirex.portadasperiodicos.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Alarms are wiped by a reboot or an app update, so the daily reminder is rescheduled here. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val scheduler = context.notificationScheduler()
            launchAsync { scheduler.sync() }
        }
    }
}
