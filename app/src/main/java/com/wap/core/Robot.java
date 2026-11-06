package com.wap.core;
/** Personaje: se mueve como Mega Man, dispara como Liero. */
public final class Robot {
    public static final int HELMET = 0, CHEST = 1, BELLY = 2, HIP = 3, SH_F = 4, SH_B = 5, BR_F = 6, BR_B = 7, TH_F = 8, TH_B = 9, SN_F = 10, SN_B = 11, PIECES = 12;
    static final float[] PX = {0, 0, 0, 0, 8, -8, 13, -13, 3, -3, 3, -3}, PY = {50, 37, 30, 26, 39, 39, 27, 27, 18, 18, 8, 8};
    public static final int SLOTS = 4, STAND_H = 54, SLIDE_H = 26, HW = 7;
    /** Margen en ticks para que dirección + botón cuente como habilidad y no como correr + acción. */
    public static final int SMASH_WINDOW = 9, BUFFER = 3;
    public static final float HOOK_SHOT = 15, HOOK_PULL = 7.5f, HOOK_RANGE = 400;
    public static final float RUN_FAST = 1.55f, AIR_JUMP = 8.4f, STOMP_DMG = 6, THROW_VX = 8.5f, THROW_VY = 6.5f;
    public static final int STUN = 18, STOMP_COOLDOWN = 60;
    public static final float AIM_SPEED = 2.5f, AIM_MIN = -40, AIM_MAX = 90, GRAB_RANGE = 60;
    public static final float RUN = 2.5f, GRAV = .28f, JUMP = 9.2f, WALL_JUMP = 8.6f, WALL_PUSH = 4.2f, WALL_SLIDE = 1.4f, SLIDE = 4.6f;
    static final float MAX_HP = 100, BONE_HP = 25;

    public final int id; public int px, py, h = STAND_H, facing = 1; float remX, remY;
    public float vx, vy; public boolean grounded, blocked; int coyote;
    public float hp = MAX_HP; public int lives, kills, deaths, deadT, invuln, hurtT, armor = (1 << PIECES) - 1;
    public int slideT, airT, fireT, fireSlot = 3; boolean jumping;
    public int rope; public float rx, ry, rvx, rvy, rlen; // 0 sin gancho, 1 volando, 2 enganchado
    public final Wpn[] loadout = new Wpn[SLOTS]; public final int[] ammo = new int[SLOTS], reload = new int[SLOTS], delay = new int[SLOTS], charge = new int[SLOTS];
    public int stunT, stompCd, thrownT, rollT; public float aim = 60; public boolean aiming; int thrownBy, airJumps; Robot other; boolean ghost, stompHit;
    public int wallDir, windT; int windSlot, lastWall, wallT, wallLock;
    /** Preparación breve antes del primer disparo: 6 ticks, una décima de segundo. */
    public static final int WIND = 6;
    int prev, dirT, dirWas, heldSlot = -1, heldBtn, pending, pendT;

    Robot(int id, int lives) { this.id = id; this.lives = lives; }
    public boolean alive() { return deadT == 0 && lives >= 0; }
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
        Robot o = other;
        if (o == null || ghost || !o.alive() || o.thrownT > 0 || thrownT > 0) return false;
        return Math.abs(x - o.px) < HW * 2 && y > o.py - o.h && y - hh < o.py;
    }
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
            else { if (s > 0) { grounded = true; if (d > 2.5f && freeT(t, px, py + 1, h)) stompHit = true; } vy = 0; remY = 0; break; }
        }
        if (n >= 0 && vy >= 0 && !free(t, px, py + 1, h)) grounded = true;
    }
    public void spawnAt(World g) {
        hp = MAX_HP; armor = (1 << PIECES) - 1; deadT = 0; invuln = 90; vx = vy = 0; rope = 0; slideT = 0; h = STAND_H;
        int[] p = g.freeSpot(id == 0 ? g.robots[1] : g.robots[0]); px = p[0]; py = p[1];
        for (int i = 0; i < SLOTS; i++) { ammo[i] = loadout[i].clip; reload[i] = 0; delay[i] = 0; charge[i] = 0; }
        wallDir = 0; wallT = 0; wallLock = 0; windT = 0; fireT = 0; stunT = 0; stompCd = 0; thrownT = 0; rollT = 0; aim = 60;
        g.fx(Fx.SPAWN, px, py - 27, id, 0);
    }
    public void damage(World g, float d, float kx, float ky, int by, float hx, float hy) {
        if (!alive() || invuln > 0) return;
        if (by == id) d *= .6f;
        if (sliding() && thrownT == 0) { // golpe al deslizarse: sale lanzado en la dirección del golpe y pierde poca vida
            float s = kx != 0 ? Math.signum(kx) : hx < px ? 1 : -1;
            vx = s * 7; vy = -4.5f; slideT = 0; d *= .5f; kx = 0; ky = 0;
        }
        float mul = 1 + (12 - armorCount()) * .08f;
        vx += kx * mul; vy += ky * mul; hurtT = 12;
        if (d <= 0) return;
        hp -= d; g.fx(Fx.HIT, hx, hy, d, id);
        int want = hp <= BONE_HP ? 0 : (int) Math.ceil((hp - BONE_HP) / (MAX_HP - BONE_HP) * PIECES);
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
        if (hp <= 0) {
            hp = 0; rope = 0; deadT = 150; stunT = 0; thrownT = 0; windT = 0; java.util.Arrays.fill(charge, 0); lives--; deaths++;
            if (by != id) g.robots[by].kills++;
            g.fx(Fx.DEATH, px, py - 27, id, 0);
            if (lives < 0) { lives = 0; g.over(by == id ? 1 - id : by, id); deadT = 9999; lives = -1; }
        }
    }
    void heal(float a) {
        hp = Math.min(MAX_HP, hp + a);
        int want = hp <= BONE_HP ? 0 : (int) Math.ceil((hp - BONE_HP) / (MAX_HP - BONE_HP) * PIECES);
        for (int i = 0; i < PIECES && armorCount() < want; i++) armor |= 1 << (PIECES - 1 - i);
    }
    private boolean fire(World g, int slot) {
        Wpn w = loadout[slot];
        if (reload[slot] > 0 || delay[slot] > 0) return false;
        float deg = 0, base = 0; // todas las armas salen rectas, hacia donde mira el robot
        boolean planted = grounded && Math.abs(vx) < .4f && slideT == 0;
        float my = py - (h != STAND_H ? 13 : planted ? 30 : 33), mx = px + facing * 10; // sale de la mano extendida
        for (int d = 12; d <= 22 && !g.t.solid((int) (px + facing * d), (int) my); d += 2) mx = px + facing * d;
        if (w.grab) grab(g, w);
        else for (int i = 0; i < w.parts; i++) {
            float a = base + (float) StrictMath.toRadians(g.rng.r(-w.spread, w.spread));
            float v = w.speed * (w.parts > 1 ? g.rng.r(.82f, 1.1f) : 1);
            float dx = (float) StrictMath.cos(a) * facing, dy = (float) StrictMath.sin(a);
            if (w.beam) g.beam(w, id, mx, my, dx, dy); else g.spawn(w, id, mx, my, dx * v + (w.grav > .1f ? vx * .4f : 0), dy * v);
        }
        g.fx(Fx.FLASH, mx, my, deg * facing, id).b = Wpn.index(w.name);
        vx -= facing * w.recoil; fireT = 22; fireSlot = slot;
        ammo[slot]--; delay[slot] = w.delay;
        if (ammo[slot] <= 0) reload[slot] = w.reload;
        return true;
    }
    /** Drop: agarra al rival que está al frente y lo lanza hacia atrás. Al chocar pierde un porcentaje de vida. */
    private void grab(World g, Wpn w) {
        Robot o = g.robots[1 - id]; float dx = (o.px - px) * facing;
        if (!o.alive() || o.invuln > 0 || o.thrownT > 0 || dx < -6 || dx > GRAB_RANGE || Math.abs(o.py - py) > 44) return;
        o.damage(g, w.dmg, 0, 0, id, o.px, o.py - 30);
        o.rope = 0; o.slideT = 0; o.windT = 0; o.stunT = 0; o.thrownT = 80; o.thrownBy = id;
        o.vx = -facing * THROW_VX; o.vy = -THROW_VY; o.facing = facing;
        g.fx(Fx.GRAB, o.px, o.py - 30, 0, id);
    }
    private void action(World g, int btn, int in, boolean freshDir) {
        boolean up = (in & Input.UP) != 0, down = (in & Input.DOWN) != 0;
        boolean side = freshDir && ((in & Input.LEFT) != 0 ^ (in & Input.RIGHT) != 0);
        heldSlot = -1; heldBtn = btn;
        if (btn == Input.A) use(g, up ? 2 : down ? 0 : side ? 1 : 3);
        else if (up) throwHook(g, in);
        else if (down) { if (grounded && slideT == 0) { slideT = 26; h = SLIDE_H; g.fx(Fx.SLIDE, px, py, facing, 0); } }
        else jump(g);
    }
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
        if ((grounded || coyote > 0) && slideT == 0) { vy = -JUMP; jumping = true; grounded = false; coyote = 0; g.fx(Fx.JUMP, px, py, 0, 0); }
        else if (!grounded && airJumps > 0 && slideT == 0) { airJumps--; vy = -AIR_JUMP; jumping = true; rollT = 24; g.fx(Fx.JUMP, px, py - 20, 2, 0); } // doble salto
    }
    /** El gancho sale en el ángulo de la mira (se ajusta manteniendo X y moviendo arriba o abajo). */
    private void throwHook(World g, int in) {
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
            if (charge[i] > 0 && charge[i] < loadout[i].charge && ++charge[i] == loadout[i].charge) g.fx(Fx.CHARGE, px, py - 30, 1, id);
        }
        if (lives < 0) return;
        if (deadT > 0) { if (--deadT == 0) spawnAt(g); prev = in; return; }
        Terrain t = g.t;
        other = g.robots[1 - id]; ghost = false; ghost = body(px, py, h); // si aparecieron encimados, se separan solos
        if (ghost) vx += (px != other.px ? Math.signum(px - other.px) : id == 0 ? -1 : 1) * .4f;
        if (stunT > 0) stunT--; if (stompCd > 0) stompCd--;
        if (stunT > 0 || thrownT > 0) in = 0;
        if (rollT > 0) rollT--;
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
            float target = dir * (RUN + (12 - armorCount()) * .035f) * (h != STAND_H ? .6f : 1) * fast;
            if (wallLock > 0) wallLock--;
            else if (dir != 0 && rope == 0) facing = dir;
            if (rope != 2 && wallLock == 0) {
                float acc = grounded ? .55f : .3f, d = target - vx;
                if (Math.abs(vx) > RUN * 1.4f * fast && Math.signum(vx) == Math.signum(target)) d = 0;
                if (dir == 0 && !grounded) acc = .04f;
                vx += Math.max(-acc, Math.min(acc, d));
            }
        }
        if (rope != 0 && (in & Input.B) == 0) rope = 0; // soltar B suelta el gancho
        if (rope == 1) {
            for (int i = 0; i < 3 && rope == 1; i++) {
                rx += rvx / 3; ry += rvy / 3;
                if (t.solid((int) rx, (int) ry)) { rope = 2; g.fx(Fx.HOOK, rx, ry, 1, id); }
            }
            if (rope == 1 && dist() > HOOK_RANGE) rope = 0;
        } else if (rope == 2) {
            if (!t.solid((int) rx, (int) ry) && !t.solid((int) rx + 2, (int) ry) && !t.solid((int) rx - 2, (int) ry) && !t.solid((int) rx, (int) ry - 2) && !t.solid((int) rx, (int) ry + 2)) rope = 0;
            else {
                float dx = rx - px, dy = ry - (py - 44), d = (float) StrictMath.hypot(dx, dy);
                if (Math.abs(dx) > 4) facing = dx > 0 ? 1 : -1;
                if (d > 24) { vx = dx / d * HOOK_PULL; vy = dy / d * HOOK_PULL; } else { vx = 0; vy = 0; }
            }
        }
        if (rope != 2) vy = Math.min(10, vy + GRAV);
        if (wallDir != 0) vy = Math.min(vy, WALL_SLIDE);
        if (vy > 0 && rope != 2 && Math.abs(vx) > 6) vx *= .97f;
        boolean wasGround = grounded; float preVx = vx, preVy = vy;
        moveX(t, vx);
        if (slideT > 0 && blocked && body(px + facing, py, h) && other.invuln == 0) { // barrida: el rival sale volando
            other.damage(g, 5, 0, 0, id, other.px, other.py - 15); other.vx = facing * 6.5f; other.vy = -4.5f; slideT = 0;
            g.fx(Fx.STOMP, other.px, other.py - 20, 1, other.id);
        }
        moveY(t, vy);
        if (stompHit) { // pisotón, como a un Goomba
            stompHit = false; Robot o = other;
            if (o.invuln == 0 && o.stompCd == 0) { o.damage(g, STOMP_DMG, 0, 0, id, o.px, o.py - o.h); o.stunT = STUN; o.stompCd = STOMP_COOLDOWN; }
            vy = (in & Input.B) != 0 ? -JUMP * .9f : -5.5f; jumping = (in & Input.B) != 0; grounded = false; airJumps = 1;
            g.fx(Fx.STOMP, o.px, o.py - o.h, 0, o.id);
        }
        if (thrownT > 0) {
            thrownT--;
            boolean hit = blocked && Math.abs(preVx) > 2.5f || grounded && preVy > 3 || !grounded && vy == 0 && preVy < -3;
            if (hit) {
                float sp = Math.min(1, (Math.abs(preVx) + Math.abs(preVy)) / 12f); thrownT = 0;
                damage(g, MAX_HP * (.12f + .13f * sp), 0, 0, thrownBy, px, py - 27); // porcentaje de vida según la fuerza del choque
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
        if (grounded || wallDir != 0 || rope == 2) airJumps = 1;
        if (grounded && slideT == 0 && h != STAND_H && dir == 0) vx *= .7f;
    }
    private float dist() { return (float) StrictMath.hypot(rx - px, ry - (py - 40)); }
}
