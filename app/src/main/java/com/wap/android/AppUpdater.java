package com.wap.android;
import android.content.Context; import android.content.Intent; import android.content.pm.PackageManager; import android.net.Uri; import android.os.Build; import android.provider.Settings;
import com.wap.render.Updater;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream; import java.io.File; import java.io.FileOutputStream; import java.io.InputStream; import java.net.HttpURLConnection; import java.net.URL;
/** Busca la última versión en GitHub Releases, descarga la APK y abre el instalador de Android. */
final class AppUpdater implements Updater {
    private static final String API = "https://api.github.com/repos/edoloco/wap/releases/tags/ultima";
    private static final String APK = "https://github.com/edoloco/wap/releases/download/ultima/wap.apk";
    private static final int IDLE = 0, BUSY = 1, CURRENT = 2, AVAILABLE = 3, LOADING = 4, READY = 5, ERROR = 6, NEED_PERMISSION = 7;
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
            case NEED_PERMISSION: return "PERMITE INSTALAR Y TOCA AQUÍ";
            default: return "BUSCAR ACTUALIZACIÓN";
        }
    }
    @Override public void act() {
        int s = state;
        if (s == IDLE || s == CURRENT || s == ERROR) check();
        else if (s == AVAILABLE || s == NEED_PERMISSION) {
            if (!allowed()) { state = NEED_PERMISSION; openPermission(); return; }
            download();
        } else if (s == READY) install();
    }
    void check() {
        state = BUSY;
        new Thread(() -> {
            try {
                HttpURLConnection c = open(API); c.setRequestProperty("Accept", "application/vnd.github+json");
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                try (InputStream in = c.getInputStream()) { byte[] q = new byte[4096]; int n; while ((n = in.read(q)) > 0) b.write(q, 0, n); }
                String name = new JSONObject(b.toString("UTF-8")).getString("name");
                latest = Integer.parseInt(name.replaceAll("[^0-9]", ""));
                state = latest > mine ? AVAILABLE : CURRENT;
            } catch (Exception e) { state = ERROR; }
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
                File f = ApkProvider.file(ctx); File tmp = new File(f.getPath() + ".part");
                HttpURLConnection c = open(APK); long total = c.getContentLengthLong(), got = 0;
                try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(tmp)) {
                    byte[] q = new byte[16384]; int n;
                    while ((n = in.read(q)) > 0) { out.write(q, 0, n); got += n; if (total > 0) percent = (int) (got * 100 / total); }
                }
                if (got == 0 || total > 0 && got != total || !tmp.renameTo(f)) throw new java.io.IOException("descarga incompleta");
                state = READY; install();
            } catch (Exception e) { state = ERROR; }
        }, "update-download").start();
    }
    private void install() {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW).setDataAndType(ApkProvider.uri(), ApkProvider.TYPE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ctx.startActivity(i);
        } catch (Exception e) { state = ERROR; }
    }
    private static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(10000); c.setReadTimeout(20000); c.setInstanceFollowRedirects(true); c.setRequestProperty("User-Agent", "WAP-app");
        if (c.getResponseCode() != 200) throw new java.io.IOException("HTTP " + c.getResponseCode());
        return c;
    }
}
