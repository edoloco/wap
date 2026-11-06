package com.wap.core;
import java.util.ArrayList;
/** Estado completo de una partida. Avanza con step(entradas) a 60 ticks por segundo, sin leer el reloj ni el dispositivo. */
public final class World {
    /** Versión de las reglas: dos teléfonos solo juegan en línea si coincide. */
    public static final int RULES = 8;
    public final Terrain t = new Terrain(); public final Rng rng; public int tick, bonusT = 400;
    public final Robot[] robots = new Robot[2];
    public final ArrayList<Shot> shots = new ArrayList<>(); public final ArrayList<Bonus> bonus = new ArrayList<>(); public final ArrayList<Prop> props = new ArrayList<>();
    public final ArrayList<Fx> fx = new ArrayList<>(); public Fx lastFx;
    public boolean finished; public int winner = -1;

    public World(int seed, int lives, Wpn[] a, Wpn[] b) {
        rng = new Rng(seed); t.generate(rng);
        robots[0] = new Robot(0, lives); robots[1] = new Robot(1, lives);
        System.arraycopy(a, 0, robots[0].loadout, 0, Robot.SLOTS); System.arraycopy(b, 0, robots[1].loadout, 0, Robot.SLOTS);
        for (int[] q : t.props) props.add(new Prop(q));
        robots[1].px = -9999; robots[0].spawnAt(this); robots[1].spawnAt(this);
        robots[1].facing = robots[0].px < robots[1].px ? -1 : 1; robots[0].facing = -robots[1].facing;
    }
    public Fx fx(int type, float x, float y, float a, int who) {
        Fx f = new Fx(); f.type = type; f.x = x; f.y = y; f.a = a; f.who = who; if (fx.size() < 800) fx.add(f); lastFx = f; return f;
    }
    /** Busca un lugar libre lejos del rival. */
    public int[] freeSpot(Robot other) {
        int[] best = {300, 300}; float bd = -1;
        for (int n = 0; n < 60; n++) {
            int x = 40 + rng.i(Terrain.W - 80), y = 40 + rng.i(Terrain.H - 120);
            boolean ok = true;
            for (int a = -12; a <= 12 && ok; a += 6) for (int b = -60; b <= 0 && ok; b += 6) if (t.solid(x + a, y + b)) ok = false;
            if (!ok) continue;
            while (y < Terrain.H - 20 && !t.solid(x, y) && !t.solid(x - 7, y) && !t.solid(x + 7, y)) y++;
            float d = (float) StrictMath.hypot(x - other.px, y - other.py); d = Math.min(d, 900) + rng.r(0, 200);
            if (d > bd) { bd = d; best[0] = x; best[1] = y - 1; }
        }
        return best;
    }
    public void spawn(Wpn w, int owner, float x, float y, float vx, float vy) {
        if (shots.size() > 500) return;
        Shot s = new Shot(); s.w = w; s.owner = owner; s.x = x; s.y = y; s.vx = vx; s.vy = vy; s.life = w.fuse; shots.add(s);
    }
    public void explode(float x, float y, int r, float dmg, int dig, float knock, int owner) {
        if (dig > 0) t.carve((int) x, (int) y, dig);
        fx(Fx.EXPLODE, x, y, r, owner);
        for (int i = 0; i < props.size(); i++) {
            Prop p = props.get(i); if (p.gone) continue;
            float nx = Math.max(p.x, Math.min(x, p.x + p.w)), ny = Math.max(p.y, Math.min(y, p.y + p.h)), d = (float) StrictMath.hypot(nx - x, ny - y);
            if (d < r) hurt(p, dmg * (.4f + .6f * (1 - d / r)));
        }
        float reach = r + 14;
        for (Robot o : robots) if (o.alive()) {
            float dx = o.px - x, dy = o.py - 27 - y, d = (float) StrictMath.hypot(dx, dy);
            if (d < reach) {
                float f = 1 - d / reach, nd = Math.max(1, d);
                o.damage(this, dmg * (.35f + .65f * f), dx / nd * knock * (.4f + f), dy / nd * knock * (.4f + f) - knock * .35f, owner, x, y);
            }
        }
    }
    /** Daña el objeto en ese punto, si hay uno. */
    public boolean hitProp(float x, float y, float d) {
        for (int i = 0; i < props.size(); i++) { Prop p = props.get(i); if (!p.gone && p.has(x, y)) { hurt(p, d); return true; } }
        return false;
    }
    private void hurt(Prop p, float d) {
        if (p.gone || d <= 0) return;
        p.hp -= d; p.hitT = 6;
        if (p.hp <= 0) shatter(p);
    }
    private void shatter(Prop p) {
        p.gone = true; t.erase(p.x, p.y, p.w, p.h);
        Fx f = fx(Fx.PROP, p.x, p.y, p.w, 0); f.b = p.h; f.c = p.kind;
    }
    /** Lo que se queda sin apoyo cae y se rompe. */
    void support() {
        for (int i = 0; i < props.size(); i++) {
            Prop p = props.get(i); if (p.gone) continue;
            boolean held = false;
            for (int x = p.x + 3; x < p.x + p.w && !held; x += 6) held = t.solid(x, p.y + p.h + 1);
            if (!held) { shatter(p); i = -1; }
        }
    }
    public void beam(Wpn w, int owner, float x, float y, float dx, float dy) {
        float cx = x, cy = y; int n = w.pierce ? 300 : 240; boolean hit = false;
        for (int i = 0; i < n; i++) {
            cx += dx * 3; cy += dy * 3;
            if (!hit) for (Robot r : robots) if (r.id != owner && r.alive() && r.hits(cx, cy)) {
                r.damage(this, w.dmg, dx * w.knock, dy * w.knock - .2f, owner, cx, cy); hit = true; if (!w.pierce) { i = n; }
            }
            if (t.solid((int) cx, (int) cy)) {
                if (t.at((int) cx, (int) cy) == 2) break;
                if (Terrain.isProp(t.at((int) cx, (int) cy))) { hitProp(cx, cy, w.dmg); break; }
                if (w.pierce) { if (i % 4 == 0) t.carve((int) cx, (int) cy, w.dig); }
                else { t.carve((int) cx, (int) cy, w.dig); fx(Fx.CHIPS, cx, cy, 2, 0); break; }
            }
        }
        Fx f = fx(Fx.BEAM, x, y, 0, owner); f.c = cx; f.d = cy; f.b = w.pierce ? 1 : 0;
    }
    void over(int win, int lost) { finished = true; winner = win; }
    public void step(int[] in) {
        tick++;
        for (int i = 0; i < 2; i++) robots[i].step(this, in[i]);
        for (int i = 0; i < shots.size(); i++) { Shot s = shots.get(i); if (!s.dead) s.step(this); }
        for (int i = shots.size() - 1; i >= 0; i--) if (shots.get(i).dead) shots.remove(i);
        if (--bonusT <= 0 && bonus.size() < 4) {
            bonusT = 500 + rng.i(400);
            int[] p = freeSpot(robots[0]); Bonus b = new Bonus(); b.x = p[0]; b.y = p[1] - 30; b.kind = rng.i(2); bonus.add(b);
        }
        if (tick % 20 == 0) support();
        for (Prop p : props) if (p.hitT > 0) p.hitT--;
        for (int i = bonus.size() - 1; i >= 0; i--) {
            Bonus b = bonus.get(i); if (--b.life <= 0) { bonus.remove(i); continue; }
            b.vy = Math.min(5, b.vy + .2f); float ny = b.y + b.vy;
            if (t.solid((int) b.x, (int) ny)) b.vy = 0; else b.y = ny;
            for (Robot r : robots) if (r.alive() && Math.abs(r.px - b.x) < 16 && b.y > r.py - r.h - 8 && b.y < r.py + 8) {
                if (b.kind == 0) r.heal(40);
                else for (int k = 0; k < Robot.SLOTS; k++) { r.reload[k] = 0; r.ammo[k] = r.loadout[k].clip; }
                fx(Fx.PICK, b.x, b.y, b.kind, r.id); bonus.remove(i); break;
            }
        }
    }
    /** Huella del estado: en el modo online detectará desincronizaciones. */
    public int hash() {
        int h = rng.s ^ tick;
        for (Robot r : robots) h = h * 31 + r.px * 7 + r.py * 13 + (int) (r.hp * 16) + r.lives + Float.floatToIntBits(r.vx) + r.armor;
        h = h * 31 + shots.size();
        for (Shot s : shots) h = h * 31 + Float.floatToIntBits(s.x) + 7 * Float.floatToIntBits(s.y);
        for (Prop p : props) h = h * 31 + Float.floatToIntBits(p.hp);
        for (Robot r : robots) h = h * 31 + r.stunT + 3 * r.thrownT + 5 * Float.floatToIntBits(r.vy);
        return h;
    }
}
