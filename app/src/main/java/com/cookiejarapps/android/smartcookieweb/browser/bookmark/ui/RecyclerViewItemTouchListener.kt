package com.cookiejarapps.android.smartcookieweb.browser.bookmark.ui

import android.view.Gravity
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewItemTouchListener : RecyclerView.OnItemTouchListener {
    private var location: RecyclerViewTouchLocation = RecyclerViewTouchLocation.NONE

    val gravity: Int
        get() {
            return when (location) {
                RecyclerViewTouchLocation.START -> Gravity.START
                RecyclerViewTouchLocation.END -> Gravity.END
                else -> Gravity.END
            }
        }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_DOWN) {
            val half = rv.width / 2
            val x = e.x
            location = if (x <= half) RecyclerViewTouchLocation.START else RecyclerViewTouchLocation.END
        }
        return false
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}

    private enum class RecyclerViewTouchLocation {
        NONE,
        START,
        END
    }
}