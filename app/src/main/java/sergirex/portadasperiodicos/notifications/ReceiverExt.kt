package sergirex.portadasperiodicos.notifications

import android.content.BroadcastReceiver
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "launchAsync"

// The system gives a receiver about 10 seconds after onReceive() before it is considered stuck.
private const val BUDGET_MS = 9_000L

/**
 * Runs [block] while keeping the receiver's process alive: without goAsync()'s PendingResult
 * still open, the process can be killed the moment onReceive() returns. The work is bounded by
 * the receiver time limit and its failures are logged, never thrown — a crash here would take
 * the whole app process down for a background refresh nobody is looking at.
 */
fun BroadcastReceiver.launchAsync(block: suspend () -> Unit) {
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.Default).launch {
        try {
            withTimeout(BUDGET_MS) { block() }
        } catch (error: TimeoutCancellationException) {
            Log.w(TAG, "Receiver work exceeded its time budget", error)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Receiver work failed", error)
        } finally {
            pendingResult.finish()
        }
    }
}
