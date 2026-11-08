package com.fr4nx.vplinkbypasser;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

public class KioskAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence packageName = event.getPackageName();
        if (packageName == null) return;
        
        String foregroundPackage = packageName.toString();
        
        boolean isAllowed = foregroundPackage.equals("com.fr4nx.vplinkbypasser") 
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
