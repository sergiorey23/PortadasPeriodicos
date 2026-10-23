package sergirex.portadasperiodicos.ui

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.view.Window
import androidx.annotation.IdRes
import com.google.android.material.color.MaterialColors
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import com.google.android.material.transition.platform.MaterialElevationScale
import com.google.android.material.transition.platform.MaterialFade

/**
 * Transitions between the home screen and the screens opened from it. Settings fades in over the home screen. A
 * cover grows from its place in the grid into the detail screen's content area and shrinks back into the grid on
 * return (Material's container transform, like opening a photo in a gallery); the app bar just fades. The home screen
 * stays underneath, slightly scaled down, so the window background is never uncovered. Window transitions need the
 * platform flavor of Material's transitions.
 *
 * Call [asParent] / [asChild] / [prepareExpandingChild] in `onCreate` before `super.onCreate`.
 */
object ActivityTransitions {
    private const val FADE_MS = 220L
    private const val EXPAND_MS = 320L
    private const val SHRINK_MS = 260L
    private const val SCALE_MS = 300L
    private const val COVER_CONTAINER = "cover_container"

    /**
     * [contentId] is the home screen's grid of covers. It stays visible (scaled down and faded a little) while another
     * screen opens over it: without an exit transition its views are hidden and the window shows black behind the
     * animation. The app bar and tabs are left out so they stay still instead of growing out of step with the new
     * screen's app bar.
     */
    fun asParent(activity: Activity, @IdRes contentId: Int) = with(activity) {
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        window.sharedElementsUseOverlay = false // keep the system bars still during the transition
        setExitSharedElementCallback(MaterialContainerTransformSharedElementCallback())
        window.exitTransition = elevationScale(growing = false, contentId)
        window.reenterTransition = elevationScale(growing = true, contentId)
    }

    /** [rootId] is the screen's root view: the transition fades that one view instead of every child separately. */
    fun asChild(activity: Activity, @IdRes rootId: Int) = with(activity.window) {
        requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        enterTransition = fade(rootId)
        returnTransition = fade(rootId)
    }

    /** For the screen a cover grows into; finish with [expandFrom] once its views exist. */
    fun prepareExpandingChild(activity: Activity) {
        activity.window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        activity.window.sharedElementsUseOverlay = false
        activity.setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())
    }

    /**
     * Makes [content] the view the opened cover grows into (and shrinks out of when going back). The app bar,
     * [chromeId], is not part of it: it stays put and only fades.
     */
    fun expandFrom(activity: Activity, content: View, @IdRes chromeId: Int) {
        content.transitionName = COVER_CONTAINER
        val surface = MaterialColors.getColor(content, com.google.android.material.R.attr.colorSurface)
        with(activity.window) {
            sharedElementEnterTransition = container(content, surface, EXPAND_MS)
            sharedElementReturnTransition = container(content, surface, SHRINK_MS)
            enterTransition = fade(chromeId)
            returnTransition = fade(chromeId)
        }
    }

    /** The screen no longer shrinks into the grid; all of it ([rootId]) fades out instead (e.g. the user swiped to another cover). */
    fun stopSharing(activity: Activity, content: View, @IdRes rootId: Int) {
        content.transitionName = null
        activity.window.returnTransition = fade(rootId)
    }

    fun launch(activity: Activity, intent: Intent) {
        activity.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(activity).toBundle())
    }

    /** Opens [intent] growing out of [sharedView], the grid item that was tapped. */
    fun launchFrom(activity: Activity, intent: Intent, sharedView: View) {
        val options = ActivityOptions.makeSceneTransitionAnimation(activity, sharedView, COVER_CONTAINER)
        activity.startActivity(intent, options.toBundle())
    }

    private fun container(view: View, surface: Int, millis: Long) = MaterialContainerTransform().apply {
        addTarget(view.id)
        setAllContainerColors(surface)
        scrimColor = Color.TRANSPARENT
        fadeMode = MaterialContainerTransform.FADE_MODE_CROSS
        duration = millis
    }

    private fun elevationScale(growing: Boolean, @IdRes viewId: Int) = MaterialElevationScale(growing).apply {
        addTarget(viewId)
        duration = SCALE_MS
    }

    // Targeting only one view keeps the system bars still, and matters for correctness: a visibility transition that
    // animates every view can leave one stuck on screen if it was hidden while the transition ran (a loading spinner).
    private fun fade(@IdRes viewId: Int) = MaterialFade().apply {
        addTarget(viewId)
        duration = FADE_MS
        secondaryAnimatorProvider = null // a plain fade, without MaterialFade's slight scale
    }
}
