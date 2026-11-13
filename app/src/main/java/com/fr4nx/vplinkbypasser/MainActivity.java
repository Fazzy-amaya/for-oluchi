package com.fr4nx.vplinkbypasser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import android.net.http.SslError;

public class MainActivity extends Activity {

    private WebView dashboardWebView;
    private WebView manualWebView;
    private WebView hiddenWebView;
    
    private FrameLayout rootLayout;
    private FrameLayout manualBrowserContainer;
    private ProgressBar progressBar;

    private String currentGhostMode = "android";
    private SharedPreferences prefs;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xff0a0512);
        getWindow().setNavigationBarColor(0xff0a0512);

        prefs = getSharedPreferences("fr4nx_prefs", MODE_PRIVATE);
        currentGhostMode = prefs.getString("ghost_mode", "android");

        rootLayout = new FrameLayout(this);

        dashboardWebView = new WebView(this);
        WebSettings uiSettings = dashboardWebView.getSettings();
        uiSettings.setJavaScriptEnabled(true);
        uiSettings.setDomStorageEnabled(true);
        uiSettings.setAllowFileAccessFromFileURLs(true);
        uiSettings.setAllowUniversalAccessFromFileURLs(true);
        dashboardWebView.addJavascriptInterface(new AppBridge(), "Android");
        dashboardWebView.loadUrl("file:///android_asset/home.html");

        manualBrowserContainer = new FrameLayout(this);
        manualBrowserContainer.setVisibility(View.GONE);
        
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xffa855f7));
        manualBrowserContainer.addView(progressBar, new FrameLayout.LayoutParams(-1, 6));

        manualWebView = new WebView(this);
        FrameLayout.LayoutParams wLp = new FrameLayout.LayoutParams(-1, -1);
        wLp.topMargin = 6;
        manualBrowserContainer.addView(manualWebView, wLp);

        hiddenWebView = new WebView(this);
        hiddenWebView.setVisibility(View.GONE);

        rootLayout.addView(hiddenWebView);
        rootLayout.addView(dashboardWebView);
        rootLayout.addView(manualBrowserContainer);
        setContentView(rootLayout);

        setupBypassWebViews();
    }

    private int getAppVersionCode() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (Exception e) {
            return 1;
        }
    }

    private void openExternalLink(String url) {
        try {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (manualWebView != null) manualWebView.setKeepScreenOn(false);
            if (hiddenWebView != null) hiddenWebView.setKeepScreenOn(false);

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No app installed to open this link.", Toast.LENGTH_SHORT).show();
        }
    }

    public class AppBridge {
        @JavascriptInterface
        public String pasteFromClipboard() {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                ClipData clip = clipboard.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    return clip.getItemAt(0).coerceToText(MainActivity.this).toString();
                }
            }
            return "";
        }

        @JavascriptInterface
        public void openTelegramBot() {
            openExternalLink("https://t.me/STAR_BOT_FARMER_BY_FR4NX_bot");
        }

        @JavascriptInterface
        public void openLink(String url) {
            openExternalLink(url);
        }

        @JavascriptInterface
        public void retryBypass(String url) {
            runOnUiThread(() -> {
                if (manualWebView != null) manualWebView.loadUrl(url);
                if (hiddenWebView != null) hiddenWebView.loadUrl(url);
            });
        }

        @JavascriptInterface
        public void startHiddenBypass(String vplink) {
            runOnUiThread(() -> {
                if (hiddenWebView != null && vplink != null) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    hiddenWebView.setKeepScreenOn(true);
                    hiddenWebView.loadUrl(vplink);
                }
            });
        }

        @JavascriptInterface
        public void onBypassComplete(String payload) {
            runOnUiThread(() -> {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                if (hiddenWebView != null) hiddenWebView.setKeepScreenOn(false);
                if (manualWebView != null) manualWebView.setKeepScreenOn(false);
                
                if (dashboardWebView != null) {
                    dashboardWebView.post(() -> dashboardWebView.evaluateJavascript("onBypassComplete('" + payload + "')", null));
                }
            });
        }

        @JavascriptInterface
        public void startManualBypass(String url) {
            runOnUiThread(() -> {
                if (manualWebView == null || dashboardWebView == null) return;
                String finalUrl = url;
                if (!finalUrl.startsWith("http")) finalUrl = "https://" + finalUrl;
                dashboardWebView.setVisibility(View.GONE);
                manualBrowserContainer.setVisibility(View.VISIBLE);
                
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                manualWebView.setKeepScreenOn(true);
                
                manualWebView.loadUrl(finalUrl);
            });
        }

        @JavascriptInterface
        public String getGhostMode() { return currentGhostMode; }

        @JavascriptInterface
        public void setGhostMode(String mode) {
            currentGhostMode = mode;
            prefs.edit().putString("ghost_mode", mode).apply();
            runOnUiThread(() -> applyGhostModeUA());
        }

        @JavascriptInterface
        public void checkForUpdates() {
            runOnUiThread(() -> {
                new Thread(() -> {
                    LicenseManager.VersionInfo vInfo = LicenseManager.checkVersion(MainActivity.this);
                    runOnUiThread(() -> {
                        if (vInfo != null && getAppVersionCode() < vInfo.latestVersionCode) {
                            VersionManager.checkForUpdates(MainActivity.this, vInfo.latestApkUrl, false);
                        } else {
                            Toast.makeText(MainActivity.this, "App is up to date!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }).start();
            });
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupBypassWebViews() {
        WebSettings mSettings = manualWebView.getSettings();
        mSettings.setJavaScriptEnabled(true);
        mSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        mSettings.setDomStorageEnabled(true);
        mSettings.setDatabaseEnabled(true);
        mSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        mSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        mSettings.setSupportZoom(false);
        mSettings.setBuiltInZoomControls(false);
        mSettings.setDisplayZoomControls(false);
        mSettings.setUseWideViewPort(true);
        mSettings.setLoadWithOverviewMode(true);
        mSettings.setMediaPlaybackRequiresUserGesture(true);
        mSettings.setAllowFileAccess(false);
        mSettings.setAllowContentAccess(true);
        mSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        mSettings.setLoadsImagesAutomatically(true); 
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) mSettings.setSafeBrowsingEnabled(true);
        
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(manualWebView, true);
        
        manualWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        manualWebView.setVerticalScrollBarEnabled(false);
        manualWebView.setHorizontalScrollBarEnabled(false);
        manualWebView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        attachBypassClient(manualWebView, true);

        WebSettings hSettings = hiddenWebView.getSettings();
        hSettings.setJavaScriptEnabled(true);
        hSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        hSettings.setDomStorageEnabled(true);
        hSettings.setDatabaseEnabled(true);
        hSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        hSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        hSettings.setMediaPlaybackRequiresUserGesture(true);
        hSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        hSettings.setLoadsImagesAutomatically(true);
        
        CookieManager.getInstance().setAcceptThirdPartyCookies(hiddenWebView, true);
        hiddenWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        attachBypassClient(hiddenWebView, false);

        applyGhostModeUA();
    }

    private void applyGhostModeUA() {
        String ua = "";
        if (currentGhostMode.equals("iphone")) {
            ua = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4.1 Mobile/15E148 Safari/604.1";
        } else if (currentGhostMode.equals("pc")) {
            ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
        } else {
            ua = "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36";
        }
        manualWebView.getSettings().setUserAgentString(ua);
        hiddenWebView.getSettings().setUserAgentString(ua);
    }

    private void injectOptimizer(WebView webView) {
        try {
            InputStream input = getAssets().open("fr4nx_optimizer.js");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            input.close();
            String script = output.toString("UTF-8");
            webView.evaluateJavascript(script, null);
        } catch (Exception e) {
            android.util.Log.e("FR4NX", "Optimizer injection failed", e);
        }
    }

    private void attachBypassClient(WebView webView, boolean isManual) {
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                if (isManual) {
                    progressBar.setProgress(progress);
                    if (progress >= 100) progressBar.setVisibility(View.GONE);
                    else progressBar.setVisibility(View.VISIBLE);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                view.evaluateJavascript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined});", null);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString().toLowerCase();
                String host = request.getUrl().getHost();
                
                if (request.isForMainFrame()) return super.shouldInterceptRequest(view, request);
                if (host == null) return super.shouldInterceptRequest(view, request);
                
                host = host.toLowerCase();

                if (host.contains("vplink") || host.contains("cloudflare")) {
                    return super.shouldInterceptRequest(view, request);
                }

                String[] trackers = {"google-analytics.com", "googletagmanager.com", "doubleclick.net", "googlesyndication.com", "facebook.net", "hotjar.com", "clarity.ms"};
                for (String t : trackers) {
                    if (host.equals(t) || host.endsWith("." + t)) {
                        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
                    }
                }

                if (url.endsWith(".woff") || url.endsWith(".woff2") || url.endsWith(".ttf") || url.endsWith(".otf")) {
                    return new WebResourceResponse("font/woff2", "utf-8", new ByteArrayInputStream("".getBytes()));
                }
                
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                if (url.contains("linkedin.com/redir") || url.contains("lnkd.in")) {
                    return true;
                }

                if (url.startsWith("tg://") || url.contains("t.me") || url.contains("telegram.me") || url.startsWith("intent://") || url.startsWith("whatsapp://")) {
                    openExternalLink(url);
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (!url.contains("t.me") && !url.contains("telegram.org")) {
                    injectOptimizer(view);
                }

                String rawPayload = """
                (function () {
                  'use strict';
                  
                  if (!document.getElementById('fr4nx-style')) {
                      var style = document.createElement('style');
                      style.id = 'fr4nx-style';
                      style.innerHTML = `
                          body { background: linear-gradient(135deg, #0a0512 0%, #150d20 100%) !important; color: #e2d9f3 !important; }
                          
                          /* SAFELY HIDE SPAM AND AD FRAMES (DOES NOT TARGET GENERAL T.ME LINKS) */
                          #block-cont-1, .ad-overlay, .ad-container, .advertisement, .ad-banner,
                          .telegram-btn, .telegram-image, img[src*="CGzTnac.png"] { 
                              display: none !important; 
                              pointer-events: none !important; 
                              opacity: 0 !important;
                          }

                          /* FORCE CONTINUE BUTTON TO HIGHEST LAYER */
                          #btn7 {
                              position: relative !important;
                              z-index: 2147483647 !important;
                              pointer-events: auto !important;
                              display: block !important;
                          }

                          #fr4nx-wait-overlay { position: fixed; inset: 0; background: rgba(10,5,18,0.95); z-index: 2147483646; display: flex; flex-direction: column; justify-content: center; align-items: center; text-align: center; font-family: sans-serif; pointer-events: none; }
                          #fr4nx-wait-overlay h1 { color: #8b5cf6; font-size: 28px; font-weight: 900; letter-spacing: 4px; margin: 0; text-shadow: 0 0 20px #8b5cf6; }
                          #fr4nx-wait-overlay p { color: #00ffff; font-size: 14px; letter-spacing: 2px; margin-top: 10px; }
                          .fr4nx-spinner { width: 50px; height: 50px; border: 4px solid rgba(168, 85, 247, 0.3); border-top-color: #00ffff; border-radius: 50%; animation: spin 1s linear infinite; margin-bottom: 20px; }
                          @keyframes spin { to { transform: rotate(360deg); } }
                      `;
                      document.head.appendChild(style);
                      
                      var overlay = document.createElement('div');
                      overlay.id = 'fr4nx-wait-overlay';
                      overlay.innerHTML = '<div class="fr4nx-spinner"></div><h1>BYPASSING</h1><p>SIT BACK AND WAIT...</p>';
                      document.body.appendChild(overlay);
                  }

                  setTimeout(function() {
                      var ov = document.getElementById('fr4nx-wait-overlay');
                      if (ov) ov.style.display = 'none';
                  }, 2000);

                  if (document.getElementById('fr4nx-float-bypass')) return;
                  var fb = document.createElement('div');
                  fb.id = 'fr4nx-float-bypass';
                  fb.innerHTML = '⚡ BYPASS';
                  fb.style.cssText = 'position:fixed;bottom:20px;right:20px;z-index:2147483645;background:linear-gradient(135deg,#a855f7,#7c3aed);color:#fff;padding:12px 20px;border-radius:30px;font-weight:bold;font-family:sans-serif;font-size:14px;box-shadow:0 0 20px rgba(168,85,247,0.6);cursor:pointer;border:2px solid #fff;user-select:none;';
                  fb.onclick = function() {
                      forceClickNext();
                      fb.innerHTML = '⏳ WAIT...';
                      setTimeout(function() { fb.innerHTML = '⚡ BYPASS'; }, 1500);
                  };
                  document.body.appendChild(fb);

                  // TRUE INTERNAL CLICK (Direct DOM Event)
                  function forceClick(el) {
                    if (!el) return;
                    el.removeAttribute('disabled');
                    el.style.pointerEvents = 'auto';
                    el.style.display = 'block';
                    el.style.zIndex = '2147483647';
                    try { el.scrollIntoView({behavior: 'instant', block: 'center'}); } catch(e){}
                    
                    if (el.tagName.toLowerCase() === 'a') {
                        el.addEventListener('click', function(e){ e.preventDefault(); }, {once: true});
                    }
                    
                    el.dispatchEvent(new MouseEvent('mousedown', {bubbles: true, cancelable: true, view: window}));
                    el.dispatchEvent(new MouseEvent('mouseup', {bubbles: true, cancelable: true, view: window}));
                    el.dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true, view: window}));
                  }

                  function forceClickNext() {
                      var targetIds = ['startCountdownBtn', 'continueBtn', 'tp-snp2', 'cross-snp2', 'btn6', 'btn7'];
                      for (var i = 0; i < targetIds.length; i++) {
                          var el = document.getElementById(targetIds[i]);
                          if (el) { forceClick(el); return; }
                      }
                      var clickables = document.querySelectorAll('a, button, input[type="submit"]');
                      for (var j = 0; j < clickables.length; j++) {
                          var btn = clickables[j];
                          var txt = (btn.innerText || btn.value || "").toUpperCase();
                          if (txt.includes("VERIFY") || txt.includes("CONTINUE") || txt.includes("GET LINK") || txt.includes("NEXT") || txt.includes("CLICK HERE")) {
                              forceClick(btn); return;
                          }
                      }
                  }

                  var clicked = {};
                  var captchaMode = false;

                  setInterval(function() {
                    if (!document.body) return;

                    document.querySelectorAll('#block-cont-1, .telegram-btn, img[src*="CGzTnac.png"]').forEach(el => el.remove());

                    var hasTurnstile = document.querySelector('iframe[src*="challenges.cloudflare.com"]') || document.querySelector('.cf-turnstile');
                    
                    if (hasTurnstile) {
                        var turnstileResponse = document.querySelector('input[name="cf-turnstile-response"]');
                        var isVerified = turnstileResponse && turnstileResponse.value && turnstileResponse.value.length > 20;

                        if (!isVerified) {
                            if (!captchaMode) {
                                captchaMode = true;
                                var capUI = document.getElementById('fr4nx-cap');
                                if (!capUI) {
                                    capUI = document.createElement('div');
                                    capUI.id = 'fr4nx-cap';
                                    capUI.style.cssText = 'position:fixed;top:20px;left:50%;transform:translateX(-50%);z-index:2147483647;background:rgba(0,0,0,0.95);color:#fbbf24;padding:15px 30px;border-radius:12px;border:2px solid #fbbf24;font:bold 16px sans-serif;text-align:center;';
                                    capUI.innerHTML = '🛑 VERIFICATION REQUIRED 🛑<br><span style="font-size:13px;color:#fff;">Please tick the box and wait for the page to continue.</span>';
                                    document.documentElement.appendChild(capUI);
                                }
                            }
                            return; 
                        } else {
                            var existingCapUI = document.getElementById('fr4nx-cap');
                            if (existingCapUI) existingCapUI.remove();
                            captchaMode = false;
                        }
                    } else {
                        var existingCapUI = document.getElementById('fr4nx-cap');
                        if (existingCapUI) existingCapUI.remove();
                        captchaMode = false;
                    }

                    ['tp-time', 'link1s-time', 'link1s-wait1', 'wait-timer', 'countdown'].forEach(function(id) {
                        var el = document.getElementById(id);
                        if (el) el.style.display = 'none';
                    });

                    var targetIds = ['startCountdownBtn', 'continueBtn', 'tp-snp2', 'cross-snp2', 'btn6', 'btn7'];
                    for (var i = 0; i < targetIds.length; i++) {
                        var id = targetIds[i];
                        var el = document.getElementById(id);
                        if (el && !clicked[id]) {
                            clicked[id] = true;
                            forceClick(el);
                            setTimeout(function() { delete clicked[id]; }, 4000);
                            return;
                        }
                    }

                    var imgs = document.querySelectorAll('a > img');
                    for (var k = 0; k < imgs.length; k++) {
                        var imgParent = imgs[k].closest('a');
                        if (imgParent && !clicked['img_' + k]) {
                            clicked['img_' + k] = true;
                            forceClick(imgParent);
                            setTimeout(function() { delete clicked['img_' + k]; }, 4000);
                            return;
                        }
                    }

                    var clickables = document.querySelectorAll('a, button, input[type="submit"]');
                    for (var j = 0; j < clickables.length; j++) {
                        var btn = clickables[j];
                        var txt = (btn.innerText || btn.value || "").toUpperCase();
                        if (txt.includes("VERIFY")) { forceClick(btn); return; }
                    }
                    for (var j = 0; j < clickables.length; j++) {
                        var btn = clickables[j];
                        var txt = (btn.innerText || btn.value || "").toUpperCase();
                        if (txt.includes("CONTINUE") || txt.includes("GET LINK") || txt.includes("NEXT") || txt.includes("CLICK HERE")) {
                            forceClick(btn); return;
                        }
                    }

                    var links = document.querySelectorAll('a');
                    for (var l = 0; l < links.length; l++) {
                        var href = links[l].href;
                        
                        // SKIP THE VPLINK SPAM CHANNEL EXPLICITLY
                        if (href && href.includes('t.me/+') && links[l].innerText.includes('Vplink.in')) continue;
                        
                        // TARGET YOUR REAL FINAL LINK
                        if (href && (href.includes('t.me') || href.includes('tg://') || href.includes('whatsapp://')) && !links[l].fr4nxLocked) {
                            links[l].fr4nxLocked = true;
                            links[l].href = "javascript:void(0);";
                            links[l].innerHTML = "✅ TAP HERE TO OPEN LINK";
                            links[l].style.background = "#4ade80";
                            links[l].style.color = "#000";
                            links[l].style.padding = "15px";
                            links[l].style.display = "block";
                            links[l].style.borderRadius = "8px";
                            links[l].style.fontWeight = "bold";
                            links[l].style.width = "100%";
                            links[l].style.boxSizing = "border-box";
                            links[l].style.textAlign = "center";
                            links[l].style.fontSize = "16px";
                            links[l].style.zIndex = "2147483647";

                            var warnBanner = document.createElement('div');
                            warnBanner.style.cssText = 'background:#000; color:#fbbf24; border:2px solid #fbbf24; padding:15px; border-radius:10px; margin-bottom:15px; text-align:center; font-family:sans-serif; font-size:14px; line-height:1.5;';
                            warnBanner.innerHTML = '<b>⚠️ ATTENTION</b><br>Some bots detect instant bypasses.<br><b>You MUST wait at least 4 minutes</b> before clicking the button below, or you will be rejected.';
                            
                            if (links[l].parentNode) links[l].parentNode.insertBefore(warnBanner, links[l]);

                            (function(finalHref) {
                                links[l].onclick = function(e) {
                                    e.preventDefault();
                                    if (window.Android && window.Android.openLink) {
                                        window.Android.openLink(finalHref);
                                    } else {
                                        window.location.href = finalHref;
                                    }
                                };
                            })(href);
                        }
                    }
                  }, 1200); 
                })();
                """;

                if (!url.contains("t.me") && !url.contains("telegram.org")) {
                    view.viewTreeObserver();
                    view.postDelayed(() -> view.evaluateJavascript(rawPayload, null), 1000);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    String failedUrl = request.getUrl().toString();
                    if (failedUrl.startsWith("http")) {
                        String errorMsg = "<html><body style='background:#0a0512;color:#a78bfa;font-family:sans-serif;display:flex;flex-direction:column;justify-content:center;align-items:center;height:100vh;margin:0;text-align:center;'>" +
                                "<h1 style='color:#ef4444;'>Connection Error</h1>" +
                                "<p>No internet or the server is down.</p>" +
                                "<button onclick=\"window.Android.retryBypass('" + failedUrl + "')\" style='margin-top:20px;padding:15px 30px;background:#a855f7;color:#fff;border:none;border-radius:10px;font-weight:bold;'>Retry</button>" +
                                "</body></html>";
                        view.loadDataWithBaseURL(null, errorMsg, "text/html", "UTF-8", null);
                    }
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (manualBrowserContainer.getVisibility() == View.VISIBLE) {
            if (manualWebView.canGoBack()) {
                manualWebView.goBack();
            } else {
                manualBrowserContainer.setVisibility(View.GONE);
                dashboardWebView.setVisibility(View.VISIBLE);
                manualWebView.loadUrl("about:blank");
                
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                manualWebView.setKeepScreenOn(false);
                hiddenWebView.setKeepScreenOn(false);
                
                dashboardWebView.post(() -> dashboardWebView.evaluateJavascript("resetBypassButton()", null));
            }
        } else if (dashboardWebView.canGoBack()) {
            dashboardWebView.goBack();
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            manualWebView.setKeepScreenOn(false);
            hiddenWebView.setKeepScreenOn(false);
            super.onBackPressed();
        }
    }
}
