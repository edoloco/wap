package com.wap.android;
import android.content.Context; import android.content.Intent; import android.content.pm.PackageManager; import android.net.Uri; import android.os.Build; import android.provider.Settings;
import com.wap.render.UpdateNet; import com.wap.render.Updater;
import java.io.IOException;
/** Busca la última versión en GitHub Releases, descarga la APK y abre el instalador de Android. */
final class AppUpdater implements Updater {
    private static final String API = "https://api.github.com/repos/edoloco/wap/releases/tags/ultima";
    private static final String PAGE = "https://github.com/edoloco/wap/releases/tag/ultima";
    private static final String APK = "https://github.com/edoloco/wap/releases/download/ultima/wap.apk";
    private static final int IDLE = 0, BUSY = 1, CURRENT = 2, AVAILABLE = 3, LOADING = 4, READY = 5, ERROR = 6, NEED_PERMISSION = 7, FAILED = 8;
    private final Context ctx; private final int mine; private volatile int state = IDLE, percent, latest;
    AppUpdater(Context c) {
        ctx = c.getApplicationContext(); int v = 0;
        try { v = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionCode; } catch (PackageManager.NameNotFoundException e) { }
        mine = v;
    }
    @Override public String version() { return "Versión " + mine; }
    @Override public String label() {
        switch (state) {
            case BUSY: return "BUSCANDO VERSIÓN...";
            case CURRENT: return "VERSIÓN AL DÍA";
            case AVAILABLE: return "ACTUALIZAR A VERSIÓN " + latest;
            case LOADING: return "DESCARGANDO " + percent + "%";
            case READY: return "INSTALAR VERSIÓN " + latest;
            case ERROR: return "SIN CONEXIÓN: REINTENTAR";
            case FAILED: return "ERROR AL ACTUALIZAR: REINTENTAR";
            case NEED_PERMISSION: return "PERMITE INSTALAR Y TOCA AQUÍ";
            default: return "BUSCAR ACTUALIZACIÓN";
        }
    }
    @Override public void act() {
        int s = state;
        if (s == IDLE || s == CURRENT || s == ERROR || s == FAILED) check();
        else if (s == AVAILABLE || s == NEED_PERMISSION) {
            if (!allowed()) { state = NEED_PERMISSION; openPermission(); return; }
            download();
        } else if (s == READY) install();
    }
    void check() {
        state = BUSY;
        new Thread(() -> {
            try {
                latest = UpdateNet.latest(API, PAGE);
                state = latest > mine ? AVAILABLE : CURRENT;
            } catch (IOException e) { state = ERROR; } catch (RuntimeException e) { state = FAILED; }
        }, "update-check").start();
    }
    private boolean allowed() { return Build.VERSION.SDK_INT < 26 || ctx.getPackageManager().canRequestPackageInstalls(); }
    private void openPermission() {
        if (Build.VERSION.SDK_INT < 26) return;
        Intent i = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + ctx.getPackageName())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
    }
    private void download() {
        state = LOADING; percent = 0;
        new Thread(() -> {
            try {
                UpdateNet.download(APK, ApkProvider.file(ctx), p -> percent = p);
                state = READY; install();
            } catch (IOException e) { state = ERROR; } catch (RuntimeException e) { state = FAILED; }
        }, "update-download").start();
    }
    private void install() {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW).setDataAndType(ApkProvider.uri(), ApkProvider.TYPE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ctx.startActivity(i);
        } catch (RuntimeException e) { state = FAILED; }
    }
}
