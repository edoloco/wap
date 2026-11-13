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
    public final RobotArt.View[] views = {new RobotArt.View(), new RobotArt.View(), new RobotArt.View(), new RobotArt.View(), new RobotArt.View()};
    /** Equipos en pantalla: si hay equipos, la flecha y los nombres usan el color del equipo. */
    public boolean teams;
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
        if (ps.size() > (lite ? 220 : 500)) ps.remove(0);
        P p = new P(); p.kind = kind; p.x = x; p.y = y; p.vx = vx; p.vy = vy; p.life = p.max = life; p.size = size; p.color = color;
        p.rot = r.nextFloat() * 6.28f; p.vr = (r.nextFloat() - .5f) * .5f; ps.add(p); return p;
    }
    static final int LABEL = 48, SQUARE = 0, SMOKE = 1, ARMOR = 2, BONE = 3, FIRE = 4, CHUNK = 5, SPARK = 6, EMBER = 7, TRAIL = 8, DUST = 9;
    public void consume(World w, boolean reduceMotion) {
        for (Fx f : w.fx) switch (f.type) {
            case Fx.EXPLODE: { // fogonazo, bola de fuego, onda roja, chispas, brasas y humo oscuro
                int st = (int) f.b;
                booms.add(new float[]{f.x, f.y, f.a, 0, r.nextFloat() * 6, st});
                if (!reduceMotion) shake = Math.min(14, shake + f.a * .14f);
                int n = (int) Math.min(34, 6 + f.a * .6f);
                if (st > 0) { // halo de luz celeste o verde, sin humo ni fuego
                    int c1 = st == 1 ? 0xFF7FE8FF : 0xFF8CFF6A, c2 = 0xFFFFFFFF;
                    for (int i = 0; i < n; i++) { float a = r.nextFloat() * 6.28f, v = 1.5f + r.nextFloat() * 3.5f; P p = part(SPARK, f.x, f.y, (float) Math.cos(a) * v, (float) Math.sin(a) * v, 12 + r.nextInt(12), 1, i % 3 == 0 ? c2 : c1); p.g = 0; }
                    for (int i = 0; i < n / 4; i++) { float a = r.nextFloat() * 6.28f, v = r.nextFloat() * 3 + 1; part(SQUARE, f.x, f.y, (float) Math.cos(a) * v, (float) Math.sin(a) * v - 1, 30 + r.nextInt(30), 1.5f + r.nextFloat() * 2, r.nextBoolean() ? DIRT : DIRT2); }
                    break;
                }
                for (int i = 0; i < n; i++) { float a = r.nextFloat() * 6.28f, v = 2 + r.nextFloat() * (3 + f.a * .12f); P p = part(SPARK, f.x, f.y, (float) Math.cos(a) * v, (float) Math.sin(a) * v - 1, 14 + r.nextInt(16), 1, i % 3 == 0 ? 0xFFFFF4B0 : 0xFFFFA22E); p.g = .12f; }
                for (int i = 0; i < n / 3; i++) { float a = r.nextFloat() * 6.28f, v = r.nextFloat() * 2.5f; P p = part(EMBER, f.x, f.y, (float) Math.cos(a) * v, (float) Math.sin(a) * v - 1.5f, 40 + r.nextInt(40), 1.5f + r.nextFloat() * 2, 0xFFFF6A1A); p.g = .05f; }
                for (int i = 0; i < 3 + n / 5; i++) { float a = r.nextFloat() * 6.28f, d = r.nextFloat() * f.a * .5f; P p = part(SMOKE, f.x + (float) Math.cos(a) * d, f.y + (float) Math.sin(a) * d, (float) Math.cos(a) * .4f, -.25f, 50 + r.nextInt(40), f.a * (.2f + r.nextFloat() * .2f), 0x7A1A1C24); p.g = 0; }
                for (int i = 0; i < n / 4; i++) { float a = r.nextFloat() * 6.28f, v = r.nextFloat() * 4 + 1; part(SQUARE, f.x, f.y, (float) Math.cos(a) * v, (float) Math.sin(a) * v - 1, 30 + r.nextInt(30), 1.5f + r.nextFloat() * 2, r.nextBoolean() ? DIRT : DIRT2); }
                break; }
            case Fx.CHIPS: for (int i = 0; i < f.a; i++) part(SQUARE, f.x, f.y, (r.nextFloat() - .5f) * 4, -r.nextFloat() * 3, 24, 1.6f, DIRT2); break;
            case Fx.SMOKE: { P p = part(f.a > 0 ? FIRE : SMOKE, f.x, f.y, 0, -.2f, f.a > 0 ? 14 : 30, f.a > 0 ? 3 : 2.5f, f.a > 0 ? 0xFFFF8A1A : 0x5A3A3E48); p.g = 0; break; }
            case Fx.FLASH: { if (f.b < 0) { P q = part(TRAIL, f.x, f.y, 0, 0, 8, 5, 0xFFFFD27A); q.g = 0; break; } // misil del dron: solo un destello
                RobotArt.View v = views[f.who]; v.label = Wpn.ALL[(int) f.b].label; v.labelT = LABEL; if (v.shotT <= 0) v.shotT = 12; break; }
            case Fx.WOOD: { // astillas del color de la madera
                int n = Math.min(26, 5 + (int) f.a / 18), c = (int) f.b, d = RobotArt.mixDark(c);
                for (int i = 0; i < n; i++) {
                    float a = r.nextFloat() * 6.28f, v = 1.2f + r.nextFloat() * 3.5f;
                    P p = part(CHUNK, f.x + (r.nextFloat() - .5f) * 14, f.y + (r.nextFloat() - .5f) * 14, (float) Math.cos(a) * v, (float) Math.sin(a) * v - 2, 90 + r.nextInt(60), 2 + r.nextFloat() * 4, i % 3 == 0 ? d : c); p.slot = r.nextInt(99);
                }
                for (int i = 0; i < 5; i++) { P p = part(DUST, f.x, f.y, (r.nextFloat() - .5f) * 1.5f, -r.nextFloat(), 26, 3, 0x66E8D8C0); p.g = 0; }
                break; }
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
            case Fx.GRAB: { RobotArt.View v = views[f.who]; v.label = "¡PUNCH!"; v.labelT = LABEL;
                for (int i = 0; i < 12; i++) { float a = i * .52f; P p = part(SPARK, f.x, f.y, (float) Math.cos(a) * 3.5f, (float) Math.sin(a) * 3.5f, 12, 1, 0xFFFFFFFF); p.g = 0; }
                booms.add(new float[]{f.x, f.y, 20, 0, 0, 3}); break; }
            case Fx.SLAM: {
                booms.add(new float[]{f.x, f.y, 14 + f.a * 14, 0, 0}); if (!reduceMotion) shake = Math.min(14, shake + 3 + f.a * 6);
                for (int i = 0; i < 12; i++) { P p = part(SPARK, f.x, f.y, (r.nextFloat() - .5f) * 7, -r.nextFloat() * 5, 16, 1, 0xFFFFFFFF); p.g = .15f; }
                break; }
            case Fx.CHARGE: { RobotArt.View v = views[f.who]; v.label = f.a > 0 ? (Wpn.ALL[(int) f.b].trap ? "TRAMPA LISTA" : "CARGADO") : Wpn.ALL[(int) f.b].label; v.labelT = LABEL; break; }
            case Fx.LASSO: { RobotArt.View v = views[f.who]; v.label = f.a == 1 ? "¡ATRAPADO!" : f.a == 2 ? "¡SALTA!" : "¡LIBRE!"; v.labelT = LABEL;
                for (int i = 0; i < 10; i++) { float a = i * .628f; P p = part(SPARK, f.x, f.y, (float) Math.cos(a) * 3, (float) Math.sin(a) * 3, 12, 1, f.a > 0 ? 0xFF7FE8FF : 0xFFFFFFFF); p.g = 0; }
                break; }
            case Fx.TRAP: {
                if (f.a == 0) booms.add(new float[]{f.x, f.y - 6, 22, 0, 0, 3});
                else { RobotArt.View v = views[f.who]; v.label = "¡ATRAPADO!"; v.labelT = LABEL; booms.add(new float[]{f.x, f.y - 30, 46, 0, 0, 1}); if (!reduceMotion) shake = Math.min(14, shake + 6); }
                break; }
            case Fx.SPLASH: for (int i = 0; i < 6 + (int) f.a * 2; i++) { float a = -1.57f + (r.nextFloat() - .5f) * 1.6f, v = 1 + r.nextFloat() * f.a * .5f; P p = part(SQUARE, f.x + (r.nextFloat() - .5f) * 10, f.y, (float) Math.cos(a) * v, (float) Math.sin(a) * v, 24 + r.nextInt(12), 1.5f, r.nextBoolean() ? 0xFFBFE4FF : 0xFF7FB8FF); p.g = .18f; } break;
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
    /** Las estelas solo van detrás de lo lento: en lo rápido se veían como puntitos. */
    void trails(World w) {
        for (Robot o : w.robots) if (o.alive() && o.wet && w.tick % 12 == o.id * 6) { P p = part(DUST, o.px + o.facing * 4, o.py - 46, (r.nextFloat() - .5f) * .3f, -.6f, 40, 1.4f, 0x99E8F6FF); p.g = 0; } // burbujas
        for (Robot o : w.robots) if (o.alive() && o.beamT > 0 && w.tick % 2 == 0) { float a = r.nextFloat() * 6.28f, v = 1 + r.nextFloat() * 2.5f; P p = part(SPARK, o.beamX, o.beamY, (float) Math.cos(a) * v, (float) Math.sin(a) * v, 10, 1, r.nextBoolean() ? 0xFFFFB040 : 0xFF8FE9FF); p.g = 0; }
        for (Shot s : w.shots) {
            if (s.wait > 0) continue;
            float sp = (float) Math.hypot(s.vx, s.vy);
            if (s.w.fan) { // el viento arrastra partículas sueltas
                for (P p : ps) if (p.kind != ARMOR && p.kind != BONE && Math.abs(p.x - s.x) < 16 && Math.abs(p.y - s.y) < 16) { p.vx += s.vx * .12f; p.vy += s.vy * .12f; p.ground = false; }
                if (w.tick % 2 == 0) { P q = part(DUST, s.x, s.y, s.vx * .3f, s.vy * .3f, 14, 2.5f, 0x40FFFFFF); q.g = 0; }
                continue;
            }
            if (s.w.trap && s.stuck && s.hops > Shot.TRAP_OPEN && s.hops < Shot.TRAP_END) { // viento inverso hacia la trampa
                float cx = s.x, cy = s.y - 10;
                for (P p : ps) { float dx = cx - p.x, dy = cy - p.y, d = (float) Math.hypot(dx, dy); if (d < Shot.TRAP_R && d > 2 && p.kind != ARMOR && p.kind != BONE) { p.vx += dx / d * .35f; p.vy += dy / d * .35f; p.ground = false; } }
                for (int i = 0; i < 2; i++) { float a = r.nextFloat() * 6.28f, d = Shot.TRAP_R * (.6f + r.nextFloat() * .4f); P q = part(TRAIL, cx + (float) Math.cos(a) * d, cy + (float) Math.sin(a) * d * .7f, -(float) Math.cos(a) * 2.6f, -(float) Math.sin(a) * 1.8f, 30, 2.2f, 0xFFBFF4FF); q.g = 0; }
                continue;
            }
            if (s.w.look == Wpn.GAS) { // chispas como gas
                for (int i = 0; i < 3; i++) { P q = part(TRAIL, s.x + (r.nextFloat() - .5f) * 10, s.y + (r.nextFloat() - .5f) * 10, (r.nextFloat() - .5f) * .8f, (r.nextFloat() - .5f) * .8f - .1f, 22 + r.nextInt(14), 3 + r.nextFloat() * 4, i == 0 ? 0xFFFFE07A : 0xFFFF8A2A); q.g = 0; }
                continue;
            }
            if (s.w.look == Wpn.RAY) { P q = part(TRAIL, s.x, s.y, 0, 0, 26, 21, 0xFF6FDDFF); q.g = 0; P q2 = part(TRAIL, s.x, s.y, 0, 0, 16, 9, 0xFFFFFFFF); q2.g = 0; continue; } // Kamehameha
            if (sp > 5 || lite && (w.tick & 1) == 1) continue;
            int c; float z;
            switch (s.w.look) {
                case Wpn.ROCKET: c = 0xFFFF8A2A; z = 4; if (w.tick % 2 == 0) { P q = part(SMOKE, s.x, s.y, 0, -.1f, 40, 2.2f, 0x50404650); q.g = 0; } break;
                case Wpn.NUKE: c = 0xFFFFD040; z = 6; break;
                case Wpn.ORB: c = 0xFF3FD8FF; z = 3; break;
                case Wpn.PLASMA: c = 0xFFFFC23A; z = 8; break;
                case Wpn.SPARK: c = 0xFFFF9A2A; z = 2; break;
                case Wpn.MISSILE: c = 0xFFFF9A3A; z = 1.6f; if (w.tick % 2 == 0) { P q = part(SMOKE, s.x, s.y, 0, -.1f, 22, 1.4f, 0x40404650); q.g = 0; } break;
                default: continue;
            }
            if (s.w.tint != 0) c = Recolor.of(c, s.w.tint);
            P p = part(TRAIL, s.x, s.y, (r.nextFloat() - .5f) * .4f, (r.nextFloat() - .5f) * .4f, 18, z * s.w.size, c); p.g = 0;
        }
    }
    public void tick(World w) {
        feedStep(); trails(w);
        stepFx(w);
        for (int i = 0; i < w.robots.length; i++) {
            Robot o = w.robots[i]; RobotArt.update(views[i], o, w);
        }
    }
    void camera(World w, int who) {
        Robot me = w.robots[who];
        float tx = me.px + me.facing * vw * .1f + me.vx * 12, ty = me.py - 36;
        if (me.piloting()) { Shot d = me.droneShot; tx = d.x + (float) Math.cos(d.ang) * vw * .15f; ty = d.y + (float) Math.sin(d.ang) * vh * .12f; } // la cámara sigue al dron
        if (first) { camX = tx; camY = ty; first = false; }
        camX += (tx - camX) * .1f; camY += (ty - camY) * .12f;
        camX = Math.max(vw / 2f, Math.min(Terrain.W - vw / 2f, camX)); camY = Math.max(vh / 2f, Math.min(Terrain.H - vh / 2f, camY));
    }
    /** El mundo se dibuja suavizado, con zoom libre; solo la interfaz queda en pixel art. */
    public void drawWorld(Gfx g, World w, int who) {
        zoom = g.h() / 280f * zoomMul; vw = g.w() / zoom; vh = g.h() / zoom;
        if (freeCam) { // editor: se puede llevar cualquier borde o esquina del mapa hasta el centro de la pantalla
            float mx = vw * .45f, my = vh * .45f, lx = vw / 2f - mx, hx = Terrain.W - vw / 2f + mx, ly = vh / 2f - my, hy = Terrain.H - vh / 2f + my;
            camX = lx > hx ? Terrain.W / 2f : Math.max(lx, Math.min(hx, camX)); camY = ly > hy ? Terrain.H / 2f : Math.max(ly, Math.min(hy, camY));
        }
        else camera(w, who);
        float x0 = camX - vw / 2f, y0 = camY - vh / 2f; vx0 = x0; vy0 = y0;
        g.clear(0xFFC7D2E0);
        g.save(); g.scale(zoom, zoom);
        float sx = shake > .3f ? (r.nextFloat() - .5f) * shake : 0, sy = shake > .3f ? (r.nextFloat() - .5f) * shake : 0;
        g.translate(-x0 + sx, -y0 + sy);
        if (bgNew) paintBg();
        float bx0 = x0 * .55f, by0 = y0 * .55f;
        for (int q = (int) Math.floor(by0 / BG); q * BG < by0 + vh; q++) for (int k = (int) Math.floor(bx0 / BG); k * BG < bx0 + vw; k++) { g.tile(-1, bg, BG, bgNew, x0 + k * BG - bx0, y0 + q * BG - by0); bgNew = false; }
        for (Bonus b : w.bonus) crate(g, b.x, b.y, b.kind);
        for (Shot s : w.shots) look(g, s, false);
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
        for (int i = 0; i < w.robots.length; i++) {
            Robot o = w.robots[i]; if (!o.alive()) continue;
            if (o.rope != 0) { float hx = o.px + o.facing * 12, hy = o.py - 33; g.line(hx, hy, o.rx, o.ry, INK, 1.1f); g.circle(o.rx, o.ry, 2.4f, 0xFFFFFFFF, INK, .7f); }
            RobotArt.draw(g, views[i], o, RobotArt.COLOR[i]);
        }
        woodCracks(g, w);
        water(g, w, x0, y0);
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
                case SPARK: g.line(p.x, p.y, p.x - p.vx * 1.8f, p.y - p.vy * 1.8f, alpha(p.color, k), .9f); if (!lite) g.glow(p.x, p.y, 3, alpha(p.color, k * .6f)); break;
                default:
            }
        }
        for (float[] b : beams) {
            boolean gs = b[5] == 1; float k = gs ? b[4] / 12 : b[4] / 3; int c = gs ? 0xFF3F8BFF : 0xFFFF2A4A;
            g.line(b[0], b[1], b[2], b[3], alpha(c, .3f * k), gs ? 9 * k : 5); g.line(b[0], b[1], b[2], b[3], alpha(c, .8f * k), gs ? 4 * k : 2); g.line(b[0], b[1], b[2], b[3], alpha(0xFFFFFFFF, k), gs ? 1.6f * k : .8f);
            g.glow(b[2], b[3], gs ? 14 : 7, alpha(c, k));
        }
        for (float[] b : booms) boom(g, b);
        world = w;
        for (Shot sh : w.shots) look(g, sh, true);
        for (Robot o : w.robots) if (o.alive() && o.beamT > 0) proton(g, o.px + o.facing * 16, o.py - 31, o.beamX, o.beamY, w.tick); // rayo continuo
        for (Robot o : w.robots) if (o.alive() && o.lassoT > 0) { // atrapado por el Proton Ray: lazo
            float cy = o.py - 28;
            for (int i = 0; i < 2; i++) { float rr = 11 + i * 4 + (float) Math.sin((w.tick + i * 9) * .3f) * 1.5f; g.save(); g.translate(o.px, cy + (i == 0 ? -6 : 8)); g.scale(1, .35f); g.circle(0, 0, rr, 0, 0xCCFF8A1A, 2.2f); g.circle(0, 0, rr, 0, 0xEEFFE6B0, .8f); g.restore(); }
        }
        g.additive(false);
        g.restore();
        lastW = w; lastWho = who;
    }
    World lastW; int lastWho, uw = 320, uh = 190;
    /** Nombres y flecha del rival, dibujados en la capa de interfaz (pixel art) de escala s. */
    public void overlay(Gfx g, int s) {
        if (lastW == null) return;
        uw = (g.w() + s - 1) / s; uh = (g.h() + s - 1) / s; // tamaño de la capa de interfaz
        labels(g, lastW, s); pointer(g, lastW, lastWho, s); feedDraw(g);
    }
    /** Nombres de los jugadores (en línea se dibujan sobre cada personaje). */
    public String[] names = {"PLAYER 1", "PLAYER 2"}; public boolean showNames;
    /** Editor de niveles: cámara libre y zoom propio. */
    public boolean freeCam; public float zoomMul = 1;
    public float vwNow() { return vw; }
    /** Agua: celeste translúcido desde la superficie hacia abajo, con una línea ondulada. */
    void water(Gfx g, World w, float x0, float y0) {
        if (w.water < 0) return; float top = w.water;
        g.rect(x0, top, vw, Terrain.H - top, 0x553C8CFF);
        int n = (int) (vw / 6) + 2; float[] q = new float[(n + 2) * 2]; int k = 0;
        for (int i = 0; i <= n; i++) { float x = x0 + i * 6; q[k++] = x; q[k++] = top + (float) Math.sin(x * .07f + w.tick * .08f) * 1.4f; }
        q[k++] = x0 + n * 6; q[k++] = top + 6; float[] wave = java.util.Arrays.copyOf(q, k);
        g.poly(wave, 0x6687C4FF, 0, 0);
        for (int i = 0; i + 3 < (n + 1) * 2; i += 2) g.line(q[i], q[i + 1], q[i + 2], q[i + 3], 0xCCE8F6FF, 1.2f);
    }
    /** Modo liviano: si el teléfono no alcanza a dibujar a tiempo, menos partículas y luces más simples. */
    public boolean lite;
    /** Avisos de bajas arriba de la pantalla. */
    public final ArrayList<String> feedMsg = new ArrayList<>(); final ArrayList<int[]> feedInfo = new ArrayList<>(); static final int FEED_T = 300;
    public void feed(String msg, int color) { feedMsg.add(0, msg); feedInfo.add(0, new int[]{FEED_T, color}); while (feedMsg.size() > 3) { feedMsg.remove(3); feedInfo.remove(3); } }
    public void feedClear() { feedMsg.clear(); feedInfo.clear(); }
    void feedStep() { for (int i = feedInfo.size() - 1; i >= 0; i--) if (--feedInfo.get(i)[0] <= 0) { feedMsg.remove(i); feedInfo.remove(i); } }
    void feedDraw(Gfx g) {
        float cx = uw / 2f;
        for (int i = 0; i < feedMsg.size(); i++) {
            int[] f = feedInfo.get(i); if (f[0] < 20 && f[0] % 4 < 2) continue;
            String m = feedMsg.get(i); float tw = PixelFont.width(m, 8); int x = Math.round(cx - tw / 2 - 5), y = 40 + i * 15, w = Math.round(tw + 10);
            g.rect(x, y, w, 13, 0xE6101330); g.rect(x, y, 2, 13, f[1]); g.rect(x + w - 2, y, 2, 13, f[1]); g.rect(x, y + 12, w, 1, INK);
            PixelFont.draw(g, m, cx, y + 10, 8, 0xFFFFF4D6, 0, 0xFF101330);
        }
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
        float lw = uw, k = zoom / s;
        for (int i = 0; i < w.robots.length; i++) {
            Robot o = w.robots[i]; RobotArt.View v = views[i];
            if (!o.alive()) continue;
            if (showNames) { // nombre sobre el personaje
                String n = names[i]; float nw = PixelFont.width(n, 8); int ny = Math.round((o.py - o.h - 2 - vy0) * k), nx = Math.round(Math.max(nw / 2 + 3, Math.min(lw - nw / 2 - 3, (o.px - vx0) * k)));
                g.rect(nx - nw / 2 - 2, ny - 8, nw + 4, 1, RobotArt.COLOR[i]);
                PixelFont.draw(g, n, nx, ny, 8, RobotArt.COLOR[i] == 0xFF2A2A2A ? 0xFFFFF4D6 : RobotArt.mixLight(RobotArt.COLOR[i]), 0, INK);
            }
            if (v.labelT <= 0 || v.labelT < 8 && v.labelT % 2 == 0) continue;
            float tw = PixelFont.width(v.label, 8); int ly = Math.round((o.py - o.h - 10 - (showNames ? 12 / k : 0) - (LABEL - v.labelT) / 6f - vy0) * k), lx = Math.round(Math.max(tw / 2 + 3, Math.min(lw - tw / 2 - 3, (o.px - vx0) * k)));
            g.rect(lx - tw / 2 - 2, ly - 9, tw + 4, 11, INK); g.rect(lx - tw / 2 - 2, ly + 2, tw + 4, 1, RobotArt.COLOR[i]);
            PixelFont.draw(g, v.label, lx, ly, 8, 0xFFFFF4D6, 0, 0);
        }
    }
    /** Flechas en el borde hacia cada rival que está fuera de la pantalla. */
    void pointer(Gfx g, World w, int who, int s) {
        Robot me = w.robots[who];
        for (Robot e : w.robots) {
            if (e == me || !e.alive() || e.team == me.team) continue;
            float k = zoom / s, lw = uw, lh = uh, sx = (e.px - vx0) * k, sy = (e.py - 30 - vy0) * k, m = 14;
            if (sx > 0 && sx < lw && sy > 0 && sy < lh) continue;
            float cx = lw / 2f, cy = lh / 2f, dx = sx - cx, dy = sy - cy, q = Math.min((lw / 2f - m) / Math.max(1, Math.abs(dx)), (lh / 2f - m) / Math.max(1, Math.abs(dy)));
            g.save(); g.translate(Math.round(cx + dx * q), Math.round(cy + dy * q)); g.rotate((float) Math.atan2(dy, dx));
            g.poly(new float[]{7,0, -5,-6, -2,0, -5,6}, RobotArt.COLOR[e.id], INK, 1); g.restore();
        }
    }
    /** Grietas en los trozos de madera dañados. */
    void woodCracks(Gfx g, World w) {
        if (w.woodDmg == null) return;
        int a = Math.max(0, (int) (vx0 / Terrain.FRAG) - 1), b = Math.min(Terrain.FW - 1, (int) ((vx0 + vw) / Terrain.FRAG) + 1);
        int c = Math.max(0, (int) (vy0 / Terrain.FRAG) - 1), d = Math.min(Terrain.FH - 1, (int) ((vy0 + vh) / Terrain.FRAG) + 1);
        for (int j = c; j <= d; j++) for (int i = a; i <= b; i++) {
            int f = j * Terrain.FW + i, dmg = w.woodDmg[f]; if (dmg <= 0) continue;
            float k = dmg / (float) World.WOOD_HP; int x = Terrain.fragX(f), y = Terrain.fragY(f), h = hash(i, j);
            for (int n = 0; n < 2 + (int) (k * 4); n++) {
                float ang = (h >> n * 3 & 7) * .785f + n, len = 4 + k * 9;
                float x1 = x + (float) Math.cos(ang) * len, y1 = y + (float) Math.sin(ang) * len;
                if (w.t.at((int) x1, (int) y1) != Terrain.WOOD || w.t.at(x, y) != Terrain.WOOD) continue;
                g.line(x, y, x1, y1, 0xAA1A1208, 1.1f); g.line(x1, y1, x1 + (float) Math.cos(ang + 1) * len * .5f, y1 + (float) Math.sin(ang + 1) * len * .5f, 0x881A1208, .8f);
            }
        }
    }
    World world;
    static int alpha(int c, float k) { return (int) (Math.max(0, Math.min(1, k)) * (c >>> 24)) << 24 | c & 0xFFFFFF; }
    /** Explosión de luz: fogonazo blanco, bola de fuego, núcleo amarillo y onda roja que se abre. */
    void boom(Gfx g, float[] b) {
        float t = b[3], k = t / BOOM_T, R = b[2], x = b[0], y = b[1];
        int st = b.length > 5 ? (int) b[5] : 0;
        if (st > 0) { // halo circular de luz
            int c = st == 1 ? 0xFF6FDDFF : st == 2 ? 0xFF8CFF6A : 0xFFFFFFFF;
            if (t < 5) g.glow(x, y, R * 1.7f, alpha(0xFFFFFFFF, 1 - t / 5));
            g.glow(x, y, R * (.6f + .5f * k), alpha(c, (1 - k) * (1 - k)));
            float rr = R * (.3f + 1.1f * (float) Math.sqrt(k));
            g.circle(x, y, rr, 0, alpha(c, (1 - k) * .5f), Math.max(.5f, R * .5f * (1 - k)));
            g.circle(x, y, rr, 0, alpha(0xFFFFFFFF, (1 - k) * .9f), Math.max(.4f, R * .14f * (1 - k)));
            return;
        }
        if (t < 5) g.glow(x, y, R * 1.9f, alpha(0xFFFFFFFF, 1 - t / 5));
        g.glow(x, y, R * (.75f + .55f * k), alpha(0xFFFF6A1A, (float) Math.pow(1 - k, 1.3)));
        g.glow(x, y, R * (.45f + .25f * k), alpha(0xFFFFE27A, (1 - k) * (1 - k)));
        float rr = R * (.35f + 1.05f * (float) Math.sqrt(k)), wd = Math.max(.5f, R * .32f * (1 - k));
        g.circle(x, y, rr, 0, alpha(0xFFFF2A2A, (1 - k) * .55f), wd * 2.2f);
        g.circle(x, y, rr, 0, alpha(0xFFFF6A4A, (1 - k) * .9f), wd);
    }
    /** Luz propia de cada proyectil. */
    final Recolor recolor = new Recolor();
    /** Para las herramientas: dibuja un disparo completo (cuerpo y luz). */
    public void drawShot(Gfx g, Shot s) { look(g, s, false); look(g, s, true); }
    /** Para las herramientas: solo el cuerpo (light = false) o solo la luz (light = true). */
    public void drawShotPart(Gfx g, Shot s, boolean light) { look(g, s, light); }
    /** Dibuja el cuerpo o la luz de un disparo con el aspecto de armas.txt: color, tamaño, luz y dibujo propio. */
    void look(Gfx g, Shot s, boolean light) {
        Wpn w = s.w; if (s.wait > 0) return;
        if (light && !w.light) return;
        Gfx gg = w.tint != 0 ? recolor.with(g, w.tint) : g; boolean sz = w.size != 1;
        if (sz) { g.save(); g.translate(s.x, s.y); g.scale(w.size, w.size); g.translate(-s.x, -s.y); }
        if (!light && w.art != null) { // dibujo propio, mirando hacia donde vuela (sin quedar de cabeza al ir a la izquierda)
            float a = w.drone ? s.ang : (float) Math.atan2(s.vy, s.vx); boolean left = Math.cos(a) < 0;
            float tilt = 0, bob = 0; if (w.artBob) { tilt = (float) Math.sin(s.age * .11f) * .06f; bob = (float) Math.sin(s.age * .16f) * 1.05f; } // se mece como el dron
            g.save(); g.translate(s.x, s.y); g.rotate((left ? a - (float) Math.PI : a) + tilt); if (left) g.scale(-1, 1); if (bob != 0) g.translate(0, bob);
            Wpn.Art[] fr = w.frames != null && w.frames.length > 1 ? w.frames[(s.age / Math.max(1, w.artRate)) % w.frames.length] : w.art;
            for (Wpn.Art p : fr) g.shape(p.polys, p.fill, p.eo, p.stroke, p.stroke == 0 ? 0 : p.sw);
            g.restore();
        } else if (light) shotLight(gg, s); else shot(gg, s);
        if (sz) g.restore();
    }
    void shotLight(Gfx g, Shot s) {
        if (s.wait > 0) return;
        float sp = (float) Math.hypot(s.vx, s.vy);
        switch (s.w.look) {
            case Wpn.BULLET: g.line(s.x, s.y, s.x - s.vx * 1.6f, s.y - s.vy * 1.6f, 0x99FFC24A, 2.2f); g.line(s.x, s.y, s.x - s.vx * .9f, s.y - s.vy * .9f, 0xFFFFFFFF, .8f); g.glow(s.x, s.y, 4, 0x99FFD27A); break;
            case Wpn.ROCKET: { float nx = s.vx / Math.max(1, sp), ny = s.vy / Math.max(1, sp); g.glow(s.x - nx * 9, s.y - ny * 9, 7 + (s.age % 3), 0xEEFF8A2A); g.glow(s.x - nx * 8, s.y - ny * 8, 3, 0xFFFFF0C0); break; }
            case Wpn.FLAME: { float k = s.life > 0 && s.w.fuse > 0 ? s.life / (float) s.w.fuse : 1; g.glow(s.x, s.y, 4 + 9 * (1 - k * .5f), alpha(k > .6f ? 0xFFFFD040 : 0xFFFF6A10, .9f * Math.min(1, k * 2.5f))); break; }
            case Wpn.LEMON: g.glow(s.x, s.y, 8, 0xCCFFE23A); g.save(); g.translate(s.x, s.y); g.rotate((float) Math.atan2(s.vy, s.vx)); g.scale(1.5f, 1); g.circle(0, 0, 2.2f, 0xFFFFFDE0, 0, 0); g.restore(); break;
            case Wpn.PLASMA: g.glow(s.x, s.y, 20 + s.age / 3 % 2 * 2, 0xEEFFC23A); g.glow(s.x, s.y, 9, 0xFFFFF6C0); g.circle(s.x, s.y, 4, 0xFFFFFFFF, 0, 0); break;
            case Wpn.LIGHT: g.glow(s.x, s.y, 11, 0xEE58D2FF); g.glow(s.x, s.y, 5, 0xFFE8FBFF); g.circle(s.x, s.y, 2.2f, 0xFFFFFFFF, 0, 0); break;
            case Wpn.GREEN: g.glow(s.x, s.y, 13, 0xCC5CFF3A); g.circle(s.x, s.y, 5, 0xFF46D62A, 0, 0); g.circle(s.x - 1.5f, s.y - 1.5f, 2, 0xFFD8FFC0, 0, 0); break;
            case Wpn.BLASER: { float n = Math.max(1, sp), ux = s.vx / n, uy = s.vy / n; g.glow(s.x, s.y, 9, 0xCC6AFF4A); g.line(s.x - ux * 9, s.y - uy * 9, s.x + ux * 3, s.y + uy * 3, 0xFF7CFF5A, 3); g.line(s.x - ux * 8, s.y - uy * 8, s.x + ux * 2, s.y + uy * 2, 0xFFF0FFE8, 1.2f); break; }
            case Wpn.RAY: g.glow(s.x, s.y, 16, 0xEE6FDDFF); g.glow(s.x, s.y, 7, 0xFFFFFFFF); break;
            case Wpn.GAS: g.glow(s.x, s.y, 7, 0xAAFF9A2A); break;
            case Wpn.WIND: break;
            case Wpn.TRAP: {
                int t = s.stuck ? s.hops : 0; g.glow(s.x, s.y - 6.5f, 8, trapLight(s) & 0xFFFFFF | 0xCC000000);
                if (t > Shot.TRAP_OPEN && t < Shot.TRAP_END) { // haz de luz en triángulo: punta en la trampa, base ancha y brillante arriba
                    float k = Math.min(1, (t - Shot.TRAP_OPEN) / 12f) * Math.min(1, (Shot.TRAP_END - t) / 12f), pulse = 1 + .08f * (float) Math.sin(t * .35f);
                    float ty = s.y - 15, H = 150 * k, W = 46 * pulse; int N = 10;
                    for (int q = 0; q < N; q++) {
                        float a0 = q / (float) N, a1 = (q + 1) / (float) N, y0 = ty - H * a0, y1 = ty - H * a1, w0 = W * a0, w1 = W * a1;
                        g.poly(new float[]{s.x - w0, y0, s.x + w0, y0, s.x + w1, y1, s.x - w1, y1}, alpha(0xFFBFF4FF, k * (.10f + .45f * a1)), 0, 0);
                        g.poly(new float[]{s.x - w0 * .45f, y0, s.x + w0 * .45f, y0, s.x + w1 * .45f, y1, s.x - w1 * .45f, y1}, alpha(0xFFFFFFFF, k * (.08f + .4f * a1)), 0, 0);
                    }
                    g.save(); g.translate(s.x, ty - H); g.scale(1, .3f); g.glow(0, 0, W * 1.25f, alpha(0xFFE8FBFF, k)); g.restore(); // base brillante arriba
                    g.line(s.x - W, ty - H, s.x + W, ty - H, alpha(0xFFFFFFFF, k), 2.2f);
                    g.glow(s.x, s.y - 8, 14 * pulse, alpha(0xFF7FE8FF, .6f * k));
                }
                break; }
            case Wpn.ORB: g.glow(s.x, s.y, 9, 0xEE3FD8FF); g.circle(s.x, s.y, 2, 0xFFFFFFFF, 0, 0); break;
            case Wpn.NUKE: g.glow(s.x, s.y, 14 + s.age / 4 % 2 * 3, 0xAAFFD040); break;
            case Wpn.SPARK: g.glow(s.x, s.y, 5, 0xEEFF9A2A); g.circle(s.x, s.y, 1, 0xFFFFF0C0, 0, 0); break;
            case Wpn.DIRT: g.glow(s.x, s.y, 12, 0xEEB98CFF); g.glow(s.x, s.y, 5, 0xFFFFFFFF); break;
            case Wpn.MINE: if (s.age / 12 % 2 == 0) g.glow(s.x, s.y - 1, 6, 0xEEFF2A2A); break;
            case Wpn.DRONE: { g.glow(s.x, s.y, 12, 0x30FFFFFF); boolean low = s.life > 0 && s.life < 120; if (s.age / (low ? 4 : 10) % 2 == 0) g.glow(s.x, s.y - 4, 5, low ? 0xEEFF3A3A : 0xEE6AFF6A); break; } // luz: verde; roja y rápida si se acaba la batería
            case Wpn.MISSILE: { float n = Math.max(1, sp); g.glow(s.x - s.vx / n * 4, s.y - s.vy / n * 4, 4 + (s.age % 2), 0xEEFF8A2A); break; }
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
    /** Luz de la trampa: roja parpadeante; al terminar pasa de rojo a verde, cada vez más rápido. */
    static int trapLight(Shot s) {
        int t = s.stuck ? s.hops : 0;
        if (t < Shot.TRAP_END) return s.age / 8 % 2 == 0 ? 0xFFFF2A2A : 0xFF5A1010;
        float k = (t - Shot.TRAP_END) / (float) (Shot.TRAP_BOOM - Shot.TRAP_END); int rate = k < .5f ? 8 : k < .8f ? 4 : 2;
        if (s.age / rate % 2 != 0) return 0xFF102010;
        return 0xFF000000 | (int) (255 * (1 - k)) << 16 | (int) (42 + 213 * k) << 8 | 42;
    }
    /** Rayo de protones como el de los cazafantasmas: dos hebras naranja y roja que se retuercen, con arcos eléctricos celestes. */
    void proton(Gfx g, float x0, float y0, float x1, float y1, int tick) {
        float dx = x1 - x0, dy = y1 - y0, L = Math.max(1, (float) Math.hypot(dx, dy)), nx = -dy / L, ny = dx / L; int n = Math.max(6, (int) (L / 5));
        for (int strand = 0; strand < 2; strand++) {
            float[] q = new float[(n + 1) * 2];
            for (int i = 0; i <= n; i++) {
                float t = i / (float) n, env = (float) Math.sin(Math.PI * Math.min(1, t * 1.15f)) * (2.5f + 3.5f * t); // se abre hacia la punta
                float o = env * (float) Math.sin(t * L / 13f - tick * .55f + strand * Math.PI) + (strand == 0 ? 0 : env * .3f * (float) Math.sin(t * L / 5f + tick));
                q[i * 2] = x0 + dx * t + nx * o; q[i * 2 + 1] = y0 + dy * t + ny * o;
            }
            int c = strand == 0 ? 0xFFFF7A14 : 0xFFFF3A1A;
            for (int i = 0; i < n; i++) {
                float ax = q[i * 2], ay = q[i * 2 + 1], bx = q[i * 2 + 2], by = q[i * 2 + 3];
                g.line(ax, ay, bx, by, alpha(c, .28f), strand == 0 ? 8 : 6);
                g.line(ax, ay, bx, by, alpha(c, .9f), strand == 0 ? 3.2f : 2.2f);
                if (strand == 0) g.line(ax, ay, bx, by, 0xFFFFE9B8, 1.1f);
            }
        }
        for (int k = 0; k < 3; k++) { // arcos eléctricos que saltan alrededor
            float a = r.nextFloat() * .7f, b = Math.min(1, a + .15f + r.nextFloat() * .3f), px = x0 + dx * a, py = y0 + dy * a; int m = 5;
            for (int i = 1; i <= m; i++) {
                float t = a + (b - a) * i / m, o = (r.nextFloat() - .5f) * 16, qx = x0 + dx * t + nx * o, qy = y0 + dy * t + ny * o;
                g.line(px, py, qx, qy, k == 0 ? 0xEE8FE9FF : 0xAAD8FAFF, k == 0 ? 1.1f : .7f); px = qx; py = qy;
            }
        }
        g.glow(x0, y0, 10, 0xDDFF9A2A); g.glow(x0, y0, 4, 0xFFFFF0C8); g.glow(x1, y1, 12 + r.nextFloat() * 4, 0xBBFF8A2A);
    }
    /** Guante de box del color del traje de quien lo lanza. */
    static void glove(Gfx g, float x, float y, int dir, int c) {
        int lite = RobotArt.mixLight(c), dark = RobotArt.mixDark(c);
        g.save(); g.translate(x, y); g.scale(dir, 1);
        g.rrect(-9, -3.4f, 5, 6.8f, 1.5f, 0xFFF2F2F2, 0, 0); // puño elástico
        g.rrect(-5, -6, 11, 12, 5.5f, c, 0, 0);
        g.rrect(-2, -6.6f, 6, 4.4f, 2.2f, c, 0, 0); // pulgar arriba
        g.rrect(-3.5f, -4.8f, 7, 3, 1.5f, lite, 0, 0);
        g.line(-4, 3, 4, 3, dark, 1);
        g.restore();
    }
    /** Dron: cuerpo del color del piloto, dos rotores que giran, luz que parpadea y una antena; mira hacia donde va. */
    /** Dron: se mece en el aire, las hélices giran, se inclina al virar y la luz de cola parpadea más rápido cuando se le acaba la batería. */
    void drone(Gfx g, Shot s) {
        int col = RobotArt.COLOR[s.owner]; float c = (float) Math.cos(s.ang); boolean left = c < 0; int t = s.age;
        float bob = (float) Math.sin(t * .16f) * .7f, tilt = (float) Math.sin(t * .11f) * .06f; // meneo suave de estar flotando
        g.save(); g.translate(s.x, s.y); g.rotate((left ? s.ang - (float) Math.PI : s.ang) + tilt); g.scale(left ? -1.5f : 1.5f, 1.5f); g.translate(0, bob);
        g.line(-8, -4, 8, -4, 0xFF1A1A20, 1.4f); // brazos de los rotores
        for (int k = -1; k <= 1; k += 2) {
            float x = k * 8, ph = t * .55f + k * 1.3f;
            g.poly(ellipse(x, -6, 6.5f, 1.1f), 0x30E8EEF8, 0, 0); // disco borroso de la hélice
            for (int b = 0; b < 2; b++) { float w = 6 * (float) Math.cos(ph + b * 1.5708f); g.line(x - w, -6, x + w, -6, b == 0 ? 0xE0E8EEF8 : 0x90C8D0E0, 1.1f); } // dos aspas que se cruzan
            g.line(x, -5.5f, x, -4, 0xFF1A1A20, 1); g.circle(x, -6, .8f, 0xFF1A1A20, 0, 0);
        }
        g.rrect(-7, -4, 14, 7, 3, col, 0xFF000000, .9f); // cuerpo
        g.rrect(-5.5f, -3, 6, 1.4f, .7f, 0x40FFFFFF, 0, 0); // brillo del cuerpo
        float scan = (float) Math.sin(t * .09f) * .9f; // la cámara mira de un lado a otro
        g.rrect(1.5f, -2.5f, 5, 3.5f, 1.5f, 0xFF2A2F3A, 0, 0); g.circle(4 + scan * .6f, -.8f, 1, 0xFF7FE0FF, 0, 0); g.circle(3.7f + scan * .6f, -1.1f, .35f, 0xFFFFFFFF, 0, 0);
        int per = s.life > 0 && s.life < 120 ? 8 : s.life > 0 && s.life < 300 ? 18 : 36; // con poca batería parpadea más rápido
        boolean on = t % per < per / 2; g.circle(-6.2f, -.5f, .9f, on ? (s.life > 0 && s.life < 120 ? 0xFFFF3030 : 0xFF40FF70) : 0xFF303038, 0, 0);
        float sw = (float) Math.sin(t * .2f) * .6f; g.line(-5, 3, -6.5f + sw, 6, 0xFF1A1A20, 1); g.line(5, 3, 6.5f + sw, 6, 0xFF1A1A20, 1); // patas que cuelgan y se balancean
        g.restore();
    }
    static float[] ellipse(float cx, float cy, float rx, float ry) { float[] p = new float[24]; for (int i = 0; i < 12; i++) { double a = i * Math.PI / 6; p[2 * i] = cx + rx * (float) Math.cos(a); p[2 * i + 1] = cy + ry * (float) Math.sin(a); } return p; }
    void shot(Gfx g, Shot s) {
        if (s.wait > 0) return;
        Wpn w = s.w; float a = (float) Math.atan2(s.vy, s.vx);
        switch (w.look) {
            case Wpn.ROCKET: g.save(); g.translate(s.x, s.y); g.rotate(a); g.poly(new float[]{-6,-2, 3,-2, 7,0, 3,2, -6,2}, 0xFFFF2A1A, 0, 0); g.poly(new float[]{-6,-2, 3,-2, 5,-1, -6,-1}, 0xFFFF8A6A, 0, 0); g.poly(new float[]{-6,-1.4f, -11,0, -6,1.4f}, 0xFFFFB020, 0, 0); g.restore(); break;
            case Wpn.GRENADE: g.circle(s.x, s.y, 3.1f, 0xFF2F7A3B, 0, 0); g.circle(s.x - 1, s.y - 1, 1.2f, 0xFF6BBF6E, 0, 0); g.rect(s.x - 1, s.y - 4.6f, 2, 2, 0xFF5A5A5A); break;
            case Wpn.MINE: g.circle(s.x, s.y, 4, 0xFF4A4A4A, 0, 0); g.circle(s.x - 1.2f, s.y - 1.2f, 1.4f, 0xFF7A7A7A, 0, 0); if (s.age / 12 % 2 == 0) g.circle(s.x, s.y - 1, 1.3f, 0xFFFF2020, 0, 0); break;
            case Wpn.CRATE: crate(g, s.x, s.y + 4, 0); break;
            case Wpn.FLAME: break;
            case Wpn.SPIKE: { for (int i = 0; i < 4; i++) { float c = i * .785f + s.age * .2f; g.line(s.x - (float) Math.cos(c) * 4.5f, s.y - (float) Math.sin(c) * 4.5f, s.x + (float) Math.cos(c) * 4.5f, s.y + (float) Math.sin(c) * 4.5f, INK, 1); } break; }
            case Wpn.TRAP: {
                int t = s.stuck ? s.hops : 0; float k = t <= Shot.TRAP_OPEN ? 0 : t < Shot.TRAP_END ? Math.min(1, (t - Shot.TRAP_OPEN) / 10f) : Math.max(0, 1 - (t - Shot.TRAP_END) / 10f);
                g.rrect(s.x - 7, s.y - 13, 14, 13, 2, 0xFF1A1A20, 0, 0); g.rect(s.x - 6, s.y - 12, 12, 1.2f, 0xFF3C3C48); g.rect(s.x - 6, s.y - 2, 12, 1, 0xFF0C0C10);
                g.rect(s.x - 7 - k * 6, s.y - 15, 7, 2.2f, 0xFF2A2A32); g.rect(s.x + k * 6, s.y - 15, 7, 2.2f, 0xFF2A2A32); // tapa que se abre
                g.circle(s.x, s.y - 6.5f, 2.2f, trapLight(s), 0, 0); break; }
            case Wpn.GLOVE: glove(g, s.x, s.y, s.vx > 0 ? 1 : -1, RobotArt.COLOR[s.owner]); break;
            case Wpn.DRONE: drone(g, s); break;
            case Wpn.MISSILE: g.save(); g.translate(s.x, s.y); g.rotate(a); g.poly(new float[]{-3.5f,-1, 1.5f,-1, 3.5f,0, 1.5f,1, -3.5f,1}, 0xFFD8D8E0, 0x99000000, .5f); g.restore(); break;
            case Wpn.DIRT: case Wpn.LEMON: case Wpn.PLASMA: case Wpn.ORB: break; // son pura luz
            case Wpn.NUKE: g.save(); g.translate(s.x, s.y); g.rotate(a); g.poly(new float[]{-8,-3.4f, 5,-3.4f, 9,0, 5,3.4f, -8,3.4f}, 0xFFFFD020, 0, 0); g.poly(new float[]{-8,-3.4f, 5,-3.4f, 7,-1.6f, -8,-1.6f}, 0xFFFFF09A, 0, 0); g.circle(-1, .8f, 1.8f, 0xFF8A5A10, 0, 0); g.restore(); break;
            case Wpn.SPARK: break;
            default:
        }
    }
}
