package com.wap.android;
import android.graphics.Bitmap; import android.graphics.Canvas; import android.graphics.LinearGradient; import android.graphics.Shader; import android.graphics.RadialGradient; import android.graphics.PorterDuff; import android.graphics.PorterDuffXfermode; import android.graphics.Paint; import android.graphics.Path; import android.graphics.RectF; import android.graphics.Typeface;
import com.wap.render.Gfx;
import java.util.HashMap;
/** Implementación de Gfx sobre el Canvas de Android. */
final class AndroidGfx implements Gfx {
    Canvas c; int W, H;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), stroke = new Paint(Paint.ANTI_ALIAS_FLAG), text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path(); private final RectF rf = new RectF(); private final HashMap<Integer, Bitmap> tiles = new HashMap<>();
    AndroidGfx() {
        fill.setStyle(Paint.Style.FILL); stroke.setStyle(Paint.Style.STROKE); stroke.setStrokeCap(Paint.Cap.ROUND); stroke.setStrokeJoin(Paint.Join.ROUND);
        text.setTypeface(Typeface.DEFAULT_BOLD);
    }
    private final java.util.HashMap<Long, Bitmap> lows = new java.util.HashMap<>(); private final java.util.HashMap<Long, Canvas> lowCs = new java.util.HashMap<>();
    private final java.util.ArrayDeque<Canvas> stack = new java.util.ArrayDeque<>(); private final java.util.ArrayDeque<Bitmap> open = new java.util.ArrayDeque<>();
    private boolean pix; private final Paint blit = new Paint();
    void begin(Canvas k, int w, int h) { c = k; W = w; H = h; }
    private float sw(float w) { return pix && w < 1.5f ? 0 : w; } // en píxeles, las líneas finas miden exactamente 1
    /** Capas de pixel art apilables: la interfaz y el personaje del menú usan cada una la suya. */
    @Override public void beginPixels(int w, int h) {
        w = Math.max(1, w); h = Math.max(1, h); long key = (long) w << 20 | h;
        Bitmap b = lows.get(key); if (b == null) { b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); lows.put(key, b); lowCs.put(key, new Canvas(b)); }
        b.eraseColor(0); stack.push(c); open.push(b); c = lowCs.get(key); pix = true; fill.setAntiAlias(false); stroke.setAntiAlias(false);
    }
    @Override public void endPixels(float x, float y, int s) {
        Bitmap b = open.pop(); c = stack.pop(); pix = !open.isEmpty(); fill.setAntiAlias(!pix); stroke.setAntiAlias(!pix); blit.setFilterBitmap(false);
        c.save(); c.translate(x, y); c.scale(s, s); c.drawBitmap(b, 0, 0, blit); c.restore();
    }
    private final HashMap<Integer, Paint> glows = new HashMap<>(); private boolean add; private final PorterDuffXfermode ADD = new PorterDuffXfermode(PorterDuff.Mode.ADD);
    @Override public void additive(boolean on) { add = on; PorterDuffXfermode x = on ? ADD : null; fill.setXfermode(x); stroke.setXfermode(x); }
    @Override public void glow(float x, float y, float r, int col) {
        int a = col >>> 24; if (r <= .2f || a == 0) return;
        int rgb = col | 0xFF000000; Paint p = glows.get(rgb);
        if (p == null) { // un degradado por color, reutilizado y escalado al radio
            p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setShader(new RadialGradient(0, 0, 1, new int[]{rgb, rgb & 0xFFFFFF | 0x66000000, rgb & 0xFFFFFF}, new float[]{0, .35f, 1}, Shader.TileMode.CLAMP));
            glows.put(rgb, p);
        }
        p.setAlpha(a); p.setXfermode(add ? ADD : null);
        c.save(); c.translate(x, y); c.scale(r, r); c.drawCircle(0, 0, 1, p); c.restore();
    }
    @Override public int w() { return W; }
    @Override public int h() { return H; }
    @Override public void save() { c.save(); }
    @Override public void restore() { c.restore(); }
    @Override public void translate(float x, float y) { c.translate(x, y); }
    @Override public void rotate(float r) { c.rotate((float) Math.toDegrees(r)); }
    @Override public void scale(float x, float y) { c.scale(x, y); }
    @Override public void clear(int k) { c.drawColor(k); }
    @Override public void poly(float[] p, int f, int s, float sw) {
        path.rewind(); path.moveTo(p[0], p[1]);
        for (int i = 2; i < p.length; i += 2) path.lineTo(p[i], p[i + 1]);
        path.close();
        if (f != 0) { fill.setColor(f); c.drawPath(path, fill); }
        if (s != 0) { stroke.setColor(s); stroke.setStrokeWidth(sw(sw)); c.drawPath(path, stroke); }
    }
    /** Arte importado: el Path de cada forma se arma una vez y se reutiliza. */
    private final java.util.IdentityHashMap<float[][], Path> shapes = new java.util.IdentityHashMap<>();
    @Override public void shape(float[][] ps, int f, boolean eo, int s, float sw) {
        Path q = shapes.get(ps);
        if (q == null) {
            q = new Path(); q.setFillType(eo ? Path.FillType.EVEN_ODD : Path.FillType.WINDING);
            for (float[] p : ps) { q.moveTo(p[0], p[1]); for (int i = 2; i < p.length; i += 2) q.lineTo(p[i], p[i + 1]); q.close(); }
            shapes.put(ps, q);
        }
        if (f != 0) { fill.setColor(f); c.drawPath(q, fill); }
        if (s != 0) { stroke.setColor(s); stroke.setStrokeWidth(sw(sw)); c.drawPath(q, stroke); }
    }
    @Override public void grad(float[] p, int c0, int c1, float x0, float x1, int s, float sw) {
        path.rewind(); path.moveTo(p[0], p[1]);
        for (int i = 2; i < p.length; i += 2) path.lineTo(p[i], p[i + 1]);
        path.close();
        fill.setColor(0xFFFFFFFF); fill.setShader(new LinearGradient(x0, 0, x1, 0, c0, c1, Shader.TileMode.CLAMP)); c.drawPath(path, fill); fill.setShader(null);
        if (s != 0) { stroke.setColor(s); stroke.setStrokeWidth(sw(sw)); c.drawPath(path, stroke); }
    }
    @Override public void circle(float x, float y, float r, int f, int s, float sw) {
        if (f != 0) { fill.setColor(f); c.drawCircle(x, y, r, fill); }
        if (s != 0) { stroke.setColor(s); stroke.setStrokeWidth(sw(sw)); c.drawCircle(x, y, r, stroke); }
    }
    @Override public void line(float a, float b, float d, float e, int k, float w) { stroke.setColor(k); stroke.setStrokeWidth(sw(w)); c.drawLine(a, b, d, e, stroke); }
    @Override public void rect(float x, float y, float w, float h, int k) { fill.setColor(k); c.drawRect(x, y, x + w, y + h, fill); }
    @Override public void rrect(float x, float y, float w, float h, float r, int f, int s, float sw) {
        rf.set(x, y, x + w, y + h);
        if (f != 0) { fill.setColor(f); c.drawRoundRect(rf, r, r, fill); }
        if (s != 0) { stroke.setColor(s); stroke.setStrokeWidth(sw(sw)); c.drawRoundRect(rf, r, r, stroke); }
    }
    @Override public void text(String s, float x, float y, float size, int k, int align) {
        text.setTextSize(size); text.setColor(k);
        text.setTextAlign(align == 0 ? Paint.Align.CENTER : align > 0 ? Paint.Align.RIGHT : Paint.Align.LEFT);
        c.drawText(s, x, y, text);
    }
    @Override public float textW(String s, float size) { text.setTextSize(size); return text.measureText(s); }
    @Override public void tile(int id, int[] px, int size, boolean changed, float x, float y) {
        Bitmap b = tiles.get(id);
        if (b == null) { b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888); tiles.put(id, b); changed = true; }
        if (changed) b.setPixels(px, 0, size, 0, 0, size, size);
        tilePaint.setFilterBitmap(!pix); c.drawBitmap(b, x, y, tilePaint); // fuera del pixel art, el mapa se amplía suavizado
    }
    private final Paint tilePaint = new Paint(), imgPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    @Override public void image(com.wap.render.Sprites.Img im, float x, float y, float w, float h) {
        Bitmap b = (Bitmap) im.cache;
        if (b == null) { b = Bitmap.createBitmap(im.px, im.w, im.h, Bitmap.Config.ARGB_8888); im.cache = b; }
        imgPaint.setFilterBitmap(!pix); rf.set(x, y, x + w, y + h); c.drawBitmap(b, null, rf, imgPaint);
    }
}
