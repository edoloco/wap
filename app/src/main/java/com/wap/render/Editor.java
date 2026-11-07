package com.wap.render;
import com.wap.core.*;
/**
 * Editor de niveles: se dibuja sobre el mapa con el dedo (o con el mando moviendo un cursor).
 * Herramientas: tierra de colores, bloques, acero indestructible, cajas, metal, computador, piedra, ductos,
 * agua (solo se elige la altura), borrar y mover la vista. El nivel se guarda solo al salir o al probarlo.
 */
final class Editor {
    static final int EARTH = 0, PANEL = 1, STEEL = 2, CRATE = 3, METAL = 4, PC = 5, STONE = 6, VENT = 7, WATER = 8, ERASE = 9, MOVE = 10, TOOLS = 11;
    static final String[] NAMES = {"TIERRA", "BLOQUE", "ACERO", "CAJA", "METAL", "COMPUTADOR", "PIEDRA", "DUCTO", "AGUA", "BORRAR", "MOVER"};
    static final String[] HINT = {"DIBUJA TIERRA DESTRUCTIBLE", "BLOQUES MORADOS, SE PUEDEN ROMPER", "NO SE PUEDE ROMPER", "TOCA PARA PONER UNA CAJA", "CAJA DE METAL, MÁS RESISTENTE",
        "TOCA PARA PONER UN COMPUTADOR", "BLOQUE DE PIEDRA, MUY PESADO", "DUCTO DE VENTILACIÓN", "TOCA PARA ELEGIR LA ALTURA DEL AGUA", "BORRA LO QUE TOQUES", "ARRASTRA PARA MOVER LA VISTA"};
    static final int[] COLORS = {0xFF8B5A2B, 0xFFD9B26F, 0xFFB5482E, 0xFF5E8C3A, 0xFF7D8590, 0xFFE8EEF4, 0xFF3E6FB0, 0xFFB04FD8};
    static final int[] SIZES = {6, 12, 22};
    static final float[] ZOOMS = {1, .6f, .3f};
    final App app; World w; String name; int tool = EARTH, color, size = 1, zoom = 1; boolean dirty, confirmDelete;
    float lastX = Float.NaN, lastY, cx = 900, cy = 600; boolean stroke;
    Editor(App a) { app = a; }

    void open(Level l) {
        name = l.name; Wpn[] k = new Wpn[Robot.SLOTS]; java.util.Arrays.fill(k, Wpn.ALL[0]);
        w = new World(1, 1, k, k, l); for (Robot r : w.robots) r.deadT = Integer.MAX_VALUE; // sin personajes
        app.ren.reset(w); app.ren.freeCam = true; app.ren.zoomMul = ZOOMS[zoom]; app.ren.camX = 900; app.ren.camY = 600; app.ren.shake = 0;
        dirty = false; confirmDelete = false; stroke = false;
    }
    void close() { app.ren.freeCam = false; app.ren.zoomMul = 1; }
    Level level() { return Level.of(w, name); }

    // ---------- Aplicar herramientas en coordenadas del mapa
    void apply(float x, float y, boolean first) {
        int ix = Math.round(x), iy = Math.round(y), r = SIZES[size];
        switch (tool) {
            case EARTH: w.t.earth(ix, iy, r, COLORS[color]); break;
            case PANEL: case STEEL: w.t.block(ix, iy, tool == STEEL ? Terrain.STEEL : Terrain.PANEL); break;
            case ERASE: w.removeProps(ix, iy, r); w.t.wipe(ix, iy, r); break;
            case WATER: w.water = Math.max(Terrain.EDGE + 40, Math.min(Terrain.H - Terrain.EDGE - 8, iy)); break;
            case CRATE: case METAL: case PC: case STONE: case VENT:
                if (!first) return;
                if (tool == VENT) w.t.vent(ix, iy);
                else w.addProp(w.t.object(ix, iy, tool == CRATE ? Terrain.CRATE : tool == METAL ? Terrain.METAL : tool == PC ? Terrain.COMPUTER : Terrain.STONE));
                break;
            default: return;
        }
        dirty = true;
    }
    /** Trazo continuo: rellena entre el punto anterior y el actual. */
    void strokeTo(float x, float y, boolean first) {
        if (first || Float.isNaN(lastX) || tool >= CRATE && tool <= VENT || tool == WATER) { apply(x, y, first); lastX = x; lastY = y; return; }
        float dx = x - lastX, dy = y - lastY, d = (float) Math.hypot(dx, dy); int n = Math.max(1, (int) (d / Math.max(2, SIZES[size] * .5f)));
        for (int i = 1; i <= n; i++) apply(lastX + dx * i / n, lastY + dy * i / n, false);
        lastX = x; lastY = y;
    }
    float wx(float sx) { return app.ren.vx0 + sx / app.ren.zoom; } float wy(float sy) { return app.ren.vy0 + sy / app.ren.zoom; }
    boolean inMap(float sx, float sy) { float y = sy / app.uiScale; return y > top() && y < app.H - bottom(); }
    float top() { return 24; } float bottom() { return 24; }

    // ---------- Toque
    float panX, panY;
    void down(float sx, float sy) {
        if (!inMap(sx, sy)) { stroke = false; return; }
        stroke = true; panX = sx; panY = sy;
        if (tool != MOVE) strokeTo(wx(sx), wy(sy), true);
    }
    void drag(float sx, float sy) {
        if (!stroke) return;
        if (tool == MOVE) { app.ren.camX -= (sx - panX) / app.ren.zoom; app.ren.camY -= (sy - panY) / app.ren.zoom; panX = sx; panY = sy; return; }
        strokeTo(wx(sx), wy(sy), false);
    }
    void up() { stroke = false; lastX = Float.NaN; }

    // ---------- Mando: cursor con la cruceta, A aplica, X cambia de herramienta, B sale
    void tick(int held, int press) {
        w.tick++; // solo anima el agua
        if (!app.padSeen) return;
        float sp = 5 / ZOOMS[zoom];
        if ((held & Input.LEFT) != 0) cx -= sp; if ((held & Input.RIGHT) != 0) cx += sp;
        if ((held & Input.UP) != 0) cy -= sp; if ((held & Input.DOWN) != 0) cy += sp;
        cx = Math.max(Terrain.EDGE, Math.min(Terrain.W - Terrain.EDGE, cx)); cy = Math.max(Terrain.EDGE, Math.min(Terrain.H - Terrain.EDGE, cy));
        app.ren.camX += (cx - app.ren.camX) * .2f; app.ren.camY += (cy - app.ren.camY) * .2f;
        if ((press & Input.X) != 0) tool = (tool + 1) % TOOLS;
        if ((held & Input.A) != 0 && tool != MOVE) strokeTo(cx, cy, (press & Input.A) != 0); else lastX = Float.NaN;
        if ((press & Input.B) != 0) app.leaveEditor(false);
    }

    // ---------- Interfaz (capa de pixel art)
    void draw(Gfx g) {
        float W = app.W, H = app.H, s = app.uiScale;
        if (app.padSeen) { // cursor del mando
            float px = (cx - app.ren.vx0) * app.ren.zoom / s, py = (cy - app.ren.vy0) * app.ren.zoom / s;
            int x = Math.round(px), y = Math.round(py); g.rect(x - 6, y, 13, 1, App.CREAM); g.rect(x, y - 6, 1, 13, App.CREAM); g.rect(x - 1, y - 1, 3, 3, App.RED);
        }
        // barra de herramientas
        g.rect(0, 0, W, top(), 0xE6101330); g.rect(0, top() - 1, W, 1, App.INK);
        float acts = 3 * 44, iw = Math.min(22, (W - acts - 6) / TOOLS);
        for (int i = 0; i < TOOLS; i++) {
            float x = 3 + i * iw; boolean sel = i == tool; final int t = i;
            app.box(g, x, 2, iw - 2, 20, sel ? App.GOLD : App.BLUE, sel ? App.CREAM : App.SKY, sel ? 0xFFB8862A : App.NAVY);
            icon(g, i, x + (iw - 2) / 2, 12);
            app.btn(x, 0, iw, top(), () -> { tool = t; confirmDelete = false; });
        }
        float ax = W - acts - 2;
        action(g, ax, "PROBAR", App.MINT, () -> app.testLevel());
        action(g, ax + 44, "SALIR", App.BLUE, () -> app.leaveEditor(false));
        action(g, ax + 88, confirmDelete ? "¿SEGURO?" : "BORRAR", App.RED, () -> { if (confirmDelete) app.leaveEditor(true); else confirmDelete = true; });
        // barra inferior: nombre, opciones de la herramienta y zoom
        float by = H - bottom(); g.rect(0, by, W, bottom(), 0xE6101330); g.rect(0, by, W, 1, App.INK);
        float nw = Math.min(110, PixelFont.width(name, 8) + 16);
        app.box(g, 2, by + 3, nw, 18, App.NAVY, App.BLUE, App.DEEP); app.txt(g, name, 8, by + 15, 8, App.GOLD, -1);
        app.btn(2, by, nw, bottom(), () -> app.renameLevel());
        float x = nw + 8;
        if (tool == EARTH) for (int i = 0; i < COLORS.length; i++) {
            final int c = i; if (i == color) app.box(g, x - 1, by + 2, 16, 20, App.GOLD, App.CREAM, 0xFFB8862A);
            app.box(g, x + 1, by + 4, 12, 16, COLORS[i], RobotArt.mixLight(COLORS[i]), RobotArt.mixDark(COLORS[i]));
            app.btn(x - 1, by, 16, bottom(), () -> color = c); x += 16;
        }
        if (tool == EARTH || tool == ERASE) {
            x += 4;
            for (int i = 0; i < SIZES.length; i++) {
                final int k = i; boolean sel = i == size;
                app.box(g, x, by + 3, 16, 18, sel ? App.GOLD : App.BLUE, sel ? App.CREAM : App.SKY, sel ? 0xFFB8862A : App.NAVY);
                int d = 2 + i * 2; g.rect(Math.round(x + 8 - d / 2f), Math.round(by + 12 - d / 2f), d, d, sel ? App.INK : App.CREAM);
                app.btn(x, by, 16, bottom(), () -> size = k); x += 18;
            }
        }
        if (tool == WATER) {
            app.box(g, x, by + 3, 70, 18, w.water < 0 ? App.GOLD : App.BLUE, App.SKY, App.NAVY);
            app.txt(g, "SIN AGUA", x + 35, by + 15, 8, w.water < 0 ? App.INK : App.CREAM, 0);
            app.btn(x, by, 70, bottom(), () -> { w.water = -1; dirty = true; }); x += 76;
        }
        float zx = W - 52;
        app.box(g, zx, by + 3, 50, 18, App.BLUE, App.SKY, App.NAVY); app.txt(g, "ZOOM " + (zoom + 1), zx + 25, by + 15, 8, App.CREAM, 0);
        app.btn(zx, by, 52, bottom(), () -> { zoom = (zoom + 1) % ZOOMS.length; app.ren.zoomMul = ZOOMS[zoom]; });
        if (x < zx - 10) { String h = HINT[tool]; if (PixelFont.width(h, 8) < zx - x - 8) app.txt(g, h, (x + zx) / 2, by + 15, 8, App.MUTE, 0); }
        app.txtS(g, NAMES[tool], W / 2, top() + 12, 8, App.CREAM, 0);
    }
    void action(Gfx g, float x, String t, int c, Runnable r) {
        app.box(g, x, 2, 42, 20, c, RobotArt.mixLight(c), RobotArt.mixDark(c)); app.txtS(g, t, x + 21, 15, 8, App.CREAM, 0); app.btn(x, 0, 42, top(), r);
    }
    /** Íconos de 14 px para cada herramienta. */
    void icon(Gfx g, int t, float cx, float cy) {
        int x = Math.round(cx), y = Math.round(cy);
        switch (t) {
            case EARTH: g.rect(x - 6, y - 2, 12, 7, COLORS[color]); g.rect(x - 4, y - 4, 8, 2, COLORS[color]); g.rect(x - 6, y + 4, 12, 1, App.INK); break;
            case PANEL: g.rect(x - 5, y - 5, 10, 10, App.INK); g.rect(x - 4, y - 4, 8, 8, App.PURPLE); g.rect(x - 4, y - 4, 8, 1, 0xFFB46BFF); break;
            case STEEL: g.rect(x - 5, y - 5, 10, 10, App.INK); g.rect(x - 4, y - 4, 8, 8, 0xFF2B1A4D); g.rect(x - 2, y - 2, 1, 1, 0xFF6B5A8D); g.rect(x + 1, y + 1, 1, 1, 0xFF6B5A8D); break;
            case CRATE: g.rect(x - 5, y - 5, 10, 10, 0xFFE3AD62); g.rect(x - 4, y - 4, 8, 8, 0xFFCB8C3C); for (int i = -3; i <= 3; i++) { g.rect(x + i, y + i, 1, 1, 0xFF96601F); g.rect(x + i, y - i, 1, 1, 0xFF96601F); } break;
            case METAL: g.rect(x - 5, y - 5, 10, 10, 0xFFB4BFCC); g.rect(x - 4, y - 4, 8, 8, 0xFF8D99A8); g.rect(x - 3, y - 3, 1, 1, 0xFF5F6B7A); g.rect(x + 2, y + 2, 1, 1, 0xFF5F6B7A); break;
            case PC: g.rect(x - 6, y - 5, 12, 11, 0xFF6B6B6B); g.rect(x - 4, y - 4, 8, 4, 0xFF0B3B2C); g.rect(x - 3, y - 2, 6, 1, 0xFF46F08A); g.rect(x - 3, y + 2, 2, 2, 0xFFD6DCE6); g.rect(x + 1, y + 2, 2, 2, 0xFFD6DCE6); break;
            case STONE: g.rect(x - 5, y - 5, 10, 10, 0xFF9C9488); g.rect(x - 5, y, 10, 1, 0xFF5B554D); g.rect(x - 1, y - 5, 1, 5, 0xFF5B554D); break;
            case VENT: g.rect(x - 6, y - 4, 12, 8, 0xFFB9C2CE); for (int i = -2; i <= 2; i += 2) g.rect(x - 4, y + i, 8, 1, 0xFF262C36); break;
            case WATER: g.rect(x - 6, y - 1, 12, 6, 0xFF3C8CFF); for (int i = -6; i < 6; i += 4) { g.rect(x + i, y - 2, 2, 1, 0xFFE8F6FF); g.rect(x + i + 2, y - 3, 2, 1, 0xFFE8F6FF); } break;
            case ERASE: for (int i = -4; i <= 4; i++) { g.rect(x + i, y + i, 2, 1, App.RED); g.rect(x + i, y - i, 2, 1, App.RED); } break;
            default: g.rect(x - 5, y, 11, 1, App.CREAM); g.rect(x, y - 5, 1, 11, App.CREAM); g.rect(x - 5, y - 1, 2, 3, App.CREAM); g.rect(x + 4, y - 1, 2, 3, App.CREAM); g.rect(x - 1, y - 5, 3, 2, App.CREAM); g.rect(x - 1, y + 4, 3, 2, App.CREAM);
        }
    }
}
