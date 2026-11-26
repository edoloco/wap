package com.wap.core;
/** Un proyectil en vuelo. */
public final class Shot {
    public Wpn w; public int owner, age, hops, life, wait; public float x, y, vx, vy; public boolean stuck, dead;
    /** Dron: controles de su piloto en este cuadro y el anterior, enfriamiento de los misiles y rumbo. */
    public int ctl, ctlPrev, cool; public float ang;
    /** Cuánto gira el dron por cuadro (radianes) y cada cuántos cuadros dispara un misil. */
    public static final float DRONE_TURN = .085f; public static final int DRONE_COOL = 12;
    /** FLAME TOWER: distancia entre llamas. BOOBLE SEAL: radio con alguien adentro, golpes para romperla, daño cada cuántos cuadros. */
    public static final float TOWER_GAP = 24, BUBBLE_R = 32; public static final int BUBBLE_HITS = 7, BUBBLE_TICK = 20;
    /** BOOBLE SEAL: a quién tiene atrapado (-1 nadie). */
    public int held = -1;
    public void step(World g) {
        if (wait > 0) { wait--; return; } // cargas del Death Ray que salen después
        age++;
        if (w.drone) { droneMove(g); return; }
        if (w.trap) { trapMove(g); return; }
        if (w.pillar) { pillarMove(g); return; }
        if (w.bubble) { bubbleMove(g); return; }
        if (w.tornado) { tornadoMove(g); return; }
        if (w.fan) for (Shot o : g.shots) if (o != this && !o.w.fan && o.wait == 0 && Math.abs(o.x - x) < 12 && Math.abs(o.y - y) < 12) { o.vx += vx * .14f; o.vy += vy * .14f; o.stuck = false; } // el ventilador desvía balas
        if (w.accel != 0) { float s = (float) StrictMath.hypot(vx, vy); if (s > .01f && s < 14) { vx += vx / s * w.accel; vy += vy / s * w.accel; } }
        if (w.jitter != 0 && age % 6 == 0) { float a = g.rng.r(-w.jitter, w.jitter), c = (float) StrictMath.cos(a), n = (float) StrictMath.sin(a), t = vx * c - vy * n; vy = vx * n + vy * c; vx = t; }
        if (w.smoke && age % 2 == 0) g.fx(Fx.SMOKE, x, y, 0, 0);
        if (w.look == Wpn.FLAME && age % 3 == 0 && w.burn) g.fx(Fx.SMOKE, x, y - 3, 1, 0);
        if (w.dropEvery > 0 && age % w.dropEvery == 0) g.spawn(w.splinter, owner, x, y, 0, 1.5f);
        if (!stuck) {
            if (g.wet(y)) { // bajo el agua: menos gravedad y más roce; el fuego se apaga
                if (w.look == Wpn.FLAME) { dead = true; return; }
                vy += w.grav * .4f; vx *= w.drag * .985f; vy *= w.drag * .985f;
            } else { vy += w.grav; vx *= w.drag; vy *= w.drag; }
        }
        if (w.sticky) {
            if (w.trigger > 0 && age > 40) for (Robot r : g.robots) if (r.alive() && near(r, w.trigger)) { explode(g); return; }
            if (w.burn) for (Robot r : g.robots) if (r.alive() && age % 6 == 0 && near(r, 14)) r.damage(g, w.dmg * 3, 0, -.2f, owner, x, y);
        }
        if (life > 0 && --life == 0) { explode(g); return; }
        if (stuck) { if (!g.t.solid((int) x, (int) y + 2) && !g.t.solid((int) x, (int) y - 2) && !g.t.solid((int) x - 2, (int) y) && !g.t.solid((int) x + 2, (int) y)) stuck = false; return; }
        float sp = Math.abs(vx) + Math.abs(vy); int n = Math.max(1, (int) (sp / 3) + 1);
        float dx = vx / n, dy = vy / n;
        for (int i = 0; i < n; i++) {
            float ox = x, oy = y; x += dx; y += dy;
            if (w.impact) for (Robot r : g.robots) if (r.alive() && (r.id != owner || age > 8) && r.hits(x, y)) {
                if (hitRobot(g, r)) return;
                dead = w.fuse == 0 || w.look == Wpn.BULLET || w.look == Wpn.FLAME; if (dead) return;
            }
            if (w.fan) continue;
            if (g.t.solid((int) x, (int) y)) {
                if (w.ice) { g.t.frost((int) x, (int) y, 4); dead = true; return; } // deja escarcha
                if (w.tower != null) { towers(g, ox, oy); dead = true; return; }
                if (!w.sticky && w.radius == 0) g.hitProp(x, y, w.dmg, vx, vy);
                if (w.sticky) { stuck = true; x = ox; y = oy; vx = vy = 0; return; }
                if (w.bounce > 0) {
                    boolean hx = g.t.solid((int) (ox + dx), (int) oy), hy = g.t.solid((int) ox, (int) (oy + dy));
                    x = ox; y = oy;
                    if (hx || !hy) { vx = -vx * w.bounce; dx = -dx; }
                    if (hy || !hx) { vy = -vy * w.bounce; dy = -dy; }
                    if (w.hops > 0) { g.explode(x, y, w.radius, w.dmg, w.dig, w.knock, owner); if (++hops >= w.hops) { dead = true; return; } vy = Math.min(vy, -4.5f); }
                    if (w.maxBounce > 0 && ++hops >= w.maxBounce) { explode(g); return; }
                    if (Math.abs(vy) < .5f && Math.abs(vx) < .3f) { vx = vy = 0; }
                    return;
                }
                if (w.radius > 0 || w.addDirt > 0 || w.splinter != null) { x = ox; y = oy; explode(g); return; }
                if (w.dig > 0) g.t.carve((int) x, (int) y, w.dig);
                if (w.look == Wpn.BULLET) g.fx(Fx.CHIPS, x, y, 4, 0);
                dead = true; return;
            }
        }
        if (x < 0 || y < 0 || x > g.t.W || y > g.t.H) dead = true;
    }
    /**
     * Dron: siempre avanza; la cruceta elige hacia dónde gira (las 8 direcciones), A dispara misiles y B lo explota.
     * Explota al tocar el suelo o a alguien, al recibir un disparo o una explosión, o cuando se le acaba la batería (mecha).
     */
    private void droneMove(World g) {
        Robot o = g.robots[owner];
        if (o.droneShot != this || !o.alive()) { explode(g); return; } // sin piloto, explota
        int in = ctl, dx = ((in & Input.RIGHT) != 0 ? 1 : 0) - ((in & Input.LEFT) != 0 ? 1 : 0), dy = ((in & Input.DOWN) != 0 ? 1 : 0) - ((in & Input.UP) != 0 ? 1 : 0);
        if (dx != 0 || dy != 0) {
            float d = (float) StrictMath.atan2(dy, dx) - ang;
            while (d > 3.14159265f) d -= 6.2831853f; while (d < -3.14159265f) d += 6.2831853f;
            ang += Math.max(-DRONE_TURN, Math.min(DRONE_TURN, d));
        }
        float c = (float) StrictMath.cos(ang), s = (float) StrictMath.sin(ang), sp = w.speed * (g.wet(y) ? .6f : 1);
        vx = c * sp; vy = s * sp;
        if (cool > 0) cool--;
        if ((in & Input.A) != 0 && cool == 0 && w.splinter != null) {
            cool = DRONE_COOL; float ms = w.splinter.speed;
            g.spawn(w.splinter, owner, x + c * 8, y + s * 8, c * ms, s * ms);
            g.fx(Fx.FLASH, x + c * 8, y + s * 8, (float) StrictMath.toDegrees(ang), owner).b = -1;
        }
        if ((in & Input.B) != 0 && (ctlPrev & Input.B) == 0 && age > 6) { explode(g); return; }
        if (life > 0 && --life == 0) { explode(g); return; }
        int n = (int) (sp / 2) + 1;
        for (int i = 0; i < n; i++) {
            x += vx / n; y += vy / n;
            if (g.t.solid((int) x, (int) y)) { explode(g); return; }
            for (Robot r : g.robots) if (r.alive() && (r.id != owner || age > 45) && r.hits(x, y)) { explode(g); return; }
        }
        if (x < 0 || y < 0 || x > g.t.W || y > g.t.H) dead = true;
    }
    private boolean near(Robot r, int d) { return Math.abs(r.px - x) < d && y > r.py - r.h - d && y < r.py + d; }
    private boolean hitRobot(World g, Robot r) {
        if (w.lasso) { r.lasso(g, owner); dead = true; return true; }
        if (w.ice) { r.chill(g, owner); dead = true; return true; } // brisa helada: enfría; mucho rato seguido, lo congela
        if (w.punch) { r.launch(g, vx > 0 ? 1 : -1, owner, w.dmg); dead = true; return true; } // Cannon Punch: lo lanza en el sentido del golpe
        if (w.radius > 0) { explode(g); return true; }
        float s = Math.max(1, (float) StrictMath.hypot(vx, vy)), k = Math.max(w.knock, s * .05f); // lo rápido empuja un poco
        r.damage(g, w.dmg, vx / s * k, vy / s * k - .15f, owner, x, y);
        return false;
    }
    /** Robot Trap: se abre, sale un haz de luz y atrae todo hacia el centro; quien llega al centro pierde toda la energía. */
    public static final int TRAP_OPEN = 45, TRAP_END = 260, TRAP_BOOM = 350; public static final float TRAP_R = 100;
    /** La trampa no se pega: rebota en paredes y techos y siempre termina en el suelo. */
    private void trapMove(World g) {
        if (stuck) { if (g.t.solid((int) x, (int) y + 2)) { trap(g); return; } stuck = false; } // si le quitan el suelo, vuelve a caer
        vy = Math.min(9, vy + w.grav);
        float sp = Math.abs(vx) + Math.abs(vy); int n = Math.max(1, (int) (sp / 2) + 1);
        for (int i = 0; i < n; i++) {
            float ox = x; x += vx / n;
            if (g.t.solid((int) x, (int) y - 4)) { x = ox; vx = 0; }
            float oy = y; y += vy / n;
            if (g.t.solid((int) x, (int) y)) { y = oy; if (vy > 0) { stuck = true; vx = vy = 0; return; } vy = 0; }
        }
        if (x < 0 || y < 0 || x > g.t.W || y > g.t.H) dead = true;
    }
    private void trap(World g) {
        int t = ++hops;
        if (t == TRAP_OPEN) g.fx(Fx.TRAP, x, y, 0, owner);
        if (t > TRAP_OPEN && t < TRAP_END) {
            for (Robot r : g.robots) {
                if (!r.alive() || r.invuln > 0) continue; // atrae a todos, también a quien la puso
                float dx = x - r.px, dy = y - (r.py - 20), d = (float) StrictMath.hypot(dx, dy);
                if (d > TRAP_R) continue;
                float p = .4f + 3 * (1 - d / TRAP_R);
                r.vx += Math.signum(dx) * Math.min(p, Math.abs(dx) * .5f + .1f); if (dy < -10) r.vy = Math.min(r.vy, 0) - .2f; // hacia el centro
                if (Math.abs(dx) < 9 && r.py > y - 70 && r.py < y + 12) { g.fx(Fx.TRAP, x, y, 1, r.id); r.damage(g, 999, 0, -3, owner, r.px, r.py - 30); }
            }
            for (Shot o : g.shots) if (o != this && !o.w.trap && o.wait == 0) {
                float dx = x - o.x, dy = y - 10 - o.y, d = (float) StrictMath.hypot(dx, dy);
                if (d < TRAP_R && d > 1) { o.vx += dx / d * .5f; o.vy += dy / d * .5f; o.stuck = false; }
            }
            for (Prop p : g.props) if (!p.gone) { // y también las cajas
                float dx = x - (p.x + p.w / 2f), dy = y - (p.y + p.h / 2f), d = (float) StrictMath.hypot(dx, dy);
                if (d < TRAP_R + p.w / 2f && d > 8 && hops % 4 == 0) g.push(p, dx / d * 1.2f, dy / d * 1.2f - .3f);
            }
        }
        if (t >= TRAP_BOOM) { dead = true; g.explode(x, y - 3, 12, 6, 6, .8f, owner); }
    }
    /** FLAME TOWER: llamas a cada lado sobre el piso, una tras otra hacia afuera, hasta que se acaba el suelo o hay una pared. */
    void towers(World g, float x0, float y0) {
        g.fx(Fx.EXPLODE, x0, y0, 14, owner);
        int dir = vx >= 0 ? 1 : -1; float gy = y0; // solo hacia donde iba la bola
        for (int k = 0; k <= 7; k++) {
            float px = x0 + dir * k * TOWER_GAP; int y = ground(g, px, gy); if (y < 0) break; gy = y;
            Shot f = g.spawn(w.tower, owner, px, y, 0, 0); f.wait = k * 4; f.life = w.tower.fuse;
        }
    }
    /** Primer suelo bajo (x, cerca de y): busca 26 px arriba y 40 abajo; -1 si no hay o es una pared. */
    static int ground(World g, float x, float y) {
        int ix = (int) x; if (ix < 0 || ix >= g.t.W) return -1;
        for (int yy = (int) y - 26; yy < (int) y + 40; yy++) if (!g.t.solid(ix, yy) && g.t.solid(ix, yy + 1)) return yy + 1;
        return -1;
    }
    /** Llama de FLAME TOWER: sube del piso y quema a quien esté encima (no al que la lanzó). */
    private void pillarMove(World g) {
        if (life > 0 && --life == 0) { dead = true; return; }
        if (g.wet(y - 4)) { dead = true; return; }
        if (age % 6 == 1) for (Robot r : g.robots) if (r.alive() && r.id != owner && Math.abs(r.px - x) < 9 && r.py > y - 72 && r.py - r.h < y + 2) r.damage(g, w.dmg, 0, -1.2f, owner, x, y - 20);
    }
    /** BOOBLE SEAL: avanza lento, sube flotando y atrapa al primero que toca; con alguien adentro sube despacio y le quita vida. */
    private void bubbleMove(World g) {
        if (life > 0 && --life == 0) { pop(g); return; }
        if (held < 0) {
            vx *= .99f; vy = Math.max(-.7f, vy - .012f);
            float nx = x + vx, ny = y + vy;
            if (g.t.solid((int) nx, (int) ny - 8)) { vy = 0; ny = y; } if (g.t.solid((int) nx + (vx > 0 ? 8 : -8), (int) ny)) { vx = 0; nx = x; }
            x = nx; y = ny;
            for (Robot r : g.robots) if (r.alive() && r.bubble == null && (r.id != owner || age > 40) && Math.abs(r.px - x) < 18 && y > r.py - r.h - 14 && y < r.py + 8) {
                held = r.id; r.bubble = this; r.bubbleHits = 0; r.vx = r.vy = 0; r.rope = 0; g.fx(Fx.LASSO, x, y, 1, r.id); y = r.py - r.h / 2f; x = r.px; life = 600; break; }
            if (x < 0 || y < 0 || x > g.t.W || y > g.t.H) dead = true;
            return;
        }
        Robot r = g.robots[held];
        if (!r.alive() || r.bubble != this) { pop(g); return; }
        if (!g.t.solid((int) x, (int) (y - BUBBLE_R - 2)) && !g.t.solid((int) x - 14, (int) (y - BUBBLE_R + 4)) && !g.t.solid((int) x + 14, (int) (y - BUBBLE_R + 4))) y -= .35f; // sube despacio hasta el techo
        r.px = Math.round(x); r.py = Math.round(y + r.h / 2f); r.vx = r.vy = 0;
        if (age % BUBBLE_TICK == 0) r.damage(g, w.dmg, 0, 0, owner, x, y);
    }
    /** TORNADO GUN: corre pegado al piso; sube escalones chicos, cae si no hay suelo y se deshace contra una pared. */
    private void tornadoMove(World g) {
        if (life > 0 && --life == 0) { dead = true; return; }
        int dir = vx >= 0 ? 1 : -1; float nx = x + vx;
        if (g.t.solid((int) nx + dir * 6, (int) y - 4)) { // escalón: sube hasta 10 px
            int up = 0; while (up <= 10 && g.t.solid((int) nx + dir * 6, (int) y - 4 - up)) up++;
            if (up > 10) { dead = true; g.fx(Fx.SMOKE, x, y - 10, 1, 0); return; } y -= up;
        }
        x = nx;
        if (!g.t.solid((int) x, (int) y + 1)) { vy = Math.min(6, vy + .35f); y += vy; while (g.t.solid((int) x, (int) y)) { y--; vy = 0; } } else { vy = 0; while (g.t.solid((int) x, (int) y)) y--; }
        if (age % 3 == 0) g.fx(Fx.SMOKE, x - dir * 4, y - 2, 0, 0);
        for (Robot r : g.robots) if (r.alive() && r.id != owner && r.thrownT == 0 && Math.abs(r.px - x) < 14 && r.py > y - 46 && r.py - r.h < y) { // lo lanza para cualquier lado
            r.launch(g, g.rng.i(2) == 0 ? -1 : 1, owner, w.dmg); r.vx *= g.rng.r(.5f, 1.3f); r.vy = -g.rng.r(6, 11); }
        if (x < 0 || y < 0 || x > g.t.W || y > g.t.H) dead = true;
    }
    /** La burbuja revienta y suelta a quien tenía. */
    public void pop(World g) {
        dead = true; g.fx(Fx.LASSO, x, y, 0, held < 0 ? owner : held);
        if (held >= 0) { Robot r = g.robots[held]; if (r.bubble == this) { r.bubble = null; r.vy = -2.5f; } held = -1; }
    }
    public void explode(World g) {
        dead = true;
        if (w.addDirt > 0) { g.t.addDirt((int) x, (int) y, w.addDirt); g.fx(Fx.CHIPS, x, y, 16, 0); return; }
        if (w.radius > 0) g.explode(x, y, w.radius, w.dmg, w.dig, w.knock, owner, w.boomStyle);
        if (w.splinter != null && w.nSplinter > 0) for (int i = 0; i < w.nSplinter; i++) {
            float a = g.rng.r(0, 6.283f), v = w.splinter.speed * g.rng.r(.5f, 1.1f);
            g.spawn(w.splinter, owner, x, y - 2, (float) StrictMath.cos(a) * v, (float) StrictMath.sin(a) * v - 1);
        }
    }
}
