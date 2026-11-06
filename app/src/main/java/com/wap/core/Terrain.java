package com.wap.core;
/** Mapa destructible píxel a píxel, como en Liero. 0 aire, 1 tierra, 2 roca indestructible. */
public final class Terrain {
    public static final int W = 1800, H = 1000, TILE = 128;
    public static final int TW = (W + TILE - 1) / TILE, TH = (H + TILE - 1) / TILE;
    public final byte[] m = new byte[W * H];
    public final boolean[] dirty = new boolean[TW * TH];

    public boolean solid(int x, int y) { return x < 0 || y < 0 || x >= W || y >= H || m[y * W + x] != 0; }
    public int at(int x, int y) { return x < 0 || y < 0 || x >= W || y >= H ? 2 : m[y * W + x]; }

    private void disc(int cx, int cy, int rx, int ry, int from, int to, boolean any) {
        int x0 = Math.max(0, cx - rx), x1 = Math.min(W - 1, cx + rx), y0 = Math.max(0, cy - ry), y1 = Math.min(H - 1, cy + ry);
        long a = (long) rx * rx * ry * ry; if (a == 0) return;
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
            long dx = x - cx, dy = y - cy;
            if (dx * dx * ry * ry + dy * dy * rx * rx > a) continue;
            int i = y * W + x;
            if (any ? m[i] != to : m[i] == from) m[i] = (byte) to;
        }
        mark(x0, y0, x1, y1);
    }
    private void mark(int x0, int y0, int x1, int y1) {
        int a = Math.max(0, (x0 - 3) / TILE), b = Math.min(TW - 1, (x1 + 3) / TILE);
        int c = Math.max(0, (y0 - 3) / TILE), d = Math.min(TH - 1, (y1 + 3) / TILE);
        for (int ty = c; ty <= d; ty++) for (int tx = a; tx <= b; tx++) dirty[ty * TW + tx] = true;
    }
    public void carve(int cx, int cy, int r) { disc(cx, cy, r, r, 1, 0, false); }
    public void carveE(int cx, int cy, int rx, int ry) { disc(cx, cy, rx, ry, 1, 0, false); }
    public void addDirt(int cx, int cy, int r) { disc(cx, cy, r, r, 0, 1, false); }

    public void generate(Rng g) {
        java.util.Arrays.fill(m, (byte) 1);
        for (int n = 0; n < 70; n++) {
            int cx = g.i(W), cy = g.i(H), c = 3 + g.i(5);
            for (int i = 0; i < c; i++) { int r = 12 + g.i(30); disc(cx + g.i(70) - 35, cy + g.i(50) - 25, r, r * 3 / 4, 0, 2, true); cx += g.i(50) - 25; }
        }
        final int cols = 5, rows = 3;
        int[] px = new int[cols * rows], py = new int[cols * rows];
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
            int i = r * cols + c;
            px[i] = 190 + c * 355 + g.i(90) - 45; py[i] = 175 + r * 320 + g.i(70) - 35;
            int rx = 105 + g.i(70), ry = 72 + g.i(36);
            disc(px[i], py[i], rx, ry, 0, 0, true);
            for (int k = 0; k < 5; k++) { int a = g.i(rx) - rx / 2, b = g.i(ry) - ry / 2, q = 44 + g.i(36); disc(px[i] + a, py[i] + b, q, q, 0, 0, true); }
            disc(px[i], py[i] + ry - 8, rx - 10, 24, 0, 1, true);
        }
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
            int i = r * cols + c;
            if (c + 1 < cols) tunnel(g, px[i], py[i], px[i + 1], py[i + 1]);
            if (r + 1 < rows && (c % 2 == r % 2 || g.i(3) == 0)) tunnel(g, px[i], py[i], px[i + cols], py[i + cols]);
        }
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++)
            if (x < 14 || y < 14 || x >= W - 14 || y >= H - 14) m[y * W + x] = 2;
        java.util.Arrays.fill(dirty, true);
    }
    private void tunnel(Rng g, int x0, int y0, int x1, int y1) {
        float x = x0, y = y0; int n = (int) (Math.hypot(x1 - x0, y1 - y0) / 16);
        float w = 0;
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n; w += g.r(-10, 10); w *= .9f;
            x = x0 + (x1 - x0) * t; y = y0 + (y1 - y0) * t + w;
            disc((int) x, (int) y, 42, 42, 0, 0, true);
        }
    }
}
