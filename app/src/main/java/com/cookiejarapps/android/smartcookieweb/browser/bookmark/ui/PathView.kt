package com.cookiejarapps.android.smartcookieweb.browser.bookmark.ui

import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cookiejarapps.android.smartcookieweb.R
import com.google.android.material.color.MaterialColors
import mozilla.components.support.ktx.android.content.getColorFromAttr


class PathView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, @StyleRes defStyle: Int = R.style.TextAppearance_AppCompat_Body1) : RecyclerView(context, attrs, defStyle) {

    internal val highlightedTextColor: Int
    internal val textColor: Int
    private val arrowColor: Int

    val linearLayoutManager = LinearLayoutManager(context).apply {
        orientation = LinearLayoutManager.HORIZONTAL
    }

    var listener: OnPathViewClickListener? = null

    init {
        layoutManager = linearLayoutManager

        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.TextAppearance, 0, 0)
        highlightedTextColor = context.getColorFromAttr(android.R.attr.textColorPrimary)
        textColor = context.getColorFromAttr(android.R.attr.textColorSecondary)
        arrowColor = textColor
        a.recycle()

        addItemDecoration(PathArrowDecoration(context, arrowColor))
    }


    interface Path {
        val title: CharSequence
    }

    interface OnPathViewClickListener {
        fun onPathItemClick(position: Int)
    }
}