package sergirex.portadasperiodicos.ui

import android.app.Activity
import android.app.Application
import android.content.res.Configuration
import android.os.Bundle
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import sergirex.portadasperiodicos.R
import java.util.WeakHashMap

/**
 * Optional pure-black dark theme for AMOLED screens: a theme overlay applied to each Activity when dark mode is
 * active and the setting is on. Activities call [apply] before `super.onCreate`; [install] recreates the ones
 * already open when the setting changes.
 *
 * The stored value is read in the background from process start; only the first Activity created in dark mode
 * waits for that read (if it hasn't finished yet), so it is themed right without a flash. Light mode and processes
 * started just for a widget, alarm or boot broadcast never wait.
 */
object PureBlackTheme {
    @Volatile
    private var enabled = false

    // The setting as stored at process start, until it has been read once (or [setEnabled] got a newer value).
    @Volatile
    private var initial: Deferred<Boolean>? = null

    // What each live Activity was themed with, so it can be recreated when that stops matching the setting.
    private val applied = WeakHashMap<Activity, Boolean>()

    fun apply(activity: Activity) {
        val black = wanted(activity)
        applied[activity] = black
        if (black) activity.theme.applyStyle(R.style.ThemeOverlay_Portadas_PureBlack, true)
    }

    fun install(application: Application, initial: Deferred<Boolean>) {
        this.initial = initial
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) = recreateIfStale(activity)
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    fun setEnabled(value: Boolean) {
        initial = null
        if (enabled == value) return
        enabled = value
        applied.keys.toList().forEach(::recreateIfStale)
    }

    private fun recreateIfStale(activity: Activity) {
        val themedWith = applied[activity] ?: return
        if (themedWith != wanted(activity) && !activity.isFinishing) activity.recreate()
    }

    private fun wanted(activity: Activity): Boolean {
        val night = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        return night && isEnabled()
    }

    private fun isEnabled(): Boolean {
        initial?.let { pending ->
            enabled = runCatching { runBlocking { pending.await() } }.getOrDefault(false)
            initial = null
        }
        return enabled
    }
}
