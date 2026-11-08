package com.wap.render;
import com.wap.core.*;
import java.util.HashMap;
/**
 * Editor de niveles. Con el dedo: un dedo dibuja, dos dedos mueven la vista y el pellizco hace zoom.
 * Con el mando: la cruceta mueve un cursor, A aplica (en las formas: apretar marca el inicio y soltar las dibuja),
 * X cambia de herramienta y B sale.
 * Herramientas: pincel, línea, cuadrado, círculo, triángulo, balde, borrar (todas con tierra o madera del color
 * elegido), bloque, acero, caja, metal, computador, piedra, ducto y agua.
 */
final class Editor {
    static final int BRUSH = 0, LINE = 1, RECT = 2, OVAL = 3, TRI = 4, BUCKET = 5, ERASE = 6, PANEL = 7, STEEL = 8, CRATE = 9, METAL = 10, PC = 11, STONE = 12, VENT = 13, WATER = 14, TOOLS = 15;
    static final String[] NAMES = {"PINCEL", "LÍNEA", "CUADRADO", "CÍRCULO", "TRIÁNGULO", "BALDE", "BORRAR", "BLOQUE", "ACERO", "CAJA", "METAL", "COMPUTADOR", "PIEDRA", "DUCTO", "AGUA"};
    static final String[] HINT = {"UN DEDO DIBUJA · DOS DEDOS MUEVEN Y HACEN ZOOM", "ARRASTRA PARA HACER UNA LÍNEA", "ARRASTRA PARA HACER UN CUADRADO", "ARRASTRA PARA HACER UN CÍRCULO", "ARRASTRA PARA HACER UN TRIÁNGULO",
        "TOCA UNA ZONA PARA PINTARLA O UN HUECO CERRADO PARA LLENARLO", "BORRA LO QUE TOQUES", "BLOQUES MORADOS, SE PUEDEN ROMPER", "NO SE PUEDE ROMPER", "TOCA PARA PONER UNA CAJA", "CAJA DE METAL, MÁS RESISTENTE",
        "TOCA PARA PONER UN COMPUTADOR", "BLOQUE DE PIEDRA, MUY PESADO", "DUCTO DE VENTILACIÓN", "TOCA PARA ELEGIR LA ALTURA DEL AGUA"};
    /** Colores para tierra y madera: los mismos para los dos materiales (la tierra lleva vetas, la madera es lisa). */
    static final int[] COLORS = {0xFF8B5A2B, 0xFFD9B26F, 0xFFB5482E, 0xFF5E8C3A, 0xFF7D8590, 0xFFE8EEF4, 0xFF3E6FB0, 0xFFB04FD8, 0xFFC8955A, 0xFF6B4226};
    static final int[] SIZES = {4, 9, 18};
    final App app; World w; String name; int tool = BRUSH, color, size = 1; boolean wood, fill = true, dirty, confirmDelete;
    float cx = 900, cy = 600;
    Editor(App a) { app = a; }

    void open(Level l) {
        name = l.name; Wpn[] k = new Wpn[Robot.SLOTS]; java.util.Arrays.fill(k, Wpn.ALL[0]);
        w = new World(1, 1, k, k, l); for (Robot r : w.robots) r.deadT = Integer.MAX_VALUE; // sin personajes
        app.ren.reset(w); app.ren.freeCam = true; app.ren.zoomMul = .7f; app.ren.camX = 900; app.ren.camY = 600; app.ren.shake = 0;
        dirty = false; confirmDelete = false; mode = NONE; fingers.clear();
    }
    void close() { app.ren.freeCam = false; app.ren.zoomMul = 1; fingers.clear(); mode = NONE; }
    Level level() { return Level.of(w, name); }
    int mat() { return wood ? Terrain.WOOD : Terrain.EARTH; }
    boolean shape() { return tool >= LINE && tool <= TRI; }
    boolean paints() { return tool == BRUSH || tool == ERASE; }

    // ---------- Aplicar herramientas en coordenadas del mapa
    void apply(float x, float y) {
        int ix = Math.round(x), iy = Math.round(y), r = SIZES[size];
        switch (tool) {
            case BRUSH: w.t.brush(ix, iy, r, mat(), COLORS[color]); break;
            case ERASE: w.removeProps(ix, iy, r); w.t.wipe(ix, iy, r); break;
            case BUCKET: if (!w.t.bucket(ix, iy, mat(), COLORS[color])) { app.toast = "ESE HUECO NO ESTÁ CERRADO"; app.toastT = 120; return; } break;
            case PANEL: case STEEL: w.t.block(ix, iy, tool == STEEL ? Terrain.STEEL : Terrain.PANEL); break;
            case WATER: w.water = Math.max(Terrain.EDGE + 40, Math.min(Terrain.H - Terrain.EDGE - 8, iy)); break;
            case VENT: w.t.vent(ix, iy); break;
            case CRATE: case METAL: case PC: case STONE: w.addProp(w.t.object(ix, iy, tool == CRATE ? Terrain.CRATE : tool == METAL ? Terrain.METAL : tool == PC ? Terrain.COMPUTER : Terrain.STONE)); break;
            default: return;
        }
        dirty = true;
    }
    /** Trazo continuo del pincel o la goma: rellena entre el punto anterior y el actual. */
    float lastX = Float.NaN, lastY;
    void strokeTo(float x, float y) {
        if (Float.isNaN(lastX) || tool == PANEL || tool == STEEL) { apply(x, y); lastX = x; lastY = y; return; }
        float dx = x - lastX, dy = y - lastY, d = (float) Math.hypot(dx, dy); int n = Math.max(1, (int) (d / Math.max(2, SIZES[size] * .5f)));
        for (int i = 1; i <= n; i++) apply(lastX + dx * i / n, lastY + dy * i / n);
        lastX = x; lastY = y;
    }
    /** Dibuja la forma entre dos puntos del mapa. */
    void commitShape(float x0, float y0, float x1, float y1) {
        int a = Math.round(x0), b = Math.round(y0), c = Math.round(x1), d = Math.round(y1), r = SIZES[size], m = mat(), col = COLORS[color];
        if (Math.abs(c - a) + Math.abs(d - b) < 3) return;
        switch (tool) {
            case LINE: w.t.line(a, b, c, d, r, m, col); break;
            case RECT: w.t.rect(a, b, c, d, fill, r, m, col); break;
            case OVAL: w.t.oval(a, b, c, d, fill, r, m, col); break;
            case TRI: w.t.tri(a, b, c, d, fill, r, m, col); break;
            default: return;
        }
        dirty = true;
    }
    float wx(float sx) { return app.ren.vx0 + sx / app.ren.zoom; } float wy(float sy) { return app.ren.vy0 + sy / app.ren.zoom; }
    boolean inMap(float sy) { float y = sy / app.uiScale; return y > TOP && y < app.H - BOTTOM; }
    static final float TOP = 24, BOTTOM = 24;

    // ---------- Gestos: un dedo dibuja; con dos se mueve la vista y se hace zoom
    static final int NONE = 0, DRAW = 1, GESTURE = 2;
    final HashMap<Integer, float[]> fingers = new HashMap<>();
    int mode = NONE, drawId; boolean moved; float sx0, sy0, sx1, sy1;
    float gMid0x, gMid0y, gD0, gZ0, gWx, gWy;
    void pointer(int id, float x, float y, boolean down) {
        if (down) {
            boolean isNew = !fingers.containsKey(id); fingers.put(id, new float[]{x, y});
            if (isNew) {
                if (fingers.size() == 1 && inMap(y)) { mode = DRAW; drawId = id; moved = false; sx0 = sx1 = x; sy0 = sy1 = y; lastX = Float.NaN; }
                else if (fingers.size() >= 2) startGesture();
            } else if (mode == DRAW && id == drawId) drawMove(x, y);
            else if (mode == GESTURE) gesture();
        } else {
            float[] p = fingers.remove(id);
            if (fingers.isEmpty() && mode == GESTURE) { cx = app.ren.camX; cy = app.ren.camY; } // el cursor del mando se queda donde quedó la vista
            if (mode == DRAW && id == drawId) { if (p != null) { sx1 = x != 0 || y != 0 ? x : p[0]; sy1 = x != 0 || y != 0 ? y : p[1]; } drawEnd(); mode = NONE; }
            if (fingers.isEmpty()) mode = NONE;
        }
    }
    void drawMove(float x, float y) {
        sx1 = x; sy1 = y;
        if (!moved && Math.hypot(x - sx0, y - sy0) > 6 * app.uiScale / 3f) { moved = true; if (paints()) strokeTo(wx(sx0), wy(sy0)); }
        if (moved && paints()) strokeTo(wx(x), wy(y));
    }
    void drawEnd() {
        if (shape()) { if (moved) commitShape(wx(sx0), wy(sy0), wx(sx1), wy(sy1)); }
        else if (paints()) { if (!moved) apply(wx(sx0), wy(sy0)); }
        else apply(wx(sx1), wy(sy1)); // herramientas de un toque
        lastX = Float.NaN;
    }
    void startGesture() {
        mode = GESTURE; float[][] f = two(); if (f == null) return;
        gMid0x = (f[0][0] + f[1][0]) / 2; gMid0y = (f[0][1] + f[1][1]) / 2; gD0 = Math.max(10, dist(f)); gZ0 = app.ren.zoomMul;
        gWx = wx(gMid0x); gWy = wy(gMid0y);
    }
    void gesture() {
        float[][] f = two(); if (f == null) return;
        float mx = (f[0][0] + f[1][0]) / 2, my = (f[0][1] + f[1][1]) / 2;
        Renderer r = app.ren; float base = r.zoom / r.zoomMul, sw = r.vw * r.zoom, sh = r.vh * r.zoom;
        r.zoomMul = Math.max(.22f, Math.min(2.5f, gZ0 * dist(f) / gD0)); float z = base * r.zoomMul;
        r.camX = gWx - mx / z + sw / (2 * z); r.camY = gWy - my / z + sh / (2 * z); // el punto entre los dedos queda bajo los dedos
    }
    float[][] two() { if (fingers.size() < 2) return null; java.util.Iterator<float[]> it = fingers.values().iterator(); return new float[][]{it.next(), it.next()}; }
    static float dist(float[][] f) { return (float) Math.hypot(f[0][0] - f[1][0], f[0][1] - f[1][1]); }

    // ---------- Mando
    float gx0, gy0; boolean aHeld;
    void tick(int held, int press) {
        w.tick++; // solo anima el agua
        if (!app.padSeen) return;
        float sp = 5 / Math.max(.3f, app.ren.zoomMul);
        boolean dpad = (held & (Input.LEFT | Input.RIGHT | Input.UP | Input.DOWN)) != 0;
        if (!fingers.isEmpty()) { cx = app.ren.camX; cy = app.ren.camY; return; } // con los dedos manda el tacto; el cursor queda al centro
        if ((held & Input.LEFT) != 0) cx -= sp; if ((held & Input.RIGHT) != 0) cx += sp;
        if ((held & Input.UP) != 0) cy -= sp; if ((held & Input.DOWN) != 0) cy += sp;
        cx = Math.max(Terrain.EDGE, Math.min(Terrain.W - Terrain.EDGE, cx)); cy = Math.max(Terrain.EDGE, Math.min(Terrain.H - Terrain.EDGE, cy));
        if (dpad) { // la vista sigue al cursor solo mientras se mueve con la cruceta (antes lo hacía siempre y deshacía el zoom con los dedos)
            float vw = app.ren.vwNow(), vh = app.ren.vh, mx = vw * .3f, my = vh * .3f;
            if (cx < app.ren.camX - mx) app.ren.camX = cx + mx; if (cx > app.ren.camX + mx) app.ren.camX = cx - mx;
            if (cy < app.ren.camY - my) app.ren.camY = cy + my; if (cy > app.ren.camY + my) app.ren.camY = cy - my;
        }
        if ((press & Input.X) != 0) { tool = (tool + 1) % TOOLS; confirmDelete = false; }
        boolean a = (held & Input.A) != 0;
        if (a && !aHeld) { gx0 = cx; gy0 = cy; lastX = Float.NaN; if (!shape() && !paints()) apply(cx, cy); }
        if (a && paints()) strokeTo(cx, cy);
        if (!a && aHeld && shape()) commitShape(gx0, gy0, cx, cy);
        aHeld = a;
        if ((press & Input.B) != 0) app.leaveEditor(false);
    }

    // ---------- Interfaz (capa de pixel art)
    void draw(Gfx g) {
        float W = app.W, H = app.H, s = app.uiScale;
        preview(g, s);
        if (app.padSeen) { // cursor del mando
            float px = (cx - app.ren.vx0) * app.ren.zoom / s, py = (cy - app.ren.vy0) * app.ren.zoom / s;
            int x = Math.round(px), y = Math.round(py); g.rect(x - 6, y, 13, 1, App.CREAM); g.rect(x, y - 6, 1, 13, App.CREAM); g.rect(x - 1, y - 1, 3, 3, App.RED);
        }
        // barra de herramientas
        g.rect(0, 0, W, TOP, 0xE6101330); g.rect(0, TOP - 1, W, 1, App.INK);
        float acts = 3 * 44, iw = Math.min(21, (W - acts - 6) / TOOLS);
        for (int i = 0; i < TOOLS; i++) {
            float x = 3 + i * iw; boolean sel = i == tool; final int t = i;
            app.box(g, x, 2, iw - 1, 20, sel ? App.GOLD : App.BLUE, sel ? App.CREAM : App.SKY, sel ? 0xFFB8862A : App.NAVY);
            icon(g, i, x + (iw - 1) / 2, 12);
            app.btn(x, 0, iw, TOP, () -> { tool = t; confirmDelete = false; });
        }
        float ax = W - acts - 2;
        action(g, ax, "PROBAR", App.MINT, () -> app.testLevel());
        action(g, ax + 44, "SALIR", App.BLUE, () -> app.leaveEditor(false));
        action(g, ax + 88, confirmDelete ? "¿SEGURO?" : "BORRAR", App.RED, () -> { if (confirmDelete) app.leaveEditor(true); else confirmDelete = true; });
        // nombre del nivel (tocar para renombrar) y herramienta actual
        float nw = PixelFont.width(name, 8) + 10;
        app.box(g, 2, TOP + 2, nw, 14, App.NAVY, App.BLUE, App.DEEP); app.txt(g, name, 7, TOP + 12, 8, App.GOLD, -1);
        app.btn(2, TOP + 2, nw, 14, () -> app.renameLevel());
        app.txtS(g, NAMES[tool], W / 2, TOP + 12, 8, App.CREAM, 0);
        // barra inferior: material, colores, grosor y relleno
        float by = H - BOTTOM; g.rect(0, by, W, BOTTOM, 0xE6101330); g.rect(0, by, W, 1, App.INK);
        float x = 4;
        boolean usesMat = tool <= BUCKET;
        if (usesMat) {
            app.box(g, x, by + 3, 50, 18, wood ? 0xFFA0673A : 0xFF8B5A2B, 0xFFD9B26F, 0xFF4A2E14); app.txtS(g, wood ? "MADERA" : "TIERRA", x + 25, by + 15, 8, App.CREAM, 0);
            app.btn(x, by, 50, BOTTOM, () -> wood = !wood); x += 54;
            for (int i = 0; i < COLORS.length; i++) {
                final int c = i; if (i == color) app.box(g, x - 1, by + 2, 14, 20, App.GOLD, App.CREAM, 0xFFB8862A);
                app.box(g, x + 1, by + 4, 10, 16, COLORS[i], RobotArt.mixLight(COLORS[i]), RobotArt.mixDark(COLORS[i]));
                app.btn(x - 1, by, 14, BOTTOM, () -> color = c); x += 13;
            }
            x += 4;
        }
        if (tool == BRUSH || tool == ERASE || tool == LINE || shape() && !fill) {
            for (int i = 0; i < SIZES.length; i++) {
                final int k = i; boolean sel = i == size;
                app.box(g, x, by + 3, 15, 18, sel ? App.GOLD : App.BLUE, sel ? App.CREAM : App.SKY, sel ? 0xFFB8862A : App.NAVY);
                int d = 2 + i * 2; g.rect(Math.round(x + 7.5f - d / 2f), Math.round(by + 12 - d / 2f), d, d, sel ? App.INK : App.CREAM);
                app.btn(x, by, 15, BOTTOM, () -> size = k); x += 17;
            }
            x += 3;
        }
        if (tool == RECT || tool == OVAL || tool == TRI) {
            app.box(g, x, by + 3, 62, 18, App.BLUE, App.SKY, App.NAVY); app.txtS(g, fill ? "RELLENO" : "CONTORNO", x + 31, by + 15, 8, App.CREAM, 0);
            app.btn(x, by, 62, BOTTOM, () -> fill = !fill); x += 66;
        }
        if (tool == WATER) {
            app.box(g, x, by + 3, 70, 18, w.water < 0 ? App.GOLD : App.BLUE, App.SKY, App.NAVY);
            app.txt(g, "SIN AGUA", x + 35, by + 15, 8, w.water < 0 ? App.INK : App.CREAM, 0);
            app.btn(x, by, 70, BOTTOM, () -> { w.water = -1; dirty = true; }); x += 76;
        }
        String h = HINT[tool]; if (PixelFont.width(h, 8) < W - x - 8) app.txt(g, h, (x + W) / 2, by + 15, 8, App.MUTE, 0);
    }
    /** Vista previa de la forma mientras se arrastra. */
    void preview(Gfx g, float s) {
        float x0, y0, x1, y1;
        if (mode == DRAW && shape() && moved) { x0 = sx0 / s; y0 = sy0 / s; x1 = sx1 / s; y1 = sy1 / s; }
        else if (aHeld && shape() && app.padSeen) { Renderer r = app.ren; x0 = (gx0 - r.vx0) * r.zoom / s; y0 = (gy0 - r.vy0) * r.zoom / s; x1 = (cx - r.vx0) * r.zoom / s; y1 = (cy - r.vy0) * r.zoom / s; }
        else return;
        int c = App.CREAM; float a = Math.min(x0, x1), b = Math.max(x0, x1), t = Math.min(y0, y1), d = Math.max(y0, y1), mx = (a + b) / 2;
        switch (tool) {
            case LINE: g.line(x0, y0, x1, y1, c, 1); break;
            case RECT: g.line(a, t, b, t, c, 1); g.line(b, t, b, d, c, 1); g.line(b, d, a, d, c, 1); g.line(a, d, a, t, c, 1); break;
            case TRI: g.line(mx, t, b, d, c, 1); g.line(b, d, a, d, c, 1); g.line(a, d, mx, t, c, 1); break;
            case OVAL: { float ox = (a + b) / 2, oy = (t + d) / 2, rx = (b - a) / 2, ry = (d - t) / 2; float px = ox + rx, py = oy;
                for (int i = 1; i <= 32; i++) { double q = i * Math.PI / 16; float nx = ox + (float) Math.cos(q) * rx, ny = oy + (float) Math.sin(q) * ry; g.line(px, py, nx, ny, c, 1); px = nx; py = ny; } break; }
            default:
        }
    }
    void action(Gfx g, float x, String t, int c, Runnable r) {
        app.box(g, x, 2, 42, 20, c, RobotArt.mixLight(c), RobotArt.mixDark(c)); app.txtS(g, t, x + 21, 15, 8, App.CREAM, 0); app.btn(x, 0, 42, TOP, r);
    }
    /** Íconos de 14 px para cada herramienta. */
    void icon(Gfx g, int t, float cx, float cy) {
        int x = Math.round(cx), y = Math.round(cy), m = COLORS[color];
        switch (t) {
            case BRUSH: g.rect(x - 1, y - 6, 3, 7, 0xFFD9B26F); g.rect(x - 2, y + 1, 5, 4, m); g.rect(x - 1, y + 5, 3, 1, m); break;
            case LINE: for (int i = -5; i <= 5; i++) g.rect(x + i, y - i, 2, 2, m); break;
            case RECT: g.rect(x - 5, y - 4, 11, 9, m); g.rect(x - 3, y - 2, 7, 5, fill ? m : App.BLUE); break;
            case OVAL: for (int j = -5; j <= 5; j++) { int hw = (int) Math.round(Math.sqrt(25 - j * j)); g.rect(x - hw, y + j, hw * 2 + 1, 1, m); } break;
            case TRI: for (int j = 0; j <= 9; j++) g.rect(x - j / 2, y - 5 + j, j + 1, 1, m); break;
            case BUCKET: g.rect(x - 4, y - 2, 8, 7, 0xFFB4BFCC); g.rect(x - 3, y - 1, 6, 2, m); g.rect(x + 3, y - 5, 3, 1, App.CREAM); g.rect(x + 5, y - 5, 1, 4, App.CREAM); g.rect(x - 6, y + 3, 2, 3, m); break;
            case ERASE: for (int i = -4; i <= 4; i++) { g.rect(x + i, y + i, 2, 1, App.RED); g.rect(x + i, y - i, 2, 1, App.RED); } break;
            case PANEL: g.rect(x - 5, y - 5, 10, 10, App.INK); g.rect(x - 4, y - 4, 8, 8, App.PURPLE); g.rect(x - 4, y - 4, 8, 1, 0xFFB46BFF); break;
            case STEEL: g.rect(x - 5, y - 5, 10, 10, App.INK); g.rect(x - 4, y - 4, 8, 8, 0xFF2B1A4D); g.rect(x - 2, y - 2, 1, 1, 0xFF6B5A8D); g.rect(x + 1, y + 1, 1, 1, 0xFF6B5A8D); break;
            case CRATE: g.rect(x - 5, y - 5, 10, 10, 0xFFE3AD62); g.rect(x - 4, y - 4, 8, 8, 0xFFCB8C3C); for (int i = -3; i <= 3; i++) { g.rect(x + i, y + i, 1, 1, 0xFF96601F); g.rect(x + i, y - i, 1, 1, 0xFF96601F); } break;
            case METAL: g.rect(x - 5, y - 5, 10, 10, 0xFFB4BFCC); g.rect(x - 4, y - 4, 8, 8, 0xFF8D99A8); g.rect(x - 3, y - 3, 1, 1, 0xFF5F6B7A); g.rect(x + 2, y + 2, 1, 1, 0xFF5F6B7A); break;
            case PC: g.rect(x - 6, y - 5, 12, 11, 0xFF6B6B6B); g.rect(x - 4, y - 4, 8, 4, 0xFF0B3B2C); g.rect(x - 3, y - 2, 6, 1, 0xFF46F08A); g.rect(x - 3, y + 2, 2, 2, 0xFFD6DCE6); g.rect(x + 1, y + 2, 2, 2, 0xFFD6DCE6); break;
            case STONE: g.rect(x - 5, y - 5, 10, 10, 0xFF9C9488); g.rect(x - 5, y, 10, 1, 0xFF5B554D); g.rect(x - 1, y - 5, 1, 5, 0xFF5B554D); break;
            case VENT: g.rect(x - 6, y - 4, 12, 8, 0xFFB9C2CE); for (int i = -2; i <= 2; i += 2) g.rect(x - 4, y + i, 8, 1, 0xFF262C36); break;
            default: g.rect(x - 6, y - 1, 12, 6, 0xFF3C8CFF); for (int i = -6; i < 6; i += 4) { g.rect(x + i, y - 2, 2, 1, 0xFFE8F6FF); g.rect(x + i + 2, y - 3, 2, 1, 0xFFE8F6FF); }
        }
    }
}
