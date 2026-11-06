package com.wap.render;
import com.wap.core.*;
import java.util.ArrayList;
import java.util.Random;
/** Dibuja el mundo: mapa por baldosas, robots, proyectiles, partículas e interfaz de partida. */
public final class Renderer {
    public static final int PAPER = 0xFFFFFFFF, GRID = 0xFFDCE7F2, INK = RobotArt.INK;
    static final int DIRT = 0xFF8214FF, DIRT2 = 0xFF4A00A8;
    static final int BG = 256; final int[] bg = new int[BG * BG]; boolean bgNew = true;
    void paintBg() {
        for (int y = 0; y < BG; y++) for (int x = 0; x < BG; x++) {
            int c = 0xFFC7D2E0, gx = x % 128, gy = y % 128;
            if (gx < 2 || gy < 2) c = 0xFFADBACC; else if (gx == 2 || gy == 2) c = 0xFFDCE4EE;
            else if (gx > 20 && gx < 108 && gy > 20 && gy < 108) c = 0xFFBDC9D9;
            if (y > 150 && y < 178) c = (x / 12 + y / 12) % 2 == 0 ? 0xFFB3C0D2 : 0xFFA8B6CA;
            if ((gx - 10) * (gx - 10) + (gy - 10) * (gy - 10) < 7) c = 0xFF9AA9BE;
            bg[y * BG + x] = c;
        }
    }
    static final class P { float x, y, vx, vy, life, max, rot, vr, size, g = .2f; int kind, slot, color; boolean ground; }
    public final RobotArt.View[] views = {new RobotArt.View(), new RobotArt.View()};
    final ArrayList<P> ps = new ArrayList<>(); final ArrayList<float[]> beams = new ArrayList<>(); final ArrayList<float[]> booms = new ArrayList<>();
    final Random r = new Random(7); final int[] px = new int[Terrain.TILE * Terrain.TILE];
    public float camX = 900, camY = 500, shake; public int zoom = 3; int vx0, vy0, vw, vh; boolean first = true;

    public void reset(World w) { ps.clear(); beams.clear(); booms.clear(); first = true; for (int i = 0; i < Terrain.TW * Terrain.TH; i++) w.t.dirty[i] = true; bgNew = true; java.util.Arrays.fill(loaded, false); }
    static int hash(int x, int y) { int h = x * 374761393 + y * 668265263; h = (h ^ h >> 13) * 1274126177; return h ^ h >> 16; }
    final boolean[] loaded = new boolean[Terrain.TW * Terrain.TH];
    static final int M = 18, N = Terrain.TILE + 2 * M, SEAM = 0xFF141414;
    final int[] dIn = new int[N * N], dOut = new int[N * N]; final float[] soft = new float[N * N]; final byte[] mats = new byte[N * N];
    static final int[] BAYER = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};
    static boolean air(Terrain t, int x, int y) { int k = t.at(x, y); return k == 0 || k == Terrain.DEBRIS; }
    static int tone(int c, float m, int add) {
        return 0xFF000000 | Math.min(255, (int) ((c >> 16 & 255) * m) + add) << 16 | Math.min(255, (int) ((c >> 8 & 255) * m) + add) << 8 | Math.min(255, (int) ((c & 255) * m) + add);
    }
    static final float LX = -.476f, LY = -.667f, LZ = .571f;
    private static int group(int k) { return k == Terrain.PANEL || k == Terrain.DECOR || k == Terrain.STEEL ? 1 : k; }
    private static void chamfer(int[] d) {
        for (int y = 1; y < N - 1; y++) for (int x = 1; x < N - 1; x++) { int i = y * N + x, v = d[i];
            v = Math.min(v, d[i - 1] + 3); v = Math.min(v, d[i - N] + 3); v = Math.min(v, d[i - N - 1] + 4); d[i] = Math.min(v, d[i - N + 1] + 4); }
        for (int y = N - 2; y > 0; y--) for (int x = N - 2; x > 0; x--) { int i = y * N + x, v = d[i];
            v = Math.min(v, d[i + 1] + 3); v = Math.min(v, d[i + N] + 3); v = Math.min(v, d[i + N + 1] + 4); d[i] = Math.min(v, d[i + N - 1] + 4); }
    }
    private static float radius(int k) {
        switch (k) { case Terrain.VENT: return 15; case Terrain.CRATE: return 4.5f; case Terrain.METAL: return 5; case Terrain.COMPUTER: return 3.5f; case Terrain.STONE: return 7; default: return 6.5f; }
    }
    /** Relieve iluminado por píxel: como se calcula desde el mapa, los cráteres también quedan con volumen. */
    void fillTile(Terrain t, int tx, int ty) {
        final int S = Terrain.TILE, ox = tx * S - M, oy = ty * S - M, INF = 9999;
        for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) {
            int k = t.at(ox + x, oy + y); if (k == Terrain.DEBRIS) k = 0;
            mats[y * N + x] = (byte) k;
        }
        for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) {
            int i = y * N + x, k = mats[i]; dOut[i] = k == 0 ? INF : 0;
            if (k == 0) { dIn[i] = 0; continue; }
            int wx = ox + x, wy = oy + y; boolean e = x == 0 || y == 0 || x == N - 1 || y == N - 1;
            if (!e) { int g = group(k); e = group(mats[i - 1]) != g || group(mats[i + 1]) != g || group(mats[i - N]) != g || group(mats[i + N]) != g; }
            if (x == 0 || y == 0 || x == N - 1 || y == N - 1) e = false;
            if (!e && wx >= 0 && wy >= 0 && wx < Terrain.W && wy < Terrain.H) {
                e = t.tex[wy * Terrain.W + wx] == SEAM;
                if ((k == Terrain.CRATE || k == Terrain.METAL) && (wx % 32 == 0 || wy % 32 == 0)) e = true;
            }
            dIn[i] = e ? 0 : INF;
        }
        chamfer(dIn); chamfer(dOut);
        for (int y = 1; y < N - 1; y++) for (int x = 1; x < N - 1; x++) { int i = y * N + x, s = 0;
            for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) s += dIn[i + a * N + b];
            soft[i] = s / 27f; }
        int q = 0;
        for (int wy = oy + M; wy < oy + M + S; wy++) for (int wx = ox + M; wx < ox + M + S; wx++) {
            int c = 0, x = wx - ox, y = wy - oy, i = y * N + x, raw = t.at(wx, wy);
            if (wx >= Terrain.W || wy >= Terrain.H) { px[q++] = 0; continue; }
            if (raw == Terrain.DEBRIS) c = t.tex[wy * Terrain.W + wx] & 0xFFFFFF | 0x80000000;
            else if (raw != 0) {
                int base = t.tex[wy * Terrain.W + wx];
                float R = radius(raw), d = soft[i], k = 1 - Math.min(d, R) / R, gx = soft[i + 1] - soft[i - 1], gy = soft[i + N] - soft[i - N], g = (float) Math.sqrt(gx * gx + gy * gy) + .0001f;
                float nz = (float) Math.sqrt(Math.max(0, 1 - k * k)), nx = -gx / g * k, ny = -gy / g * k;
                float lv = Math.max(0, nx * LX + ny * LY + nz * LZ) + (BAYER[(wy & 3) * 4 + (wx & 3)] - 7.5f) * .018f;
                boolean edge = air(t, wx - 1, wy) || air(t, wx + 1, wy) || air(t, wx, wy - 1) || air(t, wx, wy + 1);
                if (edge || base == SEAM) c = INK; // contorno de 1 píxel
                else c = lv > .82f ? tone(base, 1.28f, 18) : lv < .3f ? tone(base, .62f, 0) : lv < .45f ? tone(base, .8f, 0) : base;
            } else {
                int sx = Math.max(0, x - 7), sy = Math.max(0, y - 10); float o = dOut[sy * N + sx] / 3f;
                if (o < 5) c = 0x48100A28; else if (o < 9 && ((wx + wy) & 1) == 0) c = 0x48100A28; // sombra en dos tramos, con trama
            }
            px[q++] = c;
        }
    }
    P part(int kind, float x, float y, float vx, float vy, float life, float size, int color) {
        if (ps.size() > 500) ps.remove(0);
        P p = new P(); p.kind = kind; p.x = x; p.y = y; p.vx = vx; p.vy = vy; p.life = p.max = life; p.size = size; p.color = color;
        p.rot = r.nextFloat() * 6.28f; p.vr = (r.nextFloat() - .5f) * .5f; ps.add(p); return p;
    }
    static final int LABEL = 48, SQUARE = 0, SMOKE = 1, ARMOR = 2, BONE = 3, FIRE = 4, CHUNK = 5;
    public void consume(World w, boolean reduceMotion) {
        for (Fx f : w.fx) switch (f.type) {
            case Fx.EXPLODE: {
                booms.add(new float[]{f.x, f.y, f.a, 0, r.nextFloat() * 6});
                if (!reduceMotion) shake = Math.min(14, shake + f.a * .14f);
                int n = (int) Math.min(40, f.a * .7f);
                for (int i = 0; i < n; i++) { float a = r.nextFloat() * 6.28f, v = r.nextFloat() * 5 + 1; part(SQUARE, f.x, f.y, (float) Math.cos(a) * v, (float) Math.sin(a) * v - 1, 30 + r.nextInt(30), 1.5f + r.nextFloat() * 2, r.nextBoolean() ? DIRT : DIRT2); }
                break; }
            case Fx.CHIPS: for (int i = 0; i < f.a; i++) part(SQUARE, f.x, f.y, (r.nextFloat() - .5f) * 4, -r.nextFloat() * 3, 24, 1.6f, DIRT2); break;
            case Fx.SMOKE: { P p = part(f.a > 0 ? FIRE : SMOKE, f.x, f.y, 0, -.2f, f.a > 0 ? 14 : 24, f.a > 0 ? 3 : 3.5f, 0xFF9098A8); p.g = 0; break; }
            case Fx.FLASH: { RobotArt.View v = views[f.who]; v.label = Wpn.ALL[(int) f.b].name; v.labelT = LABEL; if (v.shotT <= 0) v.shotT = 12; break; }
            case Fx.PROP: {
                int kind = (int) f.c, n = (int) (f.a * f.b / 110) + 6;
                int[] tone = kind == Terrain.METAL ? new int[]{0xFF8D99A8, 0xFFB4BFCC} : kind == Terrain.STONE ? new int[]{0xFF9C9488, 0xFF7A7369} : kind == Terrain.COMPUTER ? new int[]{0xFF2C323F, 0xFF46F08A} : new int[]{0xFFCB8C3C, 0xFF96601F};
                for (int i = 0; i < n; i++) {
                    float a = r.nextFloat() * 6.28f, v = 1.5f + r.nextFloat() * 4;
                    P p = part(CHUNK, f.x + r.nextFloat() * f.a, f.y + r.nextFloat() * f.b, (float) Math.cos(a) * v, (float) Math.sin(a) * v - 2.5f, 110 + r.nextInt(70), 3 + r.nextFloat() * 5, tone[i % 2]); p.slot = r.nextInt(99);
                }
                if (kind == Terrain.COMPUTER) for (int i = 0; i < 14; i++) part(SQUARE, f.x + 32, f.y + 30, (r.nextFloat() - .5f) * 7, -r.nextFloat() * 5, 26, 1.5f, 0xFFFFE23A);
                break; }
            case Fx.CHARGE: { RobotArt.View v = views[f.who]; v.label = f.a > 0 ? "CARGADO" : Wpn.ALL[(int) f.b].name; v.labelT = LABEL; break; }
            case Fx.WALL: { P p = part(SMOKE, f.x, f.y, 0, -.3f, 14, 2, 0xFFBFC6D2); p.g = 0; break; }
            case Fx.HOOK: if (f.a == 0) { RobotArt.View v = views[f.who]; v.label = "GANCHO"; v.labelT = LABEL; } break;
            case Fx.HIT: for (int i = 0; i < 5; i++) part(SQUARE, f.x, f.y, (r.nextFloat() - .5f) * 5, (r.nextFloat() - .5f) * 5, 14, 1.3f, 0xFFFFC020); break;
            case Fx.ARMOR: { P p = part(ARMOR, f.x, f.y, f.c, f.d, 170, 1, RobotArt.COLOR[f.who]); p.slot = (int) f.a; p.g = .22f; break; }
            case Fx.DEATH: {
                for (int k : new int[]{0, 0, 0, 0, 1, 1, 1, 1, 2, 3, 3, 4, 4}) {
                    float a = r.nextFloat() * 6.28f, v = 2 + r.nextFloat() * 5;
                    P p = part(BONE, f.x + r.nextFloat() * 16 - 8, f.y + r.nextFloat() * 30 - 15, (float) Math.cos(a) * v, (float) Math.sin(a) * v - 3, 150, 1, INK); p.slot = k;
                }
                booms.add(new float[]{f.x, f.y, 36, 0, 0}); break; }
            case Fx.BEAM: beams.add(new float[]{f.x, f.y, f.c, f.d, f.b == 1 ? 12 : 3, f.b}); break;
            case Fx.JUMP: for (int i = 0; i < 6; i++) { P p = part(SMOKE, f.x + r.nextFloat() * 14 - 7, f.y - 1, (r.nextFloat() - .5f) * 2, -.2f, 16, 2.2f, 0xFFBFC6D2); p.g = 0; } break;
            case Fx.SLIDE: { P p = part(SMOKE, f.x, f.y - 2, -f.a * .6f, -.3f, 14, 2.4f, 0xFFBFC6D2); p.g = 0; break; }
            case Fx.SPAWN: RobotArt.assemble(views[(int) f.a]); break;
            default:
        }
        w.fx.clear();
    }
    void stepFx(World w) {
        for (int i = ps.size() - 1; i >= 0; i--) {
            P p = ps.get(i);
            if (--p.life <= 0) { ps.remove(i); continue; }
            if (p.ground) continue;
            p.vy += p.g; float nx = p.x + p.vx, ny = p.y + p.vy;
            if (p.kind == SMOKE || p.kind == FIRE) { p.x = nx; p.y = ny; continue; }
            if (w.t.solid((int) nx, (int) ny)) {
                if (p.kind == ARMOR || p.kind == BONE || p.kind == CHUNK) { if (w.t.solid((int) p.x, (int) ny)) { p.vy *= -.35f; p.vx *= .6f; if (Math.abs(p.vy) < 1) p.ground = true; } else p.vx *= -.5f; }
                else ps.remove(i);
                continue;
            }
            p.x = nx; p.y = ny; p.rot += p.vr;
        }
        for (int i = booms.size() - 1; i >= 0; i--) { float[] b = booms.get(i); if (++b[3] > 14) booms.remove(i); }
        for (int i = beams.size() - 1; i >= 0; i--) if (--beams.get(i)[4] <= 0) beams.remove(i);
        shake *= .86f;
    }
    public void tick(World w) {
        stepFx(w);
        for (int i = 0; i < 2; i++) {
            Robot o = w.robots[i]; RobotArt.update(views[i], o, w);
            float ch = o.chargeLevel();
            if (o.alive() && ch > 0 && w.tick % 3 == 0) {
                float a = r.nextFloat() * 6.28f, d = 34; P p = part(SQUARE, o.px + (float) Math.cos(a) * d, o.py - 27 + (float) Math.sin(a) * d, -(float) Math.cos(a) * 2.6f, -(float) Math.sin(a) * 2.6f, 12, 1.8f, 0xFFFFE23A); p.g = 0;
            }
        }
    }
    void camera(World w, int who) {
        Robot me = w.robots[who];
        float tx = me.px + me.facing * vw * .1f + me.vx * 12, ty = me.py - 36;
        if (first) { camX = tx; camY = ty; first = false; }
        camX += (tx - camX) * .1f; camY += (ty - camY) * .12f;
        camX = Math.max(vw / 2f, Math.min(Terrain.W - vw / 2f, camX)); camY = Math.max(vh / 2f, Math.min(Terrain.H - vh / 2f, camY));
    }
    /** El mundo se dibuja a 1 píxel por unidad y se amplía por un factor entero: todo queda en pixel art. */
    public void drawWorld(Gfx g, World w, int who) {
        zoom = Math.max(1, Math.round(g.h() / 280f)); vw = (g.w() + zoom - 1) / zoom; vh = (g.h() + zoom - 1) / zoom;
        camera(w, who);
        int x0 = Math.round(camX - vw / 2f), y0 = Math.round(camY - vh / 2f); vx0 = x0; vy0 = y0;
        g.clear(0xFFC7D2E0);
        g.beginPixels(vw, vh);
        g.save();
        int sx = shake > .6f ? Math.round((r.nextFloat() - .5f) * shake) : 0, sy = shake > .6f ? Math.round((r.nextFloat() - .5f) * shake) : 0;
        g.translate(-x0 + sx, -y0 + sy);
        if (bgNew) paintBg();
        int bx0 = Math.round(x0 * .55f), by0 = Math.round(y0 * .55f);
        for (int q = Math.floorDiv(by0, BG); q * BG < by0 + vh; q++) for (int k = Math.floorDiv(bx0, BG); k * BG < bx0 + vw; k++) { g.tile(-1, bg, BG, bgNew, x0 + k * BG - bx0, y0 + q * BG - by0); bgNew = false; }
        for (Bonus b : w.bonus) crate(g, b.x, b.y, b.kind);
        for (Shot s : w.shots) shot(g, s);
        int S = Terrain.TILE, budget = 3; Terrain t = w.t;
        for (int ty = Math.max(0, (int) (y0 / S)); ty <= Math.min(Terrain.TH - 1, (int) ((y0 + vh) / S)); ty++)
            for (int tx = Math.max(0, (int) (x0 / S)); tx <= Math.min(Terrain.TW - 1, (int) ((x0 + vw) / S)); tx++) {
                int id = ty * Terrain.TW + tx; boolean d = t.dirty[id] && (!loaded[id] || budget-- > 0); // como mucho 3 relieves por cuadro
                if (d) { fillTile(t, tx, ty); t.dirty[id] = false; loaded[id] = true; }
                g.tile(id, px, S, d, tx * S, ty * S);
            }
        for (Prop p : w.props) if (!p.gone && p.x < x0 + vw && p.x + p.w > x0 && p.y < y0 + vh && p.y + p.h > y0) damage(g, p);
        for (P p : ps) if (p.kind == CHUNK) {
            g.save(); g.translate(p.x, p.y); g.rotate(p.rot); float z = p.size, k = p.slot % 3 * .2f + .6f;
            g.poly(new float[]{-z, -z * k, z * .7f, -z, z, z * .4f, -z * .3f, z * k}, p.color, 0x66141414, .5f); g.restore();
        }
        for (P p : ps) if (p.kind == ARMOR || p.kind == BONE) { g.save(); g.translate(p.x, p.y); g.rotate(p.rot); if (p.kind == ARMOR) RobotArt.piece(g, p.slot, p.color); else RobotArt.bonePiece(g, p.slot); g.restore(); }
        for (int i = 0; i < 2; i++) {
            Robot o = w.robots[i]; if (!o.alive()) continue;
            if (o.rope != 0) { float hx = o.px + o.facing * 12, hy = o.py - 33; g.line(hx, hy, o.rx, o.ry, INK, 1.1f); g.circle(o.rx, o.ry, 2.4f, 0xFFFFFFFF, INK, .7f); }
            RobotArt.draw(g, views[i], o, RobotArt.COLOR[i]);
        }
        for (P p : ps) switch (p.kind) {
            case SQUARE: g.rect(p.x, p.y, p.size, p.size, p.color); break;
            case SMOKE: { float k = p.life / p.max; g.circle(p.x, p.y, p.size * (2 - k), 0, p.color, .7f); break; }
            case FIRE: g.circle(p.x, p.y, p.size * p.life / p.max, 0xFFFFA010, 0, 0); break;
            default:
        }
        for (float[] b : beams) { boolean gs = b[5] == 1; g.line(b[0], b[1], b[2], b[3], gs ? 0xFF1266FF : 0xFFFF2040, gs ? b[4] * .35f : 1.6f); g.line(b[0], b[1], b[2], b[3], 0xFFFFFFFF, gs ? b[4] * .12f : .6f); }
        for (float[] b : booms) boom(g, b);
        g.restore();
        g.endPixels(0, 0, zoom);
        labels(g, w, x0, y0);
        pointer(g, w, who, x0, y0);
    }
    /** Grietas según la resistencia que le queda al objeto. */
    void damage(Gfx g, Prop p) {
        if (p.hitT > 0) g.rect(p.x, p.y, p.w, p.h, 0x70FFFFFF);
        float k = 1 - p.hp / p.max; if (k <= .05f) return;
        int n = 1 + (int) (k * 5), h = p.x * 31 + p.y;
        for (int i = 0; i < n; i++) {
            h = h * 1103515245 + 12345;
            float x = p.x + (h >>> 8 & 255) / 255f * p.w, y = p.y + (h >>> 16 & 255) / 255f * p.h, a = (h >>> 3 & 63) / 10f, l = p.w * (.25f + k * .3f);
            float mx = x + (float) Math.cos(a) * l * .5f, my = y + (float) Math.sin(a) * l * .5f, ex = mx + (float) Math.cos(a + .8f) * l * .5f, ey = my + (float) Math.sin(a + .8f) * l * .5f;
            mx = Math.max(p.x, Math.min(p.x + p.w, mx)); my = Math.max(p.y, Math.min(p.y + p.h, my)); ex = Math.max(p.x, Math.min(p.x + p.w, ex)); ey = Math.max(p.y, Math.min(p.y + p.h, ey));
            g.line(x, y, mx, my, 0xDD141414, 1); g.line(mx, my, ex, ey, 0xDD141414, .8f);
        }
    }
    /** Nombres sobre los personajes, en la pantalla para que se lean nítidos. */
    void labels(Gfx g, World w, int x0, int y0) {
        for (int i = 0; i < 2; i++) {
            Robot o = w.robots[i]; RobotArt.View v = views[i];
            if (!o.alive() || v.labelT <= 0) continue;
            float z = 8 * zoom, tw = g.textW(v.label, z), ly = (o.py - o.h - 9 - (LABEL - v.labelT) * .16f - y0) * zoom, lx = Math.max(tw / 2 + 5 * zoom, Math.min(g.w() - tw / 2 - 5 * zoom, (o.px - x0) * zoom));
            int a = Math.min(255, v.labelT * 18) << 24;
            g.rect(lx - tw / 2 - 3 * zoom, ly - 9 * zoom, tw + 6 * zoom, 12 * zoom, a | 0x141414);
            g.text(v.label, lx, ly, z, a | 0xFFFFFF, 0);
        }
    }
    void boom(Gfx g, float[] b) {
        float k = b[3] / 15, R = b[2] * (.45f + .75f * (float) Math.sqrt(k)); int n = 11; float[] q = new float[n * 4];
        for (int i = 0; i < n * 2; i++) {
            float a = b[4] + i * 3.14159f / n, d = i % 2 == 0 ? R : R * .62f;
            q[i * 2] = b[0] + (float) Math.cos(a) * d; q[i * 2 + 1] = b[1] + (float) Math.sin(a) * d;
        }
        int a = (int) (255 * (1 - k * k)) << 24;
        g.poly(q, a | (k < .35f ? 0xFFFFFF : k < .7f ? 0xFFC61A : 0xFF3A10), a | 0x141414, 1.1f);
    }
    void crate(Gfx g, float x, float y, int kind) {
        g.rrect(x - 8, y - 15, 16, 15, 2, 0xFFFFFFFF, INK, 1);
        int c = kind == 0 ? 0xFFFF0000 : 0xFF1A1A1A;
        if (kind == 0) { g.rect(x - 1.8f, y - 12, 3.6f, 9, c); g.rect(x - 4.5f, y - 9.3f, 9, 3.6f, c); }
        else { g.rect(x - 4.5f, y - 11, 2.4f, 7.5f, c); g.rect(x - 1, y - 11, 2.4f, 7.5f, c); g.rect(x + 2.5f, y - 11, 2.4f, 7.5f, c); }
    }
    void shot(Gfx g, Shot s) {
        Wpn w = s.w; float a = (float) Math.atan2(s.vy, s.vx);
        switch (w.look) {
            case Wpn.ROCKET: g.save(); g.translate(s.x, s.y); g.rotate(a); g.poly(new float[]{-6,-2, 3,-2, 7,0, 3,2, -6,2}, 0xFFFF0000, INK, .7f); g.poly(new float[]{-6,-1.4f, -11,0, -6,1.4f}, 0xFFFFB020, 0, 0); g.restore(); break;
            case Wpn.GRENADE: g.circle(s.x, s.y, 3.1f, 0xFF2F7A3B, INK, .8f); g.rect(s.x - 1, s.y - 4.6f, 2, 2, INK); break;
            case Wpn.MINE: g.circle(s.x, s.y, 4, 0xFF333333, INK, .8f); if (s.age / 12 % 2 == 0) g.circle(s.x, s.y - 1, 1.3f, 0xFFFF2020, 0, 0); break;
            case Wpn.CRATE: crate(g, s.x, s.y + 4, 0); break;
            case Wpn.FLAME: { float k = s.life > 0 && s.w.fuse > 0 ? s.life / (float) s.w.fuse : 1; g.circle(s.x, s.y, 1.5f + 3.2f * k, k > .5f ? 0xFFFFD040 : 0xFFFF7010, 0, 0); break; }
            case Wpn.SPIKE: { for (int i = 0; i < 4; i++) { float c = i * .785f + s.age * .2f; g.line(s.x - (float) Math.cos(c) * 4.5f, s.y - (float) Math.sin(c) * 4.5f, s.x + (float) Math.cos(c) * 4.5f, s.y + (float) Math.sin(c) * 4.5f, INK, 1); } break; }
            case Wpn.DIRT: g.circle(s.x, s.y, 5, DIRT2, INK, .8f); break;
            case Wpn.LEMON: g.save(); g.translate(s.x, s.y); g.rotate(a); g.scale(1.45f, 1); g.circle(0, 0, 2.6f, 0xFFFFE23A, INK, .7f); g.restore(); break;
            case Wpn.PLASMA: {
                float d = (float) Math.hypot(s.vx, s.vy), nx = s.vx / Math.max(1, d), ny = s.vy / Math.max(1, d), p = s.age / 3 % 2 * 1.2f;
                g.circle(s.x - nx * 20, s.y - ny * 20, 3.4f, 0xFFFFA010, INK, .7f); g.circle(s.x - nx * 12, s.y - ny * 12, 5.4f, 0xFFFFC71E, INK, .7f);
                g.circle(s.x, s.y, 9.5f + p, 0xFFFFE23A, INK, 1); g.circle(s.x + nx * 1.5f, s.y + ny * 1.5f, 5.5f, 0xFFFFFFFF, 0, 0); break; }
            case Wpn.ORB: g.circle(s.x, s.y, 3, 0xFFFFFFFF, 0xFF1266FF, 1.3f); break;
            case Wpn.NUKE: g.save(); g.translate(s.x, s.y); g.rotate(a); g.poly(new float[]{-8,-3.4f, 5,-3.4f, 9,0, 5,3.4f, -8,3.4f}, 0xFFFFD020, INK, .8f); g.circle(-1, 0, 1.8f, INK, 0, 0); g.restore(); break;
            case Wpn.SPARK: g.circle(s.x, s.y, 1.6f, 0xFFFFC020, INK, .4f); break;
            default: g.line(s.x, s.y, s.x - s.vx * .8f, s.y - s.vy * .8f, INK, 1.4f);
        }
    }
    void pointer(Gfx g, World w, int who, int x0, int y0) {
        Robot e = w.robots[1 - who]; if (!e.alive()) return;
        float sx = (e.px - x0) * zoom, sy = (e.py - 30 - y0) * zoom, W = g.w(), H = g.h(), m = H * .09f;
        if (sx > 0 && sx < W && sy > 0 && sy < H) return;
        float cx = W / 2, cy = H / 2, dx = sx - cx, dy = sy - cy, k = Math.min((W / 2 - m) / Math.max(1, Math.abs(dx)), (H / 2 - m) / Math.max(1, Math.abs(dy)));
        float x = cx + dx * k, y = cy + dy * k, u = H / 360f;
        g.save(); g.translate(x, y); g.rotate((float) Math.atan2(dy, dx)); g.scale(u, u);
        g.poly(new float[]{10,0, -8,-8, -4,0, -8,8}, RobotArt.COLOR[1 - who], INK, 1.4f); g.restore();
    }
}
