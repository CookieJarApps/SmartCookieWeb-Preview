package com.cookiejarapps.android.smartcookieweb.search

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.constraintlayout.widget.ConstraintProperties.BOTTOM
import androidx.constraintlayout.widget.ConstraintProperties.PARENT_ID
import androidx.constraintlayout.widget.ConstraintProperties.TOP
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.cookiejarapps.android.smartcookieweb.BrowserActivity
import com.cookiejarapps.android.smartcookieweb.BrowserDirection
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.cookiejarapps.android.smartcookieweb.search.*
import kotlinx.android.synthetic.main.fragment_search_dialog.*
import kotlinx.android.synthetic.main.fragment_search_dialog.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import mozilla.components.browser.toolbar.behavior.ToolbarPosition
import mozilla.components.concept.storage.HistoryStorage
import mozilla.components.lib.state.ext.consumeFlow
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.ktx.android.content.getColorFromAttr
import mozilla.components.support.ktx.android.view.hideKeyboard
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifChanged
import mozilla.components.ui.autocomplete.InlineAutocompleteEditText
import com.cookiejarapps.android.smartcookieweb.search.awesomebar.AwesomeBarView
import com.cookiejarapps.android.smartcookieweb.search.toolbar.ToolbarView

typealias SearchDialogFragmentStore = SearchFragmentStore

@SuppressWarnings("LargeClass", "TooManyFunctions")
class SearchDialogFragment : AppCompatDialogFragment(), UserInteractionHandler {
    private lateinit var interactor: SearchDialogInteractor
    private lateinit var store: SearchDialogFragmentStore
    private lateinit var toolbarView: ToolbarView
    private lateinit var awesomeBarView: AwesomeBarView
    private var firstUpdate = true

    private var dialogHandledAction = false

    override fun onStart() {
        super.onStart()
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
    }

    override fun onStop() {
        super.onStop()
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.SearchDialogStyle)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireContext(), this.theme) {
            override fun onBackPressed() {
                this@SearchDialogFragment.onBackPressed()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val args by navArgs<SearchDialogFragmentArgs>()
        val view = inflater.inflate(R.layout.fragment_search_dialog, container, false)
        val activity = requireActivity() as BrowserActivity
        val isPrivate = activity.browsingModeManager.mode.isPrivate

        store = SearchDialogFragmentStore(
            createInitialSearchFragmentState(
                components,
                tabId = args.sessionId,
                pastedText = args.pastedText
            )
        )

        interactor = SearchDialogInteractor(
            SearchDialogController(
                activity = activity,
                store = components.store,
                tabsUseCases = components.tabsUseCases,
                fragmentStore = store,
                navController = findNavController(),
                dismissDialog = {
                    dialogHandledAction = true
                    dismissAllowingStateLoss()
                },
                clearToolbarFocus = {
                    dialogHandledAction = true
                    toolbarView.view.hideKeyboard()
                    toolbarView.view.clearFocus()
                },
                focusToolbar = { toolbarView.view.edit.focus() }
            )
        )

        toolbarView = ToolbarView(
            requireContext(),
            interactor,
            historyStorageProvider(),
            isPrivate,
            view.toolbar,
            components.engine
        )

        val awesomeBar = view.awesome_bar
        awesomeBar.customizeForBottomToolbar = UserPreferences(requireContext()).toolbarPosition == ToolbarPosition.BOTTOM.ordinal

        awesomeBarView = AwesomeBarView(
            activity,
            interactor,
            awesomeBar
        )

        view.awesome_bar.setOnTouchListener { _, _ ->
            view.hideKeyboard()
            false
        }

        awesomeBarView.view.setOnEditSuggestionListener(toolbarView.view::setSearchTerms)

        val urlView = toolbarView.view
            .findViewById<InlineAutocompleteEditText>(R.id.mozac_browser_toolbar_edit_url_view)
        urlView?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO

        components.engine.speculativeCreateSession(isPrivate)

        if (findNavController().previousBackStackEntry?.destination?.id == R.id.homeFragment) {
            // When displayed above home, dispatches the touch events to scrim area to the HomeFragment
            view.search_wrapper.background = ColorDrawable(Color.TRANSPARENT)
            dialog?.window?.decorView?.setOnTouchListener { _, event ->
                requireActivity().dispatchTouchEvent(event)
                false
            }
        }

        return view
    }

    @ExperimentalCoroutinesApi
    @SuppressWarnings("LongMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        consumeFlow(components.store) { flow ->
            flow.map { state -> state.search }
                .ifChanged()
                .collect { search ->
                    store.dispatch(SearchFragmentAction.UpdateSearchState(search))
                }
        }

        setupConstraints(view)

        // When displayed above browser, dismisses dialog on clicking scrim area
        if (findNavController().previousBackStackEntry?.destination?.id == R.id.browserFragment) {
            search_wrapper.setOnClickListener {
                it.hideKeyboard()
                dismissAllowingStateLoss()
            }
        }

        view.search_engines_shortcut_button.setOnClickListener {
            interactor.onSearchShortcutsButtonClicked()
        }

        fill_link_from_clipboard.setOnClickListener {
            view.hideKeyboard()
            toolbarView.view.clearFocus()
            (activity as BrowserActivity)
                .openToBrowserAndLoad(
                    searchTermOrURL = requireContext().components.clipboardHandler.url ?: "",
                    newTab = store.state.tabId == null,
                    from = BrowserDirection.FromSearchDialog
                )
        }
        consumeFrom(store) {
            /*
            * firstUpdate is used to make sure we keep the awesomebar hidden on the first run
            *  of the searchFragmentDialog. We only turn it false after the user has changed the
            *  query as consumeFrom may run several times on fragment start due to state updates.
            * */
            if (it.url != it.query) firstUpdate = false
            awesome_bar?.visibility = if (shouldShowAwesomebar(it)) View.VISIBLE else View.INVISIBLE
            updateClipboardSuggestion(it, requireContext().components.clipboardHandler.url)
            updateToolbarContentDescription(it)
            updateSearchShortcutsIcon(it)
            toolbarView.update(it)
            awesomeBarView.update(requireContext(), it)
        }
    }

    private fun shouldShowAwesomebar(searchFragmentState: SearchFragmentState) =
        !firstUpdate && searchFragmentState.query.isNotBlank() || searchFragmentState.showSearchShortcuts


    override fun onPause() {
        super.onPause()
        view?.hideKeyboard()
    }

    /*
     * This way of dismissing the keyboard is needed to smoothly dismiss the keyboard while the dialog
     * is also dismissing. For example, when clicking a top site on home while this dialog is showing.
     */
    private fun hideDeviceKeyboard() {
        // If the interactor/controller has handled a search event itself, it will hide the keyboard.
        if (!dialogHandledAction) {
            val imm =
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        hideDeviceKeyboard()
    }

    override fun onBackPressed(): Boolean {
        view?.hideKeyboard()
        dismissAllowingStateLoss()
        return true
    }

    private fun historyStorageProvider(): HistoryStorage? {
        return components.historyStorage
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun setupConstraints(view: View) {
        if (UserPreferences(view.context).toolbarPosition == ToolbarPosition.BOTTOM.ordinal) {
            ConstraintSet().apply {
                clone(search_wrapper)

                clear(toolbar.id, TOP)
                connect(toolbar.id, BOTTOM, PARENT_ID, BOTTOM)

                clear(pill_wrapper.id, BOTTOM)
                connect(pill_wrapper.id, BOTTOM, toolbar.id, TOP)

                clear(fill_link_from_clipboard.id, TOP)
                connect(fill_link_from_clipboard.id, BOTTOM, pill_wrapper.id, TOP)

                clear(fill_link_divider.id, TOP)
                connect(fill_link_divider.id, BOTTOM, fill_link_from_clipboard.id, TOP)

                applyTo(search_wrapper)
            }
        }
    }

    private fun updateClipboardSuggestion(searchState: SearchFragmentState, clipboardUrl: String?) {
        val shouldShowView = searchState.showClipboardSuggestions &&
                searchState.query.isEmpty() &&
                !clipboardUrl.isNullOrEmpty()

        fill_link_from_clipboard.isVisible = shouldShowView
        fill_link_divider.isVisible = shouldShowView
        pill_wrapper_divider.isVisible =
            !(shouldShowView && UserPreferences(requireContext()).toolbarPosition == ToolbarPosition.BOTTOM.ordinal)
        clipboard_url.isVisible = shouldShowView
        clipboard_title.isVisible = shouldShowView
        link_icon.isVisible = shouldShowView

        clipboard_url.text = clipboardUrl

        fill_link_from_clipboard.contentDescription = "${clipboard_title.text}, ${clipboard_url.text}."

        if (clipboardUrl != null && !((activity as BrowserActivity).browsingModeManager.mode.isPrivate)) {
            components.engine.speculativeConnect(clipboardUrl)
        }
    }

    private fun updateToolbarContentDescription(searchState: SearchFragmentState) {
        val urlView = toolbarView.view
            .findViewById<InlineAutocompleteEditText>(R.id.mozac_browser_toolbar_edit_url_view)

        searchState.searchEngineSource.searchEngine?.let { engine ->
            toolbarView.view.contentDescription = engine.name + ", " + urlView.hint
        }

        urlView?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun updateSearchShortcutsIcon(searchState: SearchFragmentState) {
        view?.apply {
            search_engines_shortcut_button.isVisible = searchState.areShortcutsAvailable

            val showShortcuts = searchState.showSearchShortcuts
            search_engines_shortcut_button.isChecked = showShortcuts

            val color = android.R.attr.textColorPrimary
            search_engines_shortcut_button.compoundDrawables[0]?.setTint(
                requireContext().getColorFromAttr(color)
            )
        }
    }
}
