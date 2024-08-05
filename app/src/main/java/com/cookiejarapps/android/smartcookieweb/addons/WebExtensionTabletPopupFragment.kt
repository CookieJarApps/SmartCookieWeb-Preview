package com.cookiejarapps.android.smartcookieweb.addons

import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.databinding.FragmentExtensionPopupTabletBinding
import com.cookiejarapps.android.smartcookieweb.ext.components
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.action.CustomTabListAction
import mozilla.components.browser.state.action.WebExtensionAction
import mozilla.components.browser.state.state.CustomTabSessionState
import mozilla.components.browser.state.state.EngineState
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.state.createCustomTab
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.prompt.PromptRequest
import mozilla.components.concept.engine.window.WindowRequest
import mozilla.components.feature.prompts.PromptFeature
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import mozilla.components.support.utils.ext.requestInPlacePermissions


class WebExtensionTabletPopupFragment : DialogFragment(), UserInteractionHandler, EngineSession.Observer {
    private val promptsFeature = ViewBoundFeatureWrapper<PromptFeature>()

    protected var session: SessionState? = null
    protected var engineSession: EngineSession? = null
    private var canGoBack: Boolean = false

    private lateinit var webExtensionId: String

    private var _binding: FragmentExtensionPopupTabletBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        webExtensionId = requireNotNull(arguments?.getString("web_extension_id"))

        components.store.state.extensions[webExtensionId]?.popupSession?.let {
            initializeSession(it)
        }

        dialog?.window?.setGravity(Gravity.END or Gravity.TOP)
        if(android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.M) dialog?.window?.setDimAmount(0F)
        dialog?.window?.attributes?.windowAnimations = R.style.ExtensionPopupTabletStyle
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        _binding = FragmentExtensionPopupTabletBinding.inflate(inflater, container, false)
        val view = binding.root

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        session?.let {
            promptsFeature.set(
                PromptFeature(
                    fragment = this,
                    store = components.store,
                    tabsUseCases = components.tabsUseCases,
                    fragmentManager = parentFragmentManager,
                    fileUploadsDirCleaner = components.fileUploadsDirCleaner,
                    onNeedToRequestPermissions = { permissions ->
                        requestInPlacePermissions(REQUEST_KEY_PROMPT_PERMISSIONS, permissions) { result ->
                            promptsFeature.get()?.onPermissionsResult(
                                result.keys.toTypedArray(),
                                result.values.map {
                                    when (it) {
                                        true -> PackageManager.PERMISSION_GRANTED
                                        false -> PackageManager.PERMISSION_DENIED
                                    }
                                }.toIntArray(),
                            )
                        }
                    },
                ),
                this,
                view
            )
        }

        if (engineSession != null) {
            binding.addonPopupEngineView.render(engineSession!!)
            consumePopupSession()
        } else {
            consumeFrom(requireContext().components.store) { state ->
                state.extensions[webExtensionId]?.let { extState ->
                    extState.popupSession?.let {
                        if (engineSession == null) {
                            binding.addonPopupEngineView.render(it)
                            it.register(this, view)
                            consumePopupSession()
                            engineSession = it
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        engineSession?.close()
        session?.let {
            components.store.dispatch(CustomTabListAction.RemoveCustomTabAction(it.id))
        }
        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        engineSession?.register(this)
    }

    override fun onStop() {
        super.onStop()
        engineSession?.unregister(this)
    }

    protected fun initializeSession(fromEngineSession: EngineSession? = null) {
        engineSession = fromEngineSession ?: components.engine.createSession()
        session = createCustomTab("").copy(engineState = EngineState(engineSession))
        components.store.dispatch(CustomTabListAction.AddCustomTabAction(session as CustomTabSessionState))
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

    override fun onBackPressed(): Boolean {
        return if (this.canGoBack) {
            engineSession?.goBack()
            true
        } else {
            false
        }
    }

    override fun onPromptRequest(promptRequest: PromptRequest) {
        session?.let { session ->
            components.store.dispatch(
                    ContentAction.UpdatePromptRequestAction(
                            session.id,
                            promptRequest
                    )
            )
        }
    }

    override fun onNavigationStateChange(canGoBack: Boolean?, canGoForward: Boolean?) {
        canGoBack?.let { this.canGoBack = canGoBack }
    }


    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_PROMPT_PERMISSIONS -> promptsFeature.get()?.onPermissionsResult(permissions, grantResults)
        }
    }

    companion object {
        fun create(webExtensionId: String) = WebExtensionTabletPopupFragment().apply {
            arguments = Bundle().apply {
                putString("web_extension_id", webExtensionId)
            }
        }
        private const val REQUEST_CODE_PROMPT_PERMISSIONS = 1
        private const val REQUEST_KEY_PROMPT_PERMISSIONS = "promptFeature"
    }
}