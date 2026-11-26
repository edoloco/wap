package com.wap.core;
import java.util.ArrayList;
/**
 * Una base en juego: piezas que se mueven, enemigos (dron de ataque, torreta, badpal y el jefe), sus disparos,
 * precipicios, puntos de control por habitación y la sala del jefe.
 * Solo: al entrar a la sala del jefe se cierran las puertas y aparece la máquina; al vencerla se gana.
 * En línea: el jugador 2 juega la base reflejada y los dos llegan a la misma sala del jefe; el primero espera
 * (quieto e intocable) y, cuando llegan los dos, se cierran las puertas y pelean. El primero que muere pierde.
 * Todo es determinista (lockstep): solo usa World.rng, StrictMath y el número de tick.
 */
public final class BaseRun implements Terrain.PageSource {
    public final Base base; public final boolean online;
    int ox, oy;
    /** Habitaciones en píxeles: {x, y, ancho, alto, jefe, reflejada}. */
    public final ArrayList<int[]> rooms = new ArrayList<>();
    public final ArrayList<Kin> kin = new ArrayList<>();
    public final ArrayList<Foe> foes = new ArrayList<>();
    public final ArrayList<Bullet> bullets = new ArrayList<>();
    /** Premios puestos en el editor: {tipo de Bonus, x, y}. */
    final ArrayList<float[]> prizes = new ArrayList<>();
    final float[][] starts = new float[2][];
    final int[] check = {-1, -1}, fallT = new int[2]; final int[][] spawnCache = new int[64][];
    /** held: esperando al rival en la sala del jefe. fight: pelea final en línea. bossOn: puertas cerradas con el jefe. */
    public final boolean[] held = new boolean[2]; public boolean fight, bossOn, won; public Foe boss;
    /** Puertas cerradas (rectángulos de acero): {x, y, ancho, alto}. */
    public final ArrayList<int[]> doors = new ArrayList<>();

    BaseRun(Base b, boolean online) { base = b; this.online = online; }
    /** Piezas fijas por cuadro del mapa: {material de la base, color}. Escarcha de FREEZEE FLOW por cuadro. */
    public final java.util.HashMap<Long, int[]> cells = new java.util.HashMap<>(); public final java.util.HashSet<Long> frost = new java.util.HashSet<>();
    World world;
    /** Habitaciones activas: donde está cada jugador y las que la rodean. Las demás duermen (sus enemigos y piezas se quedan quietos) y su mapa se suelta. */
    public boolean[] active = new boolean[0];
    /** Arma una página del mapa desde los datos: pared, habitaciones (y precipicios), piezas fijas, objetos, piezas que se mueven y puertas. */
    @Override public void fill(Terrain t, int px, int py, byte[] page) {
        final int S = Terrain.PS, x0 = px * S, y0 = py * S, C = Base.CELL;
        java.util.Arrays.fill(page, Terrain.STEEL);
        for (int[] r : rooms) {
            int rx0 = Math.max(x0, r[0]), rx1 = Math.min(x0 + S, r[0] + r[2]), ry0 = Math.max(y0, r[1]), ry1 = Math.min(y0 + S, r[1] + r[3] + (r[6] == 1 ? Base.PAD * C : 0));
            if (rx0 >= rx1 || ry0 >= ry1) continue;
            for (int y = ry0; y < ry1; y++) java.util.Arrays.fill(page, (y - y0) * S + rx0 - x0, (y - y0) * S + rx1 - x0, (byte) 0);
        }
        for (int cy = y0 / C; cy < (y0 + S) / C; cy++) for (int cx = x0 / C; cx < (x0 + S) / C; cx++) {
            int[] v = cells.get(Base.key(cx, cy)); if (v == null) continue; byte k = Base.matOf(v[0]);
            for (int y = cy * C; y < cy * C + C; y++) java.util.Arrays.fill(page, (y - y0) * S + cx * C - x0, (y - y0) * S + cx * C + C - x0, k);
        }
        if (world != null) for (Prop q : world.props) if (!q.gone) box(page, x0, y0, q.x, q.y, q.w, q.h, (byte) q.kind, true);
        for (Kin k : kin) if (k.on) box(page, x0, y0, k.x(), k.y(), C, C, Terrain.KINETIC, true);
        for (int[] d : doors) box(page, x0, y0, d[0], d[1], d[2], d[3], Terrain.STEEL, false);
    }
    static void box(byte[] page, int x0, int y0, int x, int y, int w, int h, byte k, boolean onlyAir) {
        final int S = Terrain.PS;
        for (int j = Math.max(y, y0); j < Math.min(y + h, y0 + S); j++) for (int i = Math.max(x, x0); i < Math.min(x + w, x0 + S); i++) {
            int q = (j - y0) * S + i - x0; if (!onlyAir || page[q] == 0) page[q] = k;
        }
    }
    /** La habitación de una pieza (para saber si está activa). */
    boolean awake(float x, float y) { int i = roomAt(x, y); return i < 0 || i < active.length && active[i]; }
    void wake(World g) {
        if (active.length != rooms.size()) active = new boolean[rooms.size()];
        java.util.Arrays.fill(active, false);
        for (int i = 0; i < 2 && i < g.robots.length; i++) {
            Robot r = g.robots[i]; if (r.lives < 0) continue; int at = r.alive() ? roomAt(r.px, r.py - 20) : -1; if (at < 0) at = check[i]; if (at < 0) at = startRoom(i);
            if (at < 0) continue; int[] c = rooms.get(at);
            for (int k = 0; k < rooms.size(); k++) { int[] o = rooms.get(k); if (Math.abs(o[0] - c[0]) <= c[2] && Math.abs(o[1] - c[1]) <= c[3]) active[k] = true; } // ella y las 8 de alrededor
        }
    }
    public int wallColor() { switch (base.bg) { case 1: return 0xFF2E3B26; case 2: return 0xFF6B5A3E; case 3: return 0xFF2A2E3A; default: return 0xFF3A4150; } }
    public boolean held(int id) { return id >= 0 && id < 2 && held[id]; }

    // ---------- Piezas que se mueven
    /** Pieza que se mueve: en el mapa es material KINETIC (choca, no se ve); el Renderer la dibuja donde está. */
    public static final class Kin {
        public final int x0, y0, mat, color, move; public final boolean mirrored; public int dx, dy; public boolean on = true;
        Kin(int x, int y, int mat, int color, int move, boolean mirrored) { x0 = x; y0 = y; this.mat = mat; this.color = color; this.move = move; this.mirrored = mirrored; }
        public int x() { return x0 + dx; } public int y() { return y0 + dy; }
    }
    static final int PERIOD = 240, BLINK = 200, BLINK_ON = 120;
    void moveKin(World g) {
        if (kin.isEmpty()) return;
        double a = 2 * Math.PI * (g.tick % PERIOD) / PERIOD; boolean any = false;
        int[] nx = new int[kin.size()], ny = new int[kin.size()]; boolean[] non = new boolean[kin.size()], aw = new boolean[kin.size()];
        for (int i = 0; i < kin.size(); i++) {
            Kin k = kin.get(i); int s = k.mirrored ? -1 : 1, x = 0, y = 0; boolean on = true;
            aw[i] = awake(k.x0 + Base.CELL / 2f, k.y0 + Base.CELL / 2f); if (!aw[i]) { nx[i] = k.dx; ny[i] = k.dy; non[i] = k.on; continue; } // dormida: se queda quieta
            switch (k.move) {
                case Base.M_UPDOWN: y = -(int) Math.round(32 * (1 - StrictMath.cos(a))); break;
                case Base.M_SIDE: x = s * (int) Math.round(32 * (1 - StrictMath.cos(a))); break;
                case Base.M_SPIN: x = s * (int) Math.round(32 * StrictMath.sin(a)); y = -(int) Math.round(32 * (1 - StrictMath.cos(a))); break;
                case Base.M_BLINK: on = g.tick % BLINK < BLINK_ON; if (on && !k.on && robotIn(g, k.x0, k.y0, Base.CELL, Base.CELL)) on = false; break; // no aparece encima de nadie
                default:
            }
            nx[i] = x; ny[i] = y; non[i] = on; if (x != k.dx || y != k.dy || on != k.on) any = true;
        }
        if (!any) return;
        for (int i = 0; i < kin.size(); i++) { // los que van parados encima viajan con la pieza
            Kin k = kin.get(i); if (!k.on) continue; int mx = nx[i] - k.dx, my = ny[i] - k.dy; if (mx == 0 && my == 0) continue;
            for (Robot r : g.robots) if (r.alive() && Math.abs(r.py - k.y()) <= 1 && r.vy >= 0 && Math.abs(r.px - (k.x() + Base.CELL / 2)) < Base.CELL / 2 + Robot.HW && !r.carried) { r.px += mx; r.py = k.y() + my; r.carried = true; }
            for (Foe f : foes) if (f.alive && f.walks() && (int) f.y == k.y() && Math.abs(f.x - (k.x() + Base.CELL / 2)) < Base.CELL / 2 + f.hw) { f.x += mx; f.y += my; }
        }
        for (Robot r : g.robots) r.carried = false;
        Terrain t = g.t;
        for (int i = 0; i < kin.size(); i++) { Kin k = kin.get(i); if (aw[i] && k.on) paint(t, k.x(), k.y(), (byte) 0); }
        for (int i = 0; i < kin.size(); i++) { Kin k = kin.get(i); k.dx = nx[i]; k.dy = ny[i]; k.on = non[i]; }
        for (int i = 0; i < kin.size(); i++) { Kin k = kin.get(i); if (!aw[i] || !k.on) continue;
            paint(t, k.x(), k.y(), Terrain.KINETIC);
            for (Robot r : g.robots) if (r.alive() && overlaps(r, k.x(), k.y(), Base.CELL, Base.CELL)) { // si la pieza lo alcanza, lo empuja
                if (r.py - r.h / 2 < k.y() + Base.CELL / 2) r.py = k.y(); else r.px = r.px < k.x() + Base.CELL / 2 ? k.x() - Robot.HW - 1 : k.x() + Base.CELL + Robot.HW + 1;
            }
        }
    }
    static void paint(Terrain t, int x, int y, byte to) {
        for (int j = Math.max(0, y); j < Math.min(t.H, y + Base.CELL); j++) for (int i = Math.max(0, x); i < Math.min(t.W, x + Base.CELL); i++) {
            int k = t.get(i, j); if (to == 0 ? k == Terrain.KINETIC : k == 0 || k == Terrain.DEBRIS || k == Terrain.DECOR) t.set(i, j, to);
        }
    }
    static boolean overlaps(Robot r, int x, int y, int w, int h) { return r.px + Robot.HW > x && r.px - Robot.HW < x + w && r.py > y && r.py - r.h < y + h; } // el robot ocupa las filas py - h .. py - 1
    static boolean robotIn(World g, int x, int y, int w, int h) { for (Robot r : g.robots) if (r.alive() && overlaps(r, x - 2, y - 2, w + 4, h + 4)) return true; return false; }

    // ---------- Enemigos
    public static final int DRONE = Base.E_DRONE, TURRET = Base.E_TURRET, BADPAL = Base.E_BADPAL, BOSS = 9;
    public static final class Foe {
        public final int type; public float x, y, vx, vy, hp, maxHp, aim; public int t, state, dir = 1, hurtT; public boolean alive = true; public final boolean mirrored;
        public final float hw, h; final float homeX, homeY;
        Foe(int type, float x, float y, boolean mirrored) {
            this.type = type; this.x = x; this.y = y; homeX = x; homeY = y; this.mirrored = mirrored; dir = mirrored ? -1 : 1;
            switch (type) {
                case DRONE: hw = 12; h = 16; hp = 10; this.y = y - 8; break;
                case TURRET: hw = 9; h = 15; hp = 10; break;
                case BADPAL: hw = 10; h = 18; hp = 12; break;
                default: hw = 30; h = 62; hp = 300;
            }
            maxHp = hp;
        }
        boolean walks() { return type == TURRET || type == BADPAL || type == BOSS; }
        public boolean sitting() { return type == BADPAL && state == 1; }
        float top() { return y - (sitting() ? 12 : h); }
        /** Los enemigos del suelo son chicos, pero se les pega a la altura de la mano del personaje. */
        boolean hit(float px, float py, float pad) { float tp = type == TURRET || type == BADPAL ? Math.min(top(), y - 38) : top(); return alive && px > x - hw - pad && px < x + hw + pad && py > tp - pad && py < y + pad; }
    }
    /** Disparo de un enemigo: 0 bola de energía, 1 misil que cae, 2 onda por el suelo (jefe). */
    public static final class Bullet { public int kind, life = 300; public float x, y, vx, vy; boolean dead; public boolean mine; } // mine: la devolvió el FAN, ahora daña a los enemigos

    void thing(int what, float x, float bottom, boolean mirrored) {
        if (what == Base.START) { starts[mirrored ? 1 : 0] = new float[]{x, bottom}; return; }
        if (what == Base.P_CAPSULE || what == Base.P_LIFE || what == Base.P_WEAPON) { prizes.add(new float[]{what - Base.P_CAPSULE, x, bottom - 10}); return; }
        if (what == DRONE || what == TURRET || what == BADPAL) foes.add(new Foe(what, x, bottom, mirrored));
    }
    Robot target(World g, Foe f, float range) {
        Robot best = null; float bd = range;
        for (Robot r : g.robots) if (r.alive() && !held(r.id)) { float d = (float) StrictMath.hypot(r.px - f.x, r.py - 27 - (f.y - f.h / 2)); if (d < bd) { bd = d; best = r; } }
        return best;
    }
    boolean sees(World g, float x0, float y0, float x1, float y1) {
        float d = (float) StrictMath.hypot(x1 - x0, y1 - y0); int n = (int) (d / 6);
        for (int i = 1; i < n; i++) { float k = i / (float) n; if (g.t.solid((int) (x0 + (x1 - x0) * k), (int) (y0 + (y1 - y0) * k))) return false; }
        return true;
    }
    void fire(World g, float x, float y, float tx, float ty, float speed, int kind) {
        if (bullets.size() > 120) return;
        float d = Math.max(1, (float) StrictMath.hypot(tx - x, ty - y)); Bullet b = new Bullet(); b.kind = kind; b.x = x; b.y = y; b.vx = (tx - x) / d * speed; b.vy = (ty - y) / d * speed; bullets.add(b);
        g.fx(Fx.FOE, x, y, kind, -1);
    }
    void fall(World g, Foe f) {
        f.vy = Math.min(6, f.vy + .25f); float ny = f.y + f.vy;
        if (g.t.solid((int) f.x, (int) ny + 1) || g.t.solid((int) (f.x - f.hw + 2), (int) ny + 1) || g.t.solid((int) (f.x + f.hw - 2), (int) ny + 1)) {
            int y = (int) f.y; while (y < (int) ny + 1 && !g.t.solid((int) f.x, y + 1) && !g.t.solid((int) (f.x - f.hw + 2), y + 1) && !g.t.solid((int) (f.x + f.hw - 2), y + 1)) y++;
            f.y = y; if (f.vy > 4 && f.type == BOSS) { g.fx(Fx.SLAM, f.x, f.y, 2, -1); for (int s = -1; s <= 1; s += 2) { Bullet b = new Bullet(); b.kind = 2; b.x = f.x + s * (f.hw + 4); b.y = f.y - 6; b.vx = s * (f.hp < f.maxHp / 2 ? 4.2f : 3.6f); b.life = 140; bullets.add(b); } }
            f.vy = 0;
        } else f.y = ny;
    }
    boolean grounded(World g, Foe f) { return g.t.solid((int) f.x, (int) f.y + 1) || g.t.solid((int) (f.x - f.hw + 2), (int) f.y + 1) || g.t.solid((int) (f.x + f.hw - 2), (int) f.y + 1); }
    void stepFoe(World g, Foe f) {
        f.t++; if (f.hurtT > 0) f.hurtT--;
        int[] room = rooms.get(Math.max(0, roomAt(f.homeX, f.homeY - 4)));
        switch (f.type) {
            case DRONE: {
                Robot r = target(g, f, 230);
                if (f.state == 0) { f.y = f.homeY + (float) StrictMath.sin(f.t * .05) * 4; if (r != null) { f.state = 1; f.dir = r.px < f.x ? -1 : 1; f.t = 0; } }
                else {
                    float nx = f.x + f.dir * 1.2f; if (g.t.solid((int) (nx + f.dir * f.hw), (int) (f.y - f.h / 2)) || nx < room[0] + f.hw || nx > room[0] + room[2] - f.hw) f.dir = -f.dir; else f.x = nx;
                    f.y = f.homeY + (float) StrictMath.sin(f.t * .05) * 22;
                    if (f.t % 80 == 40) { Bullet b = new Bullet(); b.kind = 1; b.x = f.x; b.y = f.y + 2; b.vy = 1; bullets.add(b); g.fx(Fx.FOE, f.x, f.y, 1, -1); }
                }
                if (r != null) f.aim = (float) StrictMath.atan2(r.py - 27 - f.y, r.px - f.x);
                break; }
            case TURRET: {
                fall(g, f); Robot r = target(g, f, 260); float ey = f.y - 10;
                if (r != null && sees(g, f.x, ey, r.px, r.py - 27)) {
                    f.aim = (float) StrictMath.atan2(r.py - 27 - ey, r.px - f.x); f.dir = r.px < f.x ? -1 : 1;
                    if (++f.state >= 70) { f.state = 0; fire(g, f.x + (float) StrictMath.cos(f.aim) * 9, ey + (float) StrictMath.sin(f.aim) * 9, r.px, r.py - 27, 2.6f, 0); }
                } else f.state = Math.min(f.state, 40);
                break; }
            case BADPAL: {
                fall(g, f); Robot r = target(g, f, 180); boolean ground = grounded(g, f);
                if (f.state == 0 || f.state == 2) { // camina; en 2 se está cambiando de lugar
                    if (ground) {
                        float nx = f.x + f.dir * .8f; boolean wall = g.t.solid((int) (nx + f.dir * f.hw), (int) f.y - 8), edge = !g.t.solid((int) (nx + f.dir * (f.hw + 2)), (int) f.y + 3);
                        if (wall || edge || nx < room[0] + f.hw || nx > room[0] + room[2] - f.hw) f.dir = -f.dir; else f.x = nx;
                    }
                    if (f.state == 2 && f.t > 70) { f.state = 0; f.t = 0; }
                    if (f.state == 0 && r != null && ground) { f.state = 1; f.t = 0; f.dir = r.px < f.x ? -1 : 1; } // se sienta
                } else { // sentado: apunta, dispara y se va a otro lugar
                    if (r != null) { f.aim = (float) StrictMath.atan2(r.py - 27 - (f.y - 8), r.px - f.x); f.dir = r.px < f.x ? -1 : 1; }
                    if (f.t == 34 && r != null) fire(g, f.x + f.dir * 8, f.y - 8, r.px, r.py - 27, 2.8f, 0);
                    if (f.t > 60) { f.state = 2; f.t = 0; f.dir = g.rng.i(2) == 0 ? -1 : 1; }
                }
                break; }
            default: { // jefe: salta hacia el jugador y aplasta (ondas por el suelo); entre saltos dispara en abanico. Con media vida, furioso: más rápido y más balas
                fall(g, f); Robot r = target(g, f, 2000); boolean ground = grounded(g, f), mad = f.hp < f.maxHp / 2;
                if (r != null) { f.dir = r.px < f.x ? -1 : 1; float lead = r.vx * 14; f.aim = (float) StrictMath.atan2(r.py - 27 - (f.y - 40), r.px + lead - f.x); } // apunta adelantándose
                if (ground) f.vx = 0;
                float nx = f.x + f.vx; if (!g.t.solid((int) (nx + Math.signum(f.vx) * f.hw), (int) f.y - 20) && nx > room[0] + f.hw && nx < room[0] + room[2] - f.hw) f.x = nx; else f.vx = 0;
                int period = mad ? 110 : 150, cyc = f.t % period;
                if (r != null && ground && cyc == 0) { f.vy = mad ? -8.5f : -7.5f; f.vx = Math.max(-4.5f, Math.min(4.5f, (r.px - f.x) / 44f)); f.y -= 2; }
                if (r != null && ground && (cyc == 50 || cyc == 75 || mad && cyc == 95)) { int n = mad ? 2 : 1;
                    for (int s = -n; s <= n; s++) { float a = f.aim + s * .2f; fire(g, f.x + f.dir * 20, f.y - 40, f.x + f.dir * 20 + (float) StrictMath.cos(a) * 100, f.y - 40 + (float) StrictMath.sin(a) * 100, mad ? 3.8f : 3.3f, 0); } }
                if (r != null && !ground && f.vy > 0 && Math.abs(r.px - f.x) < 60) f.vx *= .8f; // cae encima
            }
        }
        for (Robot r : g.robots) if (r.alive() && !held(r.id) && r.px + Robot.HW > f.x - f.hw && r.px - Robot.HW < f.x + f.hw && r.py > f.top() && r.py - r.h < f.y) {
            boolean crush = f.type == BOSS && f.vy > 2 && r.py - r.h > f.y - 30;
            r.damage(g, crush ? 28 : f.type == BOSS ? 14 : 7, (r.px < f.x ? -1 : 1) * 2.5f, -2, -1, f.x, f.y - f.h / 2); // tocarlo duele
        }
    }
    /** Un disparo o una explosión del jugador. Devuelve si tocó a alguien. */
    public boolean hurtFoe(World g, float x, float y, float dmg, float pad) {
        for (Foe f : foes) if (f.hit(x, y, pad)) { hurt(g, f, dmg); return true; }
        return false;
    }
    void hurt(World g, Foe f, float dmg) {
        if (!f.alive || dmg <= 0) return; f.hp -= Math.max(3, dmg) * (f.type == BOSS ? .8f : 1); f.hurtT = 8;
        if (f.hp <= 0) {
            f.alive = false; g.fx(Fx.EXPLODE, f.x, f.y - f.h / 2, f.type == BOSS ? 60 : 20, -1).b = 0; g.fx(Fx.FOE_DIE, f.x, f.y - f.h / 2, f.type, -1);
            if (f == boss) win(g);
        }
    }
    void blast(World g, float x, float y, int r, float dmg) {
        for (Foe f : foes) if (f.alive) { float d = (float) StrictMath.hypot(f.x - x, f.y - f.h / 2 - y); if (d < r + f.hw) hurt(g, f, dmg * Math.max(.35f, 1 - d / (r + f.hw))); }
    }
    void stepBullets(World g) {
        for (Bullet b : bullets) {
            if (--b.life <= 0) { b.dead = true; continue; }
            if (b.kind == 1) b.vy = Math.min(6, b.vy + .12f);
            b.x += b.vx; b.y += b.vy;
            if (b.kind == 2 && !g.t.solid((int) b.x, (int) b.y + 8)) { b.dead = true; continue; } // la onda se acaba donde se acaba el suelo
            if (b.mine && hurtFoe(g, b.x, b.y, b.kind == 2 ? 10 : 8, 2)) { b.dead = true; if (b.kind == 1) g.explode(b.x, b.y, 18, 9, 8, 2, -1); continue; }
            Robot hit = null; if (!b.mine) for (Robot r : g.robots) if (r.alive() && !held(r.id) && Math.abs(r.px - b.x) < Robot.HW + 3 && b.y > r.py - r.h - 3 && b.y < r.py + 3) { hit = r; break; }
            boolean wall = g.t.solid((int) b.x, (int) b.y) && g.t.at((int) b.x, (int) b.y) != Terrain.KINETIC || b.kind != 2 && g.t.at((int) b.x, (int) b.y) == Terrain.KINETIC;
            if (hit != null || wall) {
                b.dead = true;
                if (b.kind == 1) g.explode(b.x, b.y, 18, 9, 8, 2, -1);
                else if (hit != null) hit.damage(g, b.kind == 2 ? 10 : 6, Math.signum(b.vx) * 1.5f, -1, -1, b.x, b.y);
                else g.fx(Fx.CHIPS, b.x, b.y, 2, 0);
            }
        }
        bullets.removeIf(b -> b.dead);
    }
    /** Los disparos del jugador contra los enemigos. */
    void playerShots(World g) {
        for (int i = 0, n = g.shots.size(); i < n && i < g.shots.size(); i++) { // por índice: una explosión puede soltar esquirlas (nuevos disparos) mientras se recorre
            Shot s = g.shots.get(i);
            if (s.dead || s.wait > 0 || s.owner < 0 || s.w.dmg <= 0 && s.w.radius <= 0) continue;
            for (Foe f : foes) if (f.hit(s.x, s.y, 3)) {
                if (s.w.radius > 0) s.explode(g); else { hurt(g, f, s.w.dmg); if (!s.w.pierce) s.dead = true; }
                break;
            }
        }
    }

    // ---------- Habitaciones, precipicios, inicio y la sala del jefe
    /** Hay habitación pegada en esa dirección (para la cámara y el negro de los precipicios). */
    public boolean neighbor(int idx, int dx, int dy) {
        int[] r = rooms.get(idx); float x = r[0] + r[2] / 2f + dx * r[2], y = r[1] + r[3] / 2f + dy * r[3]; return roomAt(x, y) >= 0;
    }
    public int lastRoom(int who) { return who >= 0 && who < 2 ? check[who] : -1; }
    /** En el precipicio bajo una habitación sin piso. */
    public boolean pitAt(float x, float y) { for (int[] r : rooms) if (r[6] == 1 && x >= r[0] && x < r[0] + r[2] && y >= r[1] + r[3] && y < r[1] + r[3] + Base.PAD * Base.CELL) return true; return false; }
    public int roomAt(float x, float y) { for (int i = 0; i < rooms.size(); i++) { int[] r = rooms.get(i); if (x >= r[0] && x < r[0] + r[2] && y >= r[1] && y < r[1] + r[3]) return i; } return -1; }
    int startRoom(int who) { float[] s = start(who); return roomAt(s[0], s[1] - 4); }
    float[] start(int who) {
        float[] s = starts[who]; if (s != null) return s;
        if (who == 1 && starts[0] != null && base.bossRoom() != null) { // reflejo del inicio
            int[] b = base.bossRoom(); float cx = ox + b[0] * Base.ROOM_PX + Base.ROOM_PX / 2f; return new float[]{2 * cx - starts[0][0], starts[0][1]};
        }
        int[] r = rooms.get(0); return new float[]{r[0] + 64, r[1] + r[3] - 40};
    }
    /** Dónde reaparece: el inicio, o la entrada de la habitación donde murió (punto de control). */
    public int[] spawnFor(World g, int who) {
        int room = who < 2 ? check[who] : -1;
        if (room < 0 || room == startRoom(who)) { float[] s = start(who); return new int[]{(int) s[0], (int) s[1]}; }
        if (spawnCache[room % 64] == null) spawnCache[room % 64] = entry(g, room);
        return spawnCache[room % 64].clone();
    }
    /** Primer lugar firme de la habitación (desde el lado por donde se entra), con espacio para el personaje. */
    int[] entry(World g, int idx) {
        int[] r = rooms.get(idx); boolean fromRight = r[5] == 1;
        for (int k = 1; k < Base.RW - 1; k++) {
            int cx = fromRight ? Base.RW - 1 - k : k, x = r[0] + cx * Base.CELL + Base.CELL / 2;
            for (int cy = Base.RH - 1; cy >= 2; cy--) {
                int y = r[1] + cy * Base.CELL; // borde de arriba del cuadro: el piso
                if (g.t.solid(x, y) && g.t.at(x, y) != Terrain.KINETIC && !g.t.solid(x, y - 2) && !g.t.solid(x, y - 58) && !g.t.solid(x - 8, y - 30) && !g.t.solid(x + 8, y - 30)) return new int[]{x, y};
            }
        }
        return new int[]{r[0] + r[2] / 2, r[1] + 40};
    }
    /** Adentro de verdad: lejos de todas las puertas (si entra cayendo desde arriba, recién cuenta al pisar el suelo). */
    boolean insideBoss(Robot r, int[] room) { return r.px - Robot.HW > room[0] + 16 && r.px + Robot.HW < room[0] + room[2] - 16 && r.py - r.h > room[1] + 16 && r.py < room[1] + room[3] - 4 && r.grounded; }
    /** El FAN sopla todo: balas de los enemigos (las devuelve contra ellos) y a los enemigos mismos (el jefe apenas se mueve). */
    public void fan(World g, float x, float y, float vx, float vy) {
        for (Bullet b : bullets) if (Math.abs(b.x - x) < 14 && Math.abs(b.y - y) < 14) { b.vx += vx * .22f; b.vy += vy * .22f; if (b.kind == 2) b.kind = 0; if (b.vx * vx > 0) b.mine = true; }
        for (Foe f : foes) if (f.alive && f.hit(x, y, 6)) {
            float k = f.type == BOSS ? .03f : f.type == DRONE ? .45f : .25f, nx = f.x + vx * k;
            if (!g.t.solid((int) (nx + Math.signum(vx) * f.hw), (int) (f.y - f.h / 2))) f.x = nx;
            if (f.type == DRONE) { float ny = f.y + vy * k; if (!g.t.solid((int) f.x, (int) (ny - f.h))) f.y = ny; }
            if (f.type == BADPAL && f.state == 1) f.t = Math.max(0, f.t - 1); // lo desconcentra: tarda más en disparar
        }
    }
    void closeDoors(World g, int[] room) {
        int x = room[0], y = room[1], w = room[2], h = room[3], th = 10;
        int[][] sides = {{x, y, th, h}, {x + w - th, y, th, h}, {x, y, w, th}, {x, y + h - th, w, th}};
        for (int[] s : sides) {
            int mx = s[0] + s[2] / 2 + (s == sides[0] ? -20 : s == sides[1] ? 20 : 0), my = s[1] + s[3] / 2 + (s == sides[2] ? -20 : s == sides[3] ? 20 : 0);
            if (roomAt(mx, my) < 0) continue; // ahí ya hay pared o precipicio
            doors.add(s);
            for (Robot r : g.robots) if (r.alive() && r.px + Robot.HW > s[0] && r.px - Robot.HW < s[0] + s[2] && r.py > s[1] && r.py - r.h < s[1] + s[3]) { // nadie queda atrapado en una puerta
                if (s[3] > s[2]) r.px = s[0] < x + w / 2 ? s[0] + s[2] + Robot.HW + 1 : s[0] - Robot.HW - 1; else r.py = s[1] < y + h / 2 ? s[1] + s[3] + r.h + 1 : s[1];
            }
            for (int j = s[1]; j < s[1] + s[3]; j++) for (int i = s[0]; i < s[0] + s[2]; i++) g.t.set(i, j, Terrain.STEEL);
            g.t.markArea(s[0], s[1], s[0] + s[2], s[1] + s[3]);
        }
        g.fx(Fx.DOOR, x + w / 2f, y + h / 2f, 0, -1);
    }
    void openDoors(World g) {
        java.util.ArrayList<int[]> old = new java.util.ArrayList<>(doors); doors.clear();
        for (int[] s : old) { for (int j = s[1]; j < s[1] + s[3]; j++) for (int i = s[0]; i < s[0] + s[2]; i++) g.t.set(i, j, 0); g.t.markArea(s[0], s[1], s[0] + s[2], s[1] + s[3]); }
    }
    public int bossIdx() { for (int i = 0; i < rooms.size(); i++) if (rooms.get(i)[4] == 1) return i; return -1; }
    void boss(World g) {
        int bi = bossIdx(); if (bi < 0 || g.finished) return; int[] room = rooms.get(bi);
        if (!online) {
            Robot p = g.robots[0];
            if (!bossOn && p.alive() && insideBoss(p, room)) {
                bossOn = true; closeDoors(g, room);
                float bx = p.px < room[0] + room[2] / 2 ? room[0] + room[2] * .75f : room[0] + room[2] * .25f;
                boss = new Foe(BOSS, bx, room[1] + 40, false); foes.add(boss);
            }
            return;
        }
        if (fight) { for (Robot r : g.robots) if (r.deadT > 0 || r.hp <= 0 || r.lives < 0) { finish(g, 1 - r.id); return; } return; }
        for (Robot r : g.robots) if (r.alive() && !held[r.id] && insideBoss(r, room)) { held[r.id] = true; r.vx = 0; g.fx(Fx.DOOR, r.px, r.py - 27, 1, r.id); }
        if (held[0] && held[1]) { // llegaron los dos: se cierran las puertas y pelean
            fight = true; held[0] = held[1] = false; closeDoors(g, room);
            for (Robot r : g.robots) { r.hp = r.maxHp; r.invuln = 90; }
            g.fx(Fx.FIGHT, room[0] + room[2] / 2f, room[1] + room[3] / 2f, 0, -1);
        }
    }
    void win(World g) { won = true; openDoors(g); finish(g, 0); }
    void finish(World g, int winner) { if (g.finished) return; g.finished = true; g.winner = winner; g.winnerTeam = winner < 0 ? -1 : g.robots[winner].team; }
    /** Sin vidas: solo, se pierde; en línea, gana el otro. */
    void checkEnd(World g) {
        if (g.finished) return;
        if (!online) { if (g.robots[0].lives < 0) finish(g, -1); return; }
        for (Robot r : g.robots) if (r.lives < 0) { finish(g, 1 - r.id); return; }
    }

    // ---------- Cada tick
    /** Antes de mover a los personajes: el que espera en la sala del jefe no se mueve. */
    void pre(World g, int[] in) { for (int i = 0; i < Math.min(2, in.length); i++) if (held[i]) in[i] = 0; }
    void post(World g) {
        wake(g);
        moveKin(g);
        for (Foe f : foes) if (f.alive && awake(f.homeX, f.homeY - 4)) stepFoe(g, f); // los de habitaciones dormidas esperan
        if (g.tick % 60 == 0) { java.util.ArrayList<int[]> keep = new java.util.ArrayList<>(); for (int i = 0; i < rooms.size(); i++) if (active[i]) keep.add(rooms.get(i)); g.t.release(keep, Base.PAD * Base.CELL + 8); }
        playerShots(g); stepBullets(g);
        for (int i = 0; i < g.robots.length && i < 2; i++) {
            Robot r = g.robots[i]; if (!r.alive() || r.finalT > 0) continue;
            int at = roomAt(r.px, r.py - 20);
            if (at < 0) { // fuera de las habitaciones: cae al precipicio, a lo negro, y al rato pierde la vida
                int[] last = check[i] >= 0 ? rooms.get(check[i]) : null;
                if (++fallT[i] > 45 || last == null || r.py - r.h > last[1] + last[3] + 8) { fallT[i] = 0; g.fx(Fx.FALL, r.px, r.py, 0, r.id); r.die(g, -1); }
                continue;
            }
            fallT[i] = 0; check[i] = at;
            if (held[i]) { r.vx = 0; r.invuln = Math.max(r.invuln, 2); }
        }
        boss(g);
        if (bossOn && boss != null && !boss.alive && !won) win(g);
    }
    int hash() {
        int h = bullets.size() * 31 + (fight ? 7 : 0) + (bossOn ? 13 : 0);
        for (Foe f : foes) h = h * 31 + Float.floatToIntBits(f.x) + 3 * Float.floatToIntBits(f.y) + (int) (f.hp * 8) + (f.alive ? 1 : 0);
        for (Bullet b : bullets) h = h * 31 + Float.floatToIntBits(b.x) + 7 * Float.floatToIntBits(b.y);
        return h;
    }
}
