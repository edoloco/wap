package com.wap.core;
/** Un proyectil en vuelo. */
public final class Shot {
    public Wpn w; public int owner, age, hops, life; public float x, y, vx, vy; public boolean stuck, dead;
    public void step(World g) {
        age++;
        if (w.accel != 0) { float s = (float) Math.hypot(vx, vy); if (s > .01f && s < 14) { vx += vx / s * w.accel; vy += vy / s * w.accel; } }
        if (w.jitter != 0 && age % 6 == 0) { float a = g.rng.r(-w.jitter, w.jitter), c = (float) Math.cos(a), n = (float) Math.sin(a), t = vx * c - vy * n; vy = vx * n + vy * c; vx = t; }
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
                if (w.sticky) { stuck = true; x = ox; y = oy; vx = vy = 0; return; }
                if (w.bounce > 0) {
                    boolean hx = g.t.solid((int) (ox + dx), (int) oy), hy = g.t.solid((int) ox, (int) (oy + dy));
                    x = ox; y = oy;
                    if (hx || !hy) { vx = -vx * w.bounce; dx = -dx; }
                    if (hy || !hx) { vy = -vy * w.bounce; dy = -dy; }
                    if (w.hops > 0) { g.explode(x, y, w.radius, w.dmg, w.dig, w.knock, owner); if (++hops >= w.hops) { dead = true; return; } vy = Math.min(vy, -4.5f); }
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
        if (w.radius > 0) { explode(g); return true; }
        float s = Math.max(1, (float) Math.hypot(vx, vy));
        r.damage(g, w.dmg, vx / s * w.knock, vy / s * w.knock - .15f, owner, x, y);
        return false;
    }
    public void explode(World g) {
        dead = true;
        if (w.addDirt > 0) { g.t.addDirt((int) x, (int) y, w.addDirt); g.fx(Fx.CHIPS, x, y, 16, 0); return; }
        if (w.radius > 0) g.explode(x, y, w.radius, w.dmg, w.dig, w.knock, owner);
        if (w.splinter != null && w.nSplinter > 0) for (int i = 0; i < w.nSplinter; i++) {
            float a = g.rng.r(0, 6.283f), v = w.splinter.speed * g.rng.r(.5f, 1.1f);
            g.spawn(w.splinter, owner, x, y - 2, (float) Math.cos(a) * v, (float) Math.sin(a) * v - 1);
        }
    }
}
