package com.fr4nx.vplinkbypasser;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        TextView tv = new TextView(this);
        tv.setText("FR4NX Setup\n\n1. Grant Overlay Permission\n2. Grant Accessibility Permission\n3. Start Service");
        tv.setTextSize(18);
        layout.addView(tv);

        Button btnOverlay = new Button(this);
        btnOverlay.setText("1. Grant Overlay");
        btnOverlay.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
            } else {
                Toast.makeText(this, "Overlay Granted", Toast.LENGTH_SHORT).show();
            }
        });
        layout.addView(btnOverlay);

        Button btnAccess = new Button(this);
        btnAccess.setText("2. Grant Accessibility");
        btnAccess.setOnClickListener(v -> {
            if (!isAccessibilityServiceEnabled()) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } else {
                Toast.makeText(this, "Accessibility Granted", Toast.LENGTH_SHORT).show();
            }
        });
        layout.addView(btnAccess);

        Button btnStart = new Button(this);
        btnStart.setText("3. ENGAGE LOCKOUT");
        btnStart.setOnClickListener(v -> {
            if (Settings.canDrawOverlays(this) && isAccessibilityServiceEnabled()) {
                Intent intent = new Intent(this, OverlayService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
                Toast.makeText(this, "FR4NX Activated.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Missing permissions!", Toast.LENGTH_SHORT).show();
            }
        });
        layout.addView(btnStart);

        setContentView(layout);
    }

    private boolean isAccessibilityServiceEnabled() {
        ComponentName component = new ComponentName(this, KioskAccessibilityService.class);
        String expectedComponent = component.flattenToString();
        String enabledServices = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabledServices)) return false;
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabledServices);
        while (splitter.hasNext()) {
            if (expectedComponent.equalsIgnoreCase(splitter.next())) return true;
        }
        return false;
    }
}
