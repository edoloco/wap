package com.wap.android;
import android.content.ContentProvider; import android.content.ContentValues; import android.content.Context; import android.database.Cursor; import android.database.MatrixCursor; import android.net.Uri; import android.os.ParcelFileDescriptor; import android.provider.OpenableColumns;
import java.io.File; import java.io.FileNotFoundException;
/** Entrega la APK descargada al instalador de Android. */
public final class ApkProvider extends ContentProvider {
    static final String TYPE = "application/vnd.android.package-archive"; private static final String AUTH = "com.wap.apk";
    static File file(Context c) { File d = new File(c.getCacheDir(), "update"); d.mkdirs(); return new File(d, "wap.apk"); }
    static Uri uri() { return Uri.parse("content://" + AUTH + "/wap.apk"); }
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri u) { return TYPE; }
    @Override public ParcelFileDescriptor openFile(Uri u, String mode) throws FileNotFoundException { return ParcelFileDescriptor.open(file(getContext()), ParcelFileDescriptor.MODE_READ_ONLY); }
    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) {
        MatrixCursor c = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        c.addRow(new Object[]{"wap.apk", file(getContext()).length()}); return c;
    }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}
