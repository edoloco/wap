package com.wap.render;
import com.wap.core.*;
/** Personaje en pixel art según la referencia: cabeza gris, traje de color, hombros blancos y guanteletes grises sobre un esqueleto negro.
 *  Se dibuja a 1 píxel de arte por unidad del mundo, sin suavizado, y cada pieza de armadura puede caer por separado. */
public final class RobotArt {
    public static final int INK = 0xFF141414, WHITE = 0xFFFFFFFF, HEADC = 0xFF9E9E9E, PADC = 0xFFC2C3C7, GLOWC = 0xFFD8F2FF;
    /** Color del traje: el jugador lo cambia en el editor; el rival usa otro. */
    public static final int[] COLOR = {0xFF5E5650, 0xFF1266FF};
    static final float TH = 10, SH = 10, UA = 6, FA = 6.5f, ANK = 4, SW = .5f, TORSO_LEN = 15, SHOULDER = 11;
    static final float[] HEAD = new float[32];
    static { for (int i = 0; i < 16; i++) { double a = i * Math.PI / 8; HEAD[i * 2] = .6f + (float) Math.cos(a) * 7; HEAD[i * 2 + 1] = -7.8f + (float) Math.sin(a) * (a > 0 && a < Math.PI ? 6.2f : 7); } }
    static final float[] CHEST = {-5.2f,1, 5.2f,1, 5.6f,5.8f, 3.8f,8.6f, -3.8f,8.6f, -5.6f,5.8f};
    static final float[] BELLY = {-3.4f,8.8f, 3.4f,8.8f, 2.8f,10.6f, -2.8f,10.6f};
    static final float[] HIPS = {-3,-4.6f, 3,-4.6f, 3,-1.4f, 1,.4f, -1,.4f, -3,-1.4f};
    static final float[] TORSO = {-3.4f,0, 3.4f,0, 2.4f,9, -2.4f,9};
    static final float[] PELVIS = {-4.2f,11.4f, 4.2f,11.4f, 3.6f,15.2f, 1,16.6f, -1,16.6f, -3.6f,15.2f};
    static final float[] BRACER = {-.5f,-3.6f, 1.8f,-4.4f, 6.6f,-4.4f, 8,-3, 8,3, 6.6f,4.4f, 1.8f,4.4f, -.5f,3.6f};
    static final float[] HAND = {0,-2.4f, 3.6f,-2.6f, 5,-1.2f, 5,1.2f, 3.6f,2.6f, 0,2.4f};
    static final float[] OPEN = {0,-2, 3,-2.4f, 6.4f,-3.8f, 7.2f,-3, 4.4f,-1.2f, 7.4f,-.6f, 7.4f,.6f, 4.4f,1, 6.8f,3.2f, 6,4, 3,2.4f, 0,2};
    static final float[] THIGH = {-1,-2.8f, 2,-3.4f, 7,-3, 10.4f,-2.4f, 10.2f,2.4f, 6,3.2f, 1,3.4f, -1,2.4f};
    static final float[] SHIN = {.5f,-2.2f, 3,-2.6f, 7,-2.4f, 9.6f,-2.8f, 9.6f,2.8f, 5,2.6f, 1,2.6f};
    static final float[] KNEE = {-1.6f,-2.4f, 1.6f,-2.6f, 2.2f,0, 1.6f,2.4f, -1.6f,2.4f};
    static final float[] FOOT = {-3,-1.6f, 2,-2, 6,0, 7.6f,1.6f, 7.6f,4, -3,4};

    /** Estado de animación solo visual de un robot. */
    public static final class View {
        float phase, pel = -22, lean, fx = 4, fy = -ANK, bx = -3, by = -ANK, a1 = 86, a2 = 96, b1 = 94, b2 = 86, flash; int assemble, clock, state = -1; public int shotT; boolean open, ball;
        float gFront, gBack; public String label = ""; public int labelT;
    }
    /** Cuadros que se mantiene cada dibujo: a 60 ticks por segundo, 5 equivalen a 12 cuadros por segundo. */
    public static final int HOLD = 5; static final float STRIDE = 13;
    // a1, a2: brazo cercano (el que dispara); b1, b2: brazo lejano.
    //                           fx   fy   bx   by   a1   a2   b1   b2   pel    lean
    static final float[][] RUN_F = {
        { 10,-ANK,  -9, -7, 128, 72,  48,-12, -21.5f, 8},
        {  3,-ANK,   2,-11,  92, 30,  96, 34, -23, 8},
        { -9, -7,   10,-ANK,  48,-12, 128, 72, -21.5f, 8},
        {  2,-11,    3,-ANK,  96, 34,  92, 30, -23, 8}};
    static final float[] IDLE_A = {5,-ANK,-4,-ANK, 104, 96, 76, 84, -22.5f, 2}, IDLE_B = {5,-ANK,-4,-ANK, 105, 98, 75, 82, -22, 2};
    /** Preparación (puños frente al pecho) y postura de disparo, según las referencias. */
    static final float[] MID_F = {9,-ANK,-10,-6, 55,-45, 95,15, -20, 6}, STANCE_F = {11,-ANK,-10,-ANK, 0, 0, 80,-30, -20, 2};
    static final float[] UP_F = {7,-11,-4,-1, 4,-2,172,108,-23.5f,5}, DOWN_F = {7,-7,-5,-2, 20,10,165,120,-23.5f,5};
    static final float[] WALL_F = {7,-10,5,-4,-8,-8,160,110,-23.5f,-7}, SLIDE_F = {17,-ANK,-2,-ANK,150,185,120,150,-9,-58}, HOOK_F = {6,-6,-5,-2,-60,-60,120,60,-24,4};
    /** Tras el doble salto, el robot se estira: piernas abajo y brazos arriba. */
    static final float[] STRETCH_F = {2,-5,-1,-4.5f, -96,-92,-84,-88,-25,0};
    private static float ground(Terrain t, int x, int y) {
        for (int k = -11; k < 13; k++) if (t.solid(x, y + k)) return k;
        return 0;
    }
    private static void pose(View v, float[] f) { v.fx = f[0]; v.fy = f[1]; v.bx = f[2]; v.by = f[3]; v.a1 = f[4]; v.a2 = f[5]; v.b1 = f[6]; v.b2 = f[7]; v.pel = f[8]; v.lean = f[9]; }
    /** Avanza la animación. El cuerpo se mueve a 60, pero el dibujo cambia por cuadros, como un sprite. */
    public static void update(View v, Robot r, World g) {
        float sp = Math.abs(r.vx); boolean run = r.grounded && sp > .3f && !r.sliding() && r.rope != 2;
        if (run) v.phase = (v.phase + sp) % (STRIDE * 4);
        int st = r.thrownT > 0 ? 3 : r.rollT > 9 && !r.grounded ? 7 : r.rollT > 0 && !r.grounded ? 8 : r.sliding() ? 4 : r.rope == 2 ? 5 : r.wallDir != 0 ? 6 : !r.grounded ? (r.vy < 1 ? 2 : 3) : run ? 1 : 0;
        boolean act = r.windT > 0 || r.fireT > 0;
        if (act) st = st == 0 ? (r.windT > 3 ? 20 : 21) : st + 10;
        v.clock++;
        if (st != v.state || v.clock % HOLD == 0) {
            v.state = st;
            switch (st >= 20 ? st : st % 10) {
                case 20: pose(v, MID_F); break;
                case 21: pose(v, STANCE_F); break;
                case 4: pose(v, SLIDE_F); break;
                case 8: pose(v, STRETCH_F); break;
                case 6: pose(v, WALL_F); break;
                case 5: {
                    pose(v, HOOK_F);
                    v.a1 = v.a2 = (float) Math.toDegrees(Math.atan2(r.ry - (r.py - 33), (r.rx - r.px) * r.facing));
                    break; }
                case 2: pose(v, UP_F); break;
                case 3: pose(v, DOWN_F); break;
                case 1: pose(v, RUN_F[(int) (v.phase / STRIDE) & 3]); break;
                default: pose(v, v.clock / 25 % 2 == 0 ? IDLE_A : IDLE_B);
            }
            if (st > 9 && st < 20 && !r.sliding() && r.rope != 2) { v.a1 = 0; v.a2 = 0; }
            v.open = act && st != 20 && !r.sliding();
            v.ball = st % 10 == 7;
            v.gFront = r.grounded ? ground(g.t, r.px + (int) (v.fx * r.facing), r.py) : 0;
            v.gBack = r.grounded ? ground(g.t, r.px + (int) (v.bx * r.facing), r.py) : 0;
        }
        if (v.flash > 0) v.flash--;
        if (r.hurtT > 8) v.flash = 3;
        if (v.assemble > 0) v.assemble--;
        if (v.labelT > 0) v.labelT--;
        if (v.shotT > 0) v.shotT--;
    }
    private static void bone(Gfx g, float x, float y, float deg) { g.save(); g.translate(x, y); g.rotate((float) Math.toRadians(deg)); }
    private static void stick(Gfx g, float len, float w) { g.rrect(0, -w, len, w * 2, w, INK, 0, 0); }
    private static void ball(Gfx g, float x, float r) { g.circle(x, 0, r, INK, 0, 0); }
    private static void limb(Gfx g, float[] q, int c) { g.poly(q, c, INK, SW); }
    private static void hand(Gfx g, boolean open, boolean glow) {
        limb(g, open ? OPEN : HAND, INK);
        if (open) { if (glow) g.rect(6, -1, 2, 2, 0xFF7FD8FF); }
        else { g.line(2, -.8f, 4, -.8f, WHITE, SW); g.line(2, 1, 4, 1, WHITE, SW); }
    }
    /** Dibuja una pieza suelta, centrada, para los fragmentos que salen volando. */
    public static void piece(Gfx g, int slot, int c) {
        switch (slot) {
            case Robot.HELMET: g.translate(0, 7.8f); limb(g, HEAD, HEADC); break;
            case Robot.CHEST: g.translate(0, -5); limb(g, CHEST, c); break;
            case Robot.BELLY: g.translate(0, -10); limb(g, BELLY, c); break;
            case Robot.HIP: g.translate(0, 2); limb(g, HIPS, PADC); break;
            case Robot.SH_F: case Robot.SH_B: g.circle(0, 0, 3.4f, WHITE, INK, SW); break;
            case Robot.BR_F: case Robot.BR_B: g.translate(-4, 0); limb(g, BRACER, PADC); break;
            case Robot.TH_F: case Robot.TH_B: g.translate(-5, 0); limb(g, THIGH, c); break;
            default: g.translate(-5, 0); limb(g, SHIN, c);
        }
    }
    /** Hueso suelto para el momento en que el robot se desarma. */
    public static void bonePiece(Gfx g, int kind) {
        switch (kind) {
            case 0: stick(g, 10, 1.4f); ball(g, 10, 1.8f); break;
            case 1: g.translate(-3, 0); stick(g, 6.5f, 1.1f); ball(g, 0, 1.6f); ball(g, 6.5f, 1.6f); break;
            case 2: g.poly(TORSO, INK, 0, 0); break;
            case 3: limb(g, FOOT, INK); break;
            default: limb(g, HAND, INK);
        }
    }
    private static void arm(Gfx g, float sx, float sy, float d1, float d2, boolean bracer, boolean open, boolean glow) {
        float ex = sx + (float) Math.cos(Math.toRadians(d1)) * UA, ey = sy + (float) Math.sin(Math.toRadians(d1)) * UA;
        bone(g, sx, sy, d1); stick(g, UA, 1.2f); ball(g, 0, 1.6f); ball(g, UA, 1.5f); g.restore();
        bone(g, ex, ey, d2); stick(g, FA, 1.1f); g.translate(FA, 0); hand(g, open, glow); g.translate(-FA, 0);
        if (bracer) limb(g, BRACER, glow ? GLOWC : PADC);
        g.restore();
    }
    private static void leg(Gfx g, float hx, float hy, float ax, float ay, boolean th, boolean sn, int c) {
        float dx = ax - hx, dy = ay - hy, d = (float) Math.hypot(dx, dy), max = TH + SH - .1f;
        if (d > max) { dx *= max / d; dy *= max / d; d = max; ax = hx + dx; ay = hy + dy; }
        float a = (d * d + TH * TH - SH * SH) / (2 * d), h2 = TH * TH - a * a, h = h2 > 0 ? (float) Math.sqrt(h2) : 0;
        float kx = hx + dx / d * a + dy / d * h, ky = hy + dy / d * a - dx / d * h;
        if (kx < hx + dx / d * a) { kx = hx + dx / d * a - dy / d * h; ky = hy + dy / d * a + dx / d * h; }
        float t = (float) Math.toDegrees(Math.atan2(ky - hy, kx - hx)), s = (float) Math.toDegrees(Math.atan2(ay - ky, ax - kx));
        bone(g, ax, ay, ay < -6.5f ? Math.max(-10, Math.min(55, (s - 90) * -.9f + 28)) : 0); limb(g, FOOT, INK); g.line(-3, 3.6f, 7.4f, 3.6f, WHITE, SW); g.restore();
        bone(g, kx, ky, s); g.poly(new float[]{0,-1.8f, SH,-1.1f, SH,1.1f, 0,1.8f}, INK, 0, 0); if (sn) limb(g, SHIN, c); g.restore();
        bone(g, hx, hy, t); g.poly(new float[]{0,-2, TH,-1.2f, TH,1.2f, 0,2}, INK, 0, 0); if (th) limb(g, THIGH, c); g.restore();
        bone(g, kx, ky, s); limb(g, KNEE, INK); g.restore();
    }
    public static void draw(Gfx g, View v, Robot r, int color) {
        if (r.invuln > 0 && r.invuln < 80 && r.invuln / 4 % 2 == 0) return;
        g.save(); g.translate(r.px, r.py);
        if (r.thrownT > 0) { g.translate(0, -24); g.rotate(-r.facing * (v.clock / 3) * .785f); g.translate(0, 24); } // va girando por el aire
        g.scale(r.facing, 1);
        float ch = r.chargeLevel(); int col = v.flash > 0 ? 0xFFFFD040 : color;
        if (ch > 0) { boolean full = ch >= 1; int rate = full ? 4 : ch > .5f ? 6 : 10; if (v.clock / rate % 2 == 0) col = full ? 0xFFFFF27A : mix(color); }
        drawPose(g, v, r.armor, col, r.py);
        g.restore();
        if (r.stunT > 0) for (int k = 0; k < 3; k++) { // estrellas de aturdido
            double a = (v.clock / 4) * .5 + k * 2.09; int x = Math.round(r.px + (float) Math.cos(a) * 9), y = Math.round(r.py - 57 + (float) Math.sin(a) * 2.5f);
            g.rect(x - 1, y, 3, 1, 0xFFFFE23A); g.rect(x, y - 1, 1, 3, 0xFFFFE23A);
        }
        if (ch >= 1) {
            float R = 30 + v.clock / 4 % 3 * 3; int n = 8; float[] q = new float[n * 4];
            for (int i = 0; i < n * 2; i++) { float a = i * 3.14159f / n + v.clock / 4 * .4f, d = i % 2 == 0 ? R : R * .72f; q[i * 2] = r.px + (float) Math.cos(a) * d; q[i * 2 + 1] = r.py - 24 + (float) Math.sin(a) * d; }
            g.poly(q, 0, 0xFFFFE23A, 1);
        }
    }
    private static int mix(int c) { return 0xFF000000 | ((c >> 16 & 255) + 255) / 2 << 16 | ((c >> 8 & 255) + 255) / 2 << 8 | ((c & 255) + 255) / 2; }
    /** El dibujo en sí, en coordenadas locales mirando a la derecha. */
    public static void drawPose(Gfx g, View v, int ar, int c, int unused) {
        if (v.ball) { drawBall(g, v, ar, c); return; }
        float e = (v.assemble / HOLD * HOLD) / 30f; e *= e;
        boolean on = e < .1f, glow = v.shotT > 0;
        float px = 0, py = v.pel, lr = (float) Math.toRadians(v.lean), sn = (float) Math.sin(lr), cs = (float) Math.cos(lr);
        float cx = px + sn * SHOULDER, cy = py - cs * SHOULDER, sp = e * 16;
        float ffx = cx + cs * 4.6f, ffy = cy + sn * 4.6f + 1, nx = cx - cs * 4.6f, ny = cy - sn * 4.6f + 1;
        arm(g, ffx, ffy, v.b1, v.b2, (ar >> Robot.BR_F & 1) != 0 && on, false, false);
        if ((ar >> Robot.SH_F & 1) != 0) g.circle(ffx + sp * .8f, ffy, 3, WHITE, INK, SW);
        leg(g, px + 2, py + 1, v.fx, v.fy - 1 + v.gFront, (ar >> Robot.TH_F & 1) != 0 && on, (ar >> Robot.SN_F & 1) != 0 && on, c);
        g.save(); g.translate(px, py); g.rotate(lr); g.translate(0, -TORSO_LEN);
        g.rrect(-1.2f, 8, 2.4f, 7, .6f, INK, 0, 0); g.poly(TORSO, INK, 0, 0); g.poly(PELVIS, INK, 0, 0);
        g.rrect(-1, -2, 2, 3, .5f, INK, 0, 0);
        if ((ar >> Robot.BELLY & 1) != 0) { g.save(); g.translate(0, sp * .3f); limb(g, BELLY, c); g.restore(); }
        if ((ar >> Robot.CHEST & 1) != 0) { g.save(); g.translate(0, -sp * .3f); limb(g, CHEST, c); g.restore(); }
        if ((ar >> Robot.HELMET & 1) != 0) { g.save(); g.translate(0, -sp); g.rotate(-lr * .8f); limb(g, HEAD, HEADC); g.restore(); }
        else { g.circle(0, -5.5f, 3.4f, INK, 0, 0); }
        g.restore();
        leg(g, px - 2, py + 1, v.bx, v.by + v.gBack, (ar >> Robot.TH_B & 1) != 0 && on, (ar >> Robot.SN_B & 1) != 0 && on, c);
        if ((ar >> Robot.HIP & 1) != 0) { g.save(); g.translate(px, py); g.rotate(lr); g.translate(0, sp * .5f); limb(g, HIPS, PADC); g.restore(); }
        arm(g, nx, ny, v.a1, v.a2, (ar >> Robot.BR_B & 1) != 0 && on, v.open, glow);
        if ((ar >> Robot.SH_B & 1) != 0) g.circle(nx - sp * .8f, ny, 3.6f, WHITE, INK, SW);
        if (v.shotT > 0 && v.open) { float d = UA + FA + 7; muzzle(g, nx + (float) Math.cos(Math.toRadians(v.a1)) * d, ny + (float) Math.sin(Math.toRadians(v.a1)) * d, 2 - (v.shotT - 1) / 4); }
    }
    /** Doble salto: se enrolla como un chanchito de tierra, con el caparazón del color del traje. */
    private static void drawBall(Gfx g, View v, int ar, int c) {
        boolean shell = (ar >> Robot.CHEST & 1) != 0;
        g.save(); g.translate(0, -16); g.rotate((v.clock / HOLD) * 1.2f);
        g.circle(0, 0, 11, shell ? c : INK, INK, SW);
        for (int i = -1; i <= 1; i++) { float y = i * 5.2f, hx = (float) Math.sqrt(121 - y * y) - .6f; g.line(-hx, y, hx, y, INK, SW); }
        if ((ar >> Robot.HELMET & 1) != 0) g.circle(6.5f, -5, 4, HEADC, INK, SW);
        g.circle(-6, 5, 3, WHITE, INK, SW); g.rect(-3, 8, 6, 3, INK);
        g.restore();
    }
    static final float[] CONE = {0,-4, 9,-2.4f, 15,0, 9,2.4f, 0,4}, CORE = {1,-2, 9,0, 1,2};
    /** Destello azul por cuadros frente a la mano: chispa, llamarada y restos. */
    private static void muzzle(Gfx g, float x, float y, int frame) {
        final int LIGHT = 0xFFB8F0FF, BLUE = 0xFF3E8BFF;
        g.save(); g.translate(x, y);
        if (frame <= 0) { g.circle(2, 0, 3.5f, LIGHT, BLUE, SW); g.rect(1, -1, 2, 2, WHITE); }
        else if (frame == 1) {
            g.poly(CONE, LIGHT, 0, 0); g.poly(CORE, WHITE, 0, 0);
            g.line(2, -6, 5, -9, BLUE, SW); g.line(2, 6, 5, 9, BLUE, SW); g.line(6, -5, 8, -8, LIGHT, SW); g.line(6, 5, 8, 8, LIGHT, SW);
        } else { g.rect(4, -3, 2, 2, BLUE); g.rect(9, 1, 2, 2, BLUE); g.rect(13, -2, 1, 1, LIGHT); g.rect(16, 3, 2, 1, BLUE); g.rect(7, 4, 1, 1, LIGHT); }
        g.restore();
    }
    public static View poster() { View v = new View(); pose(v, STANCE_F); v.open = true; return v; }
    public static void assemble(View v) { v.assemble = 30; }
}
