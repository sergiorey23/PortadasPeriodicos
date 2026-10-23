package sergirex.portadasperiodicos.ui

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.core.view.NestedScrollingParent3
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat
import kotlin.math.min

/**
 * Pull-to-refresh with Material's loading indicator. The first child is the content (a scrolling list, usually); the
 * second is the indicator. Dragging the content down past its top drags the whole content down with it while the
 * indicator grows into the gap above; releasing past [holdOffset] starts a refresh and the content rests there, with
 * the indicator animating, until [isRefreshing] is turned off.
 */
class PullToRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), NestedScrollingParent3 {

    private val density = resources.displayMetrics.density
    private val holdOffset = HOLD_OFFSET_DP * density
    private val maxOffset = holdOffset * MAX_PULL_FACTOR
    private val parentHelper = NestedScrollingParentHelper(this)

    private lateinit var content: View
    private lateinit var indicator: View

    private var offset = 0f
    private var dragging = false
    private var animator: ValueAnimator? = null
    private var onRefresh: (() -> Unit)? = null

    /** Turning it on from code (a load started elsewhere) shows the indicator without a drag; off retracts it. */
    var isRefreshing = false
        set(value) {
            if (field == value) return
            field = value
            if (!dragging) animateTo(if (value) holdOffset else 0f)
        }

    fun setOnRefreshListener(listener: () -> Unit) {
        onRefresh = listener
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        check(childCount == 2) { "PullToRefreshLayout needs the content and the indicator as its two children" }
        content = getChildAt(0)
        indicator = getChildAt(1)
        applyOffset()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        applyOffset()
    }

    private fun applyOffset() {
        if (!::content.isInitialized) return
        content.translationY = offset
        val progress = (offset / holdOffset).coerceIn(0f, 1f)
        indicator.alpha = (offset / (holdOffset * 0.5f)).coerceIn(0f, 1f)
        indicator.scaleX = 0.4f + 0.6f * progress
        indicator.scaleY = indicator.scaleX
        indicator.translationY = offset / 2f - indicator.height / 2f
    }

    private fun animateTo(target: Float) {
        animator?.cancel()
        if (offset == target) return
        animator = ValueAnimator.ofFloat(offset, target).apply {
            duration = SETTLE_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                offset = it.animatedValue as Float
                applyOffset()
            }
            start()
        }
    }

    private fun release() {
        if (!isRefreshing && offset >= holdOffset) {
            isRefreshing = true
            onRefresh?.invoke()
        }
        animateTo(if (isRefreshing) holdOffset else 0f)
    }

    // --- Nested scrolling: the list reports the part of a downward drag it can't scroll any more. ---

    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean =
        isEnabled && type == ViewCompat.TYPE_TOUCH && axes and ViewCompat.SCROLL_AXIS_VERTICAL != 0

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {
        parentHelper.onNestedScrollAccepted(child, target, axes, type)
        if (type != ViewCompat.TYPE_TOUCH) return
        animator?.cancel()
        dragging = true
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        // Dragging back up while the content is pulled down: shrink the gap before the list scrolls.
        if (type != ViewCompat.TYPE_TOUCH || dy <= 0 || offset <= 0f) return
        val used = min(dy.toFloat(), offset / DRAG_RATE)
        offset -= used * DRAG_RATE
        consumed[1] += used.toInt()
        applyOffset()
    }

    override fun onNestedScroll(
        target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int, consumed: IntArray
    ) {
        if (type != ViewCompat.TYPE_TOUCH || dyUnconsumed >= 0 || offset >= maxOffset) return
        val pull = min(-dyUnconsumed.toFloat(), (maxOffset - offset) / DRAG_RATE)
        offset += pull * DRAG_RATE
        consumed[1] += -pull.toInt()
        applyOffset()
    }

    override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int) =
        onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, IntArray(2))

    override fun onStopNestedScroll(target: View, type: Int) {
        parentHelper.onStopNestedScroll(target, type)
        if (type != ViewCompat.TYPE_TOUCH) return
        dragging = false
        release()
    }

    override fun getNestedScrollAxes(): Int = parentHelper.nestedScrollAxes

    private companion object {
        const val HOLD_OFFSET_DP = 72f
        const val MAX_PULL_FACTOR = 2f
        const val DRAG_RATE = 0.5f
        const val SETTLE_MS = 240L
    }
}
