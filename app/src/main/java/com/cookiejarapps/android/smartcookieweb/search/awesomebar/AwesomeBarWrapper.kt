package com.cookiejarapps.android.smartcookieweb.search.awesomebar

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.core.graphics.toColor
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.components.toolbar.ToolbarPosition
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import com.cookiejarapps.android.smartcookieweb.theme.FirefoxTheme
import mozilla.components.compose.browser.awesomebar.AwesomeBar
import mozilla.components.compose.browser.awesomebar.AwesomeBarDefaults
import mozilla.components.compose.browser.awesomebar.AwesomeBarOrientation
import mozilla.components.concept.awesomebar.AwesomeBar
import mozilla.components.support.ktx.android.content.getColorFromAttr

class AwesomeBarWrapper @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr), AwesomeBar {
    private val providers = mutableStateOf(emptyList<AwesomeBar.SuggestionProvider>())
    private val text = mutableStateOf("")
    private var onEditSuggestionListener: ((String) -> Unit)? = null
    private var onStopListener: (() -> Unit)? = null

    @Composable
    override fun Content() {
        if (providers.value.isEmpty()) {
            return
        }

        if (UserPreferences(context).toolbarPosition == ToolbarPosition.TOP.ordinal) {
            AwesomeBarOrientation.BOTTOM
        } else {
            AwesomeBarOrientation.TOP
        }

        FirefoxTheme {
            AwesomeBar(
                text = text.value,
                providers = providers.value,
                colors = AwesomeBarDefaults.colors(
                    background = Color.Transparent,
                    title = Color(context.getColorFromAttr(android.R.attr.textColorPrimary)),
                    description = Color(context.getColorFromAttr(android.R.attr.textColorSecondary)),
                    autocompleteIcon =  Color(context.getColorFromAttr(android.R.attr.textColorSecondary))
                ),
                onSuggestionClicked = { suggestion ->
                    suggestion.onSuggestionClicked?.invoke()
                    onStopListener?.invoke()
                },
                onAutoComplete = { suggestion ->
                    onEditSuggestionListener?.invoke(suggestion.editSuggestion!!)
                }
            )
        }
    }

    override fun addProviders(vararg providers: AwesomeBar.SuggestionProvider) {
        val newProviders = this.providers.value.toMutableList()
        newProviders.addAll(providers)
        this.providers.value = newProviders
    }

    override fun containsProvider(provider: AwesomeBar.SuggestionProvider): Boolean {
        return providers.value.any { current -> current.id == provider.id }
    }

    override fun onInputChanged(text: String) {
        this.text.value = text
    }

    override fun removeAllProviders() {
        providers.value = emptyList()
    }

    override fun removeProviders(vararg providers: AwesomeBar.SuggestionProvider) {
        val newProviders = this.providers.value.toMutableList()
        newProviders.removeAll(providers.toSet())
        this.providers.value = newProviders
    }

    override fun setOnEditSuggestionListener(listener: (String) -> Unit) {
        onEditSuggestionListener = listener
    }

    override fun setOnStopListener(listener: () -> Unit) {
        onStopListener = listener
    }
}