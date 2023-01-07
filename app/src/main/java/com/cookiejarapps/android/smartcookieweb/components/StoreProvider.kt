package com.cookiejarapps.android.smartcookieweb.components

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import mozilla.components.lib.state.Store

/**
 * Generic ViewModel to wrap a State object for state restoration.
 *
 * @property store [Store] instance attached to [ViewModel].
 */
class StoreProvider<T : Store<*, *>>(
    val store: T
) : ViewModel() {

    companion object {
        fun <T : Store<*, *>> get(fragment: Fragment, createStore: () -> T): T {
            val factory = StoreProviderFactory(createStore)
            val viewModel: StoreProvider<T> = ViewModelProvider(fragment, factory).get()
            return viewModel.store
        }
    }
}

/**
 * ViewModel factory to create [StoreProvider] instances.
 *
 * @param createStore Callback to create a new [Store], used when the [ViewModel] is first created.
 */
class StoreProviderFactory<T : Store<*, *>>(
    private val createStore: () -> T
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return StoreProvider(createStore()) as T
    }
}
