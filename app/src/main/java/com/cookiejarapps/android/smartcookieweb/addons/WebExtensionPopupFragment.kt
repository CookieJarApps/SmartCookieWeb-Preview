package com.cookiejarapps.android.smartcookieweb.addons

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.ext.components
import kotlinx.android.synthetic.main.fragment_extension_popup.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.components.browser.state.action.WebExtensionAction
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.window.WindowRequest
import mozilla.components.lib.state.ext.consumeFrom


class WebExtensionPopupFragment : DialogFragment(), EngineSession.Observer {
    private var engineSession: EngineSession? = null
    private lateinit var webExtensionId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        webExtensionId = requireNotNull(arguments?.getString("web_extension_id"))
        engineSession = components.store.state.extensions[webExtensionId]?.popupSession

        dialog?.window?.setGravity(Gravity.END or Gravity.TOP)
        dialog?.window?.attributes?.windowAnimations = R.style.ExtensionPopupStyle
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        return inflater.inflate(R.layout.fragment_extension_popup, container, false)
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = engineSession
        if (session != null) {
            addonPopupEngineView.render(session)
            session.register(this, view)
            consumePopupSession()
        } else {
            consumeFrom(requireContext().components.store) { state ->
                state.extensions[webExtensionId]?.let { extState ->
                    extState.popupSession?.let {
                        if (engineSession == null) {
                            addonPopupEngineView.render(it)
                            it.register(this, view)
                            consumePopupSession()
                            engineSession = it
                        }
                    }
                }
            }
        }
    }

    override fun onWindowRequest(windowRequest: WindowRequest) {
        if (windowRequest.type == WindowRequest.Type.CLOSE) {
            dismiss()
        } else {
            engineSession?.loadUrl(windowRequest.url)
        }
    }
    private fun consumePopupSession() {
        components.store.dispatch(
            WebExtensionAction.UpdatePopupSessionAction(webExtensionId, popupSession = null)
        )
    }

    companion object {
        fun create(webExtensionId: String) = WebExtensionPopupFragment().apply {
            arguments = Bundle().apply {
                putString("web_extension_id", webExtensionId)
            }
        }
    }
}