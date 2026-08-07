package com.joyplay.demo.activity

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.joyplay.demo.R
import com.joyplay.demo.databinding.ActivityLargeHalfScreenBinding

/**
 * Large half-screen room: shows the game WebView at the bottom with a 1:1.5 ratio after a tap.
 * 大半屏直播间：点击按钮后，在底部以 1:1.5 比例显示游戏 WebView。
 */
class LargeHalfScreenActivity : Activity() {
    private lateinit var binding: ActivityLargeHalfScreenBinding
    private lateinit var gameUrl: String
    private var gameOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gameUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        if (gameUrl.isEmpty()) return

        binding = ActivityLargeHalfScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Use immersive mode with transient system bars on swipe. / 游戏页面使用沉浸式显示，滑动时可临时显示系统栏。
        WindowCompat.enableEdgeToEdge(window)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())

            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        binding.webView.setGameEventCallbacks(
            onCloseGame = ::closeGame,
            onRecharge = ::showRechargeDialog,
            onInsufficientBalance = ::showRechargeDialog,
        )
        configureCloseButton()
        binding.openGameButton.setOnClickListener { openGame() }
    }

    private fun configureCloseButton() {
        binding.closeRoomButton.setOnClickListener { finish() }
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
        binding.webView.notifyRechargeComplete()
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
            binding.webView.release()
        }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_URL = "url"
    }
}
