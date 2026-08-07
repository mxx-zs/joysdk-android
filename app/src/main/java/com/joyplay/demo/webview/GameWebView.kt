package com.joyplay.demo.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.util.AttributeSet
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Reusable JoyPlay game view. The host only provides a complete URL and handles game events.
 * 可复用的 JoyPlay 游戏组件。宿主只需传入完整 URL，并处理游戏事件。
 */
class GameWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : WebView(context, attrs, defStyleAttr) {
    private var onCloseGame: (() -> Unit)? = null
    private var onRecharge: (() -> Unit)? = null
    private var onInsufficientBalance: (() -> Unit)? = null

    init {
        configureWebView()
    }

    /** Connects H5 events to business actions implemented by the host app. / 将 H5 事件交给宿主 App 处理。 */
    fun setGameEventCallbacks(
        onCloseGame: () -> Unit,
        onRecharge: () -> Unit,
        onInsufficientBalance: () -> Unit,
    ) {
        this.onCloseGame = onCloseGame
        this.onRecharge = onRecharge
        this.onInsufficientBalance = onInsufficientBalance
    }

    /** Notifies H5 after the host app completes recharge. / 宿主 App 完成充值后通知 H5 刷新余额。 */
    fun notifyRechargeComplete() {
        post {
            loadUrl("javascript:HttpTool.NativeToJs('recharge')")
        }
    }

    /** Releases the bridge and WebView resources when the host page is destroyed. / 宿主页面销毁时释放桥接和 WebView 资源。 */
    fun release() {
        onCloseGame = null
        onRecharge = null
        onInsufficientBalance = null
        removeJavascriptInterface(JS_BRIDGE_NAME)
        destroy()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        // Configure WebView and JSBridge before the host loads a URL. / 在宿主加载 URL 前完成 WebView 和 JSBridge 配置。
        setBackgroundColor(Color.TRANSPARENT)
        settings.javaScriptEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.databasePath = context.filesDir.absolutePath
        addJavascriptInterface(JSInterface(), JS_BRIDGE_NAME)
        webViewClient = createWebViewClient()
    }

    private fun createWebViewClient() = object : WebViewClient() {
        private fun isContainCloudFront(view: WebView, targetUrl: String?): Boolean {
            if (!TextUtils.isEmpty(targetUrl) && targetUrl!!.contains("cloudfront")) {
                view.loadUrl(targetUrl)
                return true
            }
            return false
        }

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
            isContainCloudFront(view, url) || super.shouldOverrideUrlLoading(view, url)

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val targetUrl = request.url?.toString().orEmpty()
            return isContainCloudFront(view, targetUrl) ||
                super.shouldOverrideUrlLoading(view, request)
        }
    }

    // Entry points called by the H5 game. / H5 调用原生的方法入口。
    private inner class JSInterface {
        @JavascriptInterface
        fun newTppClose() {
            // Called when the user taps Close in the game. / 用户在游戏内点击关闭按钮。
            post { onCloseGame?.invoke() }
        }

        @JavascriptInterface
        fun clickRecharge() {
            // Called when the user taps Recharge in the game. / 用户在游戏内点击充值按钮。
            post { onRecharge?.invoke() }
        }

        @JavascriptInterface
        fun recharge() {
            // Called when the balance is insufficient while placing a bet. / 用户下注时余额不足。
            post { onInsufficientBalance?.invoke() }
        }
    }

    companion object {
        private const val TAG = "GameWebView"
        private const val JS_BRIDGE_NAME = "JSBridgeService"
    }
}
