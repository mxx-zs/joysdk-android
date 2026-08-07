package com.joyplay.demo.activity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.joyplay.demo.R

/**
 * Compose integration sample: hosts a configured Android WebView through AndroidView.
 * Compose 接入示例：通过 AndroidView 承载并配置原生 WebView。
 */
class ComposeWebActivity : ComponentActivity() {
    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gameUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        if (gameUrl.isEmpty()) {
            finish()
            return
        }

        // Keep the game full screen with transient system bars on swipe. / 游戏全屏显示，滑动时可临时显示系统栏。
        WindowCompat.enableEdgeToEdge(window)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            ComposeGameWebView(gameUrl)
        }
    }

    @Composable
    private fun ComposeGameWebView(gameUrl: String) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                createWebView(context, gameUrl).also {
                    webView = it
                }
            },
            onRelease = { releasedWebView ->
                if (webView === releasedWebView) webView = null
                releasedWebView.apply {
                    stopLoading()
                    loadUrl("about:blank")
                    removeJavascriptInterface(JS_BRIDGE_NAME)
                    destroy()
                }
            },
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(context: Context, gameUrl: String) =
        WebView(context).apply {
            // Configure WebView and JSBridge before loading the URL. / 先配置 WebView 和 JSBridge，最后加载 URL。
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.databasePath = context.filesDir.absolutePath
            addJavascriptInterface(JSInterface(), JS_BRIDGE_NAME)
            webViewClient = createWebViewClient()
            loadUrl(gameUrl)
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

    private fun nativeToJs() {
        webView?.post {
            webView?.loadUrl("javascript:HttpTool.NativeToJs('recharge')")
        }
    }

    // TODO: Replace this demo dialog with the host app's recharge UI. / TODO: 请由接入方 App 实现充值界面。
    private fun showRechargeDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.recharge_dialog_title)
            .setMessage(R.string.recharge_dialog_message)
            .setNegativeButton(R.string.recharge_dialog_cancel, null)
            .setPositiveButton(R.string.recharge_dialog_confirm) { _, _ -> nativeToJs() }
            .show()
    }

    // Entry points called by the H5 game. / H5 调用原生的方法入口。
    inner class JSInterface {
        @JavascriptInterface
        fun newTppClose() {
            Log.d(TAG, "newTppClose")
            // Called when the user taps Close in the game. / 用户在游戏内点击关闭按钮。
            runOnUiThread { finish() }
        }

        @JavascriptInterface
        fun clickRecharge() {
            Log.d(TAG, "clickRecharge")
            // Called when the user taps Recharge in the game. / 用户在游戏内点击充值按钮。
            runOnUiThread { showRechargeDialog() }
        }

        @JavascriptInterface
        fun recharge() {
            Log.d(TAG, "recharge")
            // Called when the balance is insufficient while placing a bet. / 用户下注时余额不足。
            runOnUiThread { showRechargeDialog() }
        }
    }

    companion object {
        const val EXTRA_URL = "url"

        private const val TAG = "ComposeWebActivity"
        private const val JS_BRIDGE_NAME = "JSBridgeService"
    }
}
