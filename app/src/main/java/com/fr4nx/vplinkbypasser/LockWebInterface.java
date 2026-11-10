package com.system.updatex;

import android.webkit.JavascriptInterface;

public class LockWebInterface {
    private OverlayService service;

    public LockWebInterface(OverlayService service) {
        this.service = service;
    }

    @JavascriptInterface
    public void verifyPasskey(String pass) {
        service.authenticateUser(pass);
    }

    @JavascriptInterface
    public void unlockOverlay() {
        service.unlockOverlay();
    }
}
