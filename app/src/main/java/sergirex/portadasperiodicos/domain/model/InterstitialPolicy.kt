package sergirex.portadasperiodicos.domain.model

import javax.inject.Inject
import javax.inject.Singleton

/**
 * When the detail screen may show an interstitial as the user leaves it. It counts covers opened since the last one was
 * shown and also keeps a minimum time between two, so someone flicking through covers is not interrupted every few
 * seconds. The counters live for the app process only: a fresh start begins a new count, which is gentler on users.
 * Use from the main thread.
 */
@Singleton
class InterstitialPolicy @Inject constructor() {
    private var opensSinceShown = 0
    private var lastShownAt: Long? = null

    fun recordOpen() {
        opensSinceShown++
    }

    fun shouldShow(now: Long = System.currentTimeMillis()): Boolean {
        if (opensSinceShown < EVERY_N_OPENS) return false
        val last = lastShownAt ?: return true
        return now - last >= MIN_INTERVAL_MS
    }

    fun recordShown(now: Long = System.currentTimeMillis()) {
        opensSinceShown = 0
        lastShownAt = now
    }

    companion object {
        const val EVERY_N_OPENS = 5
        const val MIN_INTERVAL_MS = 3 * 60 * 1000L
    }
}
