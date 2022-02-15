package com.cookiejarapps.android.smartcookieweb.browser.bookmark.ui

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.items.BookmarkFolderItem
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.items.BookmarkItem
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.items.BookmarkSiteItem
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.repository.BookmarkManager
import com.cookiejarapps.android.smartcookieweb.browser.shortcuts.ShortcutDatabase
import com.cookiejarapps.android.smartcookieweb.browser.shortcuts.ShortcutEntity
import com.cookiejarapps.android.smartcookieweb.databinding.FragmentBookmarkBinding
import com.cookiejarapps.android.smartcookieweb.databinding.FragmentBrowserBinding
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mozilla.components.browser.state.selector.selectedTab

class BookmarkFragment : Fragment(), BookmarkAdapter.OnBookmarkRecyclerListener, PathView.OnPathViewClickListener {

    private var _binding: FragmentBookmarkBinding? = null
    private val binding get() = _binding!!

    private var showPathHeader = true

    private val root: BookmarkFolderItem
        get() = manager.root

    private lateinit var pathAdapter: PathViewAdapter<BookmarkPath>

    private lateinit var adapter: BookmarkAdapter
    private lateinit var manager: BookmarkManager
    private lateinit var currentFolder: BookmarkFolderItem

    private lateinit var touchListener: RecyclerViewItemTouchListener

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setHasOptionsMenu(true)

        _binding = FragmentBookmarkBinding.inflate(inflater, container, false)
        val view = binding.root

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val activity = requireActivity()

        val recyclerView = binding.recyclerView
        val breadCrumbsView = binding.pathView

        binding.toolBar.inflateMenu(R.menu.bookmark_menu)

        binding.toolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.addBookmark -> {
                    AddBookmarkSiteDialog(activity, context?.components?.store?.state?.selectedTab?.content?.title ?: "", context?.components?.store?.state?.selectedTab?.content?.url ?: "")
                        .setOnClickListener { _, _ -> adapter.notifyDataSetChanged() }
                        .show()
                }
                R.id.addFolder -> {
                    AddBookmarkFolderDialog(activity, manager, getString(R.string.new_folder_name), currentFolder)
                        .setOnClickListener { _, _ -> adapter.notifyDataSetChanged() }
                        .show()
                }
            }
            true
        }


        val layoutManager = LinearLayoutManager(context)
        layoutManager.stackFromEnd = UserPreferences(requireContext()).stackFromBottom
        recyclerView.layoutManager = layoutManager
        val helper = ItemTouchHelper(Touch())
        helper.attachToRecyclerView(recyclerView)
        recyclerView.addItemDecoration(helper)

        manager = BookmarkManager.getInstance(activity)

        touchListener = RecyclerViewItemTouchListener()

        recyclerView.addOnItemTouchListener(touchListener)

        pathAdapter = PathViewAdapter(activity, breadCrumbsView)
        breadCrumbsView.adapter = pathAdapter

        val basePos = getFirstPosition()

        addAllFolderPaths(basePos)
        breadCrumbsView.listener = this

        setList(basePos)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val parent = currentFolder.parent
                if (parent != null) {
                    setList(parent)
                    pathAdapter.select(pathAdapter.selectedItem - 1)
                    binding.subBar.setExpanded(true, false)
                } else {
                    requireActivity().finish()
                }
            }
        })
    }

    override fun onPathItemClick(position: Int) {
        setList(pathAdapter.crumbs[position].folder)
        pathAdapter.select(position)
    }

    private fun setList(folder: BookmarkFolderItem) {
        val activity = requireActivity()

        binding.toolBar.setTitle(R.string.action_bookmarks)

        currentFolder = folder
        setPath(folder)

        adapter = BookmarkAdapter(activity, folder.itemList, this)

        binding.recyclerView.adapter = adapter
    }

    override fun onRecyclerItemClicked(v: View, position: Int) {
        when (val item = currentFolder[position]) {
            is BookmarkSiteItem -> {
                sendUrl(item.url, 0)
            }
            is BookmarkFolderItem -> {
                setList(item)
                pathAdapter.addItem(getPath(item))
                binding.subBar.setExpanded(true, false)
            }
            else -> throw IllegalStateException()
        }
    }

    override fun onRecyclerItemLongClicked(v: View, position: Int): Boolean {
        return false
    }

    override fun onIconClick(v: View, position: Int) {
        val item = adapter[position]
        if (item is BookmarkSiteItem) {
            onShowMenu(v, position)
        }
    }

    override fun onShowMenu(v: View, position: Int) {
        showContextMenu(v, position)
    }

    private fun sendUrl(url: String?, target: Int) {
        when(target){
            0 -> {
                if (url != null) {
                    components.sessionUseCases.loadUrl(url)
                    if (!requireActivity().findNavController(R.id.container).popBackStack(R.id.browserFragment, false)) {
                        requireActivity().findNavController(R.id.container).navigate(R.id.browserFragment)
                    }
                    closeDrawer()
                }
            }
            1 -> {
                if (url != null) {
                    components.tabsUseCases.addTab.invoke(url, selectTab = true)
                    if (!requireActivity().findNavController(R.id.container).popBackStack(R.id.browserFragment, false)) {
                        requireActivity().findNavController(R.id.container).navigate(R.id.browserFragment)
                    }
                }
            }
            2 -> {
                if (url != null) {
                    if (!requireActivity().findNavController(R.id.container).popBackStack(R.id.browserFragment, false)) {
                        requireActivity().findNavController(R.id.container).navigate(R.id.browserFragment)
                    }
                    components.tabsUseCases.addTab.invoke(url, selectTab = false)
                }
            }
        }
    }

    private fun closeDrawer(){
        val drawerLayout = activity?.findViewById<DrawerLayout>(R.id.drawer_layout)
        val bookmarksDrawer = if(UserPreferences(requireContext()).swapDrawers) requireActivity().findViewById<FrameLayout>(R.id.left_drawer) else requireActivity().findViewById<FrameLayout>(R.id.right_drawer)

        if (bookmarksDrawer != null) {
            drawerLayout?.closeDrawer(bookmarksDrawer)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.bookmark_menu, menu)
        showPathHeader(false)
    }

    private fun showContextMenu(v: View, index: Int) {
        val activity = requireActivity()

        val menu = PopupMenu(activity, v, touchListener.gravity)
        val inflater = menu.menuInflater

        val bookmarkItem: BookmarkItem = when {
                currentFolder[index] is BookmarkSiteItem -> {
                    inflater.inflate(R.menu.bookmark_site_menu, menu.menu)
                    adapter[index]
                }
                else -> {
                    inflater.inflate(R.menu.bookmark_folder_menu, menu.menu)
                    adapter[index]
                }
            }

        menu.setOnMenuItemClickListener { item ->
            onContextMenuClick(item.itemId, bookmarkItem, index)
            true
        }
        menu.show()
    }

    private fun onContextMenuClick(id: Int, item: BookmarkItem?, index: Int) {
        val activity = requireActivity()

        when (id) {
            R.id.open -> sendUrl((item as BookmarkSiteItem).url, 0)
            R.id.openNew -> sendUrl((item as BookmarkSiteItem).url, 1)
            R.id.openBg -> sendUrl((item as BookmarkSiteItem).url, 2)
            R.id.share -> {
                val bookmark = item as BookmarkSiteItem
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                if (bookmark.title != "") {
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, bookmark.title)
                }
                shareIntent.putExtra(Intent.EXTRA_TEXT, bookmark.url)
                ContextCompat.startActivity(
                        requireContext(),
                        Intent.createChooser(
                                shareIntent,
                                requireContext().resources.getString(R.string.mozac_selection_context_menu_share)
                        ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        null
                )
            }
            R.id.addShortcut -> if (item is BookmarkSiteItem) {
                val database = Room.databaseBuilder(
                    requireContext(),
                    ShortcutDatabase::class.java, "shortcut-database"
                ).build()

                GlobalScope.launch {
                    // UPDATE HOMEPAGE
                    database.shortcutDao().insertAll(ShortcutEntity(url = item.url, title = item.title))
                }
            }
            R.id.editBookmark -> if (item is BookmarkSiteItem) {
                AddBookmarkSiteDialog(activity, manager, item)
                    .setOnClickListener { _, _ -> adapter.notifyDataSetChanged() }
                    .show()
            } else if (item is BookmarkFolderItem) {
                AddBookmarkFolderDialog(activity, manager, item)
                    .setOnClickListener { _, _ -> adapter.notifyDataSetChanged() }
                    .show()
            }
            R.id.moveBookmark -> BookmarkFoldersDialog(activity, manager)
                .setTitle(R.string.move_bookmark)
                .setCurrentFolder(currentFolder, item)
                .setOnFolderSelectedListener { folder ->
                    manager.moveTo(currentFolder, folder, index)

                    manager.save()
                    adapter.notifyDataSetChanged()
                    false
                }
                .show()
            R.id.deleteBookmark -> AlertDialog.Builder(activity)
                .setTitle(R.string.confirm)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    manager.remove(currentFolder, index)
                    manager.save()
                    adapter.notifyDataSetChanged()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    override fun onSelectionStateChange(items: Int) {
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (UserPreferences(requireContext()).bookmarkFolder) {
            UserPreferences(requireContext()).bookmarkFolderId = currentFolder.id
        }
        _binding = null
    }

    private inner class Touch : ItemTouchHelper.Callback() {

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            return makeFlag(ItemTouchHelper.ACTION_STATE_DRAG, ItemTouchHelper.DOWN or ItemTouchHelper.UP)
        }

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            adapter.move(viewHolder.adapterPosition, target.adapterPosition)
            manager.save()
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

        override fun isLongPressDragEnabled(): Boolean {
            return true
        }
    }

    private fun showPathHeader(show: Boolean) {
        binding.apply {
            if (show) {
                if (subBar.childCount == 0) {
                    subBar.addView(pathView)
                }
            } else {
                if (subBar.childCount == 1) {
                    subBar.removeView(pathView)
                }
            }
            showPathHeader = show
        }
    }

    private fun setPath(folder: BookmarkFolderItem) {
        if (folder.title.isNullOrEmpty()) {
            showPathHeader(false)
        }
        else {
            showPathHeader(true)
        }
    }

    private fun getFirstPosition(): BookmarkFolderItem {
        var id = -1L
        if (UserPreferences(requireContext()).bookmarkFolder  || id > 0) {
            if (id < 1) {
                id = UserPreferences(requireContext()).bookmarkFolderId
            }
            val item = manager[id]
            if (item is BookmarkFolderItem) {
                return item
            }
        }
        return root
    }

    private fun getPath(folder: BookmarkFolderItem): BookmarkPath {
        val title = folder.title
        return BookmarkPath(folder, if (title.isNullOrEmpty()) getString(R.string.action_bookmarks) else title)
    }

    private fun addAllFolderPaths(target: BookmarkFolderItem) {
        val parent = target.parent
        if(parent != null){
            addAllFolderPaths(parent)
        }
        pathAdapter.addItem(getPath(target))
    }

    private class BookmarkPath(val folder: BookmarkFolderItem, override val title: String) : PathView.Path {
        override fun equals(other: Any?): Boolean {
            return other is BookmarkPath && other.folder == folder
        }

        override fun hashCode(): Int {
            return folder.hashCode()
        }
    }
}