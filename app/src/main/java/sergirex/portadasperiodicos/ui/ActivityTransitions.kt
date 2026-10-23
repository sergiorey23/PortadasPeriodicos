package sergirex.portadasperiodicos.ui

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.view.Window
import androidx.annotation.IdRes
import com.google.android.material.transition.platform.MaterialFade

/**
 * A fade between the home screen and the screens opened from it. The home screen stays put underneath and the
 * opened screen fades in over it (and out again on the way back), so the window background is never uncovered:
 * sliding both screens at once, or fading one out before the other in, flashed an empty screen. Window transitions
 * need the platform flavor of Material's transitions.
 *
 * Call [asParent] / [asChild] in `onCreate` before `super.onCreate`, and open children with [launch].
 */
object ActivityTransitions {
    private const val FADE_MS = 220L

    /** The parent only has to allow scene transitions; it does not animate itself. */
    @Suppress("UNUSED_PARAMETER")
    fun asParent(activity: Activity, @IdRes rootId: Int) {
        activity.window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
    }

    /** [rootId] is the screen's root view: the transition fades that one view instead of every child separately. */
    fun asChild(activity: Activity, @IdRes rootId: Int) = with(activity.window) {
        requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        enterTransition = fade(rootId)
        returnTransition = fade(rootId)
    }

    fun launch(activity: Activity, intent: Intent) {
        activity.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(activity).toBundle())
    }

    // Targeting only the root keeps the system bars still, and matters for correctness: a visibility transition that
    // animates every view can leave one stuck on screen if it was hidden while the transition ran (a loading spinner).
    private fun fade(@IdRes rootId: Int) = MaterialFade().apply {
        addTarget(rootId)
        duration = FADE_MS
        secondaryAnimatorProvider = null // a plain fade, without MaterialFade's slight scale
    }
}
