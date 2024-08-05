package com.cookiejarapps.android.smartcookieweb.request

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.cookiejarapps.android.smartcookieweb.BrowserActivity
import com.cookiejarapps.android.smartcookieweb.R
import com.cookiejarapps.android.smartcookieweb.addons.AddonDetailsActivity
import com.cookiejarapps.android.smartcookieweb.addons.AddonsActivity
import com.cookiejarapps.android.smartcookieweb.browser.home.HomeFragmentDirections
import com.cookiejarapps.android.smartcookieweb.ext.components
import com.cookiejarapps.android.smartcookieweb.preferences.UserPreferences
import mozilla.components.browser.errorpages.ErrorPages
import mozilla.components.browser.errorpages.ErrorType
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.request.RequestInterceptor
import mozilla.components.concept.engine.request.RequestInterceptor.InterceptionResponse
import java.lang.ref.WeakReference


class AppRequestInterceptor(val context: Context) : RequestInterceptor {

    private var navController: WeakReference<NavController>? = null

    fun setNavController(navController: NavController) {
        this.navController = WeakReference(navController)
    }

    override fun onLoadRequest(
        engineSession: EngineSession,
        uri: String,
        lastUri: String?,
        hasUserGesture: Boolean,
        isSameDomain: Boolean,
        isRedirect: Boolean,
        isDirectNavigation: Boolean,
        isSubframeRequest: Boolean
    ): InterceptionResponse? {
        interceptXpiUrl(uri, hasUserGesture)?.let { response ->
            return response
        }

       var response = context.components.appLinksInterceptor.onLoadRequest(
           engineSession, uri, lastUri, hasUserGesture, isSameDomain, isRedirect,
           isDirectNavigation, isSubframeRequest
       )

        if (response == null && !isDirectNavigation) {
            response = context.components.webAppInterceptor.onLoadRequest(
                engineSession, uri, lastUri, hasUserGesture, isSameDomain, isRedirect,
                isDirectNavigation, isSubframeRequest
            )
        }

        return response
    }

    override fun onErrorRequest(
        session: EngineSession,
        errorType: ErrorType,
        uri: String?
    ): RequestInterceptor.ErrorResponse {
        val riskLevel = getErrorCategory(errorType)

        if (uri == "about:homepage") {
            /* This needs to be in onErrorRequest because onLoadRequest doesn't load on about pages due to a GeckoView bug
            * We don't need to (and can't) check whether the URL was loaded by a link, whether the user entered the URL, or whether the browser opened it
            * This doesn't matter though - GeckoView blocks web pages from loading about URLs already
            * TODO: Option to focus on address bar when new tab is created
            */
            navController?.get()?.navigate(
                HomeFragmentDirections.actionGlobalHome(
                    focusOnAddressBar = false
                )
            )

            return RequestInterceptor.ErrorResponse("resource://android/assets/homepage.html")
        }

        val errorPageUri = ErrorPages.createUrlEncodedErrorPage(
            context = context,
            errorType = errorType,
            uri = uri,
            htmlResource = riskLevel.htmlRes
        )

        return RequestInterceptor.ErrorResponse(errorPageUri)
    }

    private fun interceptXpiUrl(
        uri: String,
        hasUserGesture: Boolean
    ): InterceptionResponse? {
        if (hasUserGesture && uri.startsWith("https://addons.mozilla.org") && !UserPreferences(
                context
            ).customAddonCollection) {

            val matchResult = "https://addons.mozilla.org/firefox/downloads/file/([^\\s]+)/([^\\s]+\\.xpi)".toRegex().matchEntire(uri)
            if (matchResult != null) {

                matchResult.groupValues.getOrNull(1)?.let { addonId ->
                    val intent = Intent(context, AddonsActivity::class.java)
                    intent.putExtra("ADDON_ID", addonId)
                    intent.putExtra("ADDON_URL", uri)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(context, intent, null)

                    return InterceptionResponse.Deny
                }
            }
        }

        // In all other case we let the original request proceed.
        return null
    }

    private fun getErrorCategory(errorType: ErrorType): ErrorCategory = when (errorType) {
        ErrorType.UNKNOWN,
        ErrorType.ERROR_CORRUPTED_CONTENT,
        ErrorType.ERROR_CONTENT_CRASHED,
        ErrorType.ERROR_CONNECTION_REFUSED,
        ErrorType.ERROR_NO_INTERNET,
        ErrorType.ERROR_NET_INTERRUPT,
        ErrorType.ERROR_NET_TIMEOUT,
        ErrorType.ERROR_NET_RESET,
        ErrorType.ERROR_UNSAFE_CONTENT_TYPE,
        ErrorType.ERROR_REDIRECT_LOOP,
        ErrorType.ERROR_INVALID_CONTENT_ENCODING,
        ErrorType.ERROR_MALFORMED_URI,
        ErrorType.ERROR_FILE_NOT_FOUND,
        ErrorType.ERROR_FILE_ACCESS_DENIED,
        ErrorType.ERROR_PROXY_CONNECTION_REFUSED,
        ErrorType.ERROR_OFFLINE,
        ErrorType.ERROR_UNKNOWN_HOST,
        ErrorType.ERROR_UNKNOWN_SOCKET_TYPE,
        ErrorType.ERROR_UNKNOWN_PROXY_HOST,
        ErrorType.ERROR_HTTPS_ONLY,
        ErrorType.ERROR_UNKNOWN_PROTOCOL -> ErrorCategory.Network

        ErrorType.ERROR_SECURITY_BAD_CERT,
        ErrorType.ERROR_SECURITY_SSL,
        ErrorType.ERROR_BAD_HSTS_CERT,
        ErrorType.ERROR_PORT_BLOCKED -> ErrorCategory.SSL

        ErrorType.ERROR_SAFEBROWSING_HARMFUL_URI,
        ErrorType.ERROR_SAFEBROWSING_PHISHING_URI,
        ErrorType.ERROR_SAFEBROWSING_MALWARE_URI,
        ErrorType.ERROR_SAFEBROWSING_UNWANTED_URI -> ErrorCategory.Malware
    }

    internal enum class ErrorCategory(val htmlRes: String) {
        Network(NETWORK_ERROR_PAGE),
        SSL(SSL_ERROR_PAGE),
        Malware(MALWARE_ERROR_PAGE),
    }

    companion object {
        internal const val NETWORK_ERROR_PAGE = "network_error_page.html"
        internal const val SSL_ERROR_PAGE = "ssl_error_page.html"
        internal const val MALWARE_ERROR_PAGE = "malware_error_page.html"
    }
}
