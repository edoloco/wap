package com.wap.core;
import java.util.ArrayList;
/** Estado completo de una partida. Avanza con step(entradas) a 60 ticks por segundo, sin leer el reloj ni el dispositivo. */
public final class World {
    /** Versión de las reglas: dos teléfonos solo juegan en línea si coincide. */
    public static final int RULES = 15;
    /** Versión de reglas para jugar en línea: incluye las armas de armas.txt, así ambos teléfonos deben tener las mismas. */
    public static int rules() { return Wpn.signature == 0 ? RULES : RULES ^ Wpn.signature & 0x7FFFFF00; }
    public final Terrain t = new Terrain(); public final Rng rng; public int tick, bonusT = 400;
    public final Robot[] robots;
    /** Equipo ganador al terminar (-1 si nadie). winner es un robot de ese equipo. */
    public int winnerTeam = -1;
    public final ArrayList<Shot> shots = new ArrayList<>(); public final ArrayList<Bonus> bonus = new ArrayList<>(); public final ArrayList<Prop> props = new ArrayList<>();
    public final ArrayList<Fx> fx = new ArrayList<>(); public Fx lastFx;
    public boolean finished; public int winner = -1;

    /** Altura del agua (-1: sin agua). Bajo el agua se cae más lento y se corre más despacio. */
    public int water = -1;
    public World(int seed, int lives, Wpn[] a, Wpn[] b) { this(seed, lives, a, b, null); }
    /** Partida en un nivel guardado (o aleatorio si level es null). */
    public World(int seed, int lives, Wpn[] a, Wpn[] b, Level level) { this(seed, lives, new Wpn[][]{a, b}, new int[]{0, 1}, level); }
    /** Partida con varios robots (2 a 5). teams[i] es el equipo de cada uno. */
    public World(int seed, int lives, Wpn[][] loads, int[] teams, Level level) {
        rng = new Rng(seed);
        if (level != null) { level.apply(t); water = level.water; } else t.generate(rng);
        int n = loads.length; robots = new Robot[n];
        for (int i = 0; i < n; i++) { robots[i] = new Robot(i, lives); robots[i].team = teams[i]; System.arraycopy(loads[i], 0, robots[i].loadout, 0, Robot.SLOTS); robots[i].px = -9999; }
        for (int[] q : t.props) props.add(new Prop(q));
        for (Robot r : robots) r.spawnAt(this);
        for (Robot r : robots) { Robot o = nearest(r, false); if (o != null) r.facing = o.px < r.px ? -1 : 1; }
        woodDmg = new int[FW * FH];
    }
    /** El robot vivo más cercano (enemigo si enemies). */
    public Robot nearest(Robot r, boolean enemies) {
        Robot best = null; float bd = 1e9f;
        for (Robot o : robots) { if (o == r || !o.alive() || o.px < -1000 || enemies && o.team == r.team) continue; float d = Math.abs(o.px - r.px) + Math.abs(o.py - r.py); if (d < bd) { bd = d; best = o; } }
        return best;
    }
    /** Termina cuando queda un solo equipo con vidas. */
    void checkEnd() {
        int team = -1; boolean many = false;
        for (Robot r : robots) if (r.lives >= 0) { if (team < 0) team = r.team; else if (r.team != team) many = true; }
        if (many) return;
        finished = true; winnerTeam = team; winner = -1;
        for (Robot r : robots) if (r.team == team) { winner = r.id; break; }
    }
    /** Editor: agrega un objeto ya puesto en el mapa. */
    public void addProp(int[] q) { if (q != null) props.add(new Prop(q)); }
    /** Editor: quita los objetos que toca un círculo. */
    public void removeProps(int cx, int cy, int r) {
        for (Prop p : props) if (!p.gone) {
            float nx = Math.max(p.x, Math.min(cx, p.x + p.w)), ny = Math.max(p.y, Math.min(cy, p.y + p.h));
            if ((nx - cx) * (nx - cx) + (ny - cy) * (ny - cy) <= r * r) { p.gone = true; t.erase(p.x, p.y, p.w, p.h); t.props.removeIf(q -> q[0] == p.x && q[1] == p.y); }
        }
        props.removeIf(p -> p.gone);
    }
    /** ¿Está bajo el agua ese punto? */
    public boolean wet(float y) { return water >= 0 && y > water; }
    public Fx fx(int type, float x, float y, float a, int who) {
        Fx f = new Fx(); f.type = type; f.x = x; f.y = y; f.a = a; f.who = who; if (fx.size() < 800) fx.add(f); lastFx = f; return f;
    }
    /** Busca un lugar libre lejos del rival. */
    /** Lugar libre lo más lejos posible del robot más cercano. */
    public int[] freeSpot(Robot self) {
        int[] best = {300, 300}; float bd = -1;
        for (int n = 0; n < 60; n++) {
            int x = 40 + rng.i(Terrain.W - 80), y = 40 + rng.i(Terrain.H - 120);
            boolean ok = true;
            for (int a = -12; a <= 12 && ok; a += 6) for (int b = -60; b <= 0 && ok; b += 6) if (t.solid(x + a, y + b)) ok = false;
            if (!ok) continue;
            while (y < Terrain.H - 20 && !t.solid(x, y) && !t.solid(x - 7, y) && !t.solid(x + 7, y)) y++;
            float d = 900;
            for (Robot o : robots) if (o != null && o != self && o.px > -1000 && o.lives >= 0) d = Math.min(d, (float) StrictMath.hypot(x - o.px, y - o.py));
            d += rng.r(0, 200);
            if (d > bd) { bd = d; best[0] = x; best[1] = y - 1; }
        }
        return best;
    }
    public Shot spawn(Wpn w, int owner, float x, float y, float vx, float vy) {
        if (shots.size() > 600) return null;
        Shot s = new Shot(); s.w = w; s.owner = owner; s.x = x; s.y = y; s.vx = vx; s.vy = vy; s.life = w.fuse; shots.add(s); return s;
    }
    public void explode(float x, float y, int r, float dmg, int dig, float knock, int owner) { explode(x, y, r, dmg, dig, knock, owner, 0); }
    /** style: 0 fuego, 1 celeste (Death Ray), 2 verde (Bouncy Laser). */
    public void explode(float x, float y, int r, float dmg, int dig, float knock, int owner, int style) {
        if (dig > 0) t.carve((int) x, (int) y, dig);
        fx(Fx.EXPLODE, x, y, r, owner).b = style;
        blastWood(x, y, Math.max(r, dig), Math.max(dmg, 8));
        for (int i = 0; i < props.size(); i++) {
            Prop p = props.get(i); if (p.gone) continue;
            float nx = Math.max(p.x, Math.min(x, p.x + p.w)), ny = Math.max(p.y, Math.min(y, p.y + p.h)), d = (float) StrictMath.hypot(nx - x, ny - y);
            if (d < r) {
                float k = 1 - d / r, cx = p.x + p.w / 2f - x, cy = p.y + p.h / 2f - y, n = Math.max(1, (float) StrictMath.hypot(cx, cy));
                push(p, cx / n * knock * 3 * k, cy / n * knock * 3 * k - knock * k);
                hurt(p, dmg * (.4f + .6f * k));
            }
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
    public boolean hitProp(float x, float y, float d) { return hitProp(x, y, d, 0, 0); }
    /** Daña el objeto en ese punto y lo empuja en la dirección del golpe (vx, vy). */
    // ---------- Madera: cada trozo aguanta WOOD_HP y al romperse sale en astillas de su color
    public static final int WOOD_HP = 22, FW = Terrain.FW, FH = Terrain.FH; public int[] woodDmg;
    public boolean hitWood(float x, float y, float d) {
        int ix = (int) x, iy = (int) y; if (t.at(ix, iy) != Terrain.WOOD) return false;
        hurtWood(Terrain.frag(ix, iy), d); return true;
    }
    void hurtWood(int f, float d) {
        woodDmg[f] += Math.max(1, Math.round(d));
        if (woodDmg[f] >= WOOD_HP) breakWood(f);
    }
    /** Rompe un trozo: quita sus píxeles y lanza astillas del mismo color. */
    void breakWood(int f) {
        int cx = f % FW, cy = f / FW, n = 0, color = 0; long sx = 0, sy = 0;
        int x0 = (cx - 1) * Terrain.FRAG, y0 = (cy - 1) * Terrain.FRAG, x1 = (cx + 2) * Terrain.FRAG, y1 = (cy + 2) * Terrain.FRAG;
        for (int y = Math.max(0, y0); y < Math.min(Terrain.H, y1); y++) for (int x = Math.max(0, x0); x < Math.min(Terrain.W, x1); x++) {
            int i = y * Terrain.W + x; if (t.m[i] != Terrain.WOOD || Terrain.frag(x, y) != f) continue;
            t.m[i] = 0; n++; sx += x; sy += y; color = t.tex[i];
        }
        woodDmg[f] = 0;
        if (n == 0) return;
        t.markArea(x0, y0, x1, y1);
        Fx e = fx(Fx.WOOD, sx / (float) n, sy / (float) n, n, -1); e.b = color;
    }
    /** Daño de explosión a los trozos de madera dentro del radio. */
    void blastWood(float x, float y, int r, float dmg) {
        if (woodDmg == null) return;
        java.util.TreeSet<Integer> hit = new java.util.TreeSet<>();
        for (int yy = (int) y - r; yy <= y + r; yy += 3) for (int xx = (int) x - r; xx <= x + r; xx += 3)
            if ((xx - x) * (xx - x) + (yy - y) * (yy - y) <= r * r && t.at(xx, yy) == Terrain.WOOD) hit.add(Terrain.frag(xx, yy));
        for (int f : hit) { float d = (float) StrictMath.hypot(Terrain.fragX(f) - x, Terrain.fragY(f) - y); hurtWood(f, dmg * 1.6f * (.5f + .5f * Math.max(0, 1 - d / (r + 10)))); } // las explosiones astillan fuerte
    }
    public boolean hitProp(float x, float y, float d, float vx, float vy) {
        if (hitWood(x, y, d)) return true;
        for (int i = 0; i < props.size(); i++) {
            Prop p = props.get(i);
            if (!p.gone && p.has(x, y)) { float n = Math.max(1, (float) StrictMath.hypot(vx, vy)), f = .6f + d * .12f; push(p, vx / n * f, vy / n * f); hurt(p, d); return true; }
        }
        return false;
    }
    void push(Prop p, float ix, float iy) { if (p.gone) return; float m = p.mass(); p.vx += ix / m; p.vy += iy / m; p.moving = true; }
    /** ¿Cabe el objeto en (x, y)? Choca con el mapa y con los robots. */
    private boolean fits(Prop p, int x, int y) {
        for (int i = x; i < x + p.w; i += 2) if (t.solid(i, y) || t.solid(i, y + p.h - 1)) return false;
        for (int j = y; j < y + p.h; j += 2) if (t.solid(x, j) || t.solid(x + p.w - 1, j)) return false;
        if (t.solid(x + p.w - 1, y + p.h - 1)) return false;
        for (Robot r : robots) if (r.alive() && r.px + Robot.HW > x && r.px - Robot.HW < x + p.w && r.py > y && r.py - r.h < y + p.h) return false;
        return true;
    }
    /** Física de cajas: gravedad, roce y choques. Al caer fuerte se dañan. */
    void moveProps() {
        for (int i = 0; i < props.size(); i++) {
            Prop p = props.get(i); if (p.gone || !p.moving) continue;
            t.lift(p.x, p.y, p.w, p.h, p.kind);
            p.vy = Math.min(9, p.vy + .3f); p.vx = Math.max(-9, Math.min(9, p.vx));
            p.fx += p.vx; int tx = Math.round(p.fx), s = tx > p.x ? 1 : -1;
            while (p.x != tx) { if (fits(p, p.x + s, p.y)) p.x += s; else { p.vx *= -.25f; p.fx = p.x; break; } }
            p.fy += p.vy; int ty = Math.round(p.fy); boolean ground = false; float hitV = p.vy; s = ty > p.y ? 1 : -1;
            while (p.y != ty) { if (fits(p, p.x, p.y + s)) p.y += s; else { if (s > 0) ground = true; p.vy = 0; p.fy = p.y; break; } }
            if (!ground && !fits(p, p.x, p.y + 1)) ground = true;
            if (ground) { p.vx *= .82f; if (hitV > 5) hurt(p, (hitV - 5) * 6); }
            if (!p.gone) t.stamp(p.x, p.y, p.w, p.h, p.kind);
            if (ground && Math.abs(p.vx) < .08f && p.vy == 0) { p.moving = false; p.vx = 0; p.fx = p.x; p.fy = p.y; }
        }
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
            if (!held) { p.moving = true; } // sin apoyo: cae
        }
    }
    /** Proton Ray: rayo continuo hacia adelante; si alcanza al rival lo atrapa con el lazo. */
    public static final int PROTON_RANGE = 230;
    public void proton(Robot r, float x, float y) {
        float cx = x, cy = y;
        for (int i = 0; i < PROTON_RANGE / 3; i++) {
            float nx = cx + r.facing * 3;
            if (t.solid((int) nx, (int) cy)) break;
            cx = nx; boolean hit = false;
            for (Robot o : robots) if (o != r && o.alive() && o.team != r.team && o.hits(cx, cy)) { o.lasso(this, r.id); hit = true; }
            if (hit) break;
        }
        r.beamT = 3; r.beamX = cx; r.beamY = cy;
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
                if (Terrain.isProp(t.at((int) cx, (int) cy)) || t.at((int) cx, (int) cy) == Terrain.WOOD) { hitProp(cx, cy, w.dmg, dx, dy); break; }
                if (w.pierce) { if (i % 4 == 0) t.carve((int) cx, (int) cy, w.dig); }
                else { t.carve((int) cx, (int) cy, w.dig); fx(Fx.CHIPS, cx, cy, 2, 0); break; }
            }
        }
        Fx f = fx(Fx.BEAM, x, y, 0, owner); f.c = cx; f.d = cy; f.b = w.pierce ? 1 : 0;
    }

    public void step(int[] in) {
        tick++;
        for (int i = 0; i < robots.length; i++) robots[i].step(this, i < in.length ? in[i] : 0);
        for (int i = 0; i < shots.size(); i++) { Shot s = shots.get(i); if (!s.dead) s.step(this); }
        for (int i = shots.size() - 1; i >= 0; i--) if (shots.get(i).dead) shots.remove(i);
        if (--bonusT <= 0 && bonus.size() < 4) {
            bonusT = 500 + rng.i(400);
            int[] p = freeSpot(null); Bonus b = new Bonus(); b.x = p[0]; b.y = p[1] - 30; b.kind = rng.i(2); bonus.add(b);
        }
        moveProps();
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
        h = h * 31 + shots.size() + water;
        if (woodDmg != null) { int s = 0; for (int i = 0; i < woodDmg.length; i++) s += woodDmg[i] * (i + 1); h = h * 31 + s; }
        for (Shot s : shots) h = h * 31 + Float.floatToIntBits(s.x) + 7 * Float.floatToIntBits(s.y);
        for (Prop p : props) h = h * 31 + Float.floatToIntBits(p.hp) + p.x * 17 + p.y * 3;
        for (Robot r : robots) h = h * 31 + r.stunT + 3 * r.thrownT + 5 * Float.floatToIntBits(r.vy);
        return h;
    }
}
