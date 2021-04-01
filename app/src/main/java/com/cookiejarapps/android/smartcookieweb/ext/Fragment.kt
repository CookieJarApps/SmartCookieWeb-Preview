package com.cookiejarapps.android.smartcookieweb.ext

import androidx.fragment.app.Fragment
import com.cookiejarapps.android.smartcookieweb.components.Components
import com.cookiejarapps.android.smartcookieweb.ext.components

/**
 * Get the components of this application.
 */
val Fragment.components: Components
    get() = context!!.components
