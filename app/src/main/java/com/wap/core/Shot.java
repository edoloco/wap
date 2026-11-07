package com.wap.core;
/** Un proyectil en vuelo. */
public final class Shot {
    public Wpn w; public int owner, age, hops, life, wait; public float x, y, vx, vy; public boolean stuck, dead;
    public void step(World g) {
        if (wait > 0) { wait--; return; } // cargas del Death Ray que salen después
        age++;
        if (w.trap) { trapMove(g); return; }
        if (w.fan) for (Shot o : g.shots) if (o != this && !o.w.fan && o.wait == 0 && Math.abs(o.x - x) < 12 && Math.abs(o.y - y) < 12) { o.vx += vx * .14f; o.vy += vy * .14f; o.stuck = false; } // el ventilador desvía balas
        if (w.accel != 0) { float s = (float) StrictMath.hypot(vx, vy); if (s > .01f && s < 14) { vx += vx / s * w.accel; vy += vy / s * w.accel; } }
        if (w.jitter != 0 && age % 6 == 0) { float a = g.rng.r(-w.jitter, w.jitter), c = (float) StrictMath.cos(a), n = (float) StrictMath.sin(a), t = vx * c - vy * n; vy = vx * n + vy * c; vx = t; }
        if (w.smoke && age % 2 == 0) g.fx(Fx.SMOKE, x, y, 0, 0);
        if (w.look == Wpn.FLAME && age % 3 == 0 && w.burn) g.fx(Fx.SMOKE, x, y - 3, 1, 0);
        if (w.dropEvery > 0 && age % w.dropEvery == 0) g.spawn(w.splinter, owner, x, y, 0, 1.5f);
        if (!stuck) { vy += w.grav; vx *= w.drag; vy *= w.drag; }
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
        if (x < 0 || y < 0 || x > Terrain.W || y > Terrain.H) dead = true;
    }
    private boolean near(Robot r, int d) { return Math.abs(r.px - x) < d && y > r.py - r.h - d && y < r.py + d; }
    private boolean hitRobot(World g, Robot r) {
        if (w.lasso) { r.lasso(g, owner); dead = true; return true; }
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
        if (x < 0 || y < 0 || x > Terrain.W || y > Terrain.H) dead = true;
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
