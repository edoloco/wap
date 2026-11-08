package com.wap.render;
import com.wap.core.*;
/** Personaje vectorial con contorno grueso, según las referencias: cabeza gris, traje de color, hombros blancos y guanteletes grises sobre un esqueleto negro.
 *  Se dibuja a 1 píxel de arte por unidad del mundo, sin suavizado, y cada pieza de armadura puede caer por separado. */
public final class RobotArt {
    public static final int INK = 0xFF141414, WHITE = 0xFFFFFFFF, HEADC = 0xFF9E9E9E, PADC = 0xFFC2C3C7, GLOWC = 0xFFD8F2FF;
    /** Color del traje: el jugador lo cambia en el editor; el rival usa otro. */
    public static final int[] COLOR = {0xFF5E5650, 0xFF1266FF, 0xFF2BAA4A, 0xFFE8B21E, 0xFFB04FD8};
    static final float TH = 10, SH = 10, UA = 6, FA = 6.5f, ANK = 4, SW = .75f, TORSO_LEN = 15, SHOULDER = 11;
    /** Cabeza irregular como en la referencia (no un círculo), suavizada con curvas Catmull-Rom. */
    static final float[] HEAD;
    static {
        float[][] k = {{-4.5f,-14.6f},{.5f,-15.4f},{5,-14.2f},{7.3f,-11},{7.4f,-7},{6.2f,-3.6f},{3.6f,-1.4f},{0,-.9f},{-3.2f,-1.8f},{-5.4f,-4},{-6.6f,-7.6f},{-6.6f,-11.2f}};
        int n = k.length, sub = 4; HEAD = new float[n * sub * 2];
        for (int i = 0; i < n; i++) for (int j = 0; j < sub; j++) {
            float t = j / (float) sub, t2 = t * t, t3 = t2 * t; float[] p0 = k[(i - 1 + n) % n], p1 = k[i], p2 = k[(i + 1) % n], p3 = k[(i + 2) % n];
            for (int d = 0; d < 2; d++) HEAD[(i * sub + j) * 2 + d] = .5f * (2 * p1[d] + (-p0[d] + p2[d]) * t + (2 * p0[d] - 5 * p1[d] + 4 * p2[d] - p3[d]) * t2 + (-p0[d] + 3 * p1[d] - 3 * p2[d] + p3[d]) * t3);
        }
    }
    static final float[] NECK = {-2.4f,-2.2f, .6f,-1.6f, -.4f,2.4f, -3.2f,1.8f};
    static final float[] CHEST = {-5.2f,1, 5.2f,1, 5.6f,5.8f, 3.8f,8.6f, -3.8f,8.6f, -5.6f,5.8f};
    static final float[] BELLY = {-3.4f,8.8f, 3.4f,8.8f, 2.8f,10.6f, -2.8f,10.6f};
    static final float[] HIPS = {-3.6f,-5, 3.6f,-5, 3.8f,1.6f, 2.6f,3, -2.6f,3, -3.8f,1.6f};
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
        public float phase; float pel = -22, lean, fx = 4, fy = -ANK, bx = -3, by = -ANK, a1 = 86, a2 = 96, b1 = 94, b2 = 86, flash; int assemble, clock, state = -1; public int shotT; boolean open, ball;
        float gFront, gBack; public String label = ""; public int labelT;
    }
    /** Cuadros que se mantiene cada dibujo: a 60 ticks por segundo, 5 equivalen a 12 cuadros por segundo. */
    public static final int HOLD = 5; static final float STRIDE = 19; // carrera: 4 poses, cada una ~3 cuadros a 24 por segundo
    // a1, a2: brazo cercano (el que dispara); b1, b2: brazo lejano.
    //                           fx   fy   bx   by   a1   a2   b1   b2   pel    lean
    static final float[][] RUN_F = {
        { 10,-ANK, -11, -8, 120, 95,  15,-45, -22, 8},   // zancada: brazo lejano adelante
        {  3,-ANK,   5, -9,  60,-30,  35,-55, -19, 14},  // recogida, brazos al pecho
        {-11, -8,   10,-ANK,  10,-40, 125,100, -22, 8},  // zancada contraria
        {  5, -9,    3,-ANK,  45,-50,  55,-25, -19, 14}};
    static final float[] IDLE_A = {5,-ANK,-4,-ANK, 104, 96, 76, 84, -22.5f, 2}, IDLE_B = {5,-ANK,-4,-ANK, 105, 98, 75, 82, -22, 2};
    /** Preparación del disparo en dos poses (agachado con los puños juntos, luego hacia adelante) y postura con ambos brazos al frente. */
    static final float[] MID_F = {6,-ANK,-8,-ANK, 70,-60, 60,-40, -18, 20}, MID2_F = {9,-ANK,-10,-ANK, 35,-45, 25,-30, -19, 10};
    static final float[] STANCE_F = {11,-ANK,-10,-ANK, 0, 0, -6, 4, -20, 4};
    /** Salto: una rodilla arriba, la otra pierna atrás, un puño en alto. */
    static final float[] UP_F = {8,-13,-9,-3, 105,95,-35,-80,-24,4}, DOWN_F = {7,-8,-7,-4, 95,85,-10,-60,-23.5f,4};
    /** Barrida: pierna delantera estirada por el suelo y la otra doblada. */
    static final float[] SLIDE_A = {15,-3,-6,-1, 120,100,-25,-70,-11,-28}, SLIDE_B = {17,-3,-7,-1, 140,125,150,135,-10,-38};
    static final float[] WALL_F = {7,-10,5,-4,-8,-8,160,110,-23.5f,-7}, HOOK_F = {6,-6,-5,-2,-60,-60,120,60,-24,4};
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
        if (act) st = st == 0 ? (r.windT > 3 ? 20 : r.windT > 0 ? 22 : 21) : st + 10;
        if (st % 10 == 4) st = r.slideT > 14 ? 4 : 9;
        v.clock++;
        if (st != v.state || v.clock % HOLD == 0) {
            v.state = st;
            switch (st >= 20 ? st : st % 10) {
                case 20: pose(v, MID_F); break;
                case 22: pose(v, MID2_F); break;
                case 21: pose(v, STANCE_F); break;
                case 4: pose(v, SLIDE_A); break;
                case 9: pose(v, SLIDE_B); break;
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
            if (st > 9 && st < 20 && !r.sliding() && r.rope != 2) { v.a1 = 0; v.a2 = 0; if (!r.grounded) { v.b1 = 105; v.b2 = 95; } }
            v.open = act && st != 20 && st != 22 && !r.sliding();
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
        if (!open) for (int i = 0; i < 3; i++) g.rrect(2.6f, -2.2f + i * 1.5f, 2.6f, 1.2f, .5f, WHITE, INK, SW * .5f); // dedos
        else { g.rrect(4, -3.4f, 2.6f, 1.2f, .5f, WHITE, INK, SW * .5f); g.rrect(4.6f, -.6f, 2.6f, 1.2f, .5f, WHITE, INK, SW * .5f); }
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
    /** Si es verdadero, se dibuja todo (incluido el cráneo bajo el casco) para exportar la plantilla. */
    public static boolean exportAll;
    private static void arm(Gfx g, float sx, float sy, float d1, float d2, boolean bracer, boolean open, boolean glow, String side) {
        float ex = sx + (float) Math.cos(Math.toRadians(d1)) * UA, ey = sy + (float) Math.sin(Math.toRadians(d1)) * UA;
        g.layer("hueso_brazo_" + side); bone(g, sx, sy, d1); stick(g, UA, 1.2f); ball(g, 0, 1.6f); ball(g, UA, 1.5f); g.restore();
        bone(g, ex, ey, d2); stick(g, FA, 1.1f); g.layer("mano_" + side.replace("no", "na")); g.translate(FA, 0); hand(g, open, glow); g.translate(-FA, 0);
        if (bracer) { g.layer("guantelete_" + side); if (glow) g.grad(BRACER, PADC, WHITE, 0, 8, INK, SW); else limb(g, BRACER, PADC); }
        g.restore(); g.endLayer();
    }
    private static void leg(Gfx g, float hx, float hy, float ax, float ay, boolean th, boolean sn, int c, String side) {
        float dx = ax - hx, dy = ay - hy, d = (float) Math.hypot(dx, dy), max = TH + SH - .1f;
        if (d > max) { dx *= max / d; dy *= max / d; d = max; ax = hx + dx; ay = hy + dy; }
        float a = (d * d + TH * TH - SH * SH) / (2 * d), h2 = TH * TH - a * a, h = h2 > 0 ? (float) Math.sqrt(h2) : 0;
        float kx = hx + dx / d * a + dy / d * h, ky = hy + dy / d * a - dx / d * h;
        if (kx < hx + dx / d * a) { kx = hx + dx / d * a - dy / d * h; ky = hy + dy / d * a + dx / d * h; }
        float t = (float) Math.toDegrees(Math.atan2(ky - hy, kx - hx)), s = (float) Math.toDegrees(Math.atan2(ay - ky, ax - kx));
        g.layer("bota_" + side); bone(g, ax, ay, ay < -6.5f ? Math.max(-10, Math.min(55, (s - 90) * -.9f + 28)) : 0); limb(g, FOOT, INK); g.line(-3, 3.6f, 7.4f, 3.6f, WHITE, SW); g.restore();
        g.layer("hueso_canilla_" + side); bone(g, kx, ky, s); g.poly(new float[]{0,-1.8f, SH,-1.1f, SH,1.1f, 0,1.8f}, INK, 0, 0); if (sn) { g.layer("canilla_" + side); limb(g, SHIN, c); } g.restore();
        g.layer("hueso_muslo_" + side.replace("na", "no")); bone(g, hx, hy, t); g.poly(new float[]{0,-2, TH,-1.2f, TH,1.2f, 0,2}, INK, 0, 0); if (th) { g.layer("muslo_" + side.replace("na", "no")); limb(g, THIGH, c); } g.restore();
        g.layer("rodilla_" + side); bone(g, kx, ky, s); limb(g, KNEE, INK); g.restore(); g.endLayer();
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
        if (ch > 0) { // carga: chispas que aparecen y desaparecen alrededor del cuerpo, como el Mega Buster
            boolean full = ch >= 1; int n = full ? 9 : 2 + (int) (ch * 5), f = v.clock / 2;
            for (int i = 0; i < n; i++) {
                int h = (f * 73 + i * 151 + r.id * 977) * 2654435 >>> 7; float a = (h & 1023) / 1023f * 6.283f, d = 13 + (h >> 10 & 15);
                int x = Math.round(r.px + (float) Math.cos(a) * d), y = Math.round(r.py - 26 + (float) Math.sin(a) * d * 1.15f), sz = (h >> 14 & 3) == 0 ? 2 : 1;
                int spark = full ? ((h >> 16 & 1) == 0 ? 0xFFFFFFFF : 0xFF7FE3FF) : ((h >> 16 & 1) == 0 ? 0xFFFFE23A : 0xFFFFF7B0);
                g.additive(true); g.glow(x + sz / 2f, y + sz / 2f, sz * 3, spark & 0xFFFFFF | 0xAA000000); g.additive(false); g.rect(x, y, sz, sz, spark);
                if (full && sz == 2) { g.rect(x - 1, y, 1, 1, spark); g.rect(x + 2, y + 1, 1, 1, spark); }
            }
        }
        if (r.aiming && r.rope == 0) { // mira del gancho
            float a = (float) Math.toRadians(r.aim), cx = r.px + r.facing * 6, cy = r.py - 40;
            for (int i = 1; i <= 4; i++) g.rect(Math.round(cx + (float) Math.cos(a) * r.facing * i * 8), Math.round(cy - (float) Math.sin(a) * i * 8), 1, 1, INK);
            int x = Math.round(cx + (float) Math.cos(a) * r.facing * 44), y = Math.round(cy - (float) Math.sin(a) * 44);
            g.rect(x - 3, y, 2, 1, color); g.rect(x + 2, y, 2, 1, color); g.rect(x, y - 3, 1, 2, color); g.rect(x, y + 2, 1, 2, color);
            g.rect(x - 4, y - 1, 1, 3, INK); g.rect(x + 4, y - 1, 1, 3, INK); g.rect(x - 1, y - 4, 3, 1, INK); g.rect(x - 1, y + 4, 3, 1, INK);
        }
    }
    public static int mixLight(int c) { return 0xFF000000 | Math.min(255, (c >> 16 & 255) + 70) << 16 | Math.min(255, (c >> 8 & 255) + 70) << 8 | Math.min(255, (c & 255) + 70); }
    public static int mixDark(int c) { return 0xFF000000 | (c >> 16 & 255) * 3 / 5 << 16 | (c >> 8 & 255) * 3 / 5 << 8 | (c & 255) * 3 / 5; }
    private static int mix(int c) { return 0xFF000000 | ((c >> 16 & 255) + 255) / 2 << 16 | ((c >> 8 & 255) + 255) / 2 << 8 | ((c & 255) + 255) / 2; }
    /** El dibujo en sí, en coordenadas locales mirando a la derecha. */
    public static void drawPose(Gfx g, View v, int ar, int c, int unused) {
        if (v.ball) { drawBall(g, v, ar, c); return; }
        float e = (v.assemble / HOLD * HOLD) / 30f; e *= e;
        boolean on = e < .1f, glow = v.shotT > 6;
        float px = 0, py = v.pel, lr = (float) Math.toRadians(v.lean), sn = (float) Math.sin(lr), cs = (float) Math.cos(lr);
        float cx = px + sn * SHOULDER, cy = py - cs * SHOULDER, sp = e * 16;
        float ffx = cx + cs * 4.6f, ffy = cy + sn * 4.6f + 1, nx = cx - cs * 4.6f, ny = cy - sn * 4.6f + 1;
        arm(g, ffx, ffy, v.b1, v.b2, (ar >> Robot.BR_F & 1) != 0 && on, false, false, "lejano");
        if ((ar >> Robot.SH_F & 1) != 0) { g.layer("hombro_lejano"); g.circle(ffx + sp * .8f, ffy, 3, WHITE, INK, SW); g.endLayer(); }
        leg(g, px + 2, py + 1, v.fx, v.fy - 1 + v.gFront, (ar >> Robot.TH_F & 1) != 0 && on, (ar >> Robot.SN_F & 1) != 0 && on, c, "lejana");
        g.save(); g.translate(px, py); g.rotate(lr); g.translate(0, -TORSO_LEN);
        g.layer("hueso_torso"); g.rrect(-1.2f, 8, 2.4f, 7, .6f, INK, 0, 0); g.poly(TORSO, INK, 0, 0); g.poly(PELVIS, INK, 0, 0);
        g.poly(NECK, INK, 0, 0); // cuello inclinado
        if ((ar >> Robot.BELLY & 1) != 0) { g.layer("abdomen"); g.save(); g.translate(0, sp * .3f); limb(g, BELLY, c); g.restore(); }
        boolean lit = v.shotT > 6; // el destello aclara el cuerpo en los primeros cuadros
        if ((ar >> Robot.CHEST & 1) != 0) { g.layer("pecho"); g.save(); g.translate(0, -sp * .3f); if (lit) g.grad(CHEST, c, 0xFFE8E2DC, -6, 6, INK, SW); else limb(g, CHEST, c); g.restore(); }
        if ((ar >> Robot.HELMET & 1) == 0 || exportAll) { g.layer("craneo"); g.circle(0, -5.5f, 3.4f, INK, 0, 0); }
        if ((ar >> Robot.HELMET & 1) != 0) { g.layer("cabeza"); g.save(); g.translate(0, -sp); g.rotate(-lr * .8f); if (lit) g.grad(HEAD, HEADC, WHITE, -7, 8, INK, SW); else limb(g, HEAD, HEADC); g.restore(); }
        g.endLayer(); g.restore();
        leg(g, px - 2, py + 1, v.bx, v.by + v.gBack, (ar >> Robot.TH_B & 1) != 0 && on, (ar >> Robot.SN_B & 1) != 0 && on, c, "cercana");
        if ((ar >> Robot.HIP & 1) != 0) { g.layer("pelvis"); g.save(); g.translate(px, py); g.rotate(lr); g.translate(0, sp * .5f); limb(g, HIPS, PADC); g.restore(); g.endLayer(); }
        arm(g, nx, ny, v.a1, v.a2, (ar >> Robot.BR_B & 1) != 0 && on, v.open, glow, "cercano");
        if ((ar >> Robot.SH_B & 1) != 0) { g.layer("hombro_cercano"); g.circle(nx - sp * .8f, ny, 3.6f, WHITE, INK, SW); g.endLayer(); }
        if (exportAll && v.open) { float d = UA + FA + 5; g.layer("punto_disparo"); g.circle(nx + (float) Math.cos(Math.toRadians(v.a1)) * d, ny + (float) Math.sin(Math.toRadians(v.a1)) * d, 1.2f, 0xFF00E5FF, 0, 0); g.endLayer(); }
        if (v.shotT > 0 && v.open) { float d = UA + FA + 7; muzzle(g, nx + (float) Math.cos(Math.toRadians(v.a1)) * d, ny + (float) Math.sin(Math.toRadians(v.a1)) * d, 2 - (v.shotT - 1) / 4); }
    }
    /** Doble salto: se enrolla como un chanchito de tierra, con el caparazón del color del traje. */
    private static void drawBall(Gfx g, View v, int ar, int c) {
        boolean shell = (ar >> Robot.CHEST & 1) != 0;
        g.save(); g.translate(0, -16); g.rotate((v.clock / HOLD) * 1.2f);
        g.layer("pecho"); g.circle(0, 0, 12, shell ? c : INK, INK, SW);
        for (int i = -1; i <= 1; i++) { float o = i * 5.5f, h = (float) Math.sqrt(144 - o * o) - .8f; // bandas diagonales del caparazón
            float ux = .7071f, uy = -.7071f, nx = .7071f, ny = .7071f;
            g.line(nx * o - ux * h, ny * o - uy * h, nx * o + ux * h, ny * o + uy * h, INK, SW); }
        if ((ar >> Robot.HELMET & 1) != 0) { g.layer("cabeza"); g.circle(6.2f, -7, 4.6f, HEADC, INK, SW); }
        g.layer("hombro_cercano"); g.circle(-5.4f, 8, 4.2f, WHITE, INK, SW);
        g.endLayer(); g.restore();
    }
    static final float[] FLAME = {0,-2.5f, 5,-4.5f, 7,-2, 11,-3.5f, 10,-1, 17,0, 10,1.2f, 11,3.4f, 6,2, 3,4, 0,2.5f};
    /** Destello de luz frente a la mano: fogonazo, luz estirada y llama naranja que se apaga. */
    private static void muzzle(Gfx g, float x, float y, int frame) {
        g.save(); g.translate(x, y); g.additive(true);
        if (frame <= 0) { g.glow(3, 0, 13, 0xEEFFF2A0); g.glow(3, 0, 5, 0xFFFFFFFF); }
        else if (frame == 1) { g.save(); g.translate(8, 0); g.scale(2.6f, 1); g.glow(0, 0, 6, 0xEEFFF06A); g.glow(0, 0, 2.6f, 0xFFFFFFFF); g.restore(); }
        else { g.glow(6, 0, 8, 0xCCFF9A1A); g.poly(FLAME, 0xCCFFB31A, 0, 0); }
        g.additive(false); g.restore();
    }
    /** Cuadros de la plantilla: nombre de archivo y pose. */
    public static final String[] FRAMES = {"de_pie_1", "de_pie_2", "correr_1", "correr_2", "correr_3", "correr_4", "preparar_1", "preparar_2", "disparo",
        "salto_subir", "salto_caer", "salto_disparo", "doble_salto", "estirar", "barrida_1", "barrida_2", "pared", "gancho", "lanzado"};
    public static View frame(String n) {
        View v = new View();
        switch (n) {
            case "de_pie_1": pose(v, IDLE_A); break; case "de_pie_2": pose(v, IDLE_B); break;
            case "correr_1": pose(v, RUN_F[0]); break; case "correr_2": pose(v, RUN_F[1]); break; case "correr_3": pose(v, RUN_F[2]); break; case "correr_4": pose(v, RUN_F[3]); break;
            case "preparar_1": pose(v, MID_F); break; case "preparar_2": pose(v, MID2_F); break;
            case "disparo": pose(v, STANCE_F); v.open = true; break;
            case "salto_subir": pose(v, UP_F); break; case "salto_caer": case "lanzado": pose(v, DOWN_F); break;
            case "salto_disparo": pose(v, UP_F); v.a1 = 0; v.a2 = 0; v.b1 = 105; v.b2 = 95; v.open = true; break;
            case "doble_salto": v.ball = true; break; case "estirar": pose(v, STRETCH_F); break;
            case "barrida_1": pose(v, SLIDE_A); break; case "barrida_2": pose(v, SLIDE_B); break;
            case "pared": pose(v, WALL_F); break; case "gancho": pose(v, HOOK_F); break;
            default: throw new IllegalArgumentException(n);
        }
        return v;
    }
    public static View poster() { View v = new View(); pose(v, STANCE_F); v.open = true; return v; }
    public static void assemble(View v) { v.assemble = 30; }
}
