package com.denilson.music.web

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Message
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream

/**
 * WebViewClient con bloqueo de anuncios siempre activo. Toda la
 * navegacion se mantiene dentro de la misma WebView.
 */
class AdBlockWebViewClient(
    private val onBlocked: (count: Int) -> Unit = {}
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val url = request.url?.toString() ?: return null
        if (AdBlocker.shouldBlock(url)) {
            AdBlocker.registerBlock()
            view.post { onBlocked(AdBlocker.sessionBlocked) }
            return WebResourceResponse(
                "text/plain", "utf-8", ByteArrayInputStream(ByteArray(0))
            )
        }
        return null
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        injectCosmetics(view, url)
    }

    private fun injectCosmetics(view: WebView, url: String?) {
        val host = url?.substringAfter("://", "")?.substringBefore('/')?.lowercase()
        val css = AdBlocker.cosmeticCssFor(host)
        if (css.isEmpty()) return
        view.evaluateJavascript(AdBlocker.adKillerJs(css), null)
    }

    fun reapply(view: WebView, url: String?) = injectCosmetics(view, url)

    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest
    ): Boolean {
        val scheme = request.url?.scheme?.lowercase() ?: return false
        return scheme == "intent" || scheme == "market" ||
            scheme == "playstore" || scheme.startsWith("vnd.")
    }
}

/**
 * User-Agent para YouTube Web App nativo.
 */
const val CHROME_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
fun configureWebViewForInApp(
    webView: WebView,
    onProgress: (Int) -> Unit = {},
    onFullscreenChange: (View?, WebChromeClient.CustomViewCallback?) -> Unit = { _, _ -> }
) {
    webView.apply {
        setBackgroundColor(Color.parseColor("#0F0F0F"))

        settings.userAgentString = CHROME_USER_AGENT
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.setSupportMultipleWindows(false)
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false

        android.webkit.CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                onProgress(newProgress)
                if (newProgress in 1..99) {
                    val host = view.url?.substringAfter("://", "")
                        ?.substringBefore('/')?.lowercase()
                    val css = AdBlocker.cosmeticCssFor(host)
                    if (css.isNotEmpty()) {
                        view.evaluateJavascript(AdBlocker.adKillerJs(css), null)
                    }
                }
            }

            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean = false

            override fun onShowCustomView(
                view: View,
                callback: CustomViewCallback
            ) {
                onFullscreenChange(view, callback)
            }

            override fun onHideCustomView() {
                onFullscreenChange(null, null)
            }
        }
    }
}
