package com.wap.android;
import android.graphics.Bitmap; import android.graphics.Canvas; import android.graphics.LinearGradient; import android.graphics.Shader; import android.graphics.Paint; import android.graphics.Path; import android.graphics.RectF; import android.graphics.Typeface;
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
    void begin(Canvas k, int w, int h) { c = k; W = w; H = h; }
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
        if (s != 0) { stroke.setColor(s); stroke.setStrokeWidth(sw); c.drawPath(path, stroke); }
    }
    @Override public void grad(float[] p, int c0, int c1, float x0, float x1, int s, float sw) {
        path.rewind(); path.moveTo(p[0], p[1]);
        for (int i = 2; i < p.length; i += 2) path.lineTo(p[i], p[i + 1]);
        path.close();
        fill.setColor(0xFFFFFFFF); fill.setShader(new LinearGradient(x0, 0, x1, 0, c0, c1, Shader.TileMode.CLAMP)); c.drawPath(path, fill); fill.setShader(null);
        if (s != 0) { stroke.setColor(s); stroke.setStrokeWidth(sw); c.drawPath(path, stroke); }
    }
    @Override public void circle(float x, float y, float r, int f, int s, float sw) {
        if (f != 0) { fill.setColor(f); c.drawCircle(x, y, r, fill); }
        if (s != 0) { stroke.setColor(s); stroke.setStrokeWidth(sw); c.drawCircle(x, y, r, stroke); }
    }
    @Override public void line(float a, float b, float d, float e, int k, float w) { stroke.setColor(k); stroke.setStrokeWidth(w); c.drawLine(a, b, d, e, stroke); }
    @Override public void rect(float x, float y, float w, float h, int k) { fill.setColor(k); c.drawRect(x, y, x + w, y + h, fill); }
    @Override public void rrect(float x, float y, float w, float h, float r, int f, int s, float sw) {
        rf.set(x, y, x + w, y + h);
        if (f != 0) { fill.setColor(f); c.drawRoundRect(rf, r, r, fill); }
        if (s != 0) { stroke.setColor(s); stroke.setStrokeWidth(sw); c.drawRoundRect(rf, r, r, stroke); }
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
        c.drawBitmap(b, x, y, null);
    }
}
