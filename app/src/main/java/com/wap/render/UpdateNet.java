package com.wap.render;
import java.io.ByteArrayOutputStream; import java.io.File; import java.io.FileOutputStream; import java.io.IOException; import java.io.InputStream; import java.net.HttpURLConnection; import java.net.URL;
import java.util.function.IntConsumer; import java.util.regex.Matcher; import java.util.regex.Pattern;
/** Red del actualizador, sin dependencias de Android para poder probarla en escritorio. */
public final class UpdateNet {
    private static final Pattern NAME = Pattern.compile("WAP (\\d+)");
    private UpdateNet() {}
    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(10000); c.setReadTimeout(20000); c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "WAP-app"); // las cabeceras van antes de conectar
        if (c.getResponseCode() != 200) { c.disconnect(); throw new IOException("HTTP " + c.getResponseCode()); }
        return c;
    }
    static int parse(String text) { Matcher m = NAME.matcher(text); if (!m.find()) return -1; return Integer.parseInt(m.group(1)); }
    private static String read(String url) throws IOException {
        HttpURLConnection c = open(url); ByteArrayOutputStream b = new ByteArrayOutputStream();
        try (InputStream in = c.getInputStream()) { byte[] q = new byte[8192]; int n; while ((n = in.read(q)) > 0) b.write(q, 0, n); }
        return b.toString("UTF-8");
    }
    /** Versión publicada: usa la API de GitHub y, si falla, la página de la versión. */
    public static int latest(String api, String page) throws IOException {
        IOException first = null;
        for (String u : new String[]{api, page}) {
            try { int v = parse(read(u)); if (v >= 0) return v; } catch (IOException e) { if (first == null) first = e; }
        }
        throw first != null ? first : new IOException("sin versión");
    }
    public static void download(String url, File out, IntConsumer percent) throws IOException {
        File tmp = new File(out.getPath() + ".part");
        HttpURLConnection c = open(url); long total = c.getContentLengthLong(), got = 0;
        try (InputStream in = c.getInputStream(); FileOutputStream o = new FileOutputStream(tmp)) {
            byte[] q = new byte[16384]; int n;
            while ((n = in.read(q)) > 0) { o.write(q, 0, n); got += n; if (total > 0) percent.accept((int) (got * 100 / total)); }
        }
        if (got == 0 || total > 0 && got != total) throw new IOException("descarga incompleta");
        if (out.exists()) out.delete();
        if (!tmp.renameTo(out)) throw new IOException("no se pudo guardar");
    }
}
