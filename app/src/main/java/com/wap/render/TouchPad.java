package com.wap.render;
import com.wap.core.Input;
import java.util.HashMap;
/** Mando en pantalla: cruceta a la izquierda; A y B a la derecha. */
public final class TouchPad {
    final HashMap<Integer, float[]> ptrs = new HashMap<>();
    public boolean visible = true; float w = 640, h = 360, u = 1;
    float dx, dy, dr, ax, ay, bx, by, br;
    void layout(float W, float H) { w = W; h = H; u = H / 360f; dr = 74 * u; dx = 92 * u; dy = H - 92 * u; br = 38 * u; ax = W - 58 * u; ay = H - 112 * u; bx = W - 138 * u; by = H - 68 * u; }
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
                float a = (float) Math.hypot(p[0] - ax, p[1] - ay), b = (float) Math.hypot(p[0] - bx, p[1] - by);
                if (a < br * 1.5f && a <= b) m |= Input.A; else if (b < br * 1.5f) m |= Input.B;
            }
        }
        return m;
    }
    public void draw(Gfx g, int m) {
        if (!visible) return;
        final int F = 0x30141414, ON = 0xCCFF0000, L = 0xAA141414;
        float a = 25 * u, b = 74 * u;
        int[][] arm = {{Input.UP, 0}, {Input.RIGHT, 1}, {Input.DOWN, 2}, {Input.LEFT, 3}};
        for (int[] q : arm) {
            float[] dir = {0, -1, 1, 0, 0, 1, -1, 0}; float ox = dir[q[1] * 2], oy = dir[q[1] * 2 + 1];
            float c = dx + ox * (a + b) / 2, d = dy + oy * (a + b) / 2, hw = ox != 0 ? (b - a) / 2 + a * .05f : a, hh = oy != 0 ? (b - a) / 2 + a * .05f : a;
            g.rrect(c - hw, d - hh, hw * 2, hh * 2, 6 * u, (m & q[0]) != 0 ? ON : F, L, 1.5f * u);
            float tx = dx + ox * b * .68f, ty = dy + oy * b * .68f, s = 8 * u;
            g.poly(new float[]{tx + ox * s, ty + oy * s, tx - oy * s - ox * s * .4f, ty + ox * s - oy * s * .4f, tx + oy * s - ox * s * .4f, ty - ox * s - oy * s * .4f}, L, 0, 0);
        }
        g.rrect(dx - a, dy - a, a * 2, a * 2, 2 * u, F, 0, 0);
        btn(g, ax, ay, "A", (m & Input.A) != 0, ON, F, L); btn(g, bx, by, "B", (m & Input.B) != 0, ON, F, L);
    }
    void btn(Gfx g, float x, float y, String s, boolean on, int ONc, int Fc, int L) {
        g.circle(x, y, br, on ? ONc : Fc, L, 1.5f * u); g.text(s, x, y + 9 * u, 26 * u, L, 0);
    }
}
