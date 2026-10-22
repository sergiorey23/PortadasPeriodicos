package sergirex.portadasperiodicos.ui

import android.content.Context
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import com.google.android.material.motion.MotionUtils
import java.util.WeakHashMap
import com.google.android.material.R as MaterialR

/**
 * Spring motion from the theme's motion scheme. `Theme.Portadas` extends Material 3 Expressive, which defines six
 * springs (fast / default / slow, each for spatial and effects motion) as `motionSpring*` attributes: a spring is
 * its stiffness and damping ratio instead of a duration and a curve, and it keeps the speed of whatever it
 * interrupts. Spatial springs move, scale or rotate things and may overshoot a little; effects springs fade or
 * recolor and never overshoot. The `fallback` is only used if the theme doesn't define the attribute.
 */
object SpringMotion {
    enum class Spring(@AttrRes val attr: Int, @StyleRes val fallback: Int) {
        FAST_SPATIAL(MaterialR.attr.motionSpringFastSpatial, MaterialR.style.Motion_Material3_Spring_Expressive_Fast_Spatial),
        DEFAULT_SPATIAL(MaterialR.attr.motionSpringDefaultSpatial, MaterialR.style.Motion_Material3_Spring_Expressive_Default_Spatial),
        FAST_EFFECTS(MaterialR.attr.motionSpringFastEffects, MaterialR.style.Motion_Material3_Spring_Expressive_Fast_Effects),
        DEFAULT_EFFECTS(MaterialR.attr.motionSpringDefaultEffects, MaterialR.style.Motion_Material3_Spring_Expressive_Default_Effects)
    }

    // The spring running on each target property, so a new one replaces it instead of fighting it.
    private val running = WeakHashMap<Any, MutableMap<FloatPropertyCompat<*>, SpringAnimation>>()

    /** Animates [property] of [view] to [target]; [startVelocity] is in the property's units per second. */
    fun animate(
        view: View,
        property: DynamicAnimation.ViewProperty,
        target: Float,
        spring: Spring,
        startVelocity: Float = 0f,
        onEnd: (() -> Unit)? = null
    ) = animateProperty(view.context, view, property, target, spring, startVelocity, onEnd)

    /** Same for any object with a [FloatPropertyCompat], e.g. the level of a drawable. */
    fun <T : Any> animateProperty(
        context: Context,
        target: T,
        property: FloatPropertyCompat<T>,
        value: Float,
        spring: Spring,
        startVelocity: Float = 0f,
        onEnd: (() -> Unit)? = null
    ) {
        val byProperty = running.getOrPut(target) { mutableMapOf() }
        byProperty.remove(property)?.cancel()
        val force = MotionUtils.resolveThemeSpringForce(context, spring.attr, spring.fallback).setFinalPosition(value)
        val animation = SpringAnimation(target, property).apply {
            this.spring = force
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
