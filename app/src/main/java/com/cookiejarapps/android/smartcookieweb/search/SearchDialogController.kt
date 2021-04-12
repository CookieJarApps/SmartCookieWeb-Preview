package com.cookiejarapps.android.smartcookieweb.search

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.navigation.NavController
import com.cookiejarapps.android.smartcookieweb.BrowserActivity
import com.cookiejarapps.android.smartcookieweb.BrowserDirection
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.tabs.TabsUseCases

/**
 * An interface that handles the view manipulation of the Search, triggered by the Interactor
 */
@Suppress("TooManyFunctions")
interface SearchController {
    fun handleUrlCommitted(url: String)
    fun handleEditingCancelled()
    fun handleTextChanged(text: String)
    fun handleUrlTapped(url: String)
    fun handleSearchTermsTapped(searchTerms: String)
    fun handleSearchShortcutEngineSelected(searchEngine: SearchEngine)
    fun handleClickSearchEngineSettings()
    fun handleExistingSessionSelected(tabId: String)
    fun handleSearchShortcutsButtonClicked()
    fun handleCameraPermissionsNeeded()
}

@Suppress("TooManyFunctions", "LongParameterList")
class SearchDialogController(
    private val activity: BrowserActivity,
    private val store: BrowserStore,
    private val tabsUseCases: TabsUseCases,
    private val fragmentStore: SearchFragmentStore,
    private val navController: NavController,
    private val dismissDialog: () -> Unit,
    private val clearToolbarFocus: () -> Unit,
    private val focusToolbar: () -> Unit
) : SearchController {

    override fun handleUrlCommitted(url: String) {
        when (url) {
            "moz://a" -> openSearchOrUrl("https://mozilla.org")
            else ->
                if (url.isNotBlank()) {
                    openSearchOrUrl(url)
                }
        }
        dismissDialog()
    }

    private fun openSearchOrUrl(url: String) {
        clearToolbarFocus()

        val searchEngine = fragmentStore.state.searchEngineSource.searchEngine

        activity.openToBrowserAndLoad(
            searchTermOrURL = url,
            newTab = fragmentStore.state.tabId == null,
            from = BrowserDirection.FromSearchDialog,
            engine = searchEngine
        )
    }

    override fun handleEditingCancelled() {
        clearToolbarFocus()
    }

    override fun handleTextChanged(text: String) {
        // Display the search shortcuts on each entry of the search fragment (see #5308)
        val textMatchesCurrentUrl = fragmentStore.state.url == text
        val textMatchesCurrentSearch = fragmentStore.state.searchTerms == text

        fragmentStore.dispatch(SearchFragmentAction.UpdateQuery(text))
        fragmentStore.dispatch(
            SearchFragmentAction.ShowSearchShortcutEnginePicker(
                (textMatchesCurrentUrl || textMatchesCurrentSearch || text.isEmpty())
                //&&
                //settings.shouldShowSearchShortcuts
            )
        )
        fragmentStore.dispatch(
            SearchFragmentAction.AllowSearchSuggestionsInPrivateModePrompt(
                text.isNotEmpty() &&
                        activity.browsingModeManager.mode.isPrivate
                //&&
                // !settings.shouldShowSearchSuggestionsInPrivate &&
                // !settings.showSearchSuggestionsInPrivateOnboardingFinished
            )
        )
    }

    override fun handleUrlTapped(url: String) {
        clearToolbarFocus()

        activity.openToBrowserAndLoad(
            searchTermOrURL = url,
            newTab = fragmentStore.state.tabId == null,
            from = BrowserDirection.FromSearchDialog
        )
    }

    override fun handleSearchTermsTapped(searchTerms: String) {
        clearToolbarFocus()

        val searchEngine = fragmentStore.state.searchEngineSource.searchEngine

        activity.openToBrowserAndLoad(
            searchTermOrURL = searchTerms,
            newTab = fragmentStore.state.tabId == null,
            from = BrowserDirection.FromSearchDialog,
            engine = searchEngine,
            forceSearch = true
        )
    }

    override fun handleSearchShortcutEngineSelected(searchEngine: SearchEngine) {
        focusToolbar()
        fragmentStore.dispatch(SearchFragmentAction.SearchShortcutEngineSelected(searchEngine))
    }

    override fun handleSearchShortcutsButtonClicked() {
        val isOpen = fragmentStore.state.showSearchShortcuts
        fragmentStore.dispatch(SearchFragmentAction.ShowSearchShortcutEnginePicker(!isOpen))
    }

    override fun handleClickSearchEngineSettings() {
        clearToolbarFocus()
        //val directions = SearchDialogFragmentDirections.actionGlobalSearchEngineFragment()
        //navController.navigateSafe(R.id.searchDialogFragment, directions)
    }

    override fun handleExistingSessionSelected(tabId: String) {
        clearToolbarFocus()

        tabsUseCases.selectTab(tabId)

        activity.openToBrowser(
            from = BrowserDirection.FromSearchDialog
        )
    }

    /**
     * Creates and shows an [AlertDialog] when camera permissions are needed.
     *
     * In versions above M, [AlertDialog.BUTTON_POSITIVE] takes the user to the app settings. This
     * intent only exists in M and above. Below M, [AlertDialog.BUTTON_POSITIVE] routes to a SUMO
     * help page to find the app settings.
     *
     * [AlertDialog.BUTTON_NEGATIVE] dismisses the dialog.
     */
    override fun handleCameraPermissionsNeeded() {
        val dialog = buildDialog()
        dialog.show()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun buildDialog(): AlertDialog.Builder {
        return AlertDialog.Builder(activity).apply {
            val spannableText = SpannableString(
                "CAMERA PERMISSION NEEDED (NOSTRING)"
            )
            setMessage(spannableText)
            setNegativeButton("CAMERA PERMISSION NEEDED (NOSTRING)") { _, _ ->
                dismissDialog()
            }
            setPositiveButton("CAMERA PERMISSION NEEDED (NOSTRING)") {
                    dialog: DialogInterface, _ ->
                // TODO: M ONLY
                val intent: Intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                /*else {
                    SupportUtils.createCustomTabIntent(
                        activity,
                        SupportUtils.getSumoURLForTopic(
                            activity,
                            SupportUtils.SumoTopic.QR_CAMERA_ACCESS
                        )
                    )
                }*/
                val uri = Uri.fromParts("package", activity.packageName, null)
                intent.data = uri
                dialog.cancel()
                activity.startActivity(intent)
            }
            create()
        }
    }
}
