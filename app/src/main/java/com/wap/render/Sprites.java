package com.wap.render;
import java.util.LinkedHashMap; import java.util.Map; import java.util.function.Function;
/**
 * Piezas del personaje en PNG (carpeta app/src/main/assets/personaje). Cada pieza se dibuja dentro de su caja, en las
 * coordenadas del hueso que la mueve, así que un mismo PNG sirve para todas las poses y animaciones.
 * Lo pintado en magenta (#FF00FF) toma el color del traje del jugador; magenta oscuro hace sombra y magenta claro, brillo.
 * Si falta un PNG, esa pieza se dibuja con el vector original.
 */
public final class Sprites {
    /** Imagen en memoria; cache la usa cada plataforma para guardar su versión (Bitmap, BufferedImage). */
    public static final class Img {
        public final int w, h; public final int[] px; public Object cache;
        public Img(int w, int h, int[] px) { this.w = w; this.h = h; this.px = px; }
    }
    /** Píxeles por unidad del mundo con que se exportan las piezas (el personaje mide ~52 unidades). */
    public static final int PPU = 12;
    public static final class Piece {
        public final String name; public final float x0, y0, x1, y1; Img base; boolean key;
        final LinkedHashMap<Long, Img> tints = new LinkedHashMap<Long, Img>(16, .75f, true) { @Override protected boolean removeEldestEntry(Map.Entry<Long, Img> e) { return size() > 24; } };
        Piece(String n, float x0, float y0, float x1, float y1) { name = n; this.x0 = x0; this.y0 = y0; this.x1 = x1; this.y1 = y1; }
        public int pw() { return Math.round((x1 - x0) * PPU); } public int ph() { return Math.round((y1 - y0) * PPU); }
        Img variant(int suit, boolean lit) {
            if (!key && !lit) return base;
            long k = (key ? suit & 0xFFFFFFFFL : 0) | (lit ? 1L << 32 : 0);
            Img im = tints.get(k);
            if (im == null) {
                int[] o = new int[base.px.length];
                for (int i = 0; i < o.length; i++) { int c = base.px[i]; if (key) c = tint(c, suit); o[i] = lit ? light(c) : c; }
                im = new Img(base.w, base.h, o); tints.put(k, im);
            }
            return im;
        }
    }
    public static final LinkedHashMap<String, Piece> PIECES = new LinkedHashMap<>();
    /** Cuántas piezas se cargaron desde PNG. */
    public static int loaded;
    /** Caja de una pieza, ajustada para que mida un número entero de píxeles. */
    static void def(String n, float x0, float y0, float x1, float y1) {
        float m = 1; x0 -= m; y0 -= m; x1 += m; y1 += m;
        x1 = x0 + (float) Math.ceil((x1 - x0) * PPU) / PPU; y1 = y0 + (float) Math.ceil((y1 - y0) * PPU) / PPU;
        PIECES.put(n, new Piece(n, x0, y0, x1, y1));
    }
    static void def(String n, float[]... ps) {
        float a = 1e9f, b = 1e9f, c = -1e9f, d = -1e9f;
        for (float[] p : ps) for (int i = 0; i < p.length; i += 2) { a = Math.min(a, p[i]); c = Math.max(c, p[i]); b = Math.min(b, p[i + 1]); d = Math.max(d, p[i + 1]); }
        def(n, a, b, c, d);
    }
    /** Carga los PNG que existan; read devuelve null si no hay imagen para ese nombre. */
    public static void load(Function<String, Img> read) {
        RobotArt.init(); loaded = 0;
        for (Piece p : PIECES.values()) {
            Img im = null; try { im = read.apply(p.name); } catch (RuntimeException e) { }
            p.base = im; p.tints.clear(); p.key = false;
            if (im == null) continue;
            loaded++; for (int c : im.px) if (isKey(c)) { p.key = true; break; }
        }
    }
    /** Dibuja la pieza si tiene PNG; si no, devuelve false para usar el vector. */
    static boolean draw(Gfx g, String name, int suit, boolean lit) {
        Piece p = PIECES.get(name); if (p == null || p.base == null) return false;
        g.image(p.variant(suit, lit), p.x0, p.y0, p.x1 - p.x0, p.y1 - p.y0); return true;
    }
    /** Tono magenta (rojo y azul parejos, verde más bajo) con color suficiente. */
    static boolean isKey(int c) {
        if (c >>> 24 == 0) return false;
        int r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255, mx = Math.max(r, b), mn = Math.min(Math.min(r, b), g);
        return mx >= 24 && g == mn && mx - mn >= mx * .35f && Math.abs(r - b) <= (mx - mn) * .35f;
    }
    /** Magenta puro: el color del traje; más oscuro: sombra; más claro (rosado): brillo. */
    static int tint(int c, int suit) {
        if (!isKey(c)) return c;
        int r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255, mx = Math.max(r, b);
        float v = mx / 255f, s = (mx - g) / (float) mx;
        int sr = suit >> 16 & 255, sg = suit >> 8 & 255, sb = suit & 255;
        return c & 0xFF000000 | ch(sr, s, v) << 16 | ch(sg, s, v) << 8 | ch(sb, s, v);
    }
    static int ch(int x, float s, float v) { return Math.max(0, Math.min(255, Math.round((x * s + 255 * (1 - s)) * v))); }
    /** Destello del disparo: aclara todo menos los contornos oscuros. */
    static int light(int c) {
        int r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255; if (Math.max(r, Math.max(g, b)) < 60) return c;
        return c & 0xFF000000 | (r + (255 - r) * 2 / 5) << 16 | (g + (255 - g) * 2 / 5) << 8 | b + (255 - b) * 2 / 5;
    }
}
