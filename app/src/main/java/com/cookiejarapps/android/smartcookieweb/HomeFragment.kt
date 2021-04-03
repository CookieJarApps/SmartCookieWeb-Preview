package com.cookiejarapps.android.smartcookieweb

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.SearchView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cookiejarapps.android.smartcookieweb.addons.AddonsActivity
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.ui.BookmarkFragment
import com.cookiejarapps.android.smartcookieweb.browser.tabs.TabsTrayFragment
import com.cookiejarapps.android.smartcookieweb.components.BrowserMenu
import com.cookiejarapps.android.smartcookieweb.ext.components
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mozilla.components.browser.state.selector.allTabs
import mozilla.components.browser.state.selector.normalTabs
import mozilla.components.lib.state.ext.consumeFlow
import mozilla.components.support.ktx.kotlin.isUrl
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifChanged

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.supportFragmentManager?.beginTransaction()?.apply {
            replace(R.id.left_drawer, TabsTrayFragment())
            commit()
        }

        activity?.supportFragmentManager?.beginTransaction()?.apply {
            replace(R.id.right_drawer, BookmarkFragment())
            commit()
        }

        consumeFlow(components.store) { flow ->
            flow.map { state -> state.restoreComplete }
                .ifChanged()
                .collect { restored ->
                    if (restored) {
                        view.tab_button.setCount(components.store.state.tabs.size)
                    }
                }
        }

        view.tab_button.setOnClickListener {
            val drawerLayout = activity?.findViewById<DrawerLayout>(R.id.drawer_layout)
            val tabsDrawer = activity?.findViewById<FrameLayout>(R.id.left_drawer)
            if (tabsDrawer != null) {
                drawerLayout?.openDrawer(tabsDrawer)
            }
        }

        view.search_bar.setOnQueryTextFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    val imm: InputMethodManager = view.context
                        .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                }
            }

        view.search_bar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextChange(s: String): Boolean {
                return true
            }

            override fun onQueryTextSubmit(s: String): Boolean {
                findNavController().navigate(
                    R.id.browserFragment
                )
                if(s.isUrl()){
                    components.tabsUseCases.addTab.invoke(s, selectTab = true)
                }
                else{
                    components.searchUseCases.defaultSearch.invoke(s, sessionId = null, searchEngine = null)
                }
                return true
            }
        })
    }
}