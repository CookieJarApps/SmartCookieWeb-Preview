package com.cookiejarapps.android.smartcookieweb.icon

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import com.cookiejarapps.android.smartcookieweb.R
import java.text.NumberFormat
import java.util.*

class TabCountView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val numberFormat = NumberFormat.getInstance(Locale.US)
    private val clearMode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    private val overMode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
    private val paint: Paint = Paint().apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private var borderRadius: Float = 0F
    private var borderWidth: Float = 0F
    private val workingRect = RectF()

    private var count: Int = 0

    init {
        val dp: Float = context.getResources().getDisplayMetrics().density

        setLayerType(LAYER_TYPE_SOFTWARE, null)
        context.withStyledAttributes(attrs, R.styleable.TabCountView) {
            paint.color = getColor(R.styleable.TabCountView_tabIconColor, Color.BLACK)
            paint.textSize = getDimension(R.styleable.TabCountView_tabIconTextSize, 14 * dp)
            borderRadius = getDimension(R.styleable.TabCountView_tabIconBorderRadius, 3 * dp)
            borderWidth = getDimension(R.styleable.TabCountView_tabIconBorderWidth, 3 * dp)
        }
    }

    /**
     * Update the number count displayed by the view.
     */
    fun updateCount(count: Int) {
        this.count = count
        contentDescription = count.toString()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val text: String = if (count > 99) {
            "99+"
        } else {
            numberFormat.format(count)
        }

        paint.xfermode = overMode

        workingRect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(workingRect, borderRadius, borderRadius, paint)

        paint.xfermode = clearMode

        val innerRadius = borderRadius - 1
        workingRect.set(borderWidth, borderWidth, (width - borderWidth), (height - borderWidth))
        canvas.drawRoundRect(workingRect, innerRadius, innerRadius, paint)

        paint.xfermode = overMode

        val xPos = width / 2F
        val yPos = height / 2 - (paint.descent() + paint.ascent()) / 2

        canvas.drawText(text, xPos, yPos, paint)

        super.onDraw(canvas)
    }

}
