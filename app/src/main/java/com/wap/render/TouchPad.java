package com.wap.render;
import com.wap.core.Input;
import java.util.HashMap;
/**
 * Mando en pantalla en pixel art: cruceta (o joystick) y los botones A, B y X. Coordenadas de la capa de interfaz.
 * Se puede editar (PERSONALIZAR > EDITAR MANDO): mover cada control, cambiarle el tamaño y elegir cruceta o joystick.
 * La configuración se guarda como texto (save/load) con posiciones relativas al ancho y alto de la pantalla.
 */
public final class TouchPad {
    final HashMap<Integer, float[]> ptrs = new HashMap<>();
    public boolean visible = true; float w = 640, h = 360, u = 1;
    /** 0 cruceta, 1 joystick análogo (el dedo mueve una perilla; se traduce a las 8 direcciones). */
    public int kind;
    /** Controles: 0 cruceta/joystick, 1 A, 2 B, 3 X. Posición relativa (0..1) y tamaño (1 = normal). null = posición de siempre. */
    float[] rx, ry; public final float[] size = {1, 1, 1, 1};
    float dx, dy, dr; final float[][] btn = new float[3][2]; final float[] br = new float[3];
    static final int[] BIT = {Input.A, Input.B, Input.X};
    static final String[] NAME = {"A", "B", "X"}; static final int[] TONE = {0xFFE8323C, 0xFFFFC83D, 0xFF5FA8FF}, DARK = {0xFF8E1520, 0xFFB8862A, 0xFF2E4BA8};
    static final float MIN_SIZE = .6f, MAX_SIZE = 1.8f;
    public void layout(float W, float H) {
        w = W; h = H; u = H / 360f;
        float[][] def = {{92 * u, H - 92 * u}, {W - 54 * u, H - 116 * u}, {W - 130 * u, H - 62 * u}, {W - 130 * u, H - 170 * u}}; // cruceta, A, B, X
        float[][] p = new float[4][2];
        for (int i = 0; i < 4; i++) { p[i][0] = rx == null ? def[i][0] : rx[i] * W; p[i][1] = rx == null ? def[i][1] : ry[i] * H; }
        dr = 74 * u * size[0]; for (int i = 0; i < 3; i++) br[i] = 34 * u * size[i + 1];
        for (int i = 0; i < 4; i++) { float r = i == 0 ? dr : br[i - 1]; p[i][0] = Math.max(r, Math.min(W - r, p[i][0])); p[i][1] = Math.max(r, Math.min(H - r, p[i][1])); } // que no se salga de la pantalla
        dx = p[0][0]; dy = p[0][1];
        for (int i = 0; i < 3; i++) { btn[i][0] = p[i + 1][0]; btn[i][1] = p[i + 1][1]; }
    }
    /** Posición actual de un control (centro). */
    public float cx(int i) { return i == 0 ? dx : btn[i - 1][0]; } public float cy(int i) { return i == 0 ? dy : btn[i - 1][1]; }
    public float radius(int i) { return i == 0 ? (kind == 1 ? dr * .9f : dr) : br[i - 1]; }
    /** Mueve un control (coordenadas de la interfaz) sin que se salga de la pantalla. */
    public void moveTo(int i, float x, float y) {
        if (rx == null) { rx = new float[4]; ry = new float[4]; for (int k = 0; k < 4; k++) { rx[k] = cx(k) / w; ry[k] = cy(k) / h; } }
        float r = radius(i) * .6f;
        rx[i] = Math.max(r, Math.min(w - r, x)) / w; ry[i] = Math.max(r + 30 * u, Math.min(h - r, y)) / h; layout(w, h);
    }
    public void resize(int i, float k) { size[i] = Math.max(MIN_SIZE, Math.min(MAX_SIZE, Math.round(k * 10) / 10f)); layout(w, h); }
    public void reset() { rx = ry = null; java.util.Arrays.fill(size, 1); kind = 0; layout(w, h); }
    /** "tipo;x,y,tamaño;x,y,tamaño;..." (posiciones relativas). */
    public String save() {
        StringBuilder b = new StringBuilder().append(kind);
        for (int i = 0; i < 4; i++) b.append(';').append(rx == null ? -1 : Math.round(rx[i] * 1000)).append(',').append(rx == null ? -1 : Math.round(ry[i] * 1000)).append(',').append(Math.round(size[i] * 10));
        return b.toString();
    }
    public void load(String s) {
        try {
            String[] p = s.split(";"); if (p.length != 5) return;
            kind = Integer.parseInt(p[0]) == 1 ? 1 : 0; float[] x = new float[4], y = new float[4]; boolean custom = true;
            for (int i = 0; i < 4; i++) { String[] q = p[i + 1].split(","); int a = Integer.parseInt(q[0]), c = Integer.parseInt(q[1]); if (a < 0) custom = false; x[i] = a / 1000f; y[i] = c / 1000f; size[i] = Math.max(MIN_SIZE, Math.min(MAX_SIZE, Integer.parseInt(q[2]) / 10f)); }
            if (custom) { rx = x; ry = y; } else rx = ry = null;
        } catch (RuntimeException e) { reset(); }
    }
    public void pointer(int id, float x, float y, boolean down) { if (down) ptrs.put(id, new float[]{x, y}); else ptrs.remove(id); }
    public void clear() { ptrs.clear(); }
    /** El dedo que maneja la cruceta o el joystick (para dibujar la perilla). */
    float[] stick() {
        for (float[] p : ptrs.values()) if (onPad(p)) return p;
        return null;
    }
    /** Si un dedo toca la zona de la cruceta: dentro de su radio ampliado y más cerca de ella que de cualquier botón. */
    boolean onPad(float[] p) {
        float d = (float) Math.hypot(p[0] - dx, p[1] - dy); if (d > dr * 2.1f) return false;
        for (int i = 0; i < 3; i++) if (Math.hypot(p[0] - btn[i][0], p[1] - btn[i][1]) < br[i] * 1.2f) return false;
        return true;
    }
    public int mask() {
        int m = 0;
        for (float[] p : ptrs.values()) {
            if (onPad(p)) {
                float a = p[0] - dx, b = p[1] - dy, d = (float) Math.hypot(a, b);
                if (kind == 1) { // joystick: dirección por cuánto se inclina en cada eje
                    float r = dr * .55f, sx = a / Math.max(d, r), sy = b / Math.max(d, r);
                    if (d > dr * .18f) { if (sx > .38f) m |= Input.RIGHT; if (sx < -.38f) m |= Input.LEFT; if (sy < -.38f) m |= Input.UP; if (sy > .38f) m |= Input.DOWN; }
                } else if (d > 12 * u) {
                    double t = Math.atan2(b, a), s = Math.PI / 8;
                    if (Math.abs(t) < 3 * s) m |= Input.RIGHT;
                    if (Math.abs(t) > 5 * s) m |= Input.LEFT;
                    if (t < -s && t > -7 * s) m |= Input.UP;
                    if (t > s && t < 7 * s) m |= Input.DOWN;
                }
            } else {
                int best = -1; float bd = Float.MAX_VALUE;
                for (int i = 0; i < 3; i++) { float d = (float) Math.hypot(p[0] - btn[i][0], p[1] - btn[i][1]); if (d < br[i] * 1.45f && d / br[i] < bd) { bd = d / br[i]; best = i; } }
                if (best >= 0) m |= BIT[best];
            }
        }
        return m;
    }
    public void draw(Gfx g, int m) { draw(g, m, -1); }
    /** sel: control marcado (en el editor del mando), -1 ninguno. */
    void draw(Gfx g, int m, int sel) {
        if (!visible) return;
        final int F = 0x8C1B1F4A, ON = 0xEEE8323C, L = 0xFF101330, ARROW = 0xFFFFF4D6;
        int cx = Math.round(dx), cy = Math.round(dy);
        if (kind == 1) { // joystick: base redonda y perilla que sigue al dedo
            int R = Math.round(dr * .9f); g.circle(cx, cy, R, F, L, Math.max(1, Math.round(2 * u)));
            for (int k = 0; k < 4; k++) { double t = k * Math.PI / 2; float ax = cx + (float) Math.cos(t) * R * .78f, ay = cy + (float) Math.sin(t) * R * .78f, s = Math.max(2, 5 * u * size[0]);
                g.poly(new float[]{ax + (float) Math.cos(t) * s, ay + (float) Math.sin(t) * s, ax - (float) Math.sin(t) * s, ay + (float) Math.cos(t) * s, ax + (float) Math.sin(t) * s, ay - (float) Math.cos(t) * s}, 0x88FFF4D6, 0, 0); }
            float[] p = sel >= 0 ? null : stick(); float kx = cx, ky = cy;
            if (p != null) { float a = p[0] - dx, b = p[1] - dy, d = (float) Math.hypot(a, b), lim = R * .55f; if (d > lim) { a = a / d * lim; b = b / d * lim; } kx += a; ky += b; }
            int kr = Math.round(R * .45f); boolean on = (m & (Input.UP | Input.DOWN | Input.LEFT | Input.RIGHT)) != 0;
            g.circle(kx, ky + 2, kr, L, 0, 0); g.circle(kx, ky, kr, on ? ON : 0xCC3A4370, L, Math.max(1, Math.round(u))); g.circle(kx - kr / 3f, ky - kr / 3f, Math.max(1, kr / 5f), 0x88FFFFFF, 0, 0);
        } else {
            int a = Math.round(24 * u * size[0]), b = Math.round(72 * u * size[0]);
            int[][] arm = {{Input.UP, 0, -1}, {Input.RIGHT, 1, 0}, {Input.DOWN, 0, 1}, {Input.LEFT, -1, 0}};
            g.rect(cx - a, cy - b, 2 * a, 2 * b, L); g.rect(cx - b, cy - a, 2 * b, 2 * a, L);
            for (int[] q : arm) {
                int x0 = q[1] == 0 ? cx - a + 1 : q[1] > 0 ? cx + a : cx - b + 1, y0 = q[2] == 0 ? cy - a + 1 : q[2] > 0 ? cy + a : cy - b + 1;
                int ww = q[1] == 0 ? 2 * a - 2 : b - a - 1, hh = q[2] == 0 ? 2 * a - 2 : b - a - 1;
                boolean on = (m & q[0]) != 0; g.rect(x0, y0, ww, hh, on ? ON : F);
                if (!on) g.rect(x0, y0, ww, 1, 0x55FFFFFF);
                int tx = cx + q[1] * (a + b) / 2, ty = cy + q[2] * (a + b) / 2, s = Math.max(2, Math.round(7 * u * size[0]));
                g.poly(new float[]{tx + q[1] * s, ty + q[2] * s, tx - q[2] * s - q[1] * s / 2f, ty + q[1] * s - q[2] * s / 2f, tx + q[2] * s - q[1] * s / 2f, ty - q[1] * s - q[2] * s / 2f}, ARROW, 0, 0);
            }
            g.rect(cx - a + 1, cy - a + 1, 2 * a - 2, 2 * a - 2, F);
        }
        for (int i = 0; i < 3; i++) {
            boolean on = (m & BIT[i]) != 0; int x = Math.round(btn[i][0]), y = Math.round(btn[i][1]), r = Math.round(br[i]);
            g.circle(x, y + (on ? 0 : 2), r, L, 0, 0);
            g.circle(x, y + (on ? 2 : 0), r - 1, on ? TONE[i] : (TONE[i] & 0xFFFFFF) | 0xAA000000, 0, 0);
            if (!on) g.circle(x - r / 3f, y - r / 3f, Math.max(1, r / 5f), 0x88FFFFFF, 0, 0);
            float fs = 26 * u * size[i + 1];
            PixelFont.draw(g, NAME[i], x, y + (on ? 2 : 0) + 7 * PixelFont.scale(fs) / 2f, fs, 0xFFFFF4D6, 0, DARK[i]);
        }
        if (sel >= 0) { // marco del control elegido en el editor
            float r = radius(sel) + 4 * u; int x0 = Math.round(cx(sel) - r), y0 = Math.round(cy(sel) - r), s = Math.round(2 * r), t = Math.max(1, Math.round(2 * u));
            g.rect(x0, y0, s, t, 0xFFFFC83D); g.rect(x0, y0 + s - t, s, t, 0xFFFFC83D); g.rect(x0, y0, t, s, 0xFFFFC83D); g.rect(x0 + s - t, y0, t, s, 0xFFFFC83D);
        }
    }
    // ---------- Editor del mando: arrastrar mueve el control tocado
    int editSel = 0, drag = -1, dragId; float offX, offY;
    /** Devuelve true si el toque fue sobre un control (lo toma para moverlo). */
    boolean editPointer(int id, float x, float y, boolean down) {
        if (down) {
            if (drag < 0) {
                int best = -1; float bd = Float.MAX_VALUE;
                for (int i = 0; i < 4; i++) { float d = (float) Math.hypot(x - cx(i), y - cy(i)); if (d < radius(i) * 1.15f && d / radius(i) < bd) { bd = d / radius(i); best = i; } }
                if (best < 0) return false;
                drag = best; dragId = id; editSel = best; offX = cx(best) - x; offY = cy(best) - y; return true;
            }
            if (id == dragId) { moveTo(drag, x + offX, y + offY); return true; }
            return false;
        }
        if (id == dragId) { drag = -1; return true; }
        return false;
    }
}
