package com.wap.core;
/** Contrincante: solo genera entradas, igual que un jugador, y usa salto, paredes, gancho y armas. */
public final class Bot {
    static final int C = 20, GW = Terrain.W / C, GH = Terrain.H / C;
    final Rng r; final int me; final float skill; final short[] dist = new short[GW * GH]; final int[] queue = new int[GW * GH];
    int navT, comboT, combo, moveT, moveDir = 1, stuckT, lastX, lastY, jumpT, jumpHold, hookT, hookHold, fireHold;
    int hookDir, checkT, lastCost = 9999, frust, climbDir;
    public Bot(int seed, int me, float skill) { r = new Rng(seed); this.me = me; this.skill = skill; }
    private boolean clear(World g, float x0, float y0, float x1, float y1) {
        int n = (int) (StrictMath.hypot(x1 - x0, y1 - y0) / 8) + 1;
        for (int i = 0; i <= n; i++) if (g.t.solid((int) (x0 + (x1 - x0) * i / n), (int) (y0 + (y1 - y0) * i / n))) return false;
        return true;
    }
    private boolean ready(Robot m, int s) { return m.reload[s] == 0; }
    /** Distancia por el aire hasta el rival, para seguir los pasillos del nivel. */
    private void flood(World g, Robot e) {
        java.util.Arrays.fill(dist, (short) -1);
        int s = cell(e.px, e.py - 27), a = 0, b = 0;
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
    private static int cell(int x, int y) { return Math.max(0, Math.min(GH - 1, y / C)) * GW + Math.max(0, Math.min(GW - 1, x / C)); }
    private int near(int c) {
        if (dist[c] >= 0) return dist[c];
        int best = -1;
        for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) { int q = c + i * GW + j; if (q >= 0 && q < dist.length && dist[q] >= 0 && (best < 0 || dist[q] < best)) best = dist[q]; }
        return best;
    }
    private int nearCell(int c) {
        if (dist[c] >= 0) return c; int best = c;
        for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) { int q = c + i * GW + j; if (q >= 0 && q < dist.length && dist[q] >= 0) best = q; }
        return best;
    }
    /** Prueba los mismos tiros de gancho que tiene un jugador y elige el que más acerca al objetivo. */
    private boolean pickHook(World g, Robot m, int here) {
        int bestD = here - 4; boolean found = false;
        for (int o = -1; o <= 1; o++) {
            int f = o == 0 ? m.facing : o; float a = (float) StrictMath.toRadians(o == 0 ? 72 : 45);
            float dx = (float) StrictMath.cos(a) * f, dy = -(float) StrictMath.sin(a), x = m.px + f * 6, y = m.py - 44;
            for (int d = 0; d < Robot.HOOK_RANGE; d += 8) {
                x += dx * 8; y += dy * 8;
                if (!g.t.solid((int) x, (int) y)) continue;
                int q = near(cell((int) (x - dx * 16), (int) (y - dy * 16) + 40));
                if (q >= 0 && q < bestD) { bestD = q; hookDir = o; found = true; }
                break;
            }
        }
        return found;
    }
    public int think(World g) {
        Robot m = g.robots[me], e = g.robots[1 - me];
        if (!m.alive()) return 0;
        if (comboT > 0) { comboT--; return comboT == 1 ? combo & (Input.LEFT | Input.RIGHT | Input.UP | Input.DOWN) : comboT == 0 ? combo : 0; }
        if (!e.alive()) return 0;
        float dx = e.px - m.px, dy = (e.py - 30) - (m.py - 30), ad = Math.abs(dx), ady = Math.abs(dy);
        int to = dx > 0 ? 1 : -1, out = 0;
        boolean see = clear(g, m.px, m.py - 36, e.px, e.py - 30);
        if (--navT <= 0) { flood(g, e); navT = 30; }
        if (Math.abs(m.px - lastX) + Math.abs(m.py - lastY) < 1) stuckT++; else stuckT = 0;
        lastX = m.px; lastY = m.py; jumpT--; hookT--;
        int here = cell(m.px, m.py - 27), best = nearCell(here), cost = near(here), wx = m.px, wy = 0;
        boolean chase = cost >= 0 && (!see || ad > 300 || ady >= 60);
        if (--checkT <= 0) { checkT = 90; if (chase && cost >= lastCost - 2) frust = 90; lastCost = cost; } // sin avanzar: cambia de táctica
        if (frust > 0) frust--;
        if (chase) {
            for (int k = 0; k < 6; k++) {
                int t = best;
                for (int q : new int[]{best + 1, best - 1, best + GW, best - GW}) if (q >= 0 && q < dist.length && dist[q] >= 0 && dist[q] < dist[t]) t = q;
                best = t;
            }
            wx = best % GW * C + C / 2; wy = best / GW * C + C / 2 - (m.py - 27);
            moveDir = wx - m.px > 6 ? 1 : wx - m.px < -6 ? -1 : 0;
        } else {
            if (--moveT <= 0) { moveT = 25 + r.i(60); moveDir = ad > 240 ? to : ad < 90 ? (r.i(3) == 0 ? to : -to) : (r.i(3) == 0 ? -to : to); }
            if (see && m.facing != to) moveDir = to;
            else if (see && ad < 200 && ady < 40 && moveT % 40 > 20) moveDir = 0;
        }
        if (frust > 0 && chase) {
            if (!m.grounded && wy < -22) {
                if (m.wallDir != 0) climbDir = -m.wallDir;
                else if (climbDir == 0) climbDir = g.t.solid(m.px + 40, m.py - 27) ? 1 : g.t.solid(m.px - 40, m.py - 27) ? -1 : m.facing;
                moveDir = climbDir;
            } else if (ad < 300) { moveDir = m.facing == to ? 0 : to; if (m.facing == to && fireHold == 0) fireHold = 20; }
        } else climbDir = 0;
        if (m.rope != 0) {
            boolean arrived = m.rope == 2 && StrictMath.hypot(m.rx - m.px, m.ry - (m.py - 44)) < 34 || see && ady < 40;
            if (--hookHold > 0 && !arrived) out |= Input.B;
            if (moveDir > 0) out |= Input.RIGHT; else if (moveDir < 0) out |= Input.LEFT;
            if (fireHold > 0) { fireHold--; out |= Input.A; }
            return out;
        }
        if (moveDir > 0) out |= Input.RIGHT; else if (moveDir < 0) out |= Input.LEFT;
        boolean up = chase ? wy < -22 : dy < -44 && ad < 320;
        if (m.grounded && jumpT < 0 && (up || m.blocked || stuckT > 8 || r.f() < .008f)) { out |= Input.B; jumpHold = up || m.blocked ? 30 : 12; jumpT = 24; }
        else if (jumpHold > 0 && !m.grounded) { jumpHold--; out |= Input.B; }
        if (m.wallDir != 0 && (up || frust > 0)) { out &= ~Input.B; if (g.tick % 6 == 0) { out |= Input.B; out &= ~(Input.LEFT | Input.RIGHT); jumpHold = 26; } }
        if (hookT < 0 && (chase && up || stuckT > 30) && r.f() < .35f && pickHook(g, m, stuckT > 30 ? cost + 5 : cost)) {
            comboT = 2; combo = Input.UP | Input.B | (hookDir > 0 ? Input.RIGHT : hookDir < 0 ? Input.LEFT : 0); hookT = 45; hookHold = 80; return 0;
        }
        if (see && (dx > 0) == (m.facing > 0) && r.f() < .10f * skill && ady < 44 && ad < 520) {
            int q = r.i(5), side = to > 0 ? Input.RIGHT : Input.LEFT;
            if (q == 0 && ready(m, 1)) { comboT = 2; combo = side | Input.A; return 0; }
            if (q == 2 && ready(m, 0)) { comboT = 2; combo = Input.DOWN | Input.A; return 0; }
            if (q == 3 && ready(m, 2)) { comboT = 2; combo = Input.UP | Input.A; return 0; }
            fireHold = 6 + r.i(30);
        }
        if (stuckT > 28 && stuckT % 40 == 29) fireHold = 24; // si algo le corta el paso, lo destruye
        if (see && ad < 100 && ady < 20 && m.grounded && r.f() < .01f) { comboT = 2; combo = Input.DOWN | Input.B; }
        if (fireHold > 0) { fireHold--; out |= Input.A; }
        return out;
    }
}
