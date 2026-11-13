package com.wap.render;
/**
 * Pincel que cambia el color de todo lo que dibuja un disparo (cuerpo, luz y estela) al color elegido en armas.txt:
 * usa el tono y la saturación del color nuevo y conserva cuán claro u oscuro era cada parte. Los blancos, grises y
 * negros no cambian (contornos, brillos). La misma regla está en el Taller de armas para la vista previa.
 */
public final class Recolor implements Gfx {
    Gfx t; int tint;
    public Recolor with(Gfx target, int c) { t = target; tint = c; return this; }
    public static int of(int c, int tint) {
        int a = c >>> 24, r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255, mx = Math.max(r, Math.max(g, b)), mn = Math.min(r, Math.min(g, b));
        if (mx == 0 || (mx - mn) / (float) mx < .25f) return c;
        int tr = tint >> 16 & 255, tg = tint >> 8 & 255, tb = tint & 255, tv = Math.max(tr, Math.max(tg, tb));
        if (tv == 0) return a << 24;
        float k = mx / (float) tv;
        return a << 24 | Math.min(255, Math.round(tr * k)) << 16 | Math.min(255, Math.round(tg * k)) << 8 | Math.min(255, Math.round(tb * k));
    }
    private int c(int x) { return of(x, tint); }
    @Override public void layer(String name) { t.layer(name); } @Override public void endLayer() { t.endLayer(); }
    @Override public int w() { return t.w(); } @Override public int h() { return t.h(); }
    @Override public void save() { t.save(); } @Override public void restore() { t.restore(); }
    @Override public void translate(float x, float y) { t.translate(x, y); } @Override public void rotate(float r) { t.rotate(r); } @Override public void scale(float x, float y) { t.scale(x, y); }
    @Override public void clear(int k) { t.clear(k); }
    @Override public void glow(float x, float y, float r, int k) { t.glow(x, y, r, c(k)); } @Override public void additive(boolean on) { t.additive(on); }
    @Override public void beginPixels(int w, int h) { t.beginPixels(w, h); } @Override public void endPixels(float x, float y, int s) { t.endPixels(x, y, s); }
    @Override public void poly(float[] p, int f, int s, float sw) { t.poly(p, c(f), c(s), sw); }
    @Override public void grad(float[] p, int c0, int c1, float x0, float x1, int s, float sw) { t.grad(p, c(c0), c(c1), x0, x1, c(s), sw); }
    @Override public void circle(float x, float y, float r, int f, int s, float sw) { t.circle(x, y, r, c(f), c(s), sw); }
    @Override public void line(float a, float b, float x, float y, int k, float w) { t.line(a, b, x, y, c(k), w); }
    @Override public void rect(float x, float y, float w, float h, int k) { t.rect(x, y, w, h, c(k)); }
    @Override public void rrect(float x, float y, float w, float h, float r, int f, int s, float sw) { t.rrect(x, y, w, h, r, c(f), c(s), sw); }
    @Override public void shape(float[][] p, int f, boolean eo, int s, float sw) { t.shape(p, c(f), eo, c(s), sw); }
    @Override public void text(String s, float x, float y, float z, int k, int a) { t.text(s, x, y, z, c(k), a); } @Override public float textW(String s, float z) { return t.textW(s, z); }
    @Override public void tile(int id, int[] px, int size, boolean ch, float x, float y) { t.tile(id, px, size, ch, x, y); }
}
