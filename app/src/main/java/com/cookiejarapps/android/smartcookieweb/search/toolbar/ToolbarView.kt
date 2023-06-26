package com.cookiejarapps.android.smartcookieweb.search.toolbar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.VisibleForTesting
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.cookiejarapps.android.smartcookieweb.R
import mozilla.components.browser.domains.autocomplete.ShippedDomainsProvider
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.storage.HistoryStorage
import mozilla.components.feature.toolbar.ToolbarAutocompleteFeature
import mozilla.components.support.ktx.android.content.getColorFromAttr
import mozilla.components.support.ktx.android.content.res.resolveAttribute
import mozilla.components.support.ktx.android.view.hideKeyboard
import com.cookiejarapps.android.smartcookieweb.search.SearchFragmentState

interface ToolbarInteractor {
    fun onUrlCommitted(url: String)

    fun onEditingCanceled()

    fun onTextChanged(text: String)
}

/**
 * View that contains and configures the BrowserToolbar to only be used in its editing mode.
 */
class ToolbarView(
    private val context: Context,
    private val interactor: ToolbarInteractor,
    private val historyStorage: HistoryStorage?,
    private val isPrivate: Boolean,
    val view: BrowserToolbar,
    engine: Engine
) {

    @VisibleForTesting
    internal var isInitialized = false

    init {
        view.apply {
            editMode()

            setOnUrlCommitListener {
                hideKeyboard()
                interactor.onUrlCommitted(it)
                false
            }

            background = AppCompatResources.getDrawable(
                context, context.theme.resolveAttribute(R.attr.colorSurface)
            )

            edit.hint = context.getString(R.string.search)

            edit.colors = edit.colors.copy(
                text = context.getColorFromAttr(android.R.attr.textColorPrimary),
                hint = context.getColorFromAttr(android.R.attr.textColorSecondary),
                suggestionBackground = ContextCompat.getColor(
                    context,
                    R.color.photonGrey40
                ),
                clear = context.getColorFromAttr(android.R.attr.textColorPrimary)
            )

            edit.setUrlBackground(
                AppCompatResources.getDrawable(context, R.drawable.toolbar_background)
            )

            private = isPrivate

            setOnEditListener(object : mozilla.components.concept.toolbar.Toolbar.OnEditListener {
                override fun onCancelEditing(): Boolean {
                    interactor.onEditingCanceled()
                    return false
                }

                override fun onTextChanged(text: String) {
                    url = text
                    interactor.onTextChanged(text)
                }
            })
        }
    }

    fun update(searchState: SearchFragmentState) {
        if (!isInitialized) {
            view.url = searchState.pastedText ?: searchState.query

            if (searchState.pastedText.isNullOrEmpty()) {
                val termOrQuery = if (searchState.searchTerms.isNotEmpty()) {
                    searchState.searchTerms
                } else {
                    searchState.query
                }
                view.setSearchTerms(termOrQuery)
            }

            interactor.onTextChanged(view.url.toString())

            view.editMode()
            isInitialized = true
        }

        val searchEngine = searchState.searchEngineSource.searchEngine

        if (searchEngine != null) {
            val iconSize =
                context.resources.getDimensionPixelSize(R.dimen.icon_width)

            val scaledIcon = Bitmap.createScaledBitmap(
                searchEngine.icon,
                iconSize,
                iconSize,
                true
            )

            val icon = BitmapDrawable(context.resources, scaledIcon)

            view.edit.setIcon(icon, searchEngine.name)
        }
    }
}
