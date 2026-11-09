package com.wap.render;
import java.io.BufferedReader; import java.io.InputStream; import java.io.InputStreamReader; import java.nio.charset.StandardCharsets;
import java.util.ArrayList; import java.util.HashMap;
/**
 * Arte dibujado por el usuario para el personaje. Cada capa de la plantilla (bota_cercana, pecho...) trae sus
 * formas en coordenadas locales del hueso que la mueve, así que sirven para cualquier pose.
 * El recurso arte.txt lo genera tools/importar_arte.py a partir de los SVG de arte/.
 */
public final class Arte {
    /** Una forma: uno o más contornos con el mismo relleno. */
    public static final class Shape {
        public float[][] polys; public int fill, stroke; public float sw; public boolean evenOdd, suit;
    }
    /** Formas por capa y por tramo de transformación dentro de la capa. */
    final HashMap<String, ArrayList<ArrayList<Shape>>> layers = new HashMap<>();
    public static Arte current;
    /** Color del traje en los dibujos del usuario: se reemplaza por el color de cada jugador. */
    static final int SUIT = 0x5F574E;

    public static Arte load(InputStream in) throws Exception {
        Arte a = new Arte(); BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        ArrayList<ArrayList<Shape>> cur = null; Shape s = null; ArrayList<float[]> ps = new ArrayList<>(); String line;
        while ((line = r.readLine()) != null) {
            line = line.trim(); if (line.isEmpty() || line.startsWith("#")) continue;
            String[] t = line.split(" ");
            switch (t[0]) {
                case "L": if (s != null) { s.polys = ps.toArray(new float[0][]); s = null; } cur = new ArrayList<>(); a.layers.put(t[1], cur); break;
                case "S": {
                    if (s != null) s.polys = ps.toArray(new float[0][]);
                    ps = new ArrayList<>(); s = new Shape(); int j = Integer.parseInt(t[1]);
                    while (cur.size() <= j) cur.add(new ArrayList<>());
                    s.fill = (int) Long.parseLong(t[2], 16); s.stroke = (int) Long.parseLong(t[3], 16); s.sw = Float.parseFloat(t[4]); s.evenOdd = t[5].equals("1");
                    s.suit = (s.fill & 0xFFFFFF) == SUIT && s.fill != 0; cur.get(j).add(s); break; }
                case "P": { float[] p = new float[t.length - 1]; for (int i = 1; i < t.length; i++) p[i - 1] = Float.parseFloat(t[i]); ps.add(p); break; }
                default:
            }
        }
        if (s != null) s.polys = ps.toArray(new float[0][]);
        return a;
    }
    /** Carga el arte incluido en la aplicación (si existe). */
    public static void loadDefault() {
        try (InputStream in = Arte.class.getResourceAsStream("/com/wap/render/arte.txt")) { if (in != null) current = load(in); } catch (Exception e) { current = null; }
    }
    public boolean has(String layer) { return layers.containsKey(layer); }

    static int lighten(int c, float k) {
        int r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
        return c & 0xFF000000 | (int) (r + (255 - r) * k) << 16 | (int) (g + (255 - g) * k) << 8 | (int) (b + (255 - b) * k);
    }

    /**
     * Pincel que envuelve al Gfx real. Dentro de una capa con arte, cada vez que la transformación cambia empieza un
     * tramo nuevo (0, 1, 2...); el primer dibujo de cada tramo se reemplaza por las formas del usuario de ese tramo
     * y los demás dibujos originales se omiten. Con arte nulo solo mide (lo usa la herramienta de importación).
     */
    public static class Pen implements Gfx {
        protected final Gfx t; final Arte art; public int suit; protected String layer; ArrayList<ArrayList<Shape>> groups; protected int j = -1; int done = -1; boolean dirty;
        public Pen(Gfx target, Arte art) { this.t = target; this.art = art; }
        /** Devuelve true si el dibujo original se omite. */
        protected boolean hit(boolean lit, float[] box) {
            if (layer == null) return false;
            if (j < 0 || dirty) { j++; dirty = false; }
            if (groups == null) return false;
            if (j > done) { done = j; if (j < groups.size()) for (Shape s : groups.get(j)) draw(s, lit); }
            return true;
        }
        void draw(Shape s, boolean lit) {
            int f = s.suit ? (s.fill & 0xFF000000 | suit & 0xFFFFFF) : s.fill;
            if (lit && f != 0 && (f & 0xFFFFFF) != 0) f = lighten(f, .45f);
            t.shape(s.polys, f, s.evenOdd, s.stroke, s.sw);
        }
        @Override public void layer(String name) { layer = name; j = -1; done = -1; dirty = false; groups = art == null ? null : art.layers.get(name); if (art != null && groups == null) layer = null; t.layer(name); }
        @Override public void endLayer() { layer = null; groups = null; t.endLayer(); }
        @Override public int w() { return t.w(); } @Override public int h() { return t.h(); }
        @Override public void save() { dirty = true; t.save(); } @Override public void restore() { dirty = true; t.restore(); }
        @Override public void translate(float x, float y) { dirty = true; t.translate(x, y); } @Override public void rotate(float r) { dirty = true; t.rotate(r); } @Override public void scale(float x, float y) { dirty = true; t.scale(x, y); }
        @Override public void clear(int c) { t.clear(c); }
        @Override public void glow(float x, float y, float r, int c) { t.glow(x, y, r, c); } @Override public void additive(boolean on) { t.additive(on); }
        @Override public void beginPixels(int w, int h) { t.beginPixels(w, h); } @Override public void endPixels(float x, float y, int s) { t.endPixels(x, y, s); }
        @Override public void poly(float[] p, int f, int s, float sw) { if (!hit(false, p)) t.poly(p, f, s, sw); }
        @Override public void grad(float[] p, int c0, int c1, float x0, float x1, int s, float sw) { if (!hit(true, p)) t.grad(p, c0, c1, x0, x1, s, sw); }
        @Override public void circle(float x, float y, float r, int f, int s, float sw) { if (!hit(false, new float[]{x - r, y - r, x + r, y + r})) t.circle(x, y, r, f, s, sw); }
        @Override public void line(float a, float b, float c, float d, int k, float w) { if (!hit(false, new float[]{a, b, c, d})) t.line(a, b, c, d, k, w); }
        @Override public void rect(float x, float y, float w, float h, int c) { if (!hit(false, new float[]{x, y, x + w, y + h})) t.rect(x, y, w, h, c); }
        @Override public void rrect(float x, float y, float w, float h, float r, int f, int s, float sw) { if (!hit(false, new float[]{x, y, x + w, y + h})) t.rrect(x, y, w, h, r, f, s, sw); }
        @Override public void shape(float[][] p, int f, boolean eo, int s, float sw) { t.shape(p, f, eo, s, sw); }
        @Override public void text(String s, float x, float y, float z, int c, int a) { t.text(s, x, y, z, c, a); } @Override public float textW(String s, float z) { return t.textW(s, z); }
        @Override public void tile(int id, int[] px, int size, boolean ch, float x, float y) { t.tile(id, px, size, ch, x, y); }
    }
}
