package com.ielts.executor;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
        s.setSupportMultipleWindows(false);

        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl() == null ? null : request.getUrl().toString();
                return handleNavigation(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleNavigation(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                // Use a real HTTP/HTTPS navigation instead of custom schemes or JS bridges.
                // WebViewClient intercepts the navigation and hands it to Android ACTION_VIEW.
                // If interception ever fails on an OEM WebView, the page still opens inside WebView
                // rather than the button silently doing nothing.
                String js = "(function(){" +
                        "window.openLink=function(raw){" +
                        "var s=String(raw||'').trim();" +
                        "var m=s.match(/https?:\\/\\/[^\\s<>\\\"']+/i);" +
                        "var u=m?m[0]:s;" +
                        "u=u.replace(/[，。！？、）)\\]}]+$/,'');" +
                        "if(!u){alert('这条材料没有可打开的链接');return;}" +
                        "if(!/^https?:\\/\\//i.test(u)){u='https://'+u;}" +
                        "var a=document.createElement('a');" +
                        "a.href=u;a.target='_self';a.rel='external';" +
                        "a.style.display='none';document.body.appendChild(a);" +
                        "a.click();setTimeout(function(){try{a.remove();}catch(e){}},1000);" +
                        "};" +
                        "})();";
                view.evaluateJavascript(js, null);
            }
        });

        web.loadUrl("file:///android_asset/index.html");
    }

    private boolean handleNavigation(String url) {
        if (url == null || url.trim().isEmpty()) return true;

        Uri uri = Uri.parse(url.trim());
        String scheme = uri.getScheme();

        if ("file".equalsIgnoreCase(scheme)) {
            return false;
        }

        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme) || "intent".equalsIgnoreCase(scheme)) {
            openExternal(url);
            return true;
        }

        // Hand any other explicit scheme (for example an app deep link) to Android as well.
        if (scheme != null && !scheme.isEmpty()) {
            openExternal(url);
            return true;
        }

        return false;
    }

    private void openExternal(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            Toast.makeText(this, "这条材料没有可打开的链接", Toast.LENGTH_SHORT).show();
            return;
        }

        String target = rawUrl.trim();

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
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } catch (Exception e) {
            // Final fallback: load in the WebView so the click never becomes a no-op.
            try {
                web.loadUrl(target);
            } catch (Exception ignored) {
                Toast.makeText(this, "无法打开链接，请检查材料链接", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
