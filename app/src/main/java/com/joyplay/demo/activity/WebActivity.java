package com.joyplay.demo.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.joyplay.demo.R;

/**
 * Java integration sample: receives a complete URL, then configures WebView and JSBridge before loading.
 * Java 接入示例：接收外部完整 URL，配置 WebView 和 JSBridge 后加载游戏。
 */
public class WebActivity extends Activity {
    public static final String EXTRA_URL = "url";

    private static final String TAG = "WebActivity";
    private static final String JS_BRIDGE_NAME = "JSBridgeService";

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        webView = findViewById(R.id.webView);
        // A transparent background lets the host page provide its own backdrop. / 透明背景便于宿主页面自定义底色。
        webView.setBackgroundColor(Color.TRANSPARENT);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setDatabasePath(getApplicationContext().getFilesDir().getAbsolutePath());

        // JSBridgeService is the native interface name expected by the game. / JSBridgeService 是游戏约定的原生接口名。
        webView.addJavascriptInterface(new JSInterface(this), JS_BRIDGE_NAME);

        // Keep CloudFront redirects inside the current WebView. / CloudFront 跳转继续在当前 WebView 内加载。
        webView.setWebViewClient(new WebViewClient() {
            private boolean loadCloudFrontUrl(WebView view, String targetUrl) {
                if (!TextUtils.isEmpty(targetUrl) && targetUrl.contains("cloudfront")) {
                    view.loadUrl(targetUrl);
                    return true;
                }
                return false;
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String targetUrl) {
                return loadCloudFrontUrl(view, targetUrl) ||
                    super.shouldOverrideUrlLoading(view, targetUrl);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String targetUrl = request != null && request.getUrl() != null
                    ? request.getUrl().toString()
                    : "";
                return loadCloudFrontUrl(view, targetUrl) ||
                    super.shouldOverrideUrlLoading(view, request);
            }
        });

        // Load the caller-provided complete URL after configuration is finished. / 所有配置完成后再加载调用方传入的完整 URL。
        String url = getIntent().getStringExtra(EXTRA_URL);
        if (TextUtils.isEmpty(url)) {
            return;
        }
        webView.loadUrl(url);
    }

    public void nativeToJs() {
        webView.post(() -> webView.loadUrl("javascript:HttpTool.NativeToJs('recharge')"));
    }

    private void log(String event) {
        Log.d(TAG, event);
    }

    // TODO: Replace this demo dialog with the host app's recharge UI. / TODO: 请由接入方 App 实现充值界面。
    private void showRechargeDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.recharge_dialog_title)
            .setMessage(R.string.recharge_dialog_message)
            .setNegativeButton(R.string.recharge_dialog_cancel, null)
            .setPositiveButton(R.string.recharge_dialog_confirm, (dialog, which) -> nativeToJs())
            .show();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface(JS_BRIDGE_NAME);
            webView.destroy();
        }
        super.onDestroy();
    }

    // Entry points called by the H5 game. / H5 调用原生的方法入口。
    public final class JSInterface {
        private final Activity activity;

        JSInterface(Activity activity) {
            this.activity = activity;
        }

        @JavascriptInterface
        public void newTppClose() {
            log("newTppClose");
            // Called when the user taps Close in the game. / 用户在游戏内点击关闭按钮。
            activity.runOnUiThread(activity::finish);
        }

        @JavascriptInterface
        public void clickRecharge() {
            log("clickRecharge");
            // Called when the user taps Recharge in the game. / 用户在游戏内点击充值按钮。
            activity.runOnUiThread(WebActivity.this::showRechargeDialog);
        }

        @JavascriptInterface
        public void recharge() {
            log("recharge");
            // Called when the balance is insufficient while placing a bet. / 用户下注时余额不足。
            activity.runOnUiThread(WebActivity.this::showRechargeDialog);
        }
    }
}
