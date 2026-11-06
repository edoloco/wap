package com.wap.render;
import com.wap.core.*;
/** El personaje de la referencia: esqueleto negro con placas de armadura, animado con cinemática inversa en las piernas. */
public final class RobotArt {
    public static final int INK = 0xFF141414, WHITE = 0xFFFFFFFF;
    public static final int[] COLOR = {0xFFFF0000, 0xFF1266FF};
    static final float TH = 13.5f, SH = 13.5f, UA = 9, FA = 8.5f, ANK = 3.4f, SW = .34f;
    static final float[] HEAD = {-2.4f,0, 2.6f,0, 5.4f,-4.2f, 5.7f,-8.4f, 4.2f,-10.6f, 1,-11.4f, -3.4f,-11, -5,-9.5f, -5.2f,-5.2f};
    static final float[] CHEST = {-5.4f,-.4f, 6.3f,.3f, 5.8f,11, -6,10.5f, -5.7f,4.5f};
    static final float[] BELLY = {-3.6f,10.6f, 4,10.6f, 4.2f,16.6f, -2.6f,16.8f};
    static final float[] HIPS = {-5.6f,1, -3,-1.6f, 3.4f,-1.2f, 6,.8f, 3.6f,6, .6f,6.8f, -1.8f,3.4f, -5.6f,3.6f};
    static final float[] PAD = {-3.6f,-1.6f, 0,-3.1f, 3.2f,-1.6f, 3.8f,2, -.8f,3.6f, -4.2f,1.4f};
    static final float[] BRACER = {-.8f,-2.8f, 9.4f,-4.6f, 9.4f,4.4f, -.2f,3.2f};
    static final float[] THIGH = {-1,-3.2f, 1.8f,-4.2f, 8,-3.2f, 12.6f,-2.2f, 12.2f,2.4f, 7,3.8f, 1,4, -1,2.6f};
    static final float[] SHIN = {.8f,-2.4f, 3,-3.4f, 8,-2.9f, 12.4f,-2.7f, 12.2f,2.6f, 6,3.2f, 1.6f,3};
    static final float[] KNEE = {-2,-3, 0,-1.6f, 1.8f,-3.4f, 2.2f,.2f, 3,2.6f, .5f,1.6f, -1.8f,3};
    static final float[] FOOT = {-2.7f,-1.4f, 1.2f,-1.8f, 6.6f,1.5f, 7.6f,3.4f, -2.7f,3.4f};
    static final float[] TORSO = {-3.6f,-.3f, 3.6f,-.3f, 2.5f,9, -2.5f,9};
    static final float[] PELVIS = {-5,13.6f, 5,14, 3.8f,18, 1,21, -1,18, -5,17};
    static final float[] HAND = {0,-2.3f, 3.6f,-3, 5.2f,-1.3f, 5,.4f, 3,.1f, 2.8f,1.4f, 4.6f,1.8f, 4,3, .8f,3.2f, -.6f,1.4f};

    /** Estado de animación solo visual de un robot. */
    public static final class View {
        float phase, pel = -29, lean, fx = 4, fy = -ANK, bx = -3, by = -ANK, a1 = 80, a2 = 95, b1 = 100, b2 = 85, flash; int assemble, clock, state = -1;
        float gFront, gBack; public String label = ""; public int labelT;
    }
    /** Cuadros que se mantiene cada dibujo: a 60 ticks por segundo, 5 equivalen a 12 cuadros por segundo. */
    public static final int HOLD = 5; static final float STRIDE = 15;
    //                           fx   fy   bx   by   a1   a2   b1   b2   pel    lean
    static final float[][] RUN_F = {
        { 13,-ANK, -12, -9, 128, 72,  48,-12, -28.2f, 12},
        {  4,-ANK,   3,-14,  92, 30,  96, 34, -29.8f, 12},
        {-12, -9,   13,-ANK,  48,-12, 128, 72, -28.2f, 12},
        {  3,-14,    4,-ANK,  96, 34,  92, 30, -29.8f, 12}};
    static final float[] IDLE_A = {4.5f,-ANK,-3.5f,-ANK, 78, 96,102, 84, -28.8f, 3}, IDLE_B = {4.5f,-ANK,-3.5f,-ANK, 80, 98,100, 82, -28.2f, 3};
    static final float[] UP_F = {10,-15,-5,-1, 4,-2,172,108,-30,5}, DOWN_F = {9,-9,-6,-3, 20,10,165,120,-30,5};
    static final float[] WALL_F = {9,-14,7,-5,-8,-8,160,110,-30,-7}, SLIDE_F = {23,-ANK,-3,-ANK,150,185,120,150,-11,-58}, HOOK_F = {7,-8,-6,-2,-60,-60,120,60,-32,4};
    private static float ground(Terrain t, int x, int y) {
        for (int k = -11; k < 13; k++) if (t.solid(x, y + k)) return k;
        return 0;
    }
    private static void pose(View v, float[] f) { v.fx = f[0]; v.fy = f[1]; v.bx = f[2]; v.by = f[3]; v.a1 = f[4]; v.a2 = f[5]; v.b1 = f[6]; v.b2 = f[7]; v.pel = f[8]; v.lean = f[9]; }
    /** Avanza la animación. El cuerpo se mueve a 60, pero el dibujo cambia por cuadros, como un sprite. */
    public static void update(View v, Robot r, World g) {
        float sp = Math.abs(r.vx); boolean run = r.grounded && sp > .3f && !r.sliding() && r.rope != 2;
        if (run) v.phase = (v.phase + sp) % (STRIDE * 4);
        int st = r.sliding() ? 4 : r.rope == 2 ? 5 : r.wallDir != 0 ? 6 : !r.grounded ? (r.vy < 1 ? 2 : 3) : run ? 1 : 0;
        if (r.fireT > 0) st += 10;
        v.clock++;
        if (st != v.state || v.clock % HOLD == 0) {
            v.state = st;
            switch (st % 10) {
                case 4: pose(v, SLIDE_F); break;
                case 6: pose(v, WALL_F); break;
                case 5: {
                    pose(v, HOOK_F);
                    v.a1 = v.a2 = (float) Math.toDegrees(Math.atan2(r.ry - (r.py - 40), (r.rx - r.px) * r.facing));
                    break; }
                case 2: pose(v, UP_F); break;
                case 3: pose(v, DOWN_F); break;
                case 1: pose(v, RUN_F[(int) (v.phase / STRIDE) & 3]); break;
                default: pose(v, v.clock / 25 % 2 == 0 ? IDLE_A : IDLE_B);
            }
            if (r.fireT > 0 && !r.sliding()) { v.a1 = 0; v.a2 = 0; }
            v.gFront = r.grounded ? ground(g.t, r.px + (int) (v.fx * r.facing), r.py) : 0;
            v.gBack = r.grounded ? ground(g.t, r.px + (int) (v.bx * r.facing), r.py) : 0;
        }
        if (v.flash > 0) v.flash--;
        if (r.hurtT > 8) v.flash = 3;
        if (v.assemble > 0) v.assemble--;
        if (v.labelT > 0) v.labelT--;
    }
    private static void bone(Gfx g, float x, float y, float deg) { g.save(); g.translate(x, y); g.rotate((float) Math.toRadians(deg)); }
    private static void stick(Gfx g, float len, float w) { g.rrect(0, -w, len, w * 2, w, INK, 0, 0); }
    private static void ball(Gfx g, float x, float r) { g.circle(x, 0, r, INK, 0, 0); }
    private static void limb(Gfx g, float[] q, int c) { g.poly(q, c, INK, SW); }
    /** Dibuja una pieza suelta, centrada, para los fragmentos que salen volando. */
    public static void piece(Gfx g, int slot, int c) {
        switch (slot) {
            case Robot.HELMET: g.translate(0, 5); limb(g, HEAD, c); eyes(g); break;
            case Robot.CHEST: g.translate(0, -5); limb(g, CHEST, c); break;
            case Robot.BELLY: g.translate(0, -13); limb(g, BELLY, c); break;
            case Robot.HIP: g.translate(0, -2); limb(g, HIPS, WHITE); break;
            case Robot.SH_F: case Robot.SH_B: limb(g, PAD, WHITE); break;
            case Robot.BR_F: case Robot.BR_B: g.translate(-4.5f, 0); limb(g, BRACER, c); break;
            case Robot.TH_F: case Robot.TH_B: g.translate(-6, 0); limb(g, THIGH, c); break;
            default: g.translate(-6, 0); limb(g, SHIN, c);
        }
    }
    /** Hueso suelto para el momento en que el robot se desarma. */
    public static void bonePiece(Gfx g, int kind) {
        switch (kind) {
            case 0: stick(g, 12, 1.5f); ball(g, 12, 2); break;
            case 1: g.translate(-3, 0); stick(g, 8, 1.1f); ball(g, 0, 1.7f); ball(g, 8, 1.7f); break;
            case 2: g.poly(TORSO, INK, 0, 0); break;
            case 3: limb(g, FOOT, INK); break;
            default: limb(g, HAND, INK);
        }
    }
    private static void eyes(Gfx g) {
        g.circle(1.2f, -6.6f, 1.6f, WHITE, INK, SW); g.circle(4.3f, -6.6f, 1.3f, WHITE, INK, SW);
    }
    private static void arm(Gfx g, float sx, float sy, float d1, float d2, boolean bracer, int c) {
        float ex = sx + (float) Math.cos(Math.toRadians(d1)) * UA, ey = sy + (float) Math.sin(Math.toRadians(d1)) * UA;
        bone(g, sx, sy, d1); stick(g, UA, 1.25f); ball(g, 0, 1.8f); ball(g, UA, 1.7f); g.restore();
        bone(g, ex, ey, d2); stick(g, FA, 1.1f); g.translate(FA, 0); limb(g, HAND, INK); g.translate(-FA, 0);
        if (bracer) limb(g, BRACER, c);
        g.restore();
    }
    private static void leg(Gfx g, float hx, float hy, float ax, float ay, boolean th, boolean sn, int c) {
        float dx = ax - hx, dy = ay - hy, d = (float) Math.hypot(dx, dy), max = TH + SH - .1f;
        if (d > max) { dx *= max / d; dy *= max / d; d = max; ax = hx + dx; ay = hy + dy; }
        float a = (d * d + TH * TH - SH * SH) / (2 * d), h2 = TH * TH - a * a, h = h2 > 0 ? (float) Math.sqrt(h2) : 0;
        float kx = hx + dx / d * a + dy / d * h, ky = hy + dy / d * a - dx / d * h;
        if (kx < hx + dx / d * a) { kx = hx + dx / d * a - dy / d * h; ky = hy + dy / d * a + dx / d * h; }
        float t = (float) Math.toDegrees(Math.atan2(ky - hy, kx - hx)), s = (float) Math.toDegrees(Math.atan2(ay - ky, ax - kx));
        bone(g, ax, ay, ay < -6.5f ? Math.max(-10, Math.min(55, (s - 90) * -.9f + 28)) : 0); limb(g, FOOT, INK); g.restore();
        bone(g, kx, ky, s); g.poly(new float[]{0,-2, SH,-1.1f, SH,1.1f, 0,2}, INK, 0, 0); if (sn) limb(g, SHIN, c); g.restore();
        bone(g, hx, hy, t); g.poly(new float[]{0,-2.2f, TH,-1.3f, TH,1.3f, 0,2.2f}, INK, 0, 0); if (th) limb(g, THIGH, c); g.restore();
        bone(g, kx, ky, s); limb(g, KNEE, INK); g.restore();
    }
    public static void draw(Gfx g, View v, Robot r, int color) {
        if (r.invuln > 0 && r.invuln < 80 && r.invuln / 4 % 2 == 0) return;
        g.save(); g.translate(r.px, r.py); g.scale(r.facing, 1);
        float ch = r.chargeLevel(); int col = v.flash > 0 ? 0xFFFFD040 : color;
        if (ch > 0) { boolean full = ch >= 1; int rate = full ? 4 : ch > .5f ? 6 : 10; if (v.clock / rate % 2 == 0) col = full ? 0xFFFFF27A : mix(color); }
        drawPose(g, v, r.armor, col, r.py);
        g.restore();
        if (ch >= 1) {
            float R = 34 + v.clock / 4 % 3 * 3; int n = 8; float[] q = new float[n * 4];
            for (int i = 0; i < n * 2; i++) { float a = i * 3.14159f / n + v.clock / 4 * .4f, d = i % 2 == 0 ? R : R * .72f; q[i * 2] = r.px + (float) Math.cos(a) * d; q[i * 2 + 1] = r.py - 27 + (float) Math.sin(a) * d; }
            g.poly(q, 0, 0xFFFFE23A, 1.2f);
        }
    }
    private static int mix(int c) { return 0xFF000000 | ((c >> 16 & 255) + 255) / 2 << 16 | ((c >> 8 & 255) + 255) / 2 << 8 | ((c & 255) + 255) / 2; }
    /** El dibujo en sí, en coordenadas locales mirando a la derecha. */
    public static void drawPose(Gfx g, View v, int ar, int c, int unused) {
        float e = (v.assemble / HOLD * HOLD) / 30f; e *= e;
        float px = 0, py = v.pel, lr = (float) Math.toRadians(v.lean), sn = (float) Math.sin(lr), cs = (float) Math.cos(lr);
        float cx = px + sn * 17, cy = py - cs * 17;
        float sfx = cx + cs * 5.5f, sfy = cy + sn * 5.5f + 1, sbx = cx - cs * 5.5f, sby = cy - sn * 5.5f + 1;
        arm(g, sbx, sby, v.b1, v.b2, (ar >> Robot.BR_B & 1) != 0 && e < .1f, c);
        leg(g, px - 2.5f, py + 1, v.bx, v.by + v.gBack, (ar >> Robot.TH_B & 1) != 0 && e < .1f, (ar >> Robot.SN_B & 1) != 0 && e < .1f, c);
        g.save(); g.translate(px, py); g.rotate(lr); g.translate(0, -17);
        g.rrect(-1.4f, 8, 2.8f, 8, .6f, INK, 0, 0); g.poly(TORSO, INK, 0, 0); g.poly(PELVIS, INK, 0, 0);
        g.line(0, 0, 0, -6, INK, 1.8f); g.circle(0, -6.2f, 1.5f, INK, 0, 0);
        float sp = e * 16;
        if ((ar >> Robot.BELLY & 1) != 0) { g.save(); g.translate(0, sp * .3f); limb(g, BELLY, c); g.restore(); }
        if ((ar >> Robot.CHEST & 1) != 0) { g.save(); g.translate(0, -sp * .3f); limb(g, CHEST, c); g.restore(); }
        if ((ar >> Robot.HELMET & 1) != 0) { g.save(); g.translate(0, -3.4f - sp); g.rotate(-lr * .8f); limb(g, HEAD, c); eyes(g); g.restore(); }
        g.restore();
        leg(g, px + 2.5f, py + 1, v.fx, v.fy + v.gFront, (ar >> Robot.TH_F & 1) != 0 && e < .1f, (ar >> Robot.SN_F & 1) != 0 && e < .1f, c);
        if ((ar >> Robot.HIP & 1) != 0) { g.save(); g.translate(px, py); g.rotate(lr); g.translate(0, -1.4f + sp * .5f); limb(g, HIPS, WHITE); g.restore(); }
        arm(g, sfx, sfy, v.a1, v.a2, (ar >> Robot.BR_F & 1) != 0 && e < .1f, c);
        if ((ar >> Robot.SH_B & 1) != 0) { g.save(); g.translate(sbx - sp * .8f, sby); limb(g, PAD, WHITE); g.restore(); }
        if ((ar >> Robot.SH_F & 1) != 0) { g.save(); g.translate(sfx + sp * .8f, sfy); g.scale(.82f, .9f); limb(g, PAD, WHITE); g.restore(); }
    }
    public static View poster() { View v = new View(); v.pel = -30; v.fx = 10; v.fy = -15; v.bx = -5; v.by = -1; v.a1 = 4; v.a2 = -2; v.b1 = 172; v.b2 = 108; v.lean = 5; return v; }
    public static void assemble(View v) { v.assemble = 30; }
}
