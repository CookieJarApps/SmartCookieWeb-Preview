package com.cookiejarapps.android.smartcookieweb.components.toolbar

import mozilla.components.lib.state.Action
import mozilla.components.lib.state.State
import mozilla.components.lib.state.Store

class BrowserFragmentStore(initialState: BrowserFragmentState) :
    Store<BrowserFragmentState, BrowserFragmentAction>(initialState, ::browserStateReducer)

class BrowserFragmentState : State

sealed class BrowserFragmentAction : Action

private fun browserStateReducer(
    state: BrowserFragmentState,
    action: BrowserFragmentAction
): BrowserFragmentState {
    return when {
        else -> BrowserFragmentState()
    }
}