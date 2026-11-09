package com.fr4nx.vplinkbypasser;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.provider.Settings;
import android.widget.EditText;
import android.widget.Button;
import android.widget.FrameLayout;

public class OverlayService extends Service {
    private static final String CHANNEL_ID = "fr4nx_kiosk";
    private static final int NOTIFICATION_ID = 9001;
    private WindowManager windowManager;
    private WindowManager.LayoutParams windowParams;
    private View overlay;
    private FrameLayout rootView;
    private EditText authInput;
    private Button authButton;
    private int tapCount = 0;
    private long firstTapTime = 0L;
    private boolean overlayAttached = false;
    private boolean authVisible = false;
    private boolean shuttingDown = false;

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
        
        // If already unlocked, don't show the overlay
        boolean isUnlocked = getSharedPreferences("fr4nx_state", MODE_PRIVATE).getBoolean("is_unlocked", false);
        if (isUnlocked) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!overlayAttached) createOverlay();
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

        overlay = LayoutInflater.from(this).inflate(R.layout.overlay_lock, null);
        rootView = (FrameLayout) overlay;
        authInput = overlay.findViewById(R.id.authInput);
        authButton = overlay.findViewById(R.id.authButton);

        if (authInput != null) authInput.setVisibility(View.GONE);
        if (authButton != null) authButton.setVisibility(View.GONE);

        if (authButton != null && authInput != null) {
            authButton.setOnClickListener(v -> authenticateUser());
        }
        if (authInput != null) {
            authInput.setSingleLine(true);
            authInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
            authInput.setOnEditorActionListener((view, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    authenticateUser();
                    return true;
                }
                return false;
            });
        }

        rootView.setOnTouchListener((view, event) -> {
            if (authVisible) return false;
            if (event.getAction() != MotionEvent.ACTION_UP) return false;
            float x = event.getX();
            float y = event.getY();
            if (!(x >= 0 && x <= 100f && y >= 0 && y <= 100f)) {
                tapCount = 0;
                return false;
            }
            long currentTime = SystemClock.elapsedRealtime();
            if (tapCount == 0 || currentTime - firstTapTime > 3000L) {
                tapCount = 1;
                firstTapTime = currentTime;
            } else {
                tapCount++;
            }
            if (tapCount >= 5) {
                tapCount = 0;
                revealAuthentication();
            }
            return false;
        });

        try {
            windowManager.addView(overlay, windowParams);
            overlayAttached = true;
        } catch (Exception e) {
            stopSelf();
        }
    }

    private void revealAuthentication() {
        if (authVisible || authInput == null || authButton == null) return;
        authVisible = true;
        authInput.setVisibility(View.VISIBLE);
        authButton.setVisibility(View.VISIBLE);
        authInput.requestFocus();
        authInput.post(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(authInput, InputMethodManager.SHOW_IMPLICIT);
        });
    }

    private void authenticateUser() {
        if (shuttingDown || authInput == null) return;
        if ("FR4NX".equals(authInput.getText().toString().trim())) {
            shuttingDown = true;
            unlockOverlay();
        } else {
            authInput.setError("Invalid passkey");
            authInput.setText("");
        }
    }

    private void unlockOverlay() {
        // Save the unlocked state so the Accessibility Service stands down
        getSharedPreferences("fr4nx_state", MODE_PRIVATE)
            .edit()
            .putBoolean("is_unlocked", true)
            .apply();

        if (overlayAttached && overlay != null) {
            try {
                windowManager.removeView(overlay);
            } catch (Exception ignored) {}
            overlayAttached = false;
        }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (overlayAttached && overlay != null) {
            try { windowManager.removeView(overlay); } catch (Exception ignored) {}
        }
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
