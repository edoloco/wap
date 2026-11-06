package com.wap.core;
/** Contrincante: genera la misma máscara de entrada que un jugador humano. */
public final class Bot {
    final Rng r; final int me; final float skill; int hookHold, detour, detourDir = 1, comboT, combo, moveT, moveDir = 1, stuckT, lastX, jumpT, hookT, fireHold, pressT;
    public Bot(int seed, int me, float skill) { r = new Rng(seed); this.me = me; this.skill = skill; }
    private boolean clear(World g, float x0, float y0, float x1, float y1) {
        int n = (int) (Math.hypot(x1 - x0, y1 - y0) / 8) + 1;
        for (int i = 0; i <= n; i++) if (g.t.solid((int) (x0 + (x1 - x0) * i / n), (int) (y0 + (y1 - y0) * i / n))) return false;
        return true;
    }
    static final int C = 20, GW = Terrain.W / C, GH = Terrain.H / C;
    final short[] dist = new short[GW * GH]; final int[] queue = new int[GW * GH]; int navT;
    /** Mapa de distancias por el aire hasta el rival: así rodea rocas y sigue los túneles. */
    private void flood(World g, Robot e) {
        java.util.Arrays.fill(dist, (short) -1);
        int s = Math.min(GH - 1, Math.max(0, (e.py - 27) / C)) * GW + Math.min(GW - 1, Math.max(0, e.px / C)), a = 0, b = 0;
        dist[s] = 0; queue[b++] = s;
        while (a < b) {
            int c = queue[a++], cx = c % GW, cy = c / GW; short d = (short) (dist[c] + 1);
            for (int i = 0; i < 4; i++) {
                int nx = cx + (i == 0 ? 1 : i == 1 ? -1 : 0), ny = cy + (i == 2 ? 1 : i == 3 ? -1 : 0);
                if (nx < 1 || ny < 1 || nx >= GW - 1 || ny >= GH - 1) continue;
                int n = ny * GW + nx; if (dist[n] >= 0) continue;
                int wx = nx * C + C / 2, wy = ny * C + C / 2;
                if (g.t.solid(wx, wy) || g.t.solid(wx - 9, wy) || g.t.solid(wx + 9, wy) || g.t.solid(wx, wy - 12)) continue;
                dist[n] = d; queue[b++] = n;
            }
        }
    }
    private boolean ready(Robot m, int s) { return m.reload[s] == 0; }
    public int think(World g) {
        Robot m = g.robots[me], e = g.robots[1 - me];
        if (!m.alive()) return 0;
        int out = 0;
        if (comboT > 0) { comboT--; return comboT == 1 ? combo & (Input.LEFT | Input.RIGHT | Input.UP | Input.DOWN) : comboT == 0 ? combo : 0; }
        if (!e.alive()) { return out; }
        float dx = e.px - m.px, dy = (e.py - 30) - (m.py - 30), ad = Math.abs(dx), ady = Math.abs(dy);
        int to = dx > 0 ? 1 : -1;
        boolean see = clear(g, m.px, m.py - 36, e.px, e.py - 30);
        if (--moveT <= 0) {
            moveT = 25 + r.i(60);
            moveDir = !see || ad > 240 ? to : ad < 90 ? (r.i(3) == 0 ? to : -to) : (r.i(3) == 0 ? -to : to);
        }
        if (--navT <= 0) { flood(g, e); navT = 40; }
        int wy = 0; boolean nav = false;
        if (!see || ad > 260) {
            int c = Math.max(0, Math.min(GH - 1, (m.py - 27) / C)) * GW + Math.max(0, Math.min(GW - 1, m.px / C)), best = c;
            if (dist[c] < 0) for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) { int q = c + i * GW + j; if (q >= 0 && q < dist.length && dist[q] >= 0) best = q; }
            for (int k = 0; k < 4; k++) {
                int t = best; for (int q : new int[]{best + 1, best - 1, best + GW, best - GW}) if (q >= 0 && q < dist.length && dist[q] >= 0 && (dist[t] < 0 || dist[q] < dist[t])) t = q;
                best = t;
            }
            if (dist[best] >= 0) {
                nav = true; int wx = best % GW * C + C / 2; wy = best / GW * C + C / 2 - (m.py - 27);
                moveDir = wx - m.px > 6 ? 1 : wx - m.px < -6 ? -1 : 0;
            }
        }
        if (!see && !nav) moveDir = ad > 30 ? to : moveDir;
        if (detour > 0) { detour--; moveDir = detourDir; }
        else if (stuckT > 50 && r.i(30) == 0) { detour = 50 + r.i(60); detourDir = r.i(2) == 0 ? 1 : -1; }
        boolean hold = see && ad < 200 && ady < 40 && moveT % 40 > 20;
        int dir = hold ? 0 : moveDir;
        if (see && m.facing != to) dir = to;
        if (Math.abs(m.px - lastX) < 1) stuckT++; else stuckT = 0;
        lastX = m.px;
        if (dir > 0) out |= Input.RIGHT; else if (dir < 0) out |= Input.LEFT;
        if (nav && wy < -24 && m.grounded && m.rope == 0 && jumpT < 0) { out |= Input.B; jumpT = 18; }
        if (--jumpT < 0 && m.grounded && (stuckT > 12 && stuckT < 60 || r.f() < .012f || dy < -44 && ad < 330 && r.f() < .1f)) { out |= Input.B; jumpT = 22; }
        else if (!m.grounded && m.vy < 0 && jumpT > 8) out |= Input.B;
        if (--hookT < 0 && m.rope == 0 && (nav ? wy < -40 || stuckT > 60 : dy < -70 || stuckT > 60) && r.f() < .2f) { comboT = 2; combo = Input.UP | Input.B | (moveDir > 0 ? Input.RIGHT : moveDir < 0 ? Input.LEFT : 0); hookT = 40; hookHold = 70; return 0; }
        if (m.rope != 0) { hookT = 20; if (--hookHold > 0 && (nav ? wy < -12 : dy < -12)) out |= Input.B; return out; }
        if (pressT > 0) pressT--;
        float aimOK = ady < 42 ? 1 : 0;
        if (see && (dx > 0) == (m.facing > 0) && r.f() < .10f * skill) {
            if (ady < 44 && ad < 520) {
                int q = r.i(5), side = to > 0 ? Input.RIGHT : Input.LEFT;
                if (q == 0 && ready(m, 1)) { comboT = 2; combo = side | Input.A; return 0; }
                if (q == 1 && ready(m, 3)) { comboT = 2; combo = side | Input.B; return 0; }
                if (q == 2 && ready(m, 0)) { comboT = 2; combo = Input.DOWN | Input.A; return 0; }
                if (q == 3 && ready(m, 2)) { comboT = 2; combo = Input.UP | Input.A; return 0; }
                fireHold = 6 + r.i(30);
            } else if (dy < -40 && ad < 320 && m.grounded && jumpT < 0) { out |= Input.B; jumpT = 22; }
        }
        if (!see && fireHold == 0 && ad < 260 && ady < 44 && m.facing == to && r.f() < .03f * skill) fireHold = 20;
        if (stuckT > 28 && stuckT % 40 == 29) fireHold = 24; // si algo le corta el paso, lo destruye
        if (see && ad < 100 && ady < 20 && m.grounded && r.f() < .01f) { comboT = 2; combo = Input.DOWN | Input.B; }
        if (fireHold > 0) { fireHold--; if (aimOK > 0 || r.f() < .3f) out |= Input.A; }
        return out;
    }
}
