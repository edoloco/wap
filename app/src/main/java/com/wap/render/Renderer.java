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
    public float camX = 900, camY = 500, shake; public float zoom = 3; float vx0, vy0, vw, vh; boolean first = true;

    public void reset(World w) { ps.clear(); beams.clear(); booms.clear(); first = true; for (int i = 0; i < Terrain.TW * Terrain.TH; i++) w.t.dirty[i] = true; bgNew = true; java.util.Arrays.fill(loaded, false); }
    static int hash(int x, int y) { int h = x * 374761393 + y * 668265263; h = (h ^ h >> 13) * 1274126177; return h ^ h >> 16; }
    final boolean[] loaded = new boolean[Terrain.TW * Terrain.TH];
    static boolean air(Terrain t, int x, int y) { int k = t.at(x, y); return k == 0 || k == Terrain.DEBRIS || k == Terrain.DECOR; }
    static int tone(int c, float m, int add) {
        return 0xFF000000 | Math.min(255, (int) ((c >> 16 & 255) * m) + add) << 16 | Math.min(255, (int) ((c >> 8 & 255) * m) + add) << 8 | Math.min(255, (int) ((c & 255) * m) + add);
    }
    /** Plataformas planas en pixel art: color liso, contorno negro de 1 píxel y un brillo de 1 píxel arriba. */
    void fillTile(Terrain t, int tx, int ty) {
        final int S = Terrain.TILE; int q = 0;
        for (int wy = ty * S; wy < ty * S + S; wy++) for (int wx = tx * S; wx < tx * S + S; wx++) {
            int c = 0, k = t.at(wx, wy);
            if (wx >= Terrain.W || wy >= Terrain.H || k == 0 || k == Terrain.DECOR) c = 0;
            else if (Terrain.isProp(k)) c = 0;
            else if (k == Terrain.DEBRIS) c = t.tex[wy * Terrain.W + wx] & 0xFFFFFF | 0x80000000;
            else if (air(t, wx - 1, wy) || air(t, wx + 1, wy) || air(t, wx, wy - 1) || air(t, wx, wy + 1) || Terrain.isProp(t.at(wx, wy - 1))) c = INK;
            else if (!Terrain.isProp(k) && air(t, wx, wy - 2)) c = tone(t.tex[wy * Terrain.W + wx], 1.25f, 30);
            else c = t.tex[wy * Terrain.W + wx];
            px[q++] = c;
        }
    }
    P part(int kind, float x, float y, float vx, float vy, float life, float size, int color) {
        if (ps.size() > 500) ps.remove(0);
        P p = new P(); p.kind = kind; p.x = x; p.y = y; p.vx = vx; p.vy = vy; p.life = p.max = life; p.size = size; p.color = color;
        p.rot = r.nextFloat() * 6.28f; p.vr = (r.nextFloat() - .5f) * .5f; ps.add(p); return p;
    }
    static final int LABEL = 48, SQUARE = 0, SMOKE = 1, ARMOR = 2, BONE = 3, FIRE = 4, CHUNK = 5, SPARK = 6, EMBER = 7, TRAIL = 8, DUST = 9;
    public void consume(World w, boolean reduceMotion) {
        for (Fx f : w.fx) switch (f.type) {
            case Fx.EXPLODE: { // fogonazo, bola de fuego, onda roja, chispas, brasas y humo oscuro
                booms.add(new float[]{f.x, f.y, f.a, 0, r.nextFloat() * 6});
                if (!reduceMotion) shake = Math.min(14, shake + f.a * .14f);
                int n = (int) Math.min(34, 6 + f.a * .6f);
                for (int i = 0; i < n; i++) { float a = r.nextFloat() * 6.28f, v = 2 + r.nextFloat() * (3 + f.a * .12f); P p = part(SPARK, f.x, f.y, (float) Math.cos(a) * v, (float) Math.sin(a) * v - 1, 14 + r.nextInt(16), 1, i % 3 == 0 ? 0xFFFFF4B0 : 0xFFFFA22E); p.g = .12f; }
                for (int i = 0; i < n / 3; i++) { float a = r.nextFloat() * 6.28f, v = r.nextFloat() * 2.5f; P p = part(EMBER, f.x, f.y, (float) Math.cos(a) * v, (float) Math.sin(a) * v - 1.5f, 40 + r.nextInt(40), 1.5f + r.nextFloat() * 2, 0xFFFF6A1A); p.g = .05f; }
                for (int i = 0; i < 3 + n / 5; i++) { float a = r.nextFloat() * 6.28f, d = r.nextFloat() * f.a * .5f; P p = part(SMOKE, f.x + (float) Math.cos(a) * d, f.y + (float) Math.sin(a) * d, (float) Math.cos(a) * .4f, -.25f, 50 + r.nextInt(40), f.a * (.2f + r.nextFloat() * .2f), 0x7A1A1C24); p.g = 0; }
                for (int i = 0; i < n / 4; i++) { float a = r.nextFloat() * 6.28f, v = r.nextFloat() * 4 + 1; part(SQUARE, f.x, f.y, (float) Math.cos(a) * v, (float) Math.sin(a) * v - 1, 30 + r.nextInt(30), 1.5f + r.nextFloat() * 2, r.nextBoolean() ? DIRT : DIRT2); }
                break; }
            case Fx.CHIPS: for (int i = 0; i < f.a; i++) part(SQUARE, f.x, f.y, (r.nextFloat() - .5f) * 4, -r.nextFloat() * 3, 24, 1.6f, DIRT2); break;
            case Fx.SMOKE: { P p = part(f.a > 0 ? FIRE : SMOKE, f.x, f.y, 0, -.2f, f.a > 0 ? 14 : 30, f.a > 0 ? 3 : 2.5f, f.a > 0 ? 0xFFFF8A1A : 0x5A3A3E48); p.g = 0; break; }
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
            case Fx.STOMP: {
                for (int i = 0; i < 10; i++) { float a = i * .628f; P p = part(SPARK, f.x, f.y, (float) Math.cos(a) * 3, (float) Math.sin(a) * 1.6f - 1, 12, 1, 0xFFFFFFFF); p.g = 0; }
                RobotArt.View v = views[f.who]; v.label = f.a > 0 ? "¡BARRIDA!" : "¡PISADO!"; v.labelT = LABEL; break; }
            case Fx.GRAB: { RobotArt.View v = views[1 - f.who]; v.label = "¡DROP!"; v.labelT = LABEL; break; }
            case Fx.SLAM: {
                booms.add(new float[]{f.x, f.y, 14 + f.a * 14, 0, 0}); if (!reduceMotion) shake = Math.min(14, shake + 3 + f.a * 6);
                for (int i = 0; i < 12; i++) { P p = part(SPARK, f.x, f.y, (r.nextFloat() - .5f) * 7, -r.nextFloat() * 5, 16, 1, 0xFFFFFFFF); p.g = .15f; }
                break; }
            case Fx.CHARGE: { RobotArt.View v = views[f.who]; v.label = f.a > 0 ? "CARGADO" : Wpn.ALL[(int) f.b].name; v.labelT = LABEL; break; }
            case Fx.WALL: { P p = part(DUST, f.x, f.y, 0, -.3f, 14, 2, 0x88E2E7EE); p.g = 0; break; }
            case Fx.HOOK: if (f.a == 0) { RobotArt.View v = views[f.who]; v.label = "GANCHO"; v.labelT = LABEL; } break;
            case Fx.HIT: for (int i = 0; i < 7; i++) { float a = r.nextFloat() * 6.28f, v = 2 + r.nextFloat() * 3; P p = part(SPARK, f.x, f.y, (float) Math.cos(a) * v, (float) Math.sin(a) * v, 10 + r.nextInt(6), 1, 0xFFFFE9A0); p.g = .1f; } break;
            case Fx.ARMOR: { P p = part(ARMOR, f.x, f.y, f.c, f.d, 170, 1, RobotArt.COLOR[f.who]); p.slot = (int) f.a; p.g = .22f; break; }
            case Fx.DEATH: {
                for (int k : new int[]{0, 0, 0, 0, 1, 1, 1, 1, 2, 3, 3, 4, 4}) {
                    float a = r.nextFloat() * 6.28f, v = 2 + r.nextFloat() * 5;
                    P p = part(BONE, f.x + r.nextFloat() * 16 - 8, f.y + r.nextFloat() * 30 - 15, (float) Math.cos(a) * v, (float) Math.sin(a) * v - 3, 150, 1, INK); p.slot = k;
                }
                booms.add(new float[]{f.x, f.y, 36, 0, 0}); break; }
            case Fx.BEAM: beams.add(new float[]{f.x, f.y, f.c, f.d, f.b == 1 ? 12 : 3, f.b}); break;
            case Fx.JUMP: for (int i = 0; i < 6; i++) { P p = part(DUST, f.x + r.nextFloat() * 14 - 7, f.y - 1, (r.nextFloat() - .5f) * 2, -.2f, 16, 2.2f, 0x88E2E7EE); p.g = 0; } break;
            case Fx.SLIDE: { P p = part(DUST, f.x, f.y - 2, -f.a * .6f, -.3f, 14, 2.4f, 0x88E2E7EE); p.g = 0; break; }
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
            if (p.kind == SMOKE || p.kind == FIRE || p.kind == DUST || p.kind == TRAIL) { p.x = nx; p.y = ny; p.vx *= .96f; p.vy *= .96f; continue; }
            if (p.kind == SPARK || p.kind == EMBER) { if (w.t.solid((int) nx, (int) ny)) { p.vx *= -.3f; p.vy *= -.3f; } else { p.x = nx; p.y = ny; } p.vx *= .97f; continue; }
            if (w.t.solid((int) nx, (int) ny)) {
                if (p.kind == ARMOR || p.kind == BONE || p.kind == CHUNK) { if (w.t.solid((int) p.x, (int) ny)) { p.vy *= -.35f; p.vx *= .6f; if (Math.abs(p.vy) < 1) p.ground = true; } else p.vx *= -.5f; }
                else ps.remove(i);
                continue;
            }
            p.x = nx; p.y = ny; p.rot += p.vr;
        }
        for (int i = booms.size() - 1; i >= 0; i--) { float[] b = booms.get(i); if (++b[3] > BOOM_T) booms.remove(i); }
        for (int i = beams.size() - 1; i >= 0; i--) if (--beams.get(i)[4] <= 0) beams.remove(i);
        shake *= .86f;
    }
    static final int BOOM_T = 24;
    /** Estelas de luz detrás de los proyectiles. */
    void trails(World w) {
        for (Shot s : w.shots) {
            int c; float z;
            switch (s.w.look) {
                case Wpn.ROCKET: c = 0xFFFF8A2A; z = 4; if (w.tick % 2 == 0) { P q = part(SMOKE, s.x, s.y, 0, -.1f, 40, 2.2f, 0x50404650); q.g = 0; } break;
                case Wpn.NUKE: c = 0xFFFFD040; z = 6; break;
                case Wpn.ORB: c = 0xFF3FD8FF; z = 3; break;
                case Wpn.LEMON: c = 0xFFFFE23A; z = 2.5f; break;
                case Wpn.PLASMA: c = 0xFFFFC23A; z = 8; break;
                case Wpn.SPARK: c = 0xFFFF9A2A; z = 2; break;
                case Wpn.DIRT: c = 0xFFB98CFF; z = 4; break;
                default: continue;
            }
            P p = part(TRAIL, s.x, s.y, (r.nextFloat() - .5f) * .4f, (r.nextFloat() - .5f) * .4f, 18, z, c); p.g = 0;
        }
    }
    public void tick(World w) {
        trails(w);
        stepFx(w);
        for (int i = 0; i < 2; i++) {
            Robot o = w.robots[i]; RobotArt.update(views[i], o, w);
        }
    }
    void camera(World w, int who) {
        Robot me = w.robots[who];
        float tx = me.px + me.facing * vw * .1f + me.vx * 12, ty = me.py - 36;
        if (first) { camX = tx; camY = ty; first = false; }
        camX += (tx - camX) * .1f; camY += (ty - camY) * .12f;
        camX = Math.max(vw / 2f, Math.min(Terrain.W - vw / 2f, camX)); camY = Math.max(vh / 2f, Math.min(Terrain.H - vh / 2f, camY));
    }
    /** El mundo se dibuja suavizado, con zoom libre; solo la interfaz queda en pixel art. */
    public void drawWorld(Gfx g, World w, int who) {
        zoom = g.h() / 280f; vw = g.w() / zoom; vh = g.h() / zoom;
        camera(w, who);
        float x0 = camX - vw / 2f, y0 = camY - vh / 2f; vx0 = x0; vy0 = y0;
        g.clear(0xFFC7D2E0);
        g.save(); g.scale(zoom, zoom);
        float sx = shake > .3f ? (r.nextFloat() - .5f) * shake : 0, sy = shake > .3f ? (r.nextFloat() - .5f) * shake : 0;
        g.translate(-x0 + sx, -y0 + sy);
        if (bgNew) paintBg();
        float bx0 = x0 * .55f, by0 = y0 * .55f;
        for (int q = (int) Math.floor(by0 / BG); q * BG < by0 + vh; q++) for (int k = (int) Math.floor(bx0 / BG); k * BG < bx0 + vw; k++) { g.tile(-1, bg, BG, bgNew, x0 + k * BG - bx0, y0 + q * BG - by0); bgNew = false; }
        for (Bonus b : w.bonus) crate(g, b.x, b.y, b.kind);
        for (Shot s : w.shots) shot(g, s);
        int S = Terrain.TILE, budget = 3; Terrain t = w.t;
        for (int ty = Math.max(0, (int) (y0 / S)); ty <= Math.min(Terrain.TH - 1, (int) ((y0 + vh) / S)); ty++)
            for (int tx = Math.max(0, (int) (x0 / S)); tx <= Math.min(Terrain.TW - 1, (int) ((x0 + vw) / S)); tx++) {
                int id = ty * Terrain.TW + tx; boolean d = t.dirty[id] && (!loaded[id] || budget-- > 0); // como mucho 3 relieves por cuadro
                if (d) { fillTile(t, tx, ty); t.dirty[id] = false; loaded[id] = true; }
                g.tile(id, px, S, d, tx * S, ty * S);
            }
        for (Prop p : w.props) if (!p.gone && p.x < x0 + vw && p.x + p.w > x0 && p.y < y0 + vh && p.y + p.h > y0) { prop(g, p); damage(g, p); }
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
        for (P p : ps) {
            float k = p.life / p.max;
            if (p.kind == SQUARE) g.rect(p.x, p.y, p.size, p.size, p.color);
            else if (p.kind == SMOKE) g.circle(p.x, p.y, p.size * (1.6f - .8f * k), (int) ((p.color >>> 24) * Math.min(1, k * 1.6f)) << 24 | p.color & 0xFFFFFF, 0, 0);
            else if (p.kind == DUST) g.circle(p.x, p.y, p.size * (2 - k), (int) ((p.color >>> 24) * k) << 24 | p.color & 0xFFFFFF, 0, 0);
        }
        g.additive(true); // de aquí en adelante, luces
        for (P p : ps) {
            float k = p.life / p.max;
            switch (p.kind) {
                case FIRE: g.glow(p.x, p.y, p.size * 2.2f * k + 1, alpha(p.color, k)); break;
                case TRAIL: g.glow(p.x, p.y, p.size * (.4f + .8f * k), alpha(p.color, k * .9f)); break;
                case EMBER: g.glow(p.x, p.y, p.size * 2, alpha(p.color, k)); g.rect(p.x - .5f, p.y - .5f, 1, 1, alpha(0xFFFFE0A0, k)); break;
                case SPARK: g.line(p.x, p.y, p.x - p.vx * 1.8f, p.y - p.vy * 1.8f, alpha(p.color, k), .9f); g.glow(p.x, p.y, 3, alpha(p.color, k * .6f)); break;
                default:
            }
        }
        for (float[] b : beams) {
            boolean gs = b[5] == 1; float k = gs ? b[4] / 12 : b[4] / 3; int c = gs ? 0xFF3F8BFF : 0xFFFF2A4A;
            g.line(b[0], b[1], b[2], b[3], alpha(c, .3f * k), gs ? 9 * k : 5); g.line(b[0], b[1], b[2], b[3], alpha(c, .8f * k), gs ? 4 * k : 2); g.line(b[0], b[1], b[2], b[3], alpha(0xFFFFFFFF, k), gs ? 1.6f * k : .8f);
            g.glow(b[2], b[3], gs ? 14 : 7, alpha(c, k));
        }
        for (float[] b : booms) boom(g, b);
        for (Shot sh : w.shots) shotLight(g, sh);
        g.additive(false);
        g.restore();
        lastW = w; lastWho = who;
    }
    World lastW; int lastWho;
    /** Nombres y flecha del rival, dibujados en la capa de interfaz (pixel art) de escala s. */
    public void overlay(Gfx g, int s) {
        if (lastW == null) return;
        labels(g, lastW, s); pointer(g, lastW, lastWho, s);
    }
    static final int OUT = INK; static final float OW = 1.1f;
    /** Objetos del escenario dibujados en vector, encima del mapa. */
    void prop(Gfx g, Prop p) {
        float x = p.x, y = p.y, w = p.w, h = p.h;
        switch (p.kind) {
            case Terrain.COMPUTER: {
                g.rrect(x + .5f, y + .5f, w - 1, h - 1, 5, 0xFF6B6B6B, OUT, OW);
                g.rrect(x + 3, y + 3, w - 6, h - 6, 4, 0xFF5A5A5A, 0, 0);
                g.rrect(x + 6, y + 6, w - 12, 22, 2, 0xFF8A8A8A, OUT, OW * .8f);
                g.rect(x + 9, y + 9, w - 18, 16, 0xFF4E4E4E);
                float[] q = new float[22]; int ph = (p.x / 7 + (int) (p.hp * 3)) % 9;
                for (int i = 0; i < 11; i++) { q[i * 2] = x + 10 + i * (w - 20) / 10f; q[i * 2 + 1] = y + 17 + (float) Math.sin(i * 1.3 + ph) * 4 * (i % 3 == 1 ? 1.4f : .7f); }
                for (int i = 0; i < 10; i++) g.line(q[i * 2], q[i * 2 + 1], q[i * 2 + 2], q[i * 2 + 3], 0xFF2EE85A, 1.1f);
                int[] tone = {0xFFFFE23A, 0xFFFF2FD2, 0xFF2FC6FF, 0xFF3DDC97};
                for (int i = 0; i < 4; i++) { g.rect(x + 25 + i * 4, y + 33, 3, 3, tone[i]); g.rect(x + 25 + i * 4, y + 37, 3, 3, tone[(i + 2) % 4]); }
                reel(g, x + 15, y + 47); reel(g, x + w - 15, y + 47);
                break; }
            case Terrain.CRATE: {
                g.rect(x, y, w, h, OUT); g.rect(x + 1, y + 1, w - 2, h - 2, 0xFFCB8C3C);
                g.rect(x + 1, y + 1, w - 2, 4, 0xFFE3AD62); g.rect(x + 1, y + h - 5, w - 2, 4, 0xFFA66D29);
                g.line(x + 5, y + 6, x + w - 5, y + h - 6, 0xFF7D4E18, 2.6f); g.line(x + w - 5, y + 6, x + 5, y + h - 6, 0xFF7D4E18, 2.6f);
                g.rect(x + 1, y + 1, 4, h - 2, 0xFFB57A35); g.rect(x + w - 5, y + 1, 4, h - 2, 0xFFB57A35);
                break; }
            case Terrain.METAL: {
                g.rrect(x + .5f, y + .5f, w - 1, h - 1, 2, 0xFF8D99A8, OUT, OW);
                g.rect(x + 2, y + 2, w - 4, 3, 0xFFC1CBD7); g.rect(x + 2, y + h - 5, w - 4, 3, 0xFF5F6B7A);
                for (int i = 0; i < 4; i++) g.circle(x + (i % 2 == 0 ? 6 : w - 6), y + (i < 2 ? 8 : h - 8), 1.6f, 0xFFDDE3EA, OUT, .6f);
                break; }
            case Terrain.STONE: {
                g.rrect(x + .5f, y + .5f, w - 1, h - 1, 4, 0xFF9C9488, OUT, OW);
                g.rrect(x + 3, y + 3, w - 6, 6, 3, 0xFFB8B0A3, 0, 0);
                g.line(x + 4, y + h / 2, x + w - 4, y + h / 2, 0xFF5B554D, 1); g.line(x + w / 3, y + 4, x + w / 3, y + h / 2, 0xFF5B554D, 1); g.line(x + 2 * w / 3, y + h / 2, x + 2 * w / 3, y + h - 4, 0xFF5B554D, 1);
                break; }
            default:
        }
    }
    /** Carrete de cinta: círculo blanco con tres ventanas negras. */
    void reel(Gfx g, float x, float y) {
        g.circle(x, y, 9, 0xFFFFFFFF, OUT, OW);
        for (int i = 0; i < 3; i++) { double a = i * 2.094 - 1.57; g.circle(x + (float) Math.cos(a) * 4.6f, y + (float) Math.sin(a) * 4.6f, 2.1f, OUT, 0, 0); }
        g.circle(x, y, 1.4f, OUT, 0, 0);
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
    /** Nombres sobre los personajes, con la fuente de pixel art, en la capa de interfaz de escala s. */
    void labels(Gfx g, World w, int s) {
        float lw = g.w(), k = zoom / s;
        for (int i = 0; i < 2; i++) {
            Robot o = w.robots[i]; RobotArt.View v = views[i];
            if (!o.alive() || v.labelT <= 0 || v.labelT < 8 && v.labelT % 2 == 0) continue;
            float tw = PixelFont.width(v.label, 8); int ly = Math.round((o.py - o.h - 10 - (LABEL - v.labelT) / 6f - vy0) * k), lx = Math.round(Math.max(tw / 2 + 3, Math.min(lw - tw / 2 - 3, (o.px - vx0) * k)));
            g.rect(lx - tw / 2 - 2, ly - 9, tw + 4, 11, INK); g.rect(lx - tw / 2 - 2, ly + 2, tw + 4, 1, RobotArt.COLOR[i]);
            PixelFont.draw(g, v.label, lx, ly, 8, 0xFFFFF4D6, 0, 0);
        }
    }
    void pointer(Gfx g, World w, int who, int s) {
        Robot e = w.robots[1 - who]; if (!e.alive()) return;
        float k = zoom / s, lw = g.w(), lh = g.h(), sx = (e.px - vx0) * k, sy = (e.py - 30 - vy0) * k, m = 14;
        if (sx > 0 && sx < lw && sy > 0 && sy < lh) return;
        float cx = lw / 2f, cy = lh / 2f, dx = sx - cx, dy = sy - cy, q = Math.min((lw / 2f - m) / Math.max(1, Math.abs(dx)), (lh / 2f - m) / Math.max(1, Math.abs(dy)));
        g.save(); g.translate(Math.round(cx + dx * q), Math.round(cy + dy * q)); g.rotate((float) Math.atan2(dy, dx));
        g.poly(new float[]{7,0, -5,-6, -2,0, -5,6}, RobotArt.COLOR[1 - who], INK, 1); g.restore();
    }
    static int alpha(int c, float k) { return (int) (Math.max(0, Math.min(1, k)) * (c >>> 24)) << 24 | c & 0xFFFFFF; }
    /** Explosión de luz: fogonazo blanco, bola de fuego, núcleo amarillo y onda roja que se abre. */
    void boom(Gfx g, float[] b) {
        float t = b[3], k = t / BOOM_T, R = b[2], x = b[0], y = b[1];
        if (t < 5) g.glow(x, y, R * 1.9f, alpha(0xFFFFFFFF, 1 - t / 5));
        g.glow(x, y, R * (.75f + .55f * k), alpha(0xFFFF6A1A, (float) Math.pow(1 - k, 1.3)));
        g.glow(x, y, R * (.45f + .25f * k), alpha(0xFFFFE27A, (1 - k) * (1 - k)));
        float rr = R * (.35f + 1.05f * (float) Math.sqrt(k)), wd = Math.max(.5f, R * .32f * (1 - k));
        g.circle(x, y, rr, 0, alpha(0xFFFF2A2A, (1 - k) * .55f), wd * 2.2f);
        g.circle(x, y, rr, 0, alpha(0xFFFF6A4A, (1 - k) * .9f), wd);
    }
    /** Luz propia de cada proyectil. */
    void shotLight(Gfx g, Shot s) {
        float sp = (float) Math.hypot(s.vx, s.vy);
        switch (s.w.look) {
            case Wpn.BULLET: g.line(s.x, s.y, s.x - s.vx * 1.6f, s.y - s.vy * 1.6f, 0x99FFC24A, 2.2f); g.line(s.x, s.y, s.x - s.vx * .9f, s.y - s.vy * .9f, 0xFFFFFFFF, .8f); g.glow(s.x, s.y, 4, 0x99FFD27A); break;
            case Wpn.ROCKET: { float nx = s.vx / Math.max(1, sp), ny = s.vy / Math.max(1, sp); g.glow(s.x - nx * 9, s.y - ny * 9, 7 + (s.age % 3), 0xEEFF8A2A); g.glow(s.x - nx * 8, s.y - ny * 8, 3, 0xFFFFF0C0); break; }
            case Wpn.FLAME: { float k = s.life > 0 && s.w.fuse > 0 ? s.life / (float) s.w.fuse : 1; g.glow(s.x, s.y, 3 + 6 * k, alpha(k > .5f ? 0xFFFFD040 : 0xFFFF6A10, .9f)); break; }
            case Wpn.LEMON: g.glow(s.x, s.y, 8, 0xCCFFE23A); g.save(); g.translate(s.x, s.y); g.rotate((float) Math.atan2(s.vy, s.vx)); g.scale(1.5f, 1); g.circle(0, 0, 2.2f, 0xFFFFFDE0, 0, 0); g.restore(); break;
            case Wpn.PLASMA: g.glow(s.x, s.y, 20 + s.age / 3 % 2 * 2, 0xEEFFC23A); g.glow(s.x, s.y, 9, 0xFFFFF6C0); g.circle(s.x, s.y, 4, 0xFFFFFFFF, 0, 0); break;
            case Wpn.ORB: g.glow(s.x, s.y, 9, 0xEE3FD8FF); g.circle(s.x, s.y, 2, 0xFFFFFFFF, 0, 0); break;
            case Wpn.NUKE: g.glow(s.x, s.y, 14 + s.age / 4 % 2 * 3, 0xAAFFD040); break;
            case Wpn.SPARK: g.glow(s.x, s.y, 5, 0xEEFF9A2A); g.circle(s.x, s.y, 1, 0xFFFFF0C0, 0, 0); break;
            case Wpn.DIRT: g.glow(s.x, s.y, 12, 0xEEB98CFF); g.glow(s.x, s.y, 5, 0xFFFFFFFF); break;
            case Wpn.MINE: if (s.age / 12 % 2 == 0) g.glow(s.x, s.y - 1, 6, 0xEEFF2A2A); break;
            case Wpn.GRENADE: if (s.age % 8 < 4) g.glow(s.x, s.y - 4, 3, 0xCCFFB02A); break;
            default:
        }
    }
    /** Bonus: píldoras de colores para curar, caja de balas para recargar. */
    void crate(Gfx g, float x, float y, int kind) {
        if (kind == 0) { pill(g, x - 2, y - 5, -.5f, 0xFFE8323C, 0xFFFFD23D); pill(g, x + 3, y - 10, .6f, 0xFF2F7BFF, 0xFFFFFFFF); return; }
        g.rect(x - 8, y - 15, 16, 15, INK); g.rect(x - 7, y - 14, 14, 13, 0xFF6B7A3A); g.rect(x - 7, y - 14, 14, 2, 0xFF93A55A);
        for (int i = 0; i < 3; i++) { g.rect(x - 5 + i * 4, y - 11, 2, 7, 0xFFFFC83D); g.rect(x - 5 + i * 4, y - 12, 2, 1, 0xFFB8862A); }
    }
    /** Cápsula de dos colores, al estilo de los juegos de píldoras. */
    void pill(Gfx g, float x, float y, float rot, int a, int b) {
        g.save(); g.translate(Math.round(x), Math.round(y)); g.rotate(rot);
        g.rrect(-7, -3, 14, 6, 3, INK, 0, 0); g.rrect(-6, -2, 6, 4, 2, a, 0, 0); g.rrect(0, -2, 6, 4, 2, b, 0, 0);
        g.rect(-5, -2, 3, 1, 0xFFFFFFFF); g.restore();
    }
    void shot(Gfx g, Shot s) {
        Wpn w = s.w; float a = (float) Math.atan2(s.vy, s.vx);
        switch (w.look) {
            case Wpn.ROCKET: g.save(); g.translate(s.x, s.y); g.rotate(a); g.poly(new float[]{-6,-2, 3,-2, 7,0, 3,2, -6,2}, 0xFFFF0000, INK, .7f); g.poly(new float[]{-6,-1.4f, -11,0, -6,1.4f}, 0xFFFFB020, 0, 0); g.restore(); break;
            case Wpn.GRENADE: g.circle(s.x, s.y, 3.1f, 0xFF2F7A3B, INK, .8f); g.rect(s.x - 1, s.y - 4.6f, 2, 2, INK); break;
            case Wpn.MINE: g.circle(s.x, s.y, 4, 0xFF333333, INK, .8f); if (s.age / 12 % 2 == 0) g.circle(s.x, s.y - 1, 1.3f, 0xFFFF2020, 0, 0); break;
            case Wpn.CRATE: crate(g, s.x, s.y + 4, 0); break;
            case Wpn.FLAME: break;
            case Wpn.SPIKE: { for (int i = 0; i < 4; i++) { float c = i * .785f + s.age * .2f; g.line(s.x - (float) Math.cos(c) * 4.5f, s.y - (float) Math.sin(c) * 4.5f, s.x + (float) Math.cos(c) * 4.5f, s.y + (float) Math.sin(c) * 4.5f, INK, 1); } break; }
            case Wpn.DIRT: case Wpn.LEMON: case Wpn.PLASMA: case Wpn.ORB: break; // son pura luz
            case Wpn.NUKE: g.save(); g.translate(s.x, s.y); g.rotate(a); g.poly(new float[]{-8,-3.4f, 5,-3.4f, 9,0, 5,3.4f, -8,3.4f}, 0xFFFFD020, INK, .8f); g.circle(-1, 0, 1.8f, INK, 0, 0); g.restore(); break;
            case Wpn.SPARK: break;
            default:
        }
    }
}
