package com.cookiejarapps.android.smartcookieweb.ext

import android.content.Context
import com.cookiejarapps.android.smartcookieweb.BrowserApp
import com.cookiejarapps.android.smartcookieweb.components.Components

// get app from context
val Context.application: BrowserApp
    get() = applicationContext as BrowserApp

// get components from context
val Context.components: Components
    get() = application.components
