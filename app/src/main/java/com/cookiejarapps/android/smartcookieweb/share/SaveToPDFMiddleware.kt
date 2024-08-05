/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.cookiejarapps.android.smartcookieweb.share

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext

/**
 * [BrowserAction] middleware reacting in response to Save to PDF related [Action]s.
 * @property context An Application context.
 */
class SaveToPDFMiddleware(
    private val context: Context,
    private val mainScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
) : Middleware<BrowserState, BrowserAction> {

    override fun invoke(
        ctx: MiddlewareContext<BrowserState, BrowserAction>,
        next: (BrowserAction) -> Unit,
        action: BrowserAction,
    ) {
        when (action) {
            is EngineAction.SaveToPdfAction -> {
                next(action)
            }

            is EngineAction.SaveToPdfCompleteAction -> { }

            is EngineAction.SaveToPdfExceptionAction -> { }

            is EngineAction.PrintContentAction -> {
                next(action)
            }
            is EngineAction.PrintContentCompletedAction -> { }
            is EngineAction.PrintContentExceptionAction -> { }
            else -> {
                next(action)
            }
        }
    }
}