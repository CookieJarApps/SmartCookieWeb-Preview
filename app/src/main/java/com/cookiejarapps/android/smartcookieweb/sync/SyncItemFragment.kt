package com.cookiejarapps.android.smartcookieweb.sync

import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.utils.BookmarkUtils
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.fragment_sync_items.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mozilla.components.browser.storage.sync.PlacesBookmarksStorage
import mozilla.components.browser.storage.sync.PlacesHistoryStorage
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.lib.dataprotect.SecureAbove22Preferences
import mozilla.components.lib.dataprotect.generateEncryptionKey
import mozilla.components.service.fxa.SyncEngine
import mozilla.components.service.fxa.sync.GlobalSyncableStoreProvider
import mozilla.components.service.sync.logins.SyncableLoginsStorage
import org.mozilla.gecko.util.ThreadUtils

private const val PASSWORDS_ENCRYPTION_KEY_STRENGTH = 256

class SyncItemFragment: Fragment() {

    private val historyStorage = lazy {
        PlacesHistoryStorage(requireContext())
    }

    private val bookmarksStorage = lazy {
        PlacesBookmarksStorage(requireContext())
    }

    private val securePreferences by lazy { SecureAbove22Preferences(requireContext(), "key_store") }

    private val passwordsEncryptionKey by lazy {
        securePreferences.getString(SyncEngine.Passwords.nativeName)
            ?: generateEncryptionKey(PASSWORDS_ENCRYPTION_KEY_STRENGTH).also {
                securePreferences.putString(SyncEngine.Passwords.nativeName, it)
            }
    }

    private val passwordsStorage = lazy { SyncableLoginsStorage(requireContext(), passwordsEncryptionKey) }

    lateinit var bookmarkText: Spanned
    lateinit var historyText: Spanned

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sync_items, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        GlobalSyncableStoreProvider.configureStore(SyncEngine.History to historyStorage)
        GlobalSyncableStoreProvider.configureStore(SyncEngine.Bookmarks to bookmarksStorage)
        GlobalSyncableStoreProvider.configureStore(SyncEngine.Passwords to passwordsStorage)

        GlobalScope.launch(Dispatchers.Main) {
            val bookmarks = BookmarkUtils().withOptionalDesktopFolders(requireContext(), bookmarksStorage.value.getTree("root________", recursive = true)!!, bookmarksStorage)

            var bookmarksRootAndChildren = "<h1>BOOKMARKS</h1><br>"
            fun addTreeNode(node: BookmarkNode, depth: Int) {
                val desc = " ".repeat(depth * 2) + "${node.title} - ${node.url ?: ""}<br><br>"
                bookmarksRootAndChildren += desc
                node.children?.forEach {
                    addTreeNode(it, depth + 1)
                }
            }

            addTreeNode(bookmarks!!, 0)

            val history = historyStorage.value.getVisited()

            var historyChildren = "<h1>HISTORY</h1><br>"
            for(i in history){
                historyChildren += "${i}<br>"
            }

            bookmarkText = Html.fromHtml(bookmarksRootAndChildren)
            historyText = Html.fromHtml(historyChildren)

            ThreadUtils.runOnUiThread {
                tab_content.text = Html.fromHtml(bookmarksRootAndChildren)
            }
        }

        view?.findViewById<TabLayout>(R.id.tabs)?.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        tab_content.text = bookmarkText
                    }
                    1 -> {
                        tab_content.text = historyText
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
}