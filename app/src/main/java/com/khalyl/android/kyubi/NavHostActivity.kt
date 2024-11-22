package com.khalyl.android.kyubi

import androidx.appcompat.app.ActionBar

interface NavHostActivity {
    fun getSupportActionBarAndInflateIfNecessary(): ActionBar
}
