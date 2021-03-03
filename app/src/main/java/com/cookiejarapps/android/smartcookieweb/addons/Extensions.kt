package com.cookiejarapps.android.smartcookieweb.addons

import java.text.NumberFormat
import java.util.Locale

internal fun getFormattedAmount(amount: Int): String {
    return NumberFormat.getNumberInstance(Locale.getDefault()).format(amount)
}
