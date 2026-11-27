package com.wap.core;
/** Personaje: se mueve como Mega Man, dispara como Liero. */
public final class Robot {
    public static final int HELMET = 0, CHEST = 1, BELLY = 2, HIP = 3, SH_F = 4, SH_B = 5, BR_F = 6, BR_B = 7, TH_F = 8, TH_B = 9, SN_F = 10, SN_B = 11, PIECES = 12;
    static final float[] PX = {0, 0, 0, 0, 8, -8, 13, -13, 3, -3, 3, -3}, PY = {50, 37, 30, 26, 39, 39, 27, 27, 18, 18, 8, 8};
    /** Golpe: cuánto dura el retroceso, el respiro antes de poder volver a sufrirlo (para no quedar trabado por una ráfaga) y el empujón. */
    public static final int FINAL = 150, KNOCK = 14, KNOCK_CD = 16; public static final float KNOCK_PUSH = 2.6f;
    public static final int SLOTS = 4, STAND_H = 54, SLIDE_H = 26, HW = 7;
    /** Margen en ticks para que dirección + botón cuente como habilidad y no como correr + acción. */
    public static final int SMASH_WINDOW = 9, BUFFER = 3;
    public static final float HOOK_SHOT = 30, HOOK_PULL = 7.5f, HOOK_RANGE = 4000, HOOK_TUG = 1f; // alcance mayor que el mapa: siempre llega a algo (el marco es de acero)
    /** Robot Trap: velocidad de lanzamiento según la cruceta (abajo: a los pies, adelante, arriba: lejos). */
    static final float[][] TRAP_V = {{1.2f, -2}, {5.2f, -3.2f}, {4.6f, -7.6f}};
    /** Punta del rayo de protones (para dibujarlo). */
    public float beamX, beamY;
    public static final int LASSO_T = 300, LASSO_JUMPS = 3; int lassoJumps, holdX, holdY; public static final float LASSO_RANGE = 320, LASSO_DMG = 1.5f;
    public static final float RUN_FAST = 1.55f, AIR_JUMP = 8.4f, STOMP_DMG = 6, THROW_VX = 8.5f, THROW_VY = 6.5f;
    public static final int STUN = 18, STOMP_COOLDOWN = 60;
    public static final float AIM_SPEED = 2.5f, AIM_MIN = -40, AIM_MAX = 90, GRAB_RANGE = 60;
    public static final float RUN = 2.5f, GRAV = .28f, JUMP = 9.2f, WALL_JUMP = 8.6f, WALL_PUSH = 4.2f, WALL_SLIDE = 1.4f, SLIDE = 4.6f;
    static final float MAX_HP = 100, BONE_HP = 25;

    public final int id; public int px, py, h = STAND_H, facing = 1; float remX, remY;
    public float vx, vy; public boolean grounded, blocked, carried; int coyote;
    /** Vida máxima de esta partida (se elige en PERSONALIZAR; 100 por defecto). */
    public float maxHp = MAX_HP;
    public float hp = MAX_HP; public int lives, kills, deaths, deadT, invuln, hurtT, armor = (1 << PIECES) - 1;
    public int slideT, airT, fireT, fireSlot = 3; boolean jumping;
    public int rope, hookId = -1; public float rx, ry, rvx, rvy, rlen, hookDy; // 0 sin gancho, 1 volando, 2 enganchado al escenario, 3 enganchado a otro personaje (hookId)
    public final Wpn[] loadout = new Wpn[SLOTS]; public final int[] ammo = new int[SLOTS], reload = new int[SLOTS], delay = new int[SLOTS], charge = new int[SLOTS];
    public boolean wet; /** finalT: perdió la última vida; se pone blanco, brilla y explota al llegar a 0. */
    public int finalT;
    /** Caja de arma: arma que espera que elijas su lugar (con la combinación del lugar + A) y cuánto le queda; -1 nada. */
    public int pendWpn = -1, wpnT;
    /** BOOBLE SEAL: la burbuja que lo tiene atrapado (null si nada) y cuántas veces disparó para romperla. */
    public Shot bubble; public int bubbleHits;
    /** FREEZEE FLOW: frío acumulado; al llegar a FREEZE queda en un cubo de hielo (frozenT) que se rompe con 7 disparos. */
    public float chill; public int frozenT, frozenBy, iceHits, icePrev, iceImm;
    public static final float FREEZE = 150; public static final int FROZEN_MAX = 600, BOLT_STUN = 40;
    public static final int PEND_TIME = 420; public int knockT, knockCd, beamT, trapAim = 1, lassoFree, killedBy = -1, lassoT, lassoBy, flinchT, stunT, stompCd, thrownT, rollT; public float aim = 60; public boolean aiming; int thrownBy, airJumps; Robot[] all; Robot hitBody, stompOn; boolean ghost, stompHit;
    /** Equipo: los del mismo equipo no se hacen daño. En todos contra todos cada uno tiene el suyo. */
    public int team;
    public int wallDir, windT; int windSlot, lastWall, wallT, wallLock;
    /** Preparación breve antes del primer disparo: 6 ticks, una décima de segundo. */
    public static final int WIND = 6;
    int prev, dirT, dirWas, heldSlot = -1, heldBtn, pending, pendT;

    Robot(int id, int lives) { this.id = id; this.lives = lives; }
    public boolean alive() { return deadT == 0 && lives >= 0; }
    /** El dron que está piloteando (o null): mientras vuela, los controles lo manejan a él y el personaje se queda quieto. */
    public Shot droneShot;
    public boolean piloting() { return droneShot != null && !droneShot.dead; }
    public boolean sliding() { return slideT > 0 || h != STAND_H; }
    public boolean hits(float x, float y) { return Math.abs(x - px) < 10 && y >= py - h && y <= py; }
    public int armorCount() { return Integer.bitCount(armor); }
    /** Carga más alta entre las armas: 0 sin carga, 1 lista para lanzar. */
    public float chargeLevel() {
        float best = 0;
        for (int i = 0; i < SLOTS; i++) if (loadout[i].charge > 0) best = Math.max(best, charge[i] / (float) loadout[i].charge);
        return best;
    }

    private boolean freeT(Terrain t, int x, int y, int hh) {
        for (int yy = y - hh; yy < y; yy += 4) if (t.solid(x - HW, yy) || t.solid(x + HW, yy)) return false;
        for (int xx = x - HW; xx <= x + HW; xx += 3) if (t.solid(xx, y - 1) || t.solid(xx, y - hh)) return false;
        return !t.solid(x + HW, y - 1) && !t.solid(x - HW, y - 1);
    }
    private boolean free(Terrain t, int x, int y, int hh) { return freeT(t, x, y, hh) && !body(x, y, hh); }
    /** El otro personaje ocupa espacio: no se atraviesan, salvo mientras uno va lanzado. */
    boolean body(int x, int y, int hh) {
        hitBody = null;
        if (all == null || ghost || thrownT > 0) return false;
        for (Robot o : all) {
            if (o == this || !o.alive() || o.thrownT > 0) continue;
            if (Math.abs(x - o.px) < HW * 2 && y > o.py - o.h && y - hh < o.py) { hitBody = o; return true; }
        }
        return false;
    }
    boolean ally(Robot o) { return o != null && o != this && o.team == team; }
    private void moveX(Terrain t, float d) {
        remX += d; int n = (int) remX; remX -= n; int s = n > 0 ? 1 : -1; blocked = false;
        for (int i = Math.abs(n); i > 0; i--) {
            if (free(t, px + s, py, h)) { px += s; }
            else {
                boolean up = false; int lim = grounded ? 9 : 3;
                for (int k = 1; k <= lim && !up; k++) if (free(t, px + s, py - k, h)) { px += s; py -= k; up = true; }
                if (!up) { blocked = true; vx = 0; remX = 0; break; }
            }
            if (grounded && vy >= 0) for (int k = 1; k <= 9; k++) {
                if (!free(t, px, py + 1, h)) break;
                boolean ground = false; for (int q = 2; q <= 10 - k + 1; q++) if (!free(t, px, py + q, h)) { ground = true; break; }
                if (!ground) break; py++;
            }
        }
    }
    private void moveY(Terrain t, float d) {
        remY += d; int n = (int) remY; remY -= n; int s = n > 0 ? 1 : -1; grounded = false;
        for (int i = Math.abs(n); i > 0; i--) {
            if (free(t, px, py + s, h)) py += s;
            else if (s < 0 && hitBody == null && t.crumble(px - HW, py - 1 - h - 3, px + HW, py - 1 - h + 2, 16) && free(t, px, py + s, h)) py += s; // pedacitos sueltos sobre la cabeza no frenan el salto: se deshacen
            else { if (s > 0) { grounded = true; Robot on = hitBody; if (d > 2.5f && freeT(t, px, py + 1, h) && on != null) { stompHit = true; stompOn = on; } } vy = 0; remY = 0; break; }
        }
        if (n >= 0 && vy >= 0 && !free(t, px, py + 1, h)) grounded = true;
    }
    public void spawnAt(World g) {
        hp = maxHp; armor = (1 << PIECES) - 1; deadT = 0; droneShot = null; invuln = 90; vx = vy = 0; rope = 0; slideT = 0; h = STAND_H;
        int[] p = g.freeSpot(this); px = p[0]; py = p[1];
        for (int i = 0; i < SLOTS; i++) { ammo[i] = loadout[i].clip; reload[i] = 0; delay[i] = 0; charge[i] = 0; }
        wallDir = 0; wallT = 0; wallLock = 0; windT = 0; fireT = 0; stunT = 0; knockT = 0; knockCd = 0; pendWpn = -1; bubble = null; frozenT = 0; chill = 0; iceImm = 0; stompCd = 0; thrownT = 0; rollT = 0; aim = 60;
        g.fx(Fx.SPAWN, px, py - 27, id, 0);
    }
    public void damage(World g, float d, float kx, float ky, int by, float hx, float hy) {
        if (g.godId == id || g.godId == World.EVERYONE) return; // invencible (modo desarrollador)
        if (!alive() || invuln > 0) return;
        if (by >= 0 && by != id && by < g.robots.length && g.robots[by].team == team) return;
        if (g.run != null && g.run.truce(by, id)) return; // bases en línea: antes de la pelea final no se dañan entre ellos // sin fuego amigo
        if (by == id) d *= .6f;
        if (sliding() && thrownT == 0) { // golpe al deslizarse: sale lanzado en la dirección del golpe y pierde poca vida
            float s = kx != 0 ? Math.signum(kx) : hx < px ? 1 : -1;
            vx = s * 7; vy = -4.5f; slideT = 0; d *= .5f; kx = 0; ky = 0;
        }
        float mul = 1 + (12 - armorCount()) * .08f;
        vx += kx * mul; vy += ky * mul; hurtT = 12;
        if (d <= 0) return;
        if (knockT == 0 && knockCd == 0 && lassoT == 0 && bubble == null && frozenT == 0 && thrownT == 0 && stunT == 0 && !sliding() && by != id) { // golpeado: se va un poco hacia atrás, como Mega Man (sin quedar invulnerable)
            float s = kx != 0 ? Math.signum(kx) : hx < px ? 1 : hx > px ? -1 : -facing;
            knockT = KNOCK; vx = s * KNOCK_PUSH + kx * mul; if (grounded) { vy = Math.min(vy, -2.2f); grounded = false; } windT = 0;
        }
        hp -= d; g.fx(Fx.HIT, hx, hy, d, id); flinchT = Math.max(flinchT, (int) Math.min(8, 2 + d / 4));
        int want = hp <= bone() ? 0 : (int) Math.ceil((hp - bone()) / (maxHp - bone()) * PIECES);
        while (armorCount() > want) {
            int best = -1; float bd = 1e9f;
            float lx = (hx - px), ly = py - hy;
            for (int i = 0; i < PIECES; i++) if ((armor >> i & 1) != 0) {
                float a = lx * facing - PX[i], b = ly - PY[i], q = a * a + b * b;
                if (q < bd) { bd = q; best = i; }
            }
            armor &= ~(1 << best); g.fx(Fx.ARMOR, px + PX[best] * facing, py - PY[best], best, id);
            Fx f = g.lastFx; f.c = (hx < px ? 1.5f : -1.5f) + kx; f.d = -3;
        }
        if (hp <= 0) die(g, by);
    }
    /** Muere (también al caer a un precipicio en las bases). by < 0: un enemigo de la base o una caída (cuenta como él mismo). */
    public void die(World g, int by) {
        if (!alive()) return; if (by < 0 || by >= g.robots.length) by = id;
        if (droneShot != null) { Shot ds = droneShot; droneShot = null; if (!ds.dead) ds.explode(g); } // si muere el piloto, el dron explota
        hp = 0; rope = 0; deadT = 150; stunT = 0; thrownT = 0; windT = 0; lassoT = 0; knockT = 0; killedBy = by; java.util.Arrays.fill(charge, 0); lives--; deaths++;
        if (by != id) g.robots[by].kills++;
        if (lives < 0) { lives = -1; deadT = 9999; finalT = FINAL; vx = 0; vy = 0; } // última vida: pose de salto, se pone blanco, brilla y explota (ver step)
        else g.fx(Fx.DEATH, px, py - 27, id, 0).b = by; // b: quién lo eliminó (él mismo si se mató solo)
    }
    void heal(float a) {
        hp = Math.min(maxHp, hp + a);
        int want = hp <= bone() ? 0 : (int) Math.ceil((hp - bone()) / (maxHp - bone()) * PIECES);
        for (int i = 0; i < PIECES && armorCount() < want; i++) armor |= 1 << (PIECES - 1 - i);
    }
    private boolean fire(World g, int slot) {
        Wpn w = loadout[slot];
        if (reload[slot] > 0 || delay[slot] > 0) return false;
        float deg = -w.angle, base = (float) StrictMath.toRadians(deg); // rectas, salvo las que rebotan en diagonal
        boolean planted = grounded && Math.abs(vx) < .4f && slideT == 0;
        float my = py - (h != STAND_H ? 13 : planted ? 30 : 33), mx = px + facing * 10; // sale de la mano extendida
        for (int d = 12; d <= 22 && !g.t.solid((int) (px + facing * d), (int) my); d += 2) mx = px + facing * d;
        for (int i = 0; i < w.parts; i++) {
            float a = base + (float) StrictMath.toRadians(g.rng.r(-w.spread, w.spread));
            float v = w.speed * (w.parts > 1 && w.stagger == 0 ? g.rng.r(.82f, 1.1f) : 1);
            float dx = (float) StrictMath.cos(a) * facing, dy = (float) StrictMath.sin(a);
            if (w.lasso) g.proton(this, mx, my);
            else if (w.drone) { if (droneShot == null || droneShot.dead) { Shot sh = g.spawn(w, id, mx, my, facing * w.speed, 0); if (sh != null) { droneShot = sh; sh.ang = facing > 0 ? 0 : 3.14159265f; } } }
            else if (w.trap) { float[] tv = TRAP_V[trapAim]; g.spawn(w, id, mx, my, facing * tv[0] + vx * .3f, tv[1]); }
            else if (w.beam) g.beam(w, id, mx, my, dx, dy);
            else if (w.bolt) g.bolt(w, id, mx, my, dx, dy);
            else if (w.rain) { Shot sh = g.spawn(w, id, mx, my - 4, facing * (.8f + i * .42f + g.rng.r(0, .3f)), -g.rng.r(9.5f, 11)); if (sh != null) sh.wait = i * w.stagger; } // lluvia: suben y caen en arco, cada uno más lejos
            else { Shot sh = g.spawn(w, id, mx, my, dx * v + (w.grav > .1f ? vx * .4f : 0), dy * v); if (sh != null) sh.wait = i * w.stagger; }
        }
        if (!w.lasso || fireT == 0) g.fx(Fx.FLASH, mx, my, deg * facing, id).b = Wpn.index(w.name); // el rayo continuo avisa solo al empezar
        vx -= facing * w.recoil; fireT = 22; fireSlot = slot;
        if (!w.infinite && g.ammoId != id && g.ammoId != World.EVERYONE) ammo[slot]--; delay[slot] = w.delay; // ammoId: balas infinitas del modo desarrollador
        if (ammo[slot] <= 0) reload[slot] = w.reload;
        return true;
    }
    /** Un copo de la brisa helada lo enfría; con suficiente frío seguido queda congelado. */
    public void chill(World g, int by) {
        if (!alive() || frozenT > 0 || iceImm > 0 || bubble != null || invuln > 0) return;
        chill += FREEZE; if (chill >= FREEZE) { chill = 0; frozenT = FROZEN_MAX; frozenBy = by; iceHits = 0; rope = 0; slideT = 0; windT = 0; g.fx(Fx.ICE, px, py - h / 2f, 1, id); }
    }
    /** El cubo de hielo se rompe. */
    public void thaw(World g) { iceImm = 60; frozenT = 0; iceHits = 0; chill = 0; g.fx(Fx.ICE, px, py - h / 2f, 2, id); vy = Math.min(vy, -2); }
    /** Proton Ray: queda atado al rayo del rival. */
    public void lasso(World g, int by) {
        if (!alive() || invuln > 0 || lassoFree > 0) return;
        if (lassoT > 0) { lassoT = Math.max(lassoT, 60); return; } // el rayo sigue encima: no lo suelta
        lassoT = LASSO_T; lassoBy = by; rope = 0; slideT = 0; windT = 0; lassoJumps = LASSO_JUMPS; holdX = g.robots[by].px; holdY = g.robots[by].py; g.fx(Fx.LASSO, px, py - 30, 1, id);
    }
    /** Cannon Punch: sale lanzado en el sentido del golpe, girando. Al chocar pierde un porcentaje de vida. */
    public void launch(World g, int dir, int by, float dmg) {
        if (!alive() || invuln > 0 || thrownT > 0) return;
        damage(g, dmg, 0, 0, by, px, py - 30);
        rope = 0; slideT = 0; windT = 0; stunT = 0; thrownT = 80; thrownBy = by;
        vx = dir * THROW_VX; vy = -THROW_VY;
        g.fx(Fx.GRAB, px, py - 30, 0, id);
    }
    private void action(World g, int btn, int in, boolean freshDir) {
        boolean up = (in & Input.UP) != 0, down = (in & Input.DOWN) != 0;
        boolean side = freshDir && ((in & Input.LEFT) != 0 ^ (in & Input.RIGHT) != 0);
        if (btn == Input.A) { heldSlot = -1; heldBtn = btn; } // saltar o usar el gancho no corta el disparo mantenido
        if (btn == Input.A) {
            int ts = readyTrap();
            if (ts >= 0) { trapAim = up ? 2 : down ? 0 : 1; use(g, ts); } // trampa lista: la cruceta elige dónde cae
            else use(g, up ? 2 : down ? 0 : side ? 1 : 3);
        }
        else if (up) throwHook(g, in);
        else if (down) { if (grounded && slideT == 0) { slideT = 26; h = SLIDE_H; g.fx(Fx.SLIDE, px, py, facing, 0); } }
        else jump(g);
    }
    private int readyTrap() { for (int i = 0; i < SLOTS; i++) if (loadout[i].trap && loadout[i].charge > 0 && charge[i] >= loadout[i].charge && reload[i] == 0) return i; return -1; }
    /** Arma normal: dispara. Arma con carga: el primer comando carga y, ya cargada, el siguiente la lanza. */
    private void trigger(World g, int s) {
        if (reload[s] > 0 || delay[s] > 0 || windT > 0) return;
        if (fireT > 0 || h != STAND_H) fire(g, s); else { windT = WIND; windSlot = s; }
    }
    private void use(World g, int s) {
        Wpn w = loadout[s];
        if (w.charge == 0) { heldSlot = s; trigger(g, s); return; }
        if (charge[s] >= w.charge) { if (reload[s] == 0 && delay[s] == 0 && windT == 0) { charge[s] = 0; trigger(g, s); } }
        else if (charge[s] == 0 && reload[s] == 0) { charge[s] = 1; g.fx(Fx.CHARGE, px, py - 30, 0, id).b = Wpn.index(w.name); }
    }
    private void jump(World g) {
        if (rope != 0) { rope = 0; return; }
        float k = wet ? .62f : 1; // en el agua, misma altura pero más lento
        if ((grounded || coyote > 0) && slideT == 0) { vy = -JUMP * k; jumping = true; grounded = false; coyote = 0; g.fx(Fx.JUMP, px, py, 0, 0); }
        else if (!grounded && airJumps > 0 && slideT == 0) { airJumps--; vy = -AIR_JUMP * k; jumping = true; rollT = 24; g.fx(Fx.JUMP, px, py - 20, 2, 0); } // doble salto
    }
    /** El gancho sale en el ángulo de la mira (se ajusta manteniendo X y moviendo arriba o abajo). */
    private void throwHook(World g, int in) {
        if (g.run != null) return; // en las bases no hay gancho: el plataformeo es a puro salto
        int side = ((in & Input.RIGHT) != 0 ? 1 : 0) - ((in & Input.LEFT) != 0 ? 1 : 0);
        if (side != 0) facing = side;
        float a = (float) StrictMath.toRadians(aim);
        rope = 1; rx = px + facing * 6; ry = py - 40;
        rvx = facing * (float) StrictMath.cos(a) * HOOK_SHOT; rvy = -(float) StrictMath.sin(a) * HOOK_SHOT; g.fx(Fx.HOOK, rx, ry, 0, id);
    }
    void step(World g, int in) {
        for (int i = 0; i < SLOTS; i++) {
            if (delay[i] > 0) delay[i]--;
            if (reload[i] > 0 && --reload[i] == 0) ammo[i] = loadout[i].clip;
            if (charge[i] > 0 && charge[i] < loadout[i].charge && ++charge[i] == loadout[i].charge) g.fx(Fx.CHARGE, px, py - 30, 1, id).b = Wpn.index(loadout[i].name);
        }
        if (finalT > 0) { // última vida: flota, se pone blanco y brilla; al final explota y se decide la partida
            prev = in; // quieto: pose de salto, gira hacia atrás y brilla (Renderer)
            if (--finalT == 0) { g.fx(Fx.DEATH, px, py - 27, id, 0).b = killedBy; g.fx(Fx.EXPLODE, px, py - 27, 70, id); g.checkEnd(); }
            return; }
        if (lives < 0) return;
        if (frozenT > 0 && deadT == 0) { // congelado: no se mueve (pero cae); cada A agrieta el hielo; con 7 se rompe
            if (((in & ~icePrev) & Input.A) != 0) { if (++iceHits >= Shot.BUBBLE_HITS) thaw(g); else g.fx(Fx.ICE, px, py - h / 2f, 0, id); }
            icePrev = in;
            if (frozenT > 0 && --frozenT == 0) thaw(g);
            if (frozenT > 0 && frozenT % Shot.BUBBLE_TICK == 0) damage(g, 1.5f, 0, 0, frozenBy, px, py - 30);
            in = 0; vx *= .9f;
        } else if (chill > 0) chill = Math.max(0, chill - .8f);
        if (iceImm > 0) iceImm--;
        if (bubble != null && deadT == 0) { // atrapado en una burbuja: cada A cuenta; con 7 la rompe
            if (((in & ~prev) & Input.A) != 0 && ++bubbleHits >= Shot.BUBBLE_HITS) bubble.pop(g); else if (((in & ~prev) & Input.A) != 0) g.fx(Fx.HOOK, px, py - h / 2f, 0, id);
            prev = in; return; }
        if (pendWpn >= 0 && deadT == 0) { // caja de arma: la combinación del lugar + A la pone ahí
            int press = in & ~prev;
            if ((press & Input.A) != 0) {
                int s = (in & Input.DOWN) != 0 ? 0 : (in & (Input.LEFT | Input.RIGHT)) != 0 ? 1 : (in & Input.UP) != 0 ? 2 : 3;
                Wpn nw = Wpn.ALL[pendWpn]; loadout[s] = nw; ammo[s] = nw.clip; reload[s] = 0; delay[s] = 10; charge[s] = 0; pendWpn = -1;
                g.fx(Fx.PICK, px, py - 30, 3, id).b = s; prev = in; return;
            }
            if (--wpnT <= 0) pendWpn = -1;
            in &= ~Input.A; // mientras elige, A no dispara
        }
        if (deadT > 0) { if (--deadT == 0) spawnAt(g); prev = in; return; }
        if (droneShot != null) { // piloteando: la cruceta y los botones van al dron; al explotar, vuelve el control
            if (droneShot.dead) droneShot = null; else { droneShot.ctlPrev = droneShot.ctl; droneShot.ctl = in; in = 0; }
        }
        Terrain t = g.t;
        all = g.robots; ghost = false; ghost = body(px, py, h); // si aparecieron encimados, se separan solos
        if (ghost) { Robot o = hitBody; vx += (px != o.px ? Math.signum(px - o.px) : id < o.id ? -1 : 1) * .4f; }
        if (stunT > 0) stunT--; if (stompCd > 0) stompCd--;
        if (stunT > 0 || thrownT > 0) in = 0;
        if (knockT > 0) { if (--knockT == 0) knockCd = KNOCK_CD; in = 0; } else if (knockCd > 0) knockCd--; // golpeado: no se controla un instante
        if (flinchT > 0) { flinchT--; in &= ~(Input.LEFT | Input.RIGHT); } // breve tirón al recibir un impacto
        if (lassoT > 0) { // Proton Ray: atrapado, baja lento y pierde energía; solo se suelta saltando
            Robot o = g.robots[lassoBy]; lassoT--;
            if (!o.alive() || StrictMath.hypot(o.px - px, o.py - py) > LASSO_RANGE) lassoT = 0;
            else {
                boolean jumpNow = (in & Input.B) != 0 && (prev & Input.B) == 0;
                in = 0; vx *= .6f; if (vy < -1 && !jumpNow) vy = Math.max(vy, -3.5f); rope = 0; slideT = 0; // solo puede forcejear saltando
                float dx = o.px - holdX, dy = o.py - holdY; holdX = o.px; holdY = o.py; // lo arrastra quien lo atrapó
                if (dx != 0) moveX(g.t, dx); if (dy != 0) moveY(g.t, dy);
                if (jumpNow) {
                    vy = -3.5f; grounded = false;
                    if (--lassoJumps <= 0) { lassoT = 0; lassoFree = 45; g.fx(Fx.LASSO, px, py - 30, 0, id); }
                    else g.fx(Fx.LASSO, px, py - 30, 2, id);
                }
                if (lassoT > 0 && lassoT % 10 == 0) damage(g, LASSO_DMG, 0, 0, lassoBy, px, py - 30);
            }
        }
        if (rollT > 0) rollT--; if (beamT > 0) beamT--; if (lassoFree > 0) lassoFree--;
        aiming = (in & Input.X) != 0;
        if (aiming && (in & Input.UP) != 0) aim = Math.min(AIM_MAX, aim + AIM_SPEED);
        if (aiming && (in & Input.DOWN) != 0) aim = Math.max(AIM_MIN, aim - AIM_SPEED);
        if (invuln > 0) invuln--; if (hurtT > 0) hurtT--; if (fireT > 0) fireT--;
        int dir = ((in & Input.RIGHT) != 0 ? 1 : 0) - ((in & Input.LEFT) != 0 ? 1 : 0);
        if (dir != dirWas) { dirT = 0; dirWas = dir; } else if (dirT < 1000) dirT++;
        boolean fresh = dir != 0 && dirT <= SMASH_WINDOW;
        int pressed = in & ~prev; prev = in;
        if ((pressed & Input.B) != 0 && wallT > 0 && (in & (Input.UP | Input.DOWN)) == 0) {
            vx = -lastWall * WALL_PUSH; vy = -WALL_JUMP; jumping = true; wallLock = 12; facing = -lastWall; wallT = 0; wallDir = 0;
            pressed &= ~Input.B; g.fx(Fx.JUMP, px + lastWall * 7, py - 20, 0, 0);
        }
        for (int b = Input.A; b <= Input.B; b <<= 1) if ((pressed & b) != 0) {
            boolean directional = (in & (Input.UP | Input.DOWN)) != 0 || fresh;
            boolean running = dir != 0 && !fresh;
            if (directional || running) action(g, b, in, fresh);
            else { pending = b; pendT = BUFFER; }
        }
        if (pending != 0) {
            boolean dirNow = (in & (Input.UP | Input.DOWN)) != 0 || fresh;
            if (dirNow || --pendT <= 0) { int b = pending; pending = 0; action(g, b, in, fresh); }
        }
        if (windT > 0 && --windT == 0) fire(g, windSlot);
        if (heldSlot >= 0 && (in & heldBtn) != 0) trigger(g, heldSlot); else heldSlot = -1;
        if (vy >= 0) jumping = false;
        if (jumping && (in & Input.B) == 0 && vy < -2.2f) vy *= .72f;

        if (slideT > 0) {
            slideT--; vx = facing * SLIDE;
            if (slideT % 3 == 0) g.fx(Fx.SLIDE, px - facing * 8, py, facing, 1);
            if (blocked || !grounded) slideT = Math.min(slideT, 0);
        } else {
            if (h != STAND_H && free(t, px, py, STAND_H)) h = STAND_H;
            float fast = (in & Input.X) != 0 && dir != 0 && h == STAND_H ? RUN_FAST : 1; // X mantenido + dirección: correr
            float target = dir * (RUN + (12 - armorCount()) * .035f) * (h != STAND_H ? .6f : 1) * fast * (wet ? .65f : 1); // en el agua se avanza más lento
            if (wallLock > 0) wallLock--;
            else if (dir != 0 && rope == 0) facing = dir;
            if (rope < 2 && wallLock == 0) {
                float acc = grounded ? .55f : .3f, d = target - vx;
                if (Math.abs(vx) > RUN * 1.4f * fast && Math.signum(vx) == Math.signum(target)) d = 0;
                if (dir == 0 && !grounded) acc = .04f;
                vx += Math.max(-acc, Math.min(acc, d));
            }
        }
        if (rope != 0 && (in & Input.B) == 0) rope = 0; // soltar B suelta el gancho
        if (rope == 1) {
            for (int i = 0; i < 6 && rope == 1; i++) {
                rx += rvx / 6; ry += rvy / 6;
                if (t.solid((int) rx, (int) ry)) { rope = 2; g.fx(Fx.HOOK, rx, ry, 1, id); break; }
                for (Robot o : g.robots) if (o != this && o.alive() && Math.abs(rx - o.px) < 12 && ry > o.py - o.h && ry < o.py) { // engancha a otro personaje
                    rope = 3; hookId = o.id; hookDy = ry - o.py; g.fx(Fx.HOOK, rx, ry, 1, id); break; }
            }
            if (rope == 1 && dist() > HOOK_RANGE) rope = 0;
        } else if (rope == 3) { // enganchado a otro personaje: lo sigue; con B te lleva hacia él y a él lo tira un poco hacia ti
            Robot o = hookId >= 0 && hookId < g.robots.length ? g.robots[hookId] : null;
            if (o == null || !o.alive() || o.deadT > 0) rope = 0;
            else {
                rx = o.px; ry = o.py + Math.max(-o.h, hookDy);
                float dx = rx - px, dy = ry - (py - 44), d = (float) StrictMath.hypot(dx, dy);
                if (Math.abs(dx) > 4) facing = dx > 0 ? 1 : -1;
                if (d > 30) { vx = dx / d * HOOK_PULL; vy = dy / d * HOOK_PULL;
                    o.vx = Math.max(-4, Math.min(4, o.vx - dx / d * HOOK_TUG)); if (!o.grounded) o.vy = Math.max(-4, Math.min(4, o.vy - dy / d * HOOK_TUG * .5f)); }
                else { rope = 0; vx *= .3f; vy = Math.min(vy, 0); } // llegó: se suelta a su lado
            }
        } else if (rope == 2) {
            if (!t.solid((int) rx, (int) ry) && !t.solid((int) rx + 2, (int) ry) && !t.solid((int) rx - 2, (int) ry) && !t.solid((int) rx, (int) ry - 2) && !t.solid((int) rx, (int) ry + 2)) rope = 0;
            else {
                float dx = rx - px, dy = ry - (py - 44), d = (float) StrictMath.hypot(dx, dy);
                if (Math.abs(dx) > 4) facing = dx > 0 ? 1 : -1;
                if (d > 24) { vx = dx / d * HOOK_PULL; vy = dy / d * HOOK_PULL; } else { vx = 0; vy = 0; }
            }
        }
        boolean wasWet = wet; wet = g.wet(py - h * .5f);
        if (wet != wasWet && Math.abs(vy) > 1.5f) g.fx(Fx.SPLASH, px, g.water, Math.abs(vy), id); // chapuzón
        if (rope < 2) vy = Math.min(lassoT > 0 ? .7f : wet ? 3.2f : 10, vy + (wet ? GRAV * .4f : GRAV)); // atrapado o en el agua: cae despacio
        if (wallDir != 0) vy = Math.min(vy, WALL_SLIDE);
        if (vy > 0 && rope < 2 && Math.abs(vx) > 6) vx *= .97f;
        boolean wasGround = grounded; float preVx = vx, preVy = vy;
        moveX(t, vx);
        if (slideT > 0 && blocked && body(px + facing, py, h) && hitBody.invuln == 0 && !ally(hitBody)) { // barrida: el rival sale volando
            Robot o = hitBody; o.damage(g, 5, 0, 0, id, o.px, o.py - 15); o.vx = facing * 6.5f; o.vy = -4.5f; slideT = 0;
            g.fx(Fx.STOMP, o.px, o.py - 20, 1, o.id);
        }
        moveY(t, vy);
        if (stompHit) { // pisotón, como a un Goomba
            stompHit = false; Robot o = stompOn;
            if (o.invuln == 0 && o.stompCd == 0 && !ally(o)) { o.stunT = STUN; o.damage(g, STOMP_DMG, 0, 0, id, o.px, o.py - o.h); o.stompCd = STOMP_COOLDOWN; } // aturdido primero: el pisotón no lo empuja
            vy = (in & Input.B) != 0 ? -JUMP * .9f : -5.5f; jumping = (in & Input.B) != 0; grounded = false; airJumps = 1;
            g.fx(Fx.STOMP, o.px, o.py - o.h, 0, o.id);
        }
        if (thrownT > 0) {
            thrownT--;
            boolean hit = blocked && Math.abs(preVx) > 2.5f || grounded && preVy > 3 || !grounded && vy == 0 && preVy < -3;
            if (hit) {
                float sp = Math.min(1, (Math.abs(preVx) + Math.abs(preVy)) / 12f); thrownT = 0;
                damage(g, maxHp * (.12f + .13f * sp), 0, 0, thrownBy, px, py - 27); // porcentaje de vida según la fuerza del choque
                g.fx(Fx.SLAM, px, py - 20, sp, id);
            }
        }
        if (grounded) coyote = 6; else if (coyote > 0) coyote--;
        if (grounded && !wasGround && airT > 22) g.fx(Fx.JUMP, px, py, 1, 0);
        airT = grounded ? 0 : airT + 1;
        if (grounded && rollT < 20) rollT = 0;
        boolean wall = !grounded && rope == 0 && slideT == 0 && dir != 0 && wallLock == 0 && vy > -1.5f && !freeT(t, px + dir, py, h);
        if (wall) { if (wallDir == 0) g.fx(Fx.WALL, px + dir * 7, py - 20, 0, 0); else if (airT % 6 == 0) g.fx(Fx.WALL, px + dir * 7, py - 8, 1, 0); wallDir = dir; lastWall = dir; wallT = 6; }
        else { wallDir = 0; if (wallT > 0) wallT--; }
        if (grounded || wallDir != 0 || rope >= 2) airJumps = 1;
        if (grounded && slideT == 0 && h != STAND_H && dir == 0) vx *= .7f;
    }
    /** Vida desde la que se ve el esqueleto (un cuarto de la máxima). */
    float bone() { return maxHp * BONE_HP / MAX_HP; }
    private float dist() { return (float) StrictMath.hypot(rx - px, ry - (py - 40)); }
}
