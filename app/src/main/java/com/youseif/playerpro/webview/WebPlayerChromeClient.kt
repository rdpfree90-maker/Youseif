package com.youseif.playerpro.webview

import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView

class WebPlayerChromeClient(
    private val onCustomView: (View?, CustomViewCallback?) -> Unit = { _, _ -> },
    private val onHideCustomView: () -> Unit = {},
    private val onProgress: (Int) -> Unit = {},
    private val onTitle: (String?) -> Unit = {}
) : WebChromeClient() {

    private var customView: View? = null
    private var customViewCallback: CustomViewCallback? = null

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (customView != null) {
            onHideCustomView()
            return
        }
        customView = view
        customViewCallback = callback
        onCustomView(view, callback)
    }

    override fun onHideCustomView() {
        customView = null
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        onHideCustomView.invoke()
    }

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        onProgress(newProgress)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        onTitle(title)
    }

    fun isInCustomView(): Boolean = customView != null
}
