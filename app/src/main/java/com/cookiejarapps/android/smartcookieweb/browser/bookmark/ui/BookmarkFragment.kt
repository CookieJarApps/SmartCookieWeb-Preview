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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.items.BookmarkFolderItem
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.items.BookmarkItem
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.items.BookmarkSiteItem
import com.cookiejarapps.android.smartcookieweb.browser.bookmark.repository.BookmarkManager
import com.cookiejarapps.android.smartcookieweb.databinding.FragmentBookmarkBinding
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences

class BookmarkFragment : Fragment(), BookmarkAdapter.OnBookmarkRecyclerListener, PathView.OnPathViewClickListener {

    private var viewBinding: FragmentBookmarkBinding? = null

    private var showPathHeader = true

    private val binding: FragmentBookmarkBinding
        get() = viewBinding!!

    private val root: BookmarkFolderItem
        get() = manager.root

    private lateinit var pathAdapter: PathViewAdapter<BookmarkPath>

    private lateinit var adapter: BookmarkAdapter
    private lateinit var manager: BookmarkManager
    private lateinit var currentFolder: BookmarkFolderItem

    private lateinit var touchListener: RecyclerViewItemTouchListener

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setHasOptionsMenu(true)
        viewBinding = FragmentBookmarkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val activity = requireActivity()

        // TODO: swap actionbar for menu + title to fix disappearing options
        (activity as AppCompatActivity).run {
            setSupportActionBar(binding.toolBar)
        }

        val recyclerView = binding.recyclerView
        val breadCrumbsView = binding.pathView

        recyclerView.layoutManager = LinearLayoutManager(activity)
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

        val actionBar = (activity as AppCompatActivity).supportActionBar ?: return
        actionBar.setTitle(R.string.action_bookmarks)

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
                    closeDrawer()
                }
            }
            1 -> {
                if (url != null) {
                    components.tabsUseCases.addTab.invoke(url, selectTab = true)
                }
            }
            2 -> {
                if (url != null) {
                    components.tabsUseCases.addTab.invoke(url, selectTab = false)
                }
            }
        }
    }

    private fun closeDrawer(){
        val drawerLayout = activity?.findViewById<DrawerLayout>(R.id.drawer_layout)
        val bookmarksDrawer = activity?.findViewById<FrameLayout>(R.id.right_drawer)
        if (bookmarksDrawer != null) {
            drawerLayout?.closeDrawer(bookmarksDrawer)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.bookmark_menu, menu)
        showPathHeader(false)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val activity = requireActivity()

        when (item.itemId) {
            R.id.addBookmark -> {

                AddBookmarkSiteDialog(activity, context?.components?.sessionManager?.selectedSession?.title ?: "", context?.components?.sessionManager?.selectedSession?.url ?: "")
                    .setOnClickListener { _, _ -> adapter.notifyDataSetChanged() }
                    .show()
                return true
            }
            R.id.addFolder -> {
                AddBookmarkFolderDialog(activity, manager, getString(R.string.new_folder_name), currentFolder)
                    .setOnClickListener { _, _ -> adapter.notifyDataSetChanged() }
                    .show()
                return true
            }
        }
        return false
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
        viewBinding = null
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