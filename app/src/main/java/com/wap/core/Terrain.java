package com.wap.core;
/** Mapa destructible píxel a píxel, como en Liero, con bloques de escenario estilo plataformas. */
public final class Terrain {
    public static final int W = 1800, H = 1000, TILE = 128;
    /** 0 es aire; STEEL es indestructible; lo demás se destruye. */
    public static final byte PANEL = 1, STEEL = 2, CRATE = 3, VENT = 4, COMPUTER = 5;
    static final int G = 32, FILL = 0xFF4A5F80;
    public final int[] tex = new int[W * H];
    public static final int TW = (W + TILE - 1) / TILE, TH = (H + TILE - 1) / TILE;
    public final byte[] m = new byte[W * H];
    public final boolean[] dirty = new boolean[TW * TH];

    public boolean solid(int x, int y) { return x < 0 || y < 0 || x >= W || y >= H || m[y * W + x] != 0; }
    public int at(int x, int y) { return x < 0 || y < 0 || x >= W || y >= H ? 2 : m[y * W + x]; }

    private void disc(int cx, int cy, int rx, int ry, int from, int to) {
        int x0 = Math.max(0, cx - rx), x1 = Math.min(W - 1, cx + rx), y0 = Math.max(0, cy - ry), y1 = Math.min(H - 1, cy + ry);
        long a = (long) rx * rx * ry * ry; if (a == 0) return;
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
            long dx = x - cx, dy = y - cy;
            if (dx * dx * ry * ry + dy * dy * rx * rx > a) continue;
            int i = y * W + x;
            if (from < 0) { if (m[i] != 0 && m[i] != STEEL) m[i] = 0; }
            else if (m[i] == from) { m[i] = (byte) to; tex[i] = FILL; }
        }
        mark(x0, y0, x1, y1);
    }
    private void mark(int x0, int y0, int x1, int y1) {
        int a = Math.max(0, (x0 - 3) / TILE), b = Math.min(TW - 1, (x1 + 3) / TILE);
        int c = Math.max(0, (y0 - 3) / TILE), d = Math.min(TH - 1, (y1 + 3) / TILE);
        for (int ty = c; ty <= d; ty++) for (int tx = a; tx <= b; tx++) dirty[ty * TW + tx] = true;
    }
    public void carve(int cx, int cy, int r) { disc(cx, cy, r, r, -1, 0); }
    public void carveE(int cx, int cy, int rx, int ry) { disc(cx, cy, rx, ry, -1, 0); }
    public void addDirt(int cx, int cy, int r) { disc(cx, cy, r, r, 0, 1); }

    private void put(int x, int y, int mat, int c) { if (x >= 0 && y >= 0 && x < W && y < H) { m[y * W + x] = (byte) mat; tex[y * W + x] = c; } }
    private boolean clearArea(int x, int y, int w, int h) {
        for (int j = y; j < y + h; j += 4) for (int i = x; i < x + w; i += 4) if (at(i, j) != 0) return false;
        return true;
    }
    void panel(int x, int y) {
        for (int j = 0; j < G; j++) for (int i = 0; i < G; i++) {
            int c = 0xFF3B5478;
            if (i < 3 || j < 3) c = 0xFF7596BE; else if (i >= G - 3 || j >= G - 3) c = 0xFF1F2E46;
            else if (i > 9 && i < 22 && j > 9 && j < 22) c = 0xFF2E4466;
            if ((i - 6) * (i - 6) + (j - 6) * (j - 6) < 4 || (i - 25) * (i - 25) + (j - 6) * (j - 6) < 4 || (i - 6) * (i - 6) + (j - 25) * (j - 25) < 4 || (i - 25) * (i - 25) + (j - 25) * (j - 25) < 4) c = 0xFFA9C0DC;
            put(x + i, y + j, PANEL, c);
        }
    }
    void steel(int x, int y, int w, int h) {
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) {
            int gx = (x + i) % G, gy = (y + j) % G, c = 0xFF596170;
            if (gx < 2 || gy < 2) c = 0xFF8D97A8; else if (gx >= G - 2 || gy >= G - 2) c = 0xFF2C313B;
            else if (gy < 11) c = ((x + i + y + j) / 7) % 2 == 0 ? 0xFFF2C21A : 0xFF17191E;
            put(x + i, y + j, STEEL, c);
        }
    }
    void crate(int x, int y) {
        for (int j = 0; j < G; j++) for (int i = 0; i < G; i++) {
            int c = 0xFFCB8C3C;
            if (j % 10 == 9) c = 0xFFA66D29;
            if (Math.abs(i - j) < 3 || Math.abs(i + j - 31) < 3) c = 0xFF96601F;
            if (i < 4 || j < 4 || i >= G - 4 || j >= G - 4) c = 0xFFE3AD62;
            if (i == 4 || j == 4 || i == G - 5 || j == G - 5) c = 0xFF7D4E18;
            put(x + i, y + j, CRATE, c);
        }
    }
    void ventH(int x, int y, int w) {
        final int h = 32;
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) {
            float q = 1 - Math.abs(j - 11) / 21f; int v = (int) (108 + q * 110), c = 0xFF000000 | (v - 8) << 16 | v << 8 | (v + 14);
            int f = (i + 48) % 96;
            if (f < 6) c = f < 2 ? 0xFFE7EEF6 : 0xFF6F7B8C;
            else if (i % 96 > 28 && i % 96 < 68 && j > 7 && j < 25) c = (j - 8) % 5 < 3 ? 0xFF262C36 : 0xFF8996A7;
            put(x + i, y + j, VENT, c);
        }
    }
    void ventV(int x, int y, int h) {
        final int w = 32;
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) {
            float q = 1 - Math.abs(i - 11) / 21f; int v = (int) (108 + q * 110), c = 0xFF000000 | (v - 8) << 16 | v << 8 | (v + 14);
            int f = (j + 48) % 96;
            if (f < 6) c = f < 2 ? 0xFFE7EEF6 : 0xFF6F7B8C;
            else if (j % 96 > 28 && j % 96 < 68 && i > 7 && i < 25) c = (i - 8) % 5 < 3 ? 0xFF262C36 : 0xFF8996A7;
            put(x + i, y + j, VENT, c);
        }
    }
    void computer(int x, int y, int kind) {
        final int w = 64, h = 64;
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) {
            int c = 0xFF2C323F;
            if (i < 3 || j < 3) c = 0xFF67728A; else if (i >= w - 3 || j >= h - 3) c = 0xFF12151C;
            else if (j >= 8 && j < 34 && i >= 8 && i < 56) {
                c = 0xFF0B3B2C;
                if (i < 10 || j < 10 || i >= 54 || j >= 32) c = 0xFF8A95A8;
                else if (kind % 2 == 0 ? (j - 13) % 5 < 2 && i < 14 + (hash(j, kind) & 31) : Math.abs(j - 21 - (int) (8 * Math.sin(i * .4))) < 2) c = 0xFF46F08A;
            } else if (j >= 40 && j < 56) {
                int a = i - 18, b = j - 48, e = i - 46; 
                if (a * a + b * b < 64 || e * e + b * b < 64) c = (a * a + b * b < 9 || e * e + b * b < 9) ? 0xFF2C323F : 0xFFD6DCE6;
                else if (i > 27 && i < 37 && (j - 41) % 5 < 3) c = (i / 3 + j / 5 + kind) % 3 == 0 ? 0xFFFF3030 : (i / 3 + j / 5) % 3 == 1 ? 0xFFF7D21E : 0xFF45E070;
            }
            put(x + i, y + j, COMPUTER, c);
        }
    }
    static int hash(int a, int b) { int h = a * 73856093 ^ b * 19349663; return h ^ h >>> 7; }
    private void props(Rng g, int x0, int x1, int top) {
        int x = x0 + G;
        while (x < x1 - G) {
            int k = g.i(10), step = G;
            if (k < 3) {
                int n = 1 + g.i(3); for (int i = 0; i < n; i++) if (x + i * G + G <= x1) crate(x + i * G, top - G);
                if (n > 1 && g.i(2) == 0) crate(x + g.i(n - 1) * G, top - 2 * G);
                step = n * G;
            } else if (k < 5 && x + 64 <= x1) { computer(x, top - 64, g.i(99)); step = 64; }
            else if (k == 5 && x + 2 * G <= x1) { panel(x, top - G); step = G; }
            x += step + G * (2 + g.i(3));
        }
    }
    public void generate(Rng g) {
        java.util.Arrays.fill(m, (byte) 0);
        final int floors = 5, gap = 160, cols = W / G;
        int base = H - 64;
        for (int i = 0; i * G < W; i++) { for (int d = 0; d < 2; d++) if (g.i(9) == 0 && d == 0) steel(i * G, base, G, G); else panel(i * G, base + d * G); }
        for (int f = 0; f < floors; f++) {
            int y = base - (f + 1) * gap, x = 1 + g.i(4);
            y -= y % G;
            while (x < cols - 2) {
                int len = 6 + g.i(10), kind = g.i(10);
                if (x + len > cols - 1) len = cols - 1 - x;
                if (len >= 3) {
                    if (kind < 2) ventH(x * G, y, len * G);
                    else for (int i = 0; i < len; i++) { if (kind == 2 && i % 4 < 1) steel((x + i) * G, y, G, G); else panel((x + i) * G, y); }
                    props(g, x * G, (x + len) * G, y);
                    int stepX = (g.i(2) == 0 ? x - 2 : x + len);
                    if (stepX > 1 && stepX < cols - 3 && g.i(2) == 0) { int sy = y + gap / 2 - (y + gap / 2) % G; if (clearArea(stepX * G, sy - 60, 2 * G, 92)) { panel(stepX * G, sy); panel((stepX + 1) * G, sy); } }
                }
                x += len + 3 + g.i(3);
            }
        }
        for (int n = 0; n < 7; n++) {
            int x = (3 + g.i(cols - 6)) * G, y = (2 + g.i(4)) * G, hh = (3 + g.i(3)) * G;
            y = base - G - (1 + g.i(4)) * gap; y -= y % G;
            if (clearArea(x, y, G, hh)) ventV(x, y, hh);
        }
        steel(0, 0, W, 16); steel(0, H - 16, W, 16); steel(0, 0, 16, H); steel(W - 16, 0, 16, H);
        java.util.Arrays.fill(dirty, true);
    }
}
