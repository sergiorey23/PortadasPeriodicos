package sergirex.portadasperiodicos.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import sergirex.portadasperiodicos.R

/**
 * A diagonal ribbon across the top-right corner of its parent: it starts on the top edge and ends on the
 * right edge, with [text] running along it at 45 degrees. Meant to be placed at `top|end` in a card;
 * the parent's rounded corner clips it. Draws nothing but the band and its text.
 */
class CornerRibbonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    private val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.date_ribbon)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.on_date_ribbon)
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, TEXT_SP, resources.displayMetrics)
    }
    private val band = Path()

    var text: CharSequence = ""
        set(value) {
            field = value
            contentDescription = value
            invalidate()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = (SIZE_DP * density).toInt()
        setMeasuredDimension(resolveSize(size, widthMeasureSpec), resolveSize(size, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        val s = minOf(width, height).toFloat()
        // Two parallel 45-degree lines cut the top-right corner; the band between them is the ribbon.
        val near = s * NEAR
        val far = s * FAR
        band.rewind()
        band.moveTo(width - far, 0f)
        band.lineTo(width - near, 0f)
        band.lineTo(width.toFloat(), near)
        band.lineTo(width.toFloat(), far)
        band.close()
        canvas.drawPath(band, bandPaint)

        // Text sits on the band's centre line, rotated to follow it.
        val mid = (near + far) / 2f
        canvas.save()
        canvas.rotate(45f, width - mid / 2f, mid / 2f)
        val baseline = mid / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(text.toString(), width - mid / 2f, baseline, textPaint)
        canvas.restore()
    }

    private companion object {
        const val SIZE_DP = 62f
        const val NEAR = 0.34f
        const val FAR = 0.78f
        const val TEXT_SP = 9f
    }
}
