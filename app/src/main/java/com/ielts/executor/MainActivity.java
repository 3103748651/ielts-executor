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

        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (uri == null) return true;
                return handleNavigation(uri.toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleNavigation(url);
            }
        });

        web.loadUrl("file:///android_asset/index.html");
    }

    private boolean handleNavigation(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) return true;
        String url = rawUrl.trim();
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();

        if ("file".equalsIgnoreCase(scheme)) return false;

        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            openExternal(url);
            return true;
        }

        return false;
    }

    private void openExternal(String url) {
        Uri uri = Uri.parse(url);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();

        // Bilibili material: first try the official Bilibili Android app directly.
        if (host.equals("b23.tv") || host.endsWith("bilibili.com")) {
            try {
                Intent bili = new Intent(Intent.ACTION_VIEW, uri);
                bili.setPackage("tv.danmaku.bili");
                startActivity(bili);
                return;
            } catch (Exception ignored) {
                // Fall through to the system chooser/browser.
            }
        }

        try {
            Intent view = new Intent(Intent.ACTION_VIEW, uri);
            Intent chooser = Intent.createChooser(view, "打开学习材料");
            startActivity(chooser);
        } catch (ActivityNotFoundException e) {
            try {
                web.loadUrl(url);
            } catch (Exception ignored) {
                Toast.makeText(this, "没有可打开该链接的应用", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "链接打开失败：" + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
