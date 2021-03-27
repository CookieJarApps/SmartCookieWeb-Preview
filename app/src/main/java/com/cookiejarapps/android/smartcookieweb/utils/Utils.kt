package com.cookiejarapps.android.smartcookieweb

import android.content.Context
import android.text.Spanned
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

@ColorInt
fun Context.getColorResCompat(@AttrRes id: Int): Int {
  val resolvedAttr = TypedValue()
  this.theme.resolveAttribute(id, resolvedAttr, true)
  val colorRes = resolvedAttr.run { if (resourceId != 0) resourceId else data }
  return ContextCompat.getColor(this, colorRes)
}