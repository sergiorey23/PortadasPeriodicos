package sergirex.portadasperiodicos.notifications

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Plain BroadcastReceivers get their dependencies through an entry point: Hilt's
 * @AndroidEntryPoint generated base class can't be called from Kotlin for them.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationSchedulerEntryPoint {
    fun scheduler(): NotificationScheduler
}

fun Context.notificationScheduler(): NotificationScheduler =
    EntryPointAccessors.fromApplication(applicationContext, NotificationSchedulerEntryPoint::class.java).scheduler()
