package com.ielts.executor;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final int REQUEST_PICK_FILE = 140;
    private WebView web;
    private String pendingTarget = "material";

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
                return uri == null || handleNavigation(uri.toString());
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
        Uri uri = Uri.parse(rawUrl.trim());
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);

        if ("file".equals(scheme)) return false;

        if ("http".equals(scheme) || "https".equals(scheme)) {
            openExternal(uri);
            return true;
        }

        if ("appaction".equals(scheme) && "pick".equals(uri.getHost())) {
            pendingTarget = safe(uri.getQueryParameter("target"), "material");
            launchFilePicker();
            return true;
        }

        if ("appfile".equals(scheme) && "open".equals(uri.getHost())) {
            String token = uri.getQueryParameter("token");
            String mime = safe(uri.getQueryParameter("mime"), "application/octet-stream");
            String name = safe(uri.getQueryParameter("name"), "学习材料");
            openImportedFile(token, mime, name);
            return true;
        }

        return true;
    }

    private void openExternal(Uri uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (host.equals("b23.tv") || host.endsWith("bilibili.com")) {
            try {
                Intent bili = new Intent(Intent.ACTION_VIEW, uri);
                bili.setPackage("tv.danmaku.bili");
                startActivity(bili);
                return;
            } catch (Exception ignored) { }
        }

        try {
            Intent view = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(Intent.createChooser(view, "打开学习材料"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "没有可打开这个链接的应用", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "链接打开失败：" + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
    }

    private void launchFilePicker() {
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "image/*"
            });
            startActivityForResult(i, REQUEST_PICK_FILE);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开文件选择器", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_FILE || resultCode != RESULT_OK || data == null || data.getData() == null) return;

        Uri uri = data.getData();
        String name = queryDisplayName(uri);
        String mime = getContentResolver().getType(uri);
        if (mime == null || mime.isEmpty()) mime = guessMime(name);
        if (!isAllowedFile(name, mime)) {
            Toast.makeText(this, "只支持 PDF、Word 和图片文件", Toast.LENGTH_LONG).show();
            return;
        }

        File dir = new File(getFilesDir(), "imports");
        if (!dir.exists() && !dir.mkdirs()) {
            Toast.makeText(this, "无法创建材料目录", Toast.LENGTH_LONG).show();
            return;
        }

        String token = UUID.randomUUID().toString() + "_" + sanitizeFileName(name);
        File out = new File(dir, token);
        try (InputStream in = getContentResolver().openInputStream(uri);
             FileOutputStream fos = new FileOutputStream(out)) {
            if (in == null) throw new IllegalStateException("empty stream");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) fos.write(buf, 0, n);
        } catch (Exception e) {
            Toast.makeText(this, "导入文件失败", Toast.LENGTH_LONG).show();
            return;
        }

        final String js = "window.onNativeFilePicked(" +
                JSONObject.quote(pendingTarget) + "," +
                JSONObject.quote(token) + "," +
                JSONObject.quote(name) + "," +
                JSONObject.quote(mime) + ");";
        web.post(() -> web.evaluateJavascript(js, null));
        Toast.makeText(this, "文件已导入", Toast.LENGTH_SHORT).show();
    }

    private void openImportedFile(String token, String mime, String name) {
        if (token == null || token.trim().isEmpty()) {
            Toast.makeText(this, "文件记录无效", Toast.LENGTH_LONG).show();
            return;
        }
        File dir = new File(getFilesDir(), "imports");
        File f = new File(dir, token);
        try {
            String base = dir.getCanonicalPath() + File.separator;
            if (!f.getCanonicalPath().startsWith(base) || !f.exists()) {
                Toast.makeText(this, "文件不存在，可能已被清理", Toast.LENGTH_LONG).show();
                return;
            }
        } catch (Exception e) {
            Toast.makeText(this, "无法读取文件", Toast.LENGTH_LONG).show();
            return;
        }

        Uri content = new Uri.Builder()
                .scheme("content")
                .authority("com.ielts.executor.files")
                .appendPath(token)
                .build();
        try {
            Intent view = new Intent(Intent.ACTION_VIEW);
            view.setDataAndType(content, safe(mime, guessMime(name)));
            view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(view, "打开学习材料"));
        } catch (Exception e) {
            Toast.makeText(this, "没有可打开该文件的应用", Toast.LENGTH_LONG).show();
        }
    }

    private String queryDisplayName(Uri uri) {
        String name = "学习材料";
        try (Cursor c = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0 && c.getString(i) != null) name = c.getString(i);
            }
        } catch (Exception ignored) { }
        return name;
    }

    private boolean isAllowedFile(String name, String mime) {
        String m = safe(mime, "").toLowerCase(Locale.ROOT);
        String n = safe(name, "").toLowerCase(Locale.ROOT);
        return m.startsWith("image/") || m.equals("application/pdf") ||
                m.equals("application/msword") ||
                m.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                n.endsWith(".pdf") || n.endsWith(".doc") || n.endsWith(".docx") ||
                n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp") || n.endsWith(".gif");
    }

    private String guessMime(String name) {
        String n = safe(name, "").toLowerCase(Locale.ROOT);
        if (n.endsWith(".pdf")) return "application/pdf";
        if (n.endsWith(".doc")) return "application/msword";
        if (n.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".webp")) return "image/webp";
        if (n.endsWith(".gif")) return "image/gif";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }

    private String sanitizeFileName(String s) {
        return safe(s, "file").replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]", "_");
    }

    private String safe(String s, String fallback) {
        return s == null || s.isEmpty() ? fallback : s;
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
