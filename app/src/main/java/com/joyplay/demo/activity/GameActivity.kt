package com.joyplay.demo.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.joyplay.demo.R
import com.joyplay.demo.databinding.ActivityGameBinding

/**
 * Full-screen game page: receives a complete URL from the caller and loads it immediately.
 * 全屏游戏页：接收调用方传入的完整 URL，并立即加载 WebView。
 */
class GameActivity : Activity() {
    private lateinit var binding: ActivityGameBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gameUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        if (gameUrl.isEmpty()) return

        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Keep the game full screen with transient system bars on swipe. / 游戏全屏显示，滑动时可临时显示系统栏。
        WindowCompat.enableEdgeToEdge(window)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())

            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Configure WebView and register JSBridge before loading the URL. / 先完成所有配置和 JSBridge 注册，最后再加载 URL。
        binding.webView.apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.databasePath = applicationContext.filesDir.absolutePath
            addJavascriptInterface(JSInterface(), JS_BRIDGE_NAME)
            webViewClient = createWebViewClient()
            loadUrl(gameUrl)
        }
    }

    private fun createWebViewClient() = object : WebViewClient() {
        // Keep CloudFront redirects inside the current WebView. / CloudFront 跳转继续在当前 WebView 内加载。
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

    fun nativeToJs() {
        binding.webView.post {
            binding.webView.loadUrl("javascript:HttpTool.NativeToJs('recharge')")
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

    override fun onDestroy() {
        if (::binding.isInitialized) {
            binding.webView.apply {
                removeJavascriptInterface(JS_BRIDGE_NAME)
                destroy()
            }
        }
        super.onDestroy()
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

        private const val TAG = "GameActivity"
        private const val JS_BRIDGE_NAME = "JSBridgeService"
    }
}
