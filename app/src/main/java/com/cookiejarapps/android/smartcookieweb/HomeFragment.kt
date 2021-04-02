package com.cookiejarapps.android.smartcookieweb

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import androidx.fragment.app.Fragment

class HomeFragment(): Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<SearchView>(R.id.search_bar).setOnQueryTextFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    val imm: InputMethodManager = view.context
                        .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                }
            }
    }
}