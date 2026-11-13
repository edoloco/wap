package com.wap.core;
/** Mapa destructible píxel a píxel, como en Liero, con bloques de escenario estilo plataformas. */
public final class Terrain {
    public static final int W = 1800, H = 1000, TILE = 128;
    /** 0 es aire; STEEL es indestructible; lo demás se destruye. */
    public static final byte PANEL = 1, STEEL = 2, CRATE = 3, VENT = 4, COMPUTER = 5, DEBRIS = 6, DECOR = 7, METAL = 8, STONE = 9, EARTH = 10, WOOD = 11;
    /** Objetos con resistencia propia: {x, y, ancho, alto, material}. */
    public final java.util.ArrayList<int[]> props = new java.util.ArrayList<>();
    public static boolean isProp(int k) { return k == CRATE || k == COMPUTER || k == METAL || k == STONE; }
    /** El radio de una explosión no los recorta píxel a píxel. */
    static boolean hard(int k) { return k == STEEL || k == WOOD || isProp(k); }
    /** Madera: no la recortan las explosiones; se rompe por trozos irregulares (ver World.hitWood). */
    public static final int FRAG = 20, FW = W / FRAG + 1, FH = H / FRAG + 1;
    /** Trozo de madera al que pertenece un píxel: el punto semilla más cercano de una grilla con desorden. */
    public static int frag(int x, int y) {
        int cx = x / FRAG, cy = y / FRAG, best = 0, bd = Integer.MAX_VALUE;
        for (int j = cy - 1; j <= cy + 1; j++) for (int i = cx - 1; i <= cx + 1; i++) {
            if (i < 0 || j < 0 || i >= FW || j >= FH) continue;
            int sx = i * FRAG + (hash(i, j) & 15) + 2, sy = j * FRAG + (hash(j + 91, i + 7) & 15) + 2, d = (x - sx) * (x - sx) + (y - sy) * (y - sy);
            if (d < bd) { bd = d; best = j * FW + i; }
        }
        return best;
    }
    public static int fragX(int f) { int i = f % FW, j = f / FW; return i * FRAG + (hash(i, j) & 15) + 2; }
    public static int fragY(int f) { int i = f % FW, j = f / FW; return j * FRAG + (hash(j + 91, i + 7) & 15) + 2; }
    /** Tamaño bajo el cual un trozo deja de chocar: grosor en píxeles y área de una pieza suelta. */
    static final int THIN = 6, SMALL = 420;
    static final int FRONT = 0xFF8214FF, TOP = 0xFF4A00A8, DARK = 0xFF2B1A4D, DARK_TOP = 0xFF1B1036;
    static final int G = 32, FILL = 0xFF8214FF;
    public final int[] tex = new int[W * H];
    public static final int TW = (W + TILE - 1) / TILE, TH = (H + TILE - 1) / TILE;
    public final byte[] m = new byte[W * H];
    public final boolean[] dirty = new boolean[TW * TH];
    /**
     * Capas del editor que no chocan con nada (solo se ven): FONDO, detrás de todo (se dibuja más claro), y SOBRE,
     * encima de todo (más oscuro). 0 = nada. Se crean al usarlas; null si el nivel no tiene.
     */
    public int[] back, front;
    /** Materiales del editor que no van en m, sino en las capas. */
    public static final int BACKDROP = 100, OVERLAY = 101;
    int[] layer(int mat) { if (mat == BACKDROP) { if (back == null) back = new int[W * H]; return back; } if (front == null) front = new int[W * H]; return front; }

    public boolean solid(int x, int y) { if (x < 0 || y < 0 || x >= W || y >= H) return true; byte k = m[y * W + x]; return k != 0 && k != DEBRIS && k != DECOR; }
    public int at(int x, int y) { return x < 0 || y < 0 || x >= W || y >= H ? 2 : m[y * W + x]; }

    private void disc(int cx, int cy, int rx, int ry, int from, int to) {
        int x0 = Math.max(0, cx - rx), x1 = Math.min(W - 1, cx + rx), y0 = Math.max(0, cy - ry), y1 = Math.min(H - 1, cy + ry);
        long a = (long) rx * rx * ry * ry; if (a == 0) return;
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
            long dx = x - cx, dy = y - cy;
            if (dx * dx * ry * ry + dy * dy * rx * rx > a) continue;
            int i = y * W + x;
            if (from < 0) { if (m[i] != 0 && !hard(m[i])) m[i] = 0; }
            else if (m[i] == from) { m[i] = (byte) to; tex[i] = FILL; }
        }
        mark(x0, y0, x1, y1);
        if (from < 0) cleanup(x0, y0, x1, y1);
    }
    private int[] sat = new int[0], sat2 = new int[0]; private final int[] stamp = new int[W * H], stack = new int[SMALL * 5], found = new int[SMALL + 1]; private int cur;
    private boolean col(int i) { byte k = m[i]; return k != 0 && k != DEBRIS && k != DECOR; }
    /** Tras una destrucción, marca astillas y pedazos sueltos: se siguen viendo, pero ya no chocan con nada. */
    private void cleanup(int x0, int y0, int x1, int y1) {
        final int M = 14, E = 8;
        int ax = Math.max(0, x0 - M), ay = Math.max(0, y0 - M), bx = Math.min(W - 1, x1 + M), by = Math.min(H - 1, y1 + M), w = bx - ax + 1, h = by - ay + 1, st = w + 1, n = st * (h + 1);
        if (sat.length < n) { sat = new int[n]; sat2 = new int[n]; }
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++)
            sat[(j + 1) * st + i + 1] = (col((ay + j) * W + ax + i) ? 1 : 0) + sat[j * st + i + 1] + sat[(j + 1) * st + i] - sat[j * st + i];
        int K = THIN;
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) {
            boolean full = i + K <= w && j + K <= h && sat[(j + K) * st + i + K] - sat[j * st + i + K] - sat[(j + K) * st + i] + sat[j * st + i] == K * K;
            sat2[(j + 1) * st + i + 1] = (full ? 1 : 0) + sat2[j * st + i + 1] + sat2[(j + 1) * st + i] - sat2[j * st + i];
        }
        int ex0 = Math.max(ax, x0 - E), ey0 = Math.max(ay, y0 - E), ex1 = Math.min(bx, x1 + E), ey1 = Math.min(by, y1 + E);
        for (int y = ey0; y <= ey1; y++) for (int x = ex0; x <= ex1; x++) {
            int q = y * W + x; if (!col(q) || hard(m[q])) continue;
            int i1 = x - ax + 1, j1 = y - ay + 1, i0 = Math.max(0, i1 - K), j0 = Math.max(0, j1 - K);
            if (sat2[j1 * st + i1] - sat2[j0 * st + i1] - sat2[j1 * st + i0] + sat2[j0 * st + i0] == 0) m[q] = DEBRIS;
        }
        for (int y = ey0; y <= ey1; y++) for (int x = ex0; x <= ex1; x++) if (m[y * W + x] == DECOR) {
            boolean held = false; for (int k = 1; k <= 16 && !held; k++) held = y + k < H && col((y + k) * W + x);
            if (!held) m[y * W + x] = 0;
        }
        cur++;
        for (int y = ey0; y <= ey1; y++) for (int x = ex0; x <= ex1; x++) {
            int q = y * W + x; if (!col(q) || hard(m[q]) || stamp[q] == cur) continue;
            int sp = 0, c = 0; boolean big = false; stack[sp++] = q; stamp[q] = cur;
            while (sp > 0 && !big) {
                int p = stack[--sp];
                if (c >= SMALL) { big = true; break; }
                found[c++] = p;
                for (int d = 0; d < 4; d++) {
                    int o = d == 0 ? p + 1 : d == 1 ? p - 1 : d == 2 ? p + W : p - W;
                    if (o < 0 || o >= W * H || !col(o)) continue;
                    if (hard(m[o])) { big = true; break; }
                    if (stamp[o] != cur) { stamp[o] = cur; stack[sp++] = o; }
                }
            }
            if (!big) for (int i = 0; i < c; i++) m[found[i]] = DEBRIS;
        }
        mark(ex0, ey0, ex1, ey1);
    }
    void markArea(int x0, int y0, int x1, int y1) { mark(x0, y0, x1, y1); }
    private void mark(int x0, int y0, int x1, int y1) {
        int a = Math.max(0, (x0 - 3) / TILE), b = Math.min(TW - 1, (x1 + 3) / TILE);
        int c = Math.max(0, (y0 - 3) / TILE), d = Math.min(TH - 1, (y1 + 3) / TILE);
        for (int ty = c; ty <= d; ty++) for (int tx = a; tx <= b; tx++) dirty[ty * TW + tx] = true;
    }
    public void carve(int cx, int cy, int r) { disc(cx, cy, r, r, -1, 0); }
    public void carveE(int cx, int cy, int rx, int ry) { disc(cx, cy, rx, ry, -1, 0); }
    /** Borra un objeto roto del mapa. */
    public void erase(int x, int y, int w, int h) {
        for (int j = y; j < y + h; j++) for (int i = x; i < x + w; i++) if (at(i, j) != STEEL) { if (i >= 0 && j >= 0 && i < W && j < H) m[j * W + i] = 0; }
        mark(x, y, x + w, y + h);
    }
    /** Quita un objeto del mapa para moverlo (solo sus propias celdas). */
    void lift(int x, int y, int w, int h, int kind) {
        for (int j = y; j < y + h; j++) for (int i = x; i < x + w; i++) if (at(i, j) == kind) m[j * W + i] = 0;
        mark(x, y, x + w, y + h);
    }
    /** Pone un objeto en el mapa en su nueva posición. */
    void stamp(int x, int y, int w, int h, int kind) {
        for (int j = y; j < y + h; j++) for (int i = x; i < x + w; i++) if (i >= 0 && j >= 0 && i < W && j < H) { int k = m[j * W + i]; if (k == 0 || k == DEBRIS || k == DECOR) m[j * W + i] = (byte) kind; }
        mark(x, y, x + w, y + h);
    }
    public void addDirt(int cx, int cy, int r) { disc(cx, cy, r, r, 0, 1); }

    private void put(int x, int y, int mat, int c) { if (x >= 0 && y >= 0 && x < W && y < H) { m[y * W + x] = (byte) mat; tex[y * W + x] = c; } }
    private boolean clearArea(int x, int y, int w, int h) {
        for (int j = y; j < y + h; j += 4) for (int i = x; i < x + w; i += 4) if (at(i, j) != 0) return false;
        return true;
    }
    void panel(int x, int y) {
        for (int j = 0; j < G; j++) for (int i = 0; i < G; i++) put(x + i, y + j, PANEL, (x + i) % (G * 2) == 0 ? 0xFF141414 : FRONT);
    }
    void steel(int x, int y, int w, int h) {
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) put(x + i, y + j, STEEL, (x + i) % (G * 2) == 0 ? 0xFF141414 : DARK);
    }
    void crate(int x, int y, int mat) {
        for (int j = 0; j < G; j++) for (int i = 0; i < G; i++) {
            int c;
            if (mat == METAL) {
                c = 0xFF8D99A8;
                if (i < 3 || j < 3 || i >= G - 3 || j >= G - 3) c = 0xFFB4BFCC;
                if ((i - 7) * (i - 7) + (j - 7) * (j - 7) < 5 || (i - 24) * (i - 24) + (j - 7) * (j - 7) < 5 || (i - 7) * (i - 7) + (j - 24) * (j - 24) < 5 || (i - 24) * (i - 24) + (j - 24) * (j - 24) < 5) c = 0xFF5F6B7A;
            } else if (mat == STONE) {
                c = 0xFF9C9488 + ((hash(i / 3 + x, j / 3 + y) & 15) * 0x010101);
                if (j == 15 || (j < 15 ? i == 11 : i == 22)) c = 0xFF5B554D;
            } else {
                c = 0xFFCB8C3C;
                if (j % 10 == 9) c = 0xFFA66D29;
                if (Math.abs(i - j) < 3 || Math.abs(i + j - 31) < 3) c = 0xFF96601F;
                if (i < 4 || j < 4 || i >= G - 4 || j >= G - 4) c = 0xFFE3AD62;
            }
            put(x + i, y + j, mat, c);
        }
        props.add(new int[]{x, y, G, G, mat});
    }
    void ventH(int x, int y, int w) {
        final int h = 32;
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) {
            int c = 0xFFB9C2CE;
            int f = (i + 48) % 96;
            if (f < 6) c = 0xFF141414;
            else if (i % 96 > 28 && i % 96 < 68 && j > 7 && j < 25) c = (j - 8) % 5 < 3 ? 0xFF262C36 : 0xFF8996A7;
            put(x + i, y + j, VENT, c);
        }
    }
    void ventV(int x, int y, int h) {
        final int w = 32;
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) {
            int c = 0xFFB9C2CE;
            int f = (j + 48) % 96;
            if (f < 6) c = 0xFF141414;
            else if (j % 96 > 28 && j % 96 < 68 && i > 7 && i < 25) c = (i - 8) % 5 < 3 ? 0xFF262C36 : 0xFF8996A7;
            put(x + i, y + j, VENT, c);
        }
    }
    void computer(int x, int y, int kind) {
        final int w = 64, h = 64; props.add(new int[]{x, y, w, h, COMPUTER});
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) {
            int c = 0xFF2C323F;
            if (i < 3 || j < 3) c = 0xFF67728A; else if (i >= w - 3 || j >= h - 3) c = 0xFF12151C;
            else if (j >= 8 && j < 34 && i >= 8 && i < 56) {
                c = 0xFF0B3B2C;
                if (i < 10 || j < 10 || i >= 54 || j >= 32) c = 0xFF8A95A8;
                else if (kind % 2 == 0 ? (j - 13) % 5 < 2 && i < 14 + (hash(j, kind) & 31) : Math.abs(j - 21 - (int) (8 * StrictMath.sin(i * .4))) < 2) c = 0xFF46F08A;
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
            if (k < 4) {
                int n = 1 + g.i(3), mat = g.i(3) == 0 ? METAL : CRATE;
                for (int i = 0; i < n; i++) if (x + i * G + G <= x1) crate(x + i * G, top - G, mat);
                if (n > 1 && g.i(2) == 0) crate(x + g.i(n - 1) * G, top - 2 * G, mat);
                step = n * G;
            } else if (k < 6 && x + 64 <= x1) { computer(x, top - 64, g.i(99)); step = 64; }
            else if (k < 8) { crate(x, top - G, STONE); step = G; }
            x += step + G * (1 + g.i(3));
        }
    }
    private boolean block(int x, int y) { int k = at(x, y); return x >= 16 && x < W - 16 && (k == PANEL || k == STEEL); }
    /** Cara superior en perspectiva: el borde de colisión queda al medio, para que el robot pise sobre ella. */
    private void tops() {
        final int T = 10;
        for (int y = 17; y < H - 16; y++) {
            int x = 16;
            while (x < W - 16) {
                if (!block(x, y) || block(x, y - 1)) { x++; continue; }
                int a = x; while (x < W - 16 && block(x, y) && !block(x, y - 1)) x++;
                int e = x, mid = (a + e) / 2; boolean dark = at(a, y) == STEEL;
                for (int d = 0; d < T * 2; d++) {
                    int row = y + T - 1 - d, lo = a + d / 2, hi = e - 1 - d / 4;
                    for (int q = lo; q <= hi; q++) {
                        int c = d == 0 ? 0xFF141414 : dark ? DARK_TOP : TOP;
                        if (row >= y) { if (block(q, row)) tex[row * W + q] = c; }
                        else if (at(q, row) == 0) put(q, row, DECOR, c);
                    }
                    for (int k = (a / (G * 2) + 1) * G * 2; k < e; k += G * 2) {
                        int q = k + (int) (-(k - mid) / (float) Math.max(1, e - a) * d * .9f + d * .08f);
                        if (q < lo || q > hi) continue;
                        if (row >= y ? block(q, row) : at(q, row) == DECOR) tex[row * W + q] = 0xFF141414;
                    }
                }
            }
        }
    }
    public void generate(Rng g) {
        java.util.Arrays.fill(m, (byte) 0); props.clear();
        final int gap = g.i(2) == 0 ? 160 : 192, cols = W / G, minLen = 4 + g.i(4), spread = 7 + g.i(9), gapMin = 2 + g.i(3);
        int base = H - 64;
        for (int i = 0; i * G < W; i++) { for (int d = 0; d < 2; d++) if (g.i(9) == 0 && d == 0) steel(i * G, base, G, G); else panel(i * G, base + d * G); }
        for (int f = 0; base - (f + 1) * gap > 100; f++) {
            int y = base - (f + 1) * gap, x = 1 + g.i(6);
            y -= y % G;
            while (x < cols - 2) {
                int len = minLen + g.i(spread), kind = g.i(10);
                if (x + len > cols - 1) len = cols - 1 - x;
                if (len >= 3) {
                    if (kind < 2) ventH(x * G, y, len * G);
                    else for (int i = 0; i < len; i++) { if (kind == 2 && i % 4 < 1) steel((x + i) * G, y, G, G); else panel((x + i) * G, y); }
                    props(g, x * G, (x + len) * G, y);
                    int stepX = (g.i(2) == 0 ? x - 2 : x + len);
                    if (stepX > 1 && stepX < cols - 3 && g.i(2) == 0) { int sy = y + gap / 2 - (y + gap / 2) % G; if (clearArea(stepX * G, sy - 60, 2 * G, 92)) { panel(stepX * G, sy); panel((stepX + 1) * G, sy); } }
                }
                x += len + gapMin + g.i(3);
            }
        }
        for (int n = 0; n < 7; n++) {
            int x = (3 + g.i(cols - 6)) * G, y = (2 + g.i(4)) * G, hh = (3 + g.i(3)) * G;
            y = base - G - (1 + g.i(4)) * gap; y -= y % G;
            if (clearArea(x, y, G, hh)) ventV(x, y, hh);
        }
        for (int n = 0; n < 2 + g.i(2); n++) { // pasadizos de dos paredes para rebotar
            int w = 3 + g.i(2), hh = (6 + g.i(4)) * G, x = (3 + g.i(cols - 9)) * G, y = base - hh;
            for (int j = y; j < base; j++) for (int i = x + G; i < x + (w + 1) * G; i++) m[j * W + i] = 0;
            for (int j = y; j < base; j += G) { panel(x, j); panel(x + (w + 1) * G, j); }
            final int fx = x, fw = (w + 2) * G, fy = y;
            props.removeIf(q -> q[0] + q[2] > fx && q[0] < fx + fw && q[1] + q[3] > fy && q[1] < base);
        }
        frame();
        java.util.Arrays.fill(dirty, true);
    }
    /** Marco indestructible alrededor del mapa. */
    void frame() { steel(0, 0, W, 16); steel(0, H - 16, W, 16); steel(0, 0, 16, H); steel(W - 16, 0, 16, H); }
    /** Piso de bloques, como el de los niveles aleatorios. */
    void floor() { for (int i = 0; i * G < W; i++) for (int d = 0; d < 2; d++) panel(i * G, H - 64 + d * G); frame(); }

    // ---------- Herramientas del editor de niveles
    public static final int EDGE = 16, GRID = G;
    static boolean inside(int x, int y) { return x >= EDGE && y >= EDGE && x < W - EDGE && y < H - EDGE; }
    /** Color de tierra con un poco de textura (vetas en bloques de 4 px), para que no se vea plana. */
    static int earthTex(int c, int x, int y) {
        int k = vein(x, y);
        int r = Math.max(0, Math.min(255, (c >> 16 & 255) + k)), g = Math.max(0, Math.min(255, (c >> 8 & 255) + k)), b = Math.max(0, Math.min(255, (c & 255) + k));
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
    /** Pinta tierra destructible del color elegido; no tapa bloques ni objetos. */
    public void earth(int cx, int cy, int r, int color) { brush(cx, cy, r, EARTH, color); }
    /** Pincel redondo de tierra (con vetas) o madera (color sólido). */
    public void brush(int cx, int cy, int r, int mat, int color) {
        for (int y = cy - r; y <= cy + r; y++) for (int x = cx - r; x <= cx + r; x++)
            if ((x - cx) * (x - cx) + (y - cy) * (y - cy) <= r * r) paint(x, y, mat, color);
        mark(cx - r, cy - r, cx + r, cy + r);
    }
    /** Pinta un píxel: tierra (con vetas), madera, acero o una capa (fondo o sobre); estos tres últimos en color plano. No tapa bloques ni objetos. */
    public void paint(int x, int y, int mat, int color) { put(x, y, mat, color, false); }
    /** flat = color parejo, sin vetas (pixel art). */
    void put(int x, int y, int mat, int color, boolean flat) {
        if (!inside(x, y)) return;
        int i = y * W + x;
        if (mat == BACKDROP || mat == OVERLAY) { layer(mat)[i] = color | 0xFF000000; return; }
        int k = m[i]; if (k != 0 && k != DEBRIS && k != DECOR && k != EARTH && k != WOOD && !(k == STEEL && mat == STEEL)) return;
        m[i] = (byte) mat; tex[i] = flat || mat == WOOD || mat == STEEL ? color | 0xFF000000 : earthTex(color, x, y);
    }
    /** Vetas de la tierra (pruebas). */
    public static int earthTexT(int c, int x, int y) { return earthTex(c, x, y); }
    /** Solo el marco (pruebas). */
    public void frame2() { frame(); }
    /** Pixel art: llena la celda de la grilla (de lado cell) que contiene el punto, con color parejo. erase = la vacía. */
    public void cell(int x, int y, int cell, int mat, int color, boolean erase) {
        int x0 = Math.floorDiv(x, cell) * cell, y0 = Math.floorDiv(y, cell) * cell;
        for (int j = y0; j < y0 + cell; j++) for (int i = x0; i < x0 + cell; i++) {
            if (!inside(i, j)) continue;
            if (erase) clear(i, j, mat); else put(i, j, mat, color, true);
        }
        mark(x0, y0, x0 + cell, y0 + cell);
    }
    /** Borra un píxel de la capa elegida (fondo, sobre) o del mapa (si mat es otro); los objetos los quita el mundo. */
    void clear(int x, int y, int mat) {
        int i = y * W + x;
        if (mat == BACKDROP) { if (back != null) back[i] = 0; } else if (mat == OVERLAY) { if (front != null) front[i] = 0; } else if (!isProp(m[i])) m[i] = 0;
    }
    /** Polígono (herramienta de puntos): relleno par-impar o contorno de grosor r; closed = une el último con el primero. */
    public void polygon(int[] xy, boolean fill, boolean closed, int r, int mat, int color) {
        int n = xy.length / 2; if (n < 2) return;
        if (fill && n >= 3) {
            int y0 = Integer.MAX_VALUE, y1 = Integer.MIN_VALUE, x0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE;
            for (int k = 0; k < n; k++) { y0 = Math.min(y0, xy[2 * k + 1]); y1 = Math.max(y1, xy[2 * k + 1]); x0 = Math.min(x0, xy[2 * k]); x1 = Math.max(x1, xy[2 * k]); }
            float[] cut = new float[n];
            for (int y = Math.max(EDGE, y0); y <= Math.min(H - EDGE - 1, y1); y++) {
                float sy = y + .5f; int c = 0;
                for (int k = 0; k < n; k++) { float ax = xy[2 * k], ay = xy[2 * k + 1], bx = xy[(2 * k + 2) % (2 * n)], by = xy[(2 * k + 3) % (2 * n)];
                    if ((ay <= sy) != (by <= sy)) cut[c++] = ax + (sy - ay) / (by - ay) * (bx - ax); }
                java.util.Arrays.sort(cut, 0, c);
                for (int k = 0; k + 1 < c; k += 2) for (int x = Math.round(cut[k]); x < Math.round(cut[k + 1]); x++) paint(x, y, mat, color);
            }
            mark(x0, y0, x1, y1);
        } else {
            for (int k = 0; k + 1 < n; k++) line(xy[2 * k], xy[2 * k + 1], xy[2 * k + 2], xy[2 * k + 3], r, mat, color);
            if (closed && n >= 3) line(xy[2 * n - 2], xy[2 * n - 1], xy[0], xy[1], r, mat, color);
        }
    }
    // ---------- Formas del editor (relleno o contorno de grosor r)
    public void line(int x0, int y0, int x1, int y1, int r, int mat, int color) {
        int n = Math.max(1, (int) (Math.hypot(x1 - x0, y1 - y0) / Math.max(1, r * .5f)));
        for (int k = 0; k <= n; k++) brush(x0 + (x1 - x0) * k / n, y0 + (y1 - y0) * k / n, r, mat, color);
    }
    public void rect(int x0, int y0, int x1, int y1, boolean fill, int r, int mat, int color) {
        int a = Math.min(x0, x1), b = Math.max(x0, x1), c = Math.min(y0, y1), d = Math.max(y0, y1);
        if (fill) { for (int y = c; y <= d; y++) for (int x = a; x <= b; x++) paint(x, y, mat, color); mark(a, c, b, d); }
        else { line(a, c, b, c, r, mat, color); line(b, c, b, d, r, mat, color); line(b, d, a, d, r, mat, color); line(a, d, a, c, r, mat, color); }
    }
    /** Círculo (o elipse) inscrito en el rectángulo arrastrado. */
    public void oval(int x0, int y0, int x1, int y1, boolean fill, int r, int mat, int color) {
        float cx = (x0 + x1) / 2f, cy = (y0 + y1) / 2f, rx = Math.max(1, Math.abs(x1 - x0) / 2f), ry = Math.max(1, Math.abs(y1 - y0) / 2f);
        if (fill) {
            for (int y = (int) (cy - ry); y <= cy + ry; y++) for (int x = (int) (cx - rx); x <= cx + rx; x++) { float u = (x - cx) / rx, v = (y - cy) / ry; if (u * u + v * v <= 1) paint(x, y, mat, color); }
            mark((int) (cx - rx), (int) (cy - ry), (int) (cx + rx), (int) (cy + ry));
        } else {
            int n = (int) Math.max(12, (rx + ry) * 3.2f / Math.max(1, r * .5f));
            for (int k = 0; k < n; k++) { double a = k * Math.PI * 2 / n; brush((int) Math.round(cx + Math.cos(a) * rx), (int) Math.round(cy + Math.sin(a) * ry), r, mat, color); }
        }
    }
    /** Triángulo con la punta arriba al centro, inscrito en el rectángulo arrastrado. */
    public void tri(int x0, int y0, int x1, int y1, boolean fill, int r, int mat, int color) {
        int a = Math.min(x0, x1), b = Math.max(x0, x1), c = Math.min(y0, y1), d = Math.max(y0, y1), mx = (a + b) / 2;
        if (fill) {
            for (int y = c; y <= d; y++) { float k = d == c ? 1 : (y - c) / (float) (d - c); int hw = Math.round((b - a) / 2f * k); for (int x = mx - hw; x <= mx + hw; x++) paint(x, y, mat, color); }
            mark(a, c, b, d);
        } else { line(mx, c, b, d, r, mat, color); line(b, d, a, d, r, mat, color); line(a, d, mx, c, r, mat, color); }
    }
    /**
     * Balde, como el de Paint: toma lo que se ve en el punto tocado (lo de encima, si no el mapa, si no el fondo, si no
     * el aire) y la zona unida del MISMO material y MISMO color pasa al material y color elegidos. Una figura de otro
     * color dibujada encima hace de borde. Tocar el aire de un nivel vacío pinta todo el lienzo.
     * Devuelve false si se toca un bloque o un objeto (no se pintan con el balde).
     */
    public boolean bucket(int x, int y, int mat, int color) {
        if (!inside(x, y)) return false;
        final int AIR = 0, FR = 1, MAP = 2, BK = 3;
        int i0 = y * W + x, src = kindAt(i0), k0 = m[i0];
        if (src == MAP && k0 != EARTH && k0 != WOOD && k0 != STEEL) return false; // bloques y objetos no se pintan con el balde
        final int LIMIT = W * H; int[] q = new int[4096]; int n = 0, head = 0; java.util.BitSet seen = new java.util.BitSet(W * H);
        q[n++] = i0; seen.set(i0); int x0 = x, x1 = x, y0 = y, y1 = y;
        while (head < n) {
            int p = q[head++], px = p % W, py = p / W;
            x0 = Math.min(x0, px); x1 = Math.max(x1, px); y0 = Math.min(y0, py); y1 = Math.max(y1, py);
            for (int d = 0; d < 4; d++) {
                int ox = px + (d == 0 ? 1 : d == 1 ? -1 : 0), oy = py + (d == 2 ? 1 : d == 3 ? -1 : 0);
                if (!inside(ox, oy)) continue;
                int o = oy * W + ox; if (seen.get(o)) continue;
                boolean same = src == FR ? front[o] == front[i0] : src == BK ? back[o] == back[i0] && kindAt(o) == BK : src == MAP ? m[o] == k0 && (front == null || front[o] == 0) && sameColor(i0, o) : kindAt(o) == AIR;
                if (!same) continue;
                seen.set(o); if (n >= LIMIT) return false;
                if (n == q.length) q = java.util.Arrays.copyOf(q, q.length * 2);
                q[n++] = o;
            }
        }
        boolean toLayer = mat == BACKDROP || mat == OVERLAY;
        for (int j = 0; j < n; j++) { int p = q[j], px = p % W, py = p / W;
            if (src == FR && mat != OVERLAY) front[p] = 0; // sale de la capa donde estaba
            if (src == BK && mat != BACKDROP) back[p] = 0;
            if (src == MAP && toLayer) m[p] = 0;
            if (toLayer) layer(mat)[p] = color | 0xFF000000;
            else { m[p] = (byte) mat; tex[p] = mat == WOOD || mat == STEEL ? color | 0xFF000000 : earthTex(color, px, py); }
        }
        mark(x0, y0, x1, y1); return true;
    }
    /**
     * Si dos píxeles del mapa son del mismo color elegido. La tierra lleva vetas (el color sube o baja según el lugar),
     * así que se compara el color de base: el que queda al quitarle la veta de ese lugar, o el color tal cual (pixel art,
     * madera y acero son parejos).
     */
    boolean sameColor(int a, int b) {
        int ta = tex[a], tb = tex[b];
        if (ta == tb) return true;
        int ka = vein(a % W, a / W), kb = vein(b % W, b / W);
        return near(ta, 0, tb, kb) || near(ta, ka, tb, 0) || near(ta, ka, tb, kb);
    }
    static int vein(int x, int y) { int h = hash(x >> 2, y >> 2) & 7; return h < 2 ? -18 : h > 5 ? 12 : 0; }
    static boolean near(int a, int ka, int b, int kb) {
        for (int s = 0; s <= 16; s += 8) if (Math.abs(((a >> s & 255) - ka) - ((b >> s & 255) - kb)) > 6) return false;
        return true;
    }
    /** Lo que se ve en un píxel: 1 lo de encima, 2 el mapa (tierra, madera, acero), 3 el fondo, 0 aire. Otros materiales del mapa (bloques, objetos) cuentan como mapa. */
    int kindAt(int i) {
        if (front != null && front[i] != 0) return 1;
        int k = m[i]; if (k != 0 && k != DEBRIS && k != DECOR) return 2;
        if (back != null && back[i] != 0) return 3;
        return 0;
    }
    /** Borra todo lo que no sea el marco ni un objeto (los objetos los quita el mundo). */
    public void wipe(int cx, int cy, int r) { wipe(cx, cy, r, 0); }
    /** Goma de una capa: mat = BACK o FRONT borra solo esa capa; otro valor borra el mapa. */
    public void wipe(int cx, int cy, int r, int mat) {
        for (int y = cy - r; y <= cy + r; y++) for (int x = cx - r; x <= cx + r; x++) {
            if (!inside(x, y) || (x - cx) * (x - cx) + (y - cy) * (y - cy) > r * r) continue;
            clear(x, y, mat);
        }
        mark(cx - r, cy - r, cx + r, cy + r);
    }
    /** Bloque de 32 px alineado a la grilla: PANEL (morado, destructible) o STEEL (indestructible). */
    public void block(int x, int y, int mat) {
        x -= Math.floorMod(x, G); y -= Math.floorMod(y, G);
        if (!inside(x, y) || !inside(x + G - 1, y + G - 1)) return;
        for (int j = y; j < y + G; j += 4) for (int i = x; i < x + G; i += 4) if (isProp(at(i, j))) return;
        if (mat == STEEL) steel(x, y, G, G); else panel(x, y);
        mark(x, y, x + G, y + G);
    }
    /** Ducto de ventilación horizontal de tres bloques. */
    public void vent(int x, int y) {
        x -= Math.floorMod(x, G); y -= Math.floorMod(y, G);
        if (!inside(x, y) || !inside(x + 3 * G - 1, y + G - 1)) return;
        for (int j = y; j < y + G; j += 4) for (int i = x; i < x + 3 * G; i += 4) if (isProp(at(i, j))) return;
        ventH(x, y, 3 * G); mark(x, y, x + 3 * G, y + G);
    }
    /** Objeto con resistencia (caja, metal, piedra o computador) si el lugar está libre. Devuelve sus datos o null. */
    public int[] object(int x, int y, int mat) {
        int s = mat == COMPUTER ? 64 : G; x -= Math.floorMod(x, G); y -= Math.floorMod(y, G);
        if (!inside(x, y) || !inside(x + s - 1, y + s - 1)) return null;
        for (int j = y; j < y + s; j += 2) for (int i = x; i < x + s; i += 2) if (solid(i, j)) return null;
        int n = props.size();
        if (mat == COMPUTER) computer(x, y, hash(x, y) & 63); else crate(x, y, mat);
        mark(x, y, x + s, y + s);
        return props.size() > n ? props.get(props.size() - 1) : null;
    }
}
