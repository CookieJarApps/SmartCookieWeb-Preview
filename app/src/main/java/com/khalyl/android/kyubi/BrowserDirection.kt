package com.khalyl.android.kyubi

import androidx.annotation.IdRes

enum class BrowserDirection(@IdRes val fragmentId: Int) {
    FromGlobal(0),
    FromHome(R.id.homeFragment),
    FromSearchDialog(R.id.searchDialogFragment)
}
