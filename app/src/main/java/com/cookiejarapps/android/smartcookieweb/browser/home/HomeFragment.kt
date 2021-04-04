package com.cookiejarapps.android.smartcookieweb.browser.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.SearchView
import androidx.appcompat.app.AlertDialog
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.room.Room
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.ui.BookmarkFragment
import com.cookiejarapps.android.smartcookieweb.browser.shortcuts.*
import com.cookiejarapps.android.smartcookieweb.browser.tabs.TabsTrayFragment
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mozilla.components.lib.state.ext.consumeFlow
import mozilla.components.support.ktx.kotlin.isUrl
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifChanged
import org.mozilla.gecko.util.ThreadUtils.runOnUiThread


class HomeFragment : Fragment(R.layout.fragment_home) {

    private var database: ShortcutDatabase? = null

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

        GlobalScope.launch {
            database = Room.databaseBuilder(
                    requireContext(),
                    ShortcutDatabase::class.java, "shortcut-database"
            ).build()

            val shortcutDao = database?.shortcutDao()
            val shortcuts: MutableList<ShortcutEntity> = shortcutDao?.getAll() as MutableList

            val adapter = ShortcutGridAdapter(requireContext(), getList(shortcuts))

            runOnUiThread{
                view.shortcut_grid.adapter = adapter
            }
        }

        view.shortcut_grid.setOnItemClickListener { parent, _, position, id ->
            if((view.shortcut_grid.adapter.getItem(position) as ShortcutEntity).add){
                showCreateShortcutDialog(view.shortcut_grid.adapter as ShortcutGridAdapter)
            }
            else{
                findNavController().navigate(
                        R.id.browserFragment
                )
                components.tabsUseCases.addTab.invoke(
                        (view.shortcut_grid.adapter.getItem(position) as ShortcutEntity).url!!,
                        selectTab = true
                )
            }
        }

        view.shortcut_grid.setOnItemLongClickListener { parent, _, position, id ->
            if(position == view.shortcut_grid.adapter.count){
                return@setOnItemLongClickListener true
            }

            val items = arrayOf(resources.getString(R.string.edit_shortcut), resources.getString(R.string.delete_shortcut))

            MaterialAlertDialogBuilder(requireContext())
                    .setTitle(resources.getString(R.string.edit_shortcut))
                    .setItems(items) { dialog, which ->
                        when(which){
                            0 -> showEditShortcutDialog(position, view.shortcut_grid.adapter as ShortcutGridAdapter)
                            1 -> deleteShortcut(view.shortcut_grid.adapter.getItem(which) as ShortcutEntity, view.shortcut_grid.adapter as ShortcutGridAdapter)
                        }
                    }
                    .show()

            return@setOnItemLongClickListener true
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
                if (s.isUrl()) {
                    components.tabsUseCases.addTab.invoke(s, selectTab = true)
                } else {
                    components.searchUseCases.defaultSearch.invoke(
                            s,
                            sessionId = null,
                            searchEngine = null
                    )
                }
                return true
            }
        })
    }

    private fun deleteShortcut(shortcutEntity: ShortcutEntity, adapter: ShortcutGridAdapter) {
        adapter.list.remove(shortcutEntity)
        adapter.notifyDataSetChanged()

        GlobalScope.launch {
            database?.shortcutDao()?.delete(shortcutEntity)
        }
    }

    private fun getList(shortcutEntity: MutableList<ShortcutEntity>): MutableList<ShortcutEntity> {
        shortcutEntity.add(shortcutEntity.size, ShortcutEntity(url = "test", add = true))
        return shortcutEntity
    }

    private fun showEditShortcutDialog(position: Int, adapter: ShortcutGridAdapter){
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle(resources.getString(R.string.edit_shortcut))
        val viewInflated: View = LayoutInflater.from(context).inflate(R.layout.add_shortcut_dialog, view as ViewGroup?, false)
        val input = viewInflated.findViewById<View>(R.id.urlEditText) as EditText
        input.setText(adapter.list[position].url)
        builder.setView(viewInflated)

        builder.setPositiveButton(android.R.string.ok) { dialog, which ->
            val item = adapter.list[position]
            item.url = input.text.toString()
            adapter.notifyDataSetChanged()

            GlobalScope.launch {
                database?.shortcutDao()?.update(item)
            }
        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, which -> dialog.cancel() }

        builder.show()
    }

    private fun showCreateShortcutDialog(adapter: ShortcutGridAdapter){
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle(resources.getString(R.string.add_shortcut))
        val viewInflated: View = LayoutInflater.from(context).inflate(R.layout.add_shortcut_dialog, view as ViewGroup?, false)
        val input = viewInflated.findViewById<View>(R.id.urlEditText) as EditText
        builder.setView(viewInflated)

        builder.setPositiveButton(android.R.string.ok) { dialog, which ->
            dialog.dismiss()
            val list = adapter.list
            list.add(ShortcutEntity(url = input.text.toString()))
            list.removeAt(adapter.list.size - 2)
            adapter.list = getList(list)
            adapter.notifyDataSetChanged()

            GlobalScope.launch {
                database?.shortcutDao()?.insertAll(ShortcutEntity(url = input.text.toString()))
            }
        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, which -> dialog.cancel() }

        builder.show()
    }
}