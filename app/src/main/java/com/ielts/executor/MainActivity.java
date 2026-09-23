package com.ielts.executor;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WebView web;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setJavaScriptCanOpenWindowsAutomatically(false);

        web.addJavascriptInterface(new AndroidBridge(), "Android");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl().toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Replace the web-only jump method with a navigation scheme that WebViewClient
                // can reliably intercept on Android/MIUI. This avoids depending on JS bridge calls.
                String js = "(function(){" +
                        "window.openLink=function(raw){" +
                        "var s=String(raw||'').trim();" +
                        "var m=s.match(/https?:\\/\\/[^\\s<>\\\"']+/i);" +
                        "var u=m?m[0]:s;" +
                        "if(!u){alert('这条材料没有可打开的链接');return;}" +
                        "window.location.href='ieltsopen://open?url='+encodeURIComponent(u);" +
                        "};" +
                        "window.open=function(u){if(u){window.location.href='ieltsopen://open?url='+encodeURIComponent(u);}return null;};" +
                        "})();";
                view.evaluateJavascript(js, null);
            }
        });

        web.loadUrl("file:///android_asset/index.html");
    }

    private boolean handleUrl(String url) {
        if (url == null || url.trim().isEmpty()) return true;
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();

        if ("file".equalsIgnoreCase(scheme)) {
            return false;
        }

        if ("ieltsopen".equalsIgnoreCase(scheme)) {
            String target = uri.getQueryParameter("url");
            openExternal(target);
            return true;
        }

        // Any real external navigation leaves the WebView and is handed to Android.
        openExternal(url);
        return true;
    }

    private void openExternal(String url) {
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(this, "这条材料没有可打开的链接", Toast.LENGTH_SHORT).show();
            return;
        }

        String target = url.trim();
        try {
            if (target.startsWith("intent://")) {
                Intent intent = Intent.parseUri(target, Intent.URI_INTENT_SCHEME);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    String fallback = intent.getStringExtra("browser_fallback_url");
                    if (fallback != null && !fallback.isEmpty()) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fallback)));
                    } else {
                        throw e;
                    }
                }
                return;
            }

            if (!target.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) {
                target = "https://" + target;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(target));
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开链接，请检查链接是否有效", Toast.LENGTH_SHORT).show();
        }
    }

    public class AndroidBridge {
        @JavascriptInterface
        public void openExternal(String url) {
            runOnUiThread(() -> MainActivity.this.openExternal(url));
        }
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
