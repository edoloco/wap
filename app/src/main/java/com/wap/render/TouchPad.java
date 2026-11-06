package com.wap.render;
import com.wap.core.Input;
import java.util.HashMap;
/** Mando en pantalla en pixel art: cruceta a la izquierda; A, B y X a la derecha. Coordenadas de la capa de interfaz. */
public final class TouchPad {
    final HashMap<Integer, float[]> ptrs = new HashMap<>();
    public boolean visible = true; float w = 640, h = 360, u = 1;
    float dx, dy, dr, br; final float[][] btn = new float[3][2]; static final int[] BIT = {Input.A, Input.B, Input.X};
    static final String[] NAME = {"A", "B", "X"}; static final int[] TONE = {0xFFE8323C, 0xFFFFC83D, 0xFF5FA8FF}, DARK = {0xFF8E1520, 0xFFB8862A, 0xFF2E4BA8};
    void layout(float W, float H) {
        w = W; h = H; u = H / 360f; dr = 74 * u; dx = 92 * u; dy = H - 92 * u; br = 34 * u;
        btn[0][0] = W - 54 * u; btn[0][1] = H - 116 * u;   // A
        btn[1][0] = W - 130 * u; btn[1][1] = H - 62 * u;   // B
        btn[2][0] = W - 130 * u; btn[2][1] = H - 170 * u;  // X
    }
    public void pointer(int id, float x, float y, boolean down) { if (down) ptrs.put(id, new float[]{x, y}); else ptrs.remove(id); }
    public void clear() { ptrs.clear(); }
    public int mask() {
        int m = 0;
        for (float[] p : ptrs.values()) {
            if (p[0] < w * .5f) {
                float a = p[0] - dx, b = p[1] - dy, d = (float) Math.hypot(a, b);
                if (d > 12 * u && d < dr * 2.1f) {
                    double t = Math.atan2(b, a), s = Math.PI / 8;
                    if (Math.abs(t) < 3 * s) m |= Input.RIGHT;
                    if (Math.abs(t) > 5 * s) m |= Input.LEFT;
                    if (t < -s && t > -7 * s) m |= Input.UP;
                    if (t > s && t < 7 * s) m |= Input.DOWN;
                }
            } else {
                int best = -1; float bd = br * 1.45f;
                for (int i = 0; i < 3; i++) { float d = (float) Math.hypot(p[0] - btn[i][0], p[1] - btn[i][1]); if (d < bd) { bd = d; best = i; } }
                if (best >= 0) m |= BIT[best];
            }
        }
        return m;
    }
    public void draw(Gfx g, int m) {
        if (!visible) return;
        final int F = 0x8C1B1F4A, ON = 0xEEE8323C, L = 0xFF101330, ARROW = 0xFFFFF4D6;
        int a = Math.round(24 * u), b = Math.round(72 * u), cx = Math.round(dx), cy = Math.round(dy);
        int[][] arm = {{Input.UP, 0, -1}, {Input.RIGHT, 1, 0}, {Input.DOWN, 0, 1}, {Input.LEFT, -1, 0}};
        g.rect(cx - a, cy - b, 2 * a, 2 * b, L); g.rect(cx - b, cy - a, 2 * b, 2 * a, L);
        for (int[] q : arm) {
            int x0 = q[1] == 0 ? cx - a + 1 : q[1] > 0 ? cx + a : cx - b + 1, y0 = q[2] == 0 ? cy - a + 1 : q[2] > 0 ? cy + a : cy - b + 1;
            int ww = q[1] == 0 ? 2 * a - 2 : b - a - 1, hh = q[2] == 0 ? 2 * a - 2 : b - a - 1;
            boolean on = (m & q[0]) != 0; g.rect(x0, y0, ww, hh, on ? ON : F);
            if (!on) g.rect(x0, y0, ww, 1, 0x55FFFFFF);
            int tx = cx + q[1] * (a + b) / 2, ty = cy + q[2] * (a + b) / 2, s = Math.max(2, Math.round(7 * u));
            g.poly(new float[]{tx + q[1] * s, ty + q[2] * s, tx - q[2] * s - q[1] * s / 2f, ty + q[1] * s - q[2] * s / 2f, tx + q[2] * s - q[1] * s / 2f, ty - q[1] * s - q[2] * s / 2f}, ARROW, 0, 0);
        }
        g.rect(cx - a + 1, cy - a + 1, 2 * a - 2, 2 * a - 2, F);
        for (int i = 0; i < 3; i++) {
            boolean on = (m & BIT[i]) != 0; int x = Math.round(btn[i][0]), y = Math.round(btn[i][1]), r = Math.round(br);
            g.circle(x, y + (on ? 0 : 2), r, L, 0, 0);
            g.circle(x, y + (on ? 2 : 0), r - 1, on ? TONE[i] : (TONE[i] & 0xFFFFFF) | 0xAA000000, 0, 0);
            if (!on) g.circle(x - r / 3f, y - r / 3f, Math.max(1, r / 5f), 0x88FFFFFF, 0, 0);
            PixelFont.draw(g, NAME[i], x, y + (on ? 2 : 0) + 7 * PixelFont.scale(26 * u) / 2f, 26 * u, 0xFFFFF4D6, 0, DARK[i]);
        }
    }
}
