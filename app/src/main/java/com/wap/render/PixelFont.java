package com.wap.render;
/** Fuente de pixel art de 5x7 dibujada con rectángulos, para que la interfaz tenga el mismo píxel que el juego. */
public final class PixelFont {
    private static final java.util.HashMap<Character, Long> G = new java.util.HashMap<>();
    private static void put(char c, long v) { G.put(c, v); }
    static {
        put('A', 18842895918L);
        put('B', 16694887983L);
        put('C', 15603893806L);
        put('D', 16694953519L);
        put('E', 33321092159L);
        put('F', 1108837439L);
        put('G', 32801457710L);
        put('H', 18842895921L);
        put('I', 15170932878L);
        put('J', 6753100060L);
        put('K', 18560947505L);
        put('L', 33320633377L);
        put('M', 18842572657L);
        put('N', 18842703473L);
        put('O', 15621211694L);
        put('P', 1108854319L);
        put('Q', 23946905134L);
        put('R', 18561353263L);
        put('S', 16660235326L);
        put('T', 4433514655L);
        put('U', 15621211697L);
        put('V', 4648912433L);
        put('W', 11464787505L);
        put('X', 18834663985L);
        put('Y', 4433521201L);
        put('Z', 33321787935L);
        put('0', 15623448110L);
        put('1', 15170932932L);
        put('2', 33357578798L);
        put('3', 15619854623L);
        put('4', 8891181448L);
        put('5', 15620127807L);
        put('6', 15621129292L);
        put('7', 2216829471L);
        put('8', 15621113390L);
        put('9', 6728664622L);
        put('.', 6643777536L);
        put(',', 2287992832L);
        put(':', 207624384L);
        put('-', 1015808L);
        put('!', 4299296900L);
        put('¡', 4433514500L);
        put('?', 4299440686L);
        put('¿', 15603929092L);
        put('·', 131072L);
        put('(', 8726317192L);
        put(')', 2290360450L);
        put('%', 26610901603L);
        put('/', 1109533200L);
        put('+', 139432064L);
        put('\'', 132L);
        put('x', 18593694720L);
    }
    private PixelFont() {}
    /** Escala entera según el tamaño pedido: 8 unidades = 1 píxel por punto. */
    public static int scale(float size) { return Math.max(1, Math.round(size / 8f)); }
    public static float width(String s, float size) { int k = scale(size); return s.length() * 6 * k - k; }
    private static char base(char c) {
        switch (c) { case 'Á': return 'A'; case 'É': return 'E'; case 'Í': return 'I'; case 'Ó': return 'O'; case 'Ú': case 'Ü': return 'U'; case 'Ñ': return 'N'; default: return c; }
    }
    /** y es la línea base (borde inferior de las letras). align: -1 izquierda, 0 centro, 1 derecha. shadow 0 = sin sombra. */
    public static void draw(Gfx g, String s, float x, float y, float size, int color, int align, int shadow) {
        if (s == null || s.isEmpty()) return;
        String t = s.toUpperCase(); int k = scale(size); float w = width(t, size);
        int x0 = Math.round(align == 0 ? x - w / 2 : align > 0 ? x - w : x), y0 = Math.round(y) - 7 * k;
        if (shadow != 0) run(g, t, x0 + k, y0 + k, k, shadow);
        run(g, t, x0, y0, k, color);
    }
    private static void run(Gfx g, String t, int x0, int y0, int k, int color) {
        for (int i = 0; i < t.length(); i++) {
            char raw = t.charAt(i), c = base(raw); Long v = G.get(c); int cx = x0 + i * 6 * k;
            if (raw != c) {
                if (raw == 'Ñ') { g.rect(cx + k, y0 - 2 * k, 2 * k, k, color); g.rect(cx + 3 * k, y0 - 3 * k, k, k, color); g.rect(cx, y0 - 3 * k, k, k, color); g.rect(cx + 4 * k, y0 - 2 * k, k, k, color); }
                else if (raw == 'Ü') { g.rect(cx + k, y0 - 2 * k, k, k, color); g.rect(cx + 3 * k, y0 - 2 * k, k, k, color); }
                else { g.rect(cx + 2 * k, y0 - 2 * k, k, k, color); g.rect(cx + 3 * k, y0 - 3 * k, k, k, color); }
            }
            if (v == null) continue;
            for (int r = 0; r < 7; r++) {
                int c0 = -1;
                for (int q = 0; q <= 5; q++) {
                    boolean on = q < 5 && (v >> (r * 5 + q) & 1) != 0;
                    if (on && c0 < 0) c0 = q;
                    if (!on && c0 >= 0) { g.rect(cx + c0 * k, y0 + r * k, (q - c0) * k, k, color); c0 = -1; }
                }
            }
        }
    }
}
