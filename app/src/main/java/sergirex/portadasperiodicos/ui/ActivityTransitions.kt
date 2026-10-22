package sergirex.portadasperiodicos.ui

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.view.Window
import androidx.annotation.IdRes
import com.google.android.material.transition.platform.MaterialSharedAxis

/**
 * Shared-axis (horizontal) transitions between the home screen and the screens opened from it: going deeper
 * slides forward, going back slides back. The home screen is the "parent" and the detail and settings screens
 * its "children". Window transitions need the platform flavor of Material's transitions.
 *
 * Call [asParent] / [asChild] in `onCreate` before `super.onCreate`, and open children with [launch].
 */
object ActivityTransitions {
    /** [rootId] is the screen's root view: the transition moves that one view instead of every child separately. */
    fun asParent(activity: Activity, @IdRes rootId: Int) = with(activity.window) {
        requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        exitTransition = axis(forward = true, rootId)
        reenterTransition = axis(forward = false, rootId)
    }

    fun asChild(activity: Activity, @IdRes rootId: Int) = with(activity.window) {
        requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        enterTransition = axis(forward = true, rootId)
        returnTransition = axis(forward = false, rootId)
    }

    fun launch(activity: Activity, intent: Intent) {
        activity.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(activity).toBundle())
    }

    // Targeting only the root keeps the system bars still, and matters for correctness: a visibility transition that
    // animates every view can leave one stuck on screen if it was hidden while the transition ran (a loading spinner).
    private fun axis(forward: Boolean, @IdRes rootId: Int) =
        MaterialSharedAxis(MaterialSharedAxis.X, forward).apply { addTarget(rootId) }
}
