package com.wap.core;
import java.util.ArrayList;
/** Estado completo de una partida. Avanza con step(entradas) a 60 ticks por segundo, sin leer el reloj ni el dispositivo. */
public final class World {
    /** Versión de las reglas: dos teléfonos solo juegan en línea si coincide. */
    public static final int RULES = 25;
    /** Versión de reglas para jugar en línea: incluye las armas de armas.txt, así ambos teléfonos deben tener las mismas. */
    public static int rules() { return Wpn.signature == 0 ? RULES : RULES ^ Wpn.signature & 0x7FFFFF00; }
    public final Terrain t; public final Rng rng;
    /** Base en juego (modo BASES); null en las batallas. */
    public BaseRun run; public int tick, bonusT = 400;
    public final Robot[] robots;
    /** Equipo ganador al terminar (-1 si nadie). winner es un robot de ese equipo. */
    public int winnerTeam = -1;
    public final ArrayList<Shot> shots = new ArrayList<>(); public final ArrayList<Bonus> bonus = new ArrayList<>(); public final ArrayList<Prop> props = new ArrayList<>();
    public final ArrayList<Fx> fx = new ArrayList<>(); public Fx lastFx;
    public boolean finished; public int winner = -1;

    /** Altura del agua (-1: sin agua). Bajo el agua se cae más lento y se corre más despacio. */
    public int water = -1;
    /** Vida máxima de todos los personajes (para probar el equilibrio). Se llama antes del primer paso. */
    /** Modo desarrollador (solo fuera de línea): personaje invencible y personaje con balas infinitas (-1 nadie). */
    public int godId = -1, ammoId = -1; public static final int EVERYONE = -2; // EVERYONE: en línea, las opciones del anfitrión valen para los dos
    /** Modo desarrollador: velocidad de los personajes y de las armas (1 = normal). Se avanza de a pasos enteros. */
    public float robotRate = 1, shotRate = 1; float rAcc, sAcc;
    /** Premios: cuántos puede haber a la vez y cuáles salen (bits: 1 cápsulas, 2 vida extra, 4 caja de arma). */
    public int bonusMax = 4, bonusKinds = 7;
    public void setMaxHp(int v) { v = Math.max(25, Math.min(1000, v)); for (Robot r : robots) { r.maxHp = v; r.hp = v; } }
    public World(int seed, int lives, Wpn[] a, Wpn[] b) { this(seed, lives, a, b, null); }
    /** Partida en un nivel guardado (o aleatorio si level es null). */
    public World(int seed, int lives, Wpn[] a, Wpn[] b, Level level) { this(seed, lives, new Wpn[][]{a, b}, new int[]{0, 1}, level); }
    /**
     * BASES: solo (online = false: el jugador 0 y un rival que no juega) o en línea (el jugador 2 juega la base
     * reflejada y se encuentran en la sala del jefe). Sin premios al azar: solo los que se pusieron en el editor.
     */
    public World(int seed, int lives, Wpn[][] loads, Base base, boolean online) {
        rng = new Rng(seed); run = new BaseRun(base, online); t = base.build(online, run);
        robots = new Robot[2];
        for (int i = 0; i < 2; i++) { robots[i] = new Robot(i, lives); robots[i].team = i; System.arraycopy(loads[i], 0, robots[i].loadout, 0, Robot.SLOTS); robots[i].px = -9999; }
        for (int[] q : t.props) props.add(new Prop(q));
        robots[0].spawnAt(this);
        if (online) robots[1].spawnAt(this); else { robots[1].lives = -1; robots[1].deadT = Integer.MAX_VALUE; robots[1].py = -9999; }
        robots[0].facing = 1; robots[1].facing = -1;
        woodDmg = new int[t.FW * t.FH]; bonusMax = 0;
        for (float[] p : run.prizes) { Bonus b = new Bonus(); b.kind = (int) p[0]; b.x = p[1]; b.y = p[2]; b.life = Integer.MAX_VALUE; bonus.add(b); }
    }
    /** Partida con varios robots (2 a 5). teams[i] es el equipo de cada uno. */
    public World(int seed, int lives, Wpn[][] loads, int[] teams, Level level) {
        t = new Terrain(); rng = new Rng(seed);
        if (level != null) { level.apply(t); water = level.water; } else t.generate(rng);
        int n = loads.length; robots = new Robot[n];
        for (int i = 0; i < n; i++) { robots[i] = new Robot(i, lives); robots[i].team = teams[i]; System.arraycopy(loads[i], 0, robots[i].loadout, 0, Robot.SLOTS); robots[i].px = -9999; }
        for (int[] q : t.props) props.add(new Prop(q));
        for (Robot r : robots) r.spawnAt(this);
        for (Robot r : robots) { Robot o = nearest(r, false); if (o != null) r.facing = o.px < r.px ? -1 : 1; }
        woodDmg = new int[t.FW * t.FH];
    }
    /** El robot vivo más cercano (enemigo si enemies). */
    public Robot nearest(Robot r, boolean enemies) {
        Robot best = null; float bd = 1e9f;
        for (Robot o : robots) { if (o == r || !o.alive() || o.px < -1000 || enemies && o.team == r.team) continue; float d = Math.abs(o.px - r.px) + Math.abs(o.py - r.py); if (d < bd) { bd = d; best = o; } }
        return best;
    }
    /** Termina cuando queda un solo equipo con vidas. */
    void checkEnd() {
        if (run != null) { run.checkEnd(this); return; }
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
        if (run != null && self != null) return run.spawnFor(this, self.id);
        int[] best = {300, 300}; float bd = -1;
        for (int n = 0; n < 60; n++) {
            int x = 40 + rng.i(t.W - 80), y = 40 + rng.i(t.H - 120);
            boolean ok = true;
            for (int a = -12; a <= 12 && ok; a += 6) for (int b = -60; b <= 0 && ok; b += 6) if (t.solid(x + a, y + b)) ok = false;
            if (!ok) continue;
            while (y < t.H - 20 && !t.solid(x, y) && !t.solid(x - 7, y) && !t.solid(x + 7, y)) y++;
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
    /** Un disparo que toca un dron lo hace explotar (sus propios misiles no). */
    void droneHits() {
        for (int i = 0; i < shots.size(); i++) {
            Shot d = shots.get(i); if (!d.w.drone || d.dead) continue;
            for (int j = 0; j < shots.size(); j++) {
                Shot o = shots.get(j); if (o == d || o.dead || o.wait > 0 || o.w.fan || o.owner == d.owner && o.w == d.w.splinter) continue;
                if (Math.abs(o.x - d.x) < 7 && Math.abs(o.y - d.y) < 7) { if (o.w.radius > 0 || o.w.drone) o.explode(this); else o.dead = true; d.explode(this); break; }
            }
        }
    }
    public void explode(float x, float y, int r, float dmg, int dig, float knock, int owner, int style) {
        for (int i = 0; i < shots.size(); i++) { Shot d = shots.get(i); if (d.w.drone && !d.dead && StrictMath.hypot(d.x - x, d.y - y) < r + 6) d.explode(this); } // la explosión alcanza a los drones
        if (dig > 0) t.carve((int) x, (int) y, dig);
        fx(Fx.EXPLODE, x, y, r, owner).b = style;
        blastWood(x, y, Math.max(r, dig), Math.max(dmg, 8));
        if (run != null && owner >= 0) run.blast(this, x, y, r, dmg);
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
    public static final int WOOD_HP = 22; public int[] woodDmg;
    public boolean hitWood(float x, float y, float d) {
        int ix = (int) x, iy = (int) y; if (t.at(ix, iy) != Terrain.WOOD) return false;
        hurtWood(t.frag(ix, iy), d); return true;
    }
    void hurtWood(int f, float d) {
        woodDmg[f] += Math.max(1, Math.round(d));
        if (woodDmg[f] >= WOOD_HP) breakWood(f);
    }
    /** Rompe un trozo: quita sus píxeles y lanza astillas del mismo color. */
    void breakWood(int f) {
        int cx = f % t.FW, cy = f / t.FW, n = 0, color = 0; long sx = 0, sy = 0;
        int x0 = (cx - 1) * Terrain.FRAG, y0 = (cy - 1) * Terrain.FRAG, x1 = (cx + 2) * Terrain.FRAG, y1 = (cy + 2) * Terrain.FRAG;
        for (int y = Math.max(0, y0); y < Math.min(t.H, y1); y++) for (int x = Math.max(0, x0); x < Math.min(t.W, x1); x++) {
            int i = y * t.W + x; if (t.m[i] != Terrain.WOOD || t.frag(x, y) != f) continue;
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
            if ((xx - x) * (xx - x) + (yy - y) * (yy - y) <= r * r && t.at(xx, yy) == Terrain.WOOD) hit.add(t.frag(xx, yy));
        for (int f : hit) { float d = (float) StrictMath.hypot(t.fragX(f) - x, t.fragY(f) - y); hurtWood(f, dmg * 1.6f * (.5f + .5f * Math.max(0, 1 - d / (r + 10)))); } // las explosiones astillan fuerte
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
    /** ELECTRIC THUNDER: relámpago de largo alcance (unos 330 px) que se corta en el terreno; al primero que toca lo aturde y le quita vida. */
    public void bolt(Wpn w, int owner, float x, float y, float dx, float dy) {
        float cx = x, cy = y;
        for (int i = 0; i < 110; i++) {
            cx += dx * 3; cy += dy * 3;
            Robot hit = null; for (Robot r : robots) if (r.id != owner && r.alive() && r.hits(cx, cy)) { hit = r; break; }
            if (hit != null) { hit.stunT = Math.max(hit.stunT, Robot.BOLT_STUN); hit.damage(this, w.dmg, dx * w.knock, -.5f, owner, cx, cy); break; }
            if (run != null && run.hurtFoe(this, cx, cy, w.dmg, 2)) break;
            if (t.solid((int) cx, (int) cy)) { fx(Fx.CHIPS, cx, cy, 3, 0); break; }
        }
        Fx f = fx(Fx.BOLT, x, y, 0, owner); f.c = cx; f.d = cy;
    }
    public void beam(Wpn w, int owner, float x, float y, float dx, float dy) {
        float cx = x, cy = y; int n = w.pierce ? 300 : 240; boolean hit = false;
        for (int i = 0; i < n; i++) {
            cx += dx * 3; cy += dy * 3;
            boolean droneHit = false; for (int k = 0; k < shots.size(); k++) { Shot d = shots.get(k); if (d.w.drone && !d.dead && Math.abs(d.x - cx) < 6 && Math.abs(d.y - cy) < 6) { d.explode(this); droneHit = true; } }
            if (droneHit && !w.pierce) break;
            if (!hit && run != null && run.hurtFoe(this, cx, cy, w.dmg, 1)) { hit = true; if (!w.pierce) break; }
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
        if (run != null) { in = in.clone(); run.pre(this, in); }
        rAcc += robotRate; while (rAcc >= 1) { rAcc -= 1; for (int i = 0; i < robots.length; i++) robots[i].step(this, i < in.length ? in[i] : 0); }
        sAcc += shotRate; while (sAcc >= 1) { sAcc -= 1; for (int i = 0; i < shots.size(); i++) { Shot s = shots.get(i); if (!s.dead) s.step(this); } droneHits(); }
        for (int i = shots.size() - 1; i >= 0; i--) if (shots.get(i).dead) shots.remove(i);
        if (--bonusT <= 0 && bonus.size() < bonusMax && (bonusKinds & 7) != 0) {
            bonusT = (500 + rng.i(400)) * 4 / Math.max(4, bonusMax); // con más premios, salen más seguido
            int n = Integer.bitCount(bonusKinds & 7), pick = rng.i(n), kind = 0; for (int k = 0; k < 3; k++) if ((bonusKinds & 1 << k) != 0 && pick-- == 0) { kind = k; break; }
            int[] p = freeSpot(null); Bonus b = new Bonus(); b.x = p[0]; b.y = p[1] - 30; b.kind = kind; bonus.add(b);
        }
        if (run != null) run.post(this);
        moveProps();
        if (tick % 20 == 0) support();
        for (Prop p : props) if (p.hitT > 0) p.hitT--;
        for (int i = bonus.size() - 1; i >= 0; i--) {
            Bonus b = bonus.get(i); if (--b.life <= 0) { bonus.remove(i); continue; }
            b.vy = Math.min(5, b.vy + .2f); float ny = b.y + b.vy;
            if (t.solid((int) b.x, (int) ny)) b.vy = 0; else b.y = ny;
            for (Robot r : robots) if (r.alive() && Math.abs(r.px - b.x) < 16 && b.y > r.py - r.h - 8 && b.y < r.py + 8) {
                if (b.kind == 0) r.heal(r.maxHp * .4f);
                else if (b.kind == 1) r.lives++; // vida extra
                else { int[] ok = Wpn.enabled(); r.pendWpn = ok.length == 0 ? -1 : ok[rng.i(ok.length)]; r.wpnT = Robot.PEND_TIME; } // caja de arma: el jugador elige dónde ponerla
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
        if (run != null) h = h * 31 + run.hash();
        return h;
    }
}
