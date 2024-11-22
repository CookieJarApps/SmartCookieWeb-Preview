package com.khalyl.android.kyubi.ext

import android.content.Context
import com.khalyl.android.kyubi.BrowserApp
import com.khalyl.android.kyubi.components.Components

// get app from context
val Context.application: BrowserApp
    get() = applicationContext as BrowserApp

// get components from context
val Context.components: Components
    get() = application.components
