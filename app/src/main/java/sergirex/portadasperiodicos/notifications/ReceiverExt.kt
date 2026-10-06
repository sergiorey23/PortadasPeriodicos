package sergirex.portadasperiodicos.notifications

import android.content.BroadcastReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Runs [block] while keeping the receiver's process alive: without goAsync()'s PendingResult
 * still open, the process can be killed the moment onReceive() returns.
 */
fun BroadcastReceiver.launchAsync(block: suspend () -> Unit) {
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.Default).launch {
        try {
            block()
        } finally {
            pendingResult.finish()
        }
    }
}
