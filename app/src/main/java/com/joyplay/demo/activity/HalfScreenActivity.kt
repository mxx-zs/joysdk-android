package com.joyplay.demo.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.joyplay.demo.R
import com.joyplay.demo.databinding.ActivityHalfScreenBinding

/**
 * Half-screen room: shows the game WebView at the bottom with a 1:1 ratio after a tap.
 * 半屏直播间：点击按钮后，在底部以 1:1 比例显示游戏 WebView。
 */
class HalfScreenActivity : Activity() {
    private lateinit var binding: ActivityHalfScreenBinding
    private lateinit var gameUrl: String
    private var gameOpen = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gameUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        if (gameUrl.isEmpty()) return

        binding = ActivityHalfScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Use immersive mode with transient system bars on swipe. / 游戏页面使用沉浸式显示，滑动时可临时显示系统栏。
        WindowCompat.enableEdgeToEdge(window)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())

            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        configureWebView()
        configureCloseButton()
        binding.openGameButton.setOnClickListener { openGame() }
    }

    private fun configureCloseButton() {
        binding.closeRoomButton.setOnClickListener { finish() }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() = with(binding.webView) {
        // Configure WebView here; load the game only after the user taps the button. / 这里只配置 WebView，用户点击按钮后才加载游戏。
        setBackgroundColor(Color.TRANSPARENT)
        settings.javaScriptEnabled = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.databasePath = applicationContext.filesDir.absolutePath
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

    private fun openGame() {
        gameOpen = true
        binding.openGameButton.clearAnimation()
        binding.openGameButton.visibility = View.GONE
        binding.webView.visibility = View.VISIBLE
        binding.webView.loadUrl(gameUrl)
    }

    private fun closeGame() {
        gameOpen = false
        binding.webView.apply {
            stopLoading()
            loadUrl("about:blank")
            visibility = View.GONE
        }
        binding.openGameButton.visibility = View.VISIBLE
        startOpenGameHint()
    }

    override fun onResume() {
        super.onResume()
        if (!gameOpen) startOpenGameHint()
    }

    override fun onPause() {
        binding.openGameButton.clearAnimation()
        super.onPause()
    }

    private fun startOpenGameHint() {
        // A subtle pulse highlights the primary action without blocking taps. / 轻微呼吸动效突出主操作，同时不影响点击。
        binding.openGameButton.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.open_game_pulse),
        )
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
            runOnUiThread { closeGame() }
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

        private const val TAG = "HalfScreenActivity"
        private const val JS_BRIDGE_NAME = "JSBridgeService"
    }
}