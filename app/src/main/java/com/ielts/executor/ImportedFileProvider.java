package com.ielts.executor;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;

public class ImportedFileProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }

    private File resolve(Uri uri) throws FileNotFoundException {
        if (getContext() == null) throw new FileNotFoundException();
        String token = uri.getLastPathSegment();
        if (token == null || token.isEmpty()) throw new FileNotFoundException();
        File dir = new File(getContext().getFilesDir(), "imports");
        File f = new File(dir, token);
        try {
            String base = dir.getCanonicalPath() + File.separator;
            if (!f.getCanonicalPath().startsWith(base) || !f.exists()) throw new FileNotFoundException();
        } catch (FileNotFoundException e) { throw e; }
        catch (Exception e) { throw new FileNotFoundException(); }
        return f;
    }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(resolve(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public String getType(Uri uri) {
        try {
            String name = resolve(uri).getName().toLowerCase(Locale.ROOT);
            String ext = MimeTypeMap.getFileExtensionFromUrl(name);
            String m = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            return m == null ? "application/octet-stream" : m;
        } catch (Exception e) { return "application/octet-stream"; }
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        try {
            File f = resolve(uri);
            MatrixCursor c = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
            String n = f.getName();
            int underscore = n.indexOf('_');
            if (underscore >= 0 && underscore + 1 < n.length()) n = n.substring(underscore + 1);
            c.addRow(new Object[]{n, f.length()});
            return c;
        } catch (Exception e) { return null; }
    }

    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
}
