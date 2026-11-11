package com.system.updatex;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

public class IconUtils {
    public static void hideIcon(Context context) {
        PackageManager pm = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, "com.system.updatex.MainAlias");
        pm.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
        );
    }
}
