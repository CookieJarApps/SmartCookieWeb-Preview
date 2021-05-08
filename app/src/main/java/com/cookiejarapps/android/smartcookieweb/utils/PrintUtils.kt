package com.cookiejarapps.android.smartcookieweb.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient

class PrintUtils private constructor() : Runnable {
    private var mContext: Context? = null
    private var mHtmlString: String? = null
    private var mUrl: String? = null
    private var mAlreadyRunning = false
    private var mWebView: WebView? = null

    override fun run() {
        mWebView = WebView(mContext!!)
        mWebView!!.settings.javaScriptEnabled = false
        mWebView!!.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager: PrintManager = mContext!!.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val jobName = mWebView?.title ?: "Document"
                val printAdapter: PrintDocumentAdapter = mWebView!!.createPrintDocumentAdapter(jobName)
                val builder: PrintAttributes.Builder = PrintAttributes.Builder()
                builder.setMediaSize(PrintAttributes.MediaSize.ISO_A5)
                printManager.print(jobName, printAdapter, builder.build())
                destroy()
            }
        }
        mWebView!!.loadDataWithBaseURL(mUrl!!, mHtmlString!!, "text/HTML", "UTF-8", null)
    }

    fun convert(context: Context?, htmlString: String?, url: String?) {
        if (mAlreadyRunning) return
        mContext = context
        mHtmlString = htmlString
        mUrl = url
        mAlreadyRunning = true
        runOnUiThread(this)
    }

    private fun runOnUiThread(runnable: Runnable) {
        val handler = Handler(mContext!!.mainLooper)
        handler.post(runnable)
    }

    private fun destroy() {
        mContext = null
        mHtmlString = null
        mUrl = null
        mAlreadyRunning = false
        mWebView = null
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var printInstance: PrintUtils? = null

        @get:Synchronized
        val instance: PrintUtils?
            get() {
                if (printInstance == null) printInstance = PrintUtils()
                return printInstance
            }
    }
}