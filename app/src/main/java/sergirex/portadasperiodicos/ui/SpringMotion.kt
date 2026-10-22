package sergirex.portadasperiodicos.ui

import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import java.util.WeakHashMap

/**
 * Spring motion in the style of Material 3's motion system: a spring is described by its stiffness, its
 * damping ratio (below 1 it overshoots a little) and the velocity it starts with, instead of a duration and
 * a curve. Springs also stay smooth when interrupted, because they continue from the current position and speed.
 */
object SpringMotion {
    class Spring(val stiffness: Float, val dampingRatio: Float)

    /** Movement, scale and rotation: lively, with a small overshoot. */
    val SPATIAL = Spring(stiffness = 600f, dampingRatio = 0.7f)

    /** Opacity: never overshoots, since alpha can't go past 0..1. */
    val EFFECTS = Spring(stiffness = 1600f, dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY)

    // The spring running on each view property, so a new one replaces it instead of fighting it.
    private val running = WeakHashMap<Any, MutableMap<FloatPropertyCompat<*>, SpringAnimation>>()

    /** Animates [property] of [view] to [target] with [spring]; [startVelocity] is in the property's units per second. */
    fun animate(
        view: View,
        property: DynamicAnimation.ViewProperty,
        target: Float,
        spring: Spring,
        startVelocity: Float = 0f,
        onEnd: (() -> Unit)? = null
    ) = animateProperty(view, property, target, spring, startVelocity, onEnd)

    /** Same for any object with a [FloatPropertyCompat], e.g. the level of a drawable. */
    fun <T : Any> animateProperty(
        target: T,
        property: FloatPropertyCompat<T>,
        value: Float,
        spring: Spring,
        startVelocity: Float = 0f,
        onEnd: (() -> Unit)? = null
    ) {
        val byProperty = running.getOrPut(target) { mutableMapOf() }
        byProperty.remove(property)?.cancel()
        val animation = SpringAnimation(target, property).apply {
            this.spring = SpringForce(value)
                .setStiffness(spring.stiffness)
                .setDampingRatio(spring.dampingRatio)
            setStartVelocity(startVelocity)
            addEndListener { _, canceled, _, _ ->
                if (byProperty[property] === this) byProperty.remove(property)
                if (!canceled) onEnd?.invoke()
            }
        }
        byProperty[property] = animation
        animation.start()
    }
}
