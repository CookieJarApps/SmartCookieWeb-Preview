package com.cookiejarapps.android.smartcookieweb.browser.bookmark.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cookiejarapps.android.smartcookieweb.R

class PathArrowDecoration(context: Context, color: Int) : RecyclerView.ItemDecoration() {
    private val icon: Drawable = ContextCompat.getDrawable(context, R.drawable.ic_baseline_chevron_right)!!
    private val leftPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8.toFloat(), context.resources.displayMetrics)

    init {
        icon.setTint(color)
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)

        val pos = parent.getChildAdapterPosition(view)
        outRect.left = if (pos != 0) {
            icon.intrinsicWidth
        } else {
            leftPadding.toInt()
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val padding = (parent.height - icon.intrinsicHeight) / 2
        val arrowTop = parent.paddingTop + padding
        val arrowBottom = arrowTop + icon.intrinsicHeight

        val childCount = parent.childCount
        for (i in 0 until childCount - 1) {
            val child = parent.getChildAt(i)

            val params = child.layoutParams as RecyclerView.LayoutParams

            val arrowLeft = child.right + params.rightMargin
            val arrowRight = arrowLeft + icon.intrinsicWidth

            icon.setBounds(arrowLeft, arrowTop, arrowRight, arrowBottom)
            icon.draw(c)
        }
    }
}