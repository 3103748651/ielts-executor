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

        // Stable path: web button -> Android bridge -> ACTION_VIEW.
        web.addJavascriptInterface(new AndroidBridge(), "Android");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (uri != null && "file".equalsIgnoreCase(uri.getScheme())) return false;
                if (uri != null) openExternal(uri.toString());
                return true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && url.startsWith("file:///android_asset/")) return false;
                openExternal(url);
                return true;
            }
        });

        web.loadUrl("file:///android_asset/index.html");
    }

    private void openExternal(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            Toast.makeText(this, "这条材料没有链接", Toast.LENGTH_SHORT).show();
            return;
        }
        String url = rawUrl.trim();
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            Toast.makeText(this, "链接必须以 http:// 或 https:// 开头", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            try {
                web.loadUrl(url);
            } catch (Exception ignored) {
                Toast.makeText(this, "没有找到可打开这个链接的应用", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "链接打开失败，请检查链接", Toast.LENGTH_SHORT).show();
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
