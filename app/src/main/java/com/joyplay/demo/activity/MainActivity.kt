package com.joyplay.demo.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.joyplay.demo.R
import com.joyplay.demo.databinding.ActivityMainBinding

/**
 * Game mode entry: selects a page type and prepares the complete game URL before launch.
 * 游戏模式入口：选择页面类型，并在跳转前准备完整游戏 URL。
 */
class MainActivity : Activity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Use an immersive layout with transient system bars on swipe. / 使用沉浸式布局，滑动时可临时显示系统栏。
        WindowCompat.enableEdgeToEdge(window)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())

            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        binding.openGameButton.setOnClickListener {
            openScreen(GameActivity::class.java)
        }
        binding.screenModeGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.halfScreenOption -> openScreen(HalfScreenActivity::class.java, "1")
                R.id.largeHalfScreenOption -> openScreen(LargeHalfScreenActivity::class.java, "2")
            }
        }
    }

    private fun openScreen(target: Class<out Activity>, mini: String? = null) {
        // Append mini here; target activities receive only the complete URL. / mini 只在入口拼接，目标 Activity 只接收完整 URL。
        val targetUrl = if (mini == null) {
            GAME_URL
        } else {
            Uri.parse(GAME_URL)
                .buildUpon()
                .appendQueryParameter("mini", mini)
                .build()
                .toString()
        }

        startActivity(
            Intent(this, target).apply {
                putExtra(EXTRA_URL, targetUrl)
            },
        )
    }

    override fun onResume() {
        super.onResume()
        binding.fullScreenOption.isChecked = true
        startOpenGameHint()
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

    companion object {
        const val EXTRA_URL = "url"

        private const val GAME_URL = "https://joyplay.cn/release/index.html?appKey=ste5a6lxxrtu10bmnc6g&token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJuYmYiOjE3ODU0OTExNzEsImFjY291bnRJZCI6IjIwODMxMjcwNzQxNzU0NTUyMzIifQ.gdzel2RMXHKwyEG6AaQg-sObDx6H_O9Tmo2XGzfcOJU&gameId=1&safeTop=1&isNativeDemo=1"
    }
}
