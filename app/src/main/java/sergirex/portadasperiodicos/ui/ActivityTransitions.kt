package sergirex.portadasperiodicos.ui

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.view.Window
import com.google.android.material.transition.platform.MaterialSharedAxis

/**
 * Shared-axis (horizontal) transitions between the home screen and the screens opened from it: going deeper
 * slides forward, going back slides back. The home screen is the "parent" and the detail and settings screens
 * its "children". Window transitions need the platform flavor of Material's transitions.
 *
 * Call [asParent] / [asChild] in `onCreate` before `super.onCreate`, and open children with [launch].
 */
object ActivityTransitions {
    fun asParent(activity: Activity) = with(activity.window) {
        requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        exitTransition = axis(forward = true)
        reenterTransition = axis(forward = false)
    }

    fun asChild(activity: Activity) = with(activity.window) {
        requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        enterTransition = axis(forward = true)
        returnTransition = axis(forward = false)
    }

    fun launch(activity: Activity, intent: Intent) {
        activity.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(activity).toBundle())
    }

    private fun axis(forward: Boolean) = MaterialSharedAxis(MaterialSharedAxis.X, forward).apply {
        // The system bars stay put instead of sliding with the content.
        excludeTarget(android.R.id.statusBarBackground, true)
        excludeTarget(android.R.id.navigationBarBackground, true)
    }
}
