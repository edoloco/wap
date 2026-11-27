package com.wap.render;
import com.wap.core.*;
import java.util.ArrayList;
import java.util.HashMap;
/**
 * Editor de BASES: todo va en la grilla. Se parte con la "mesa base" (una habitación) y se agregan habitaciones
 * pegadas tocando los "+" (herramienta SALA). En los cuadros van piezas de material (que pueden moverse),
 * enemigos, premios y el inicio. Un dedo pone o borra; dos dedos mueven la vista y hacen zoom.
 * Con el mando: la cruceta mueve el cursor de cuadro en cuadro, A aplica, X cambia de herramienta y B sale.
 */
final class BaseEditor {
    static final int PIECE = 0, ERASE = 1, ENEMY = 2, PRIZE = 3, START = 4, ROOM = 5, TOOLS = 6;
    static final String[] TOOL_NAMES = {"PIEZA", "BORRAR", "ENEMIGO", "PREMIO", "INICIO", "SALA"};
    static final String[] HINT = {"TOCA O ARRASTRA PARA PONER PIEZAS EN LA GRILLA", "TOCA O ARRASTRA PARA BORRAR", "TOCA UN CUADRO PARA PONER EL ENEMIGO",
        "TOCA UN CUADRO PARA PONER EL PREMIO", "TOCA DONDE PARTE EL JUGADOR", "TOCA UN + PARA AGREGAR UNA HABITACIÓN · TOCA UNA PARA ELEGIRLA"};
    static final int[] MATS = {Base.T_EARTH, Base.T_WOOD, Base.T_PANEL, Base.T_STEEL, Base.T_CRATE, Base.T_METAL, Base.T_STONE, Base.T_PC, Base.T_BACK};
    static final int[] ENEMIES = {Base.E_DRONE, Base.E_TURRET, Base.E_BADPAL}; static final String[] ENEMY_NAMES = {"DRON DE ATAQUE", "TORRETA", "BADPAL"};
    static final String[] ENEMY_HINT = {"AL ACERCARTE VUELA SUBIENDO Y BAJANDO Y SUELTA MISILES", "UN OJO ROJO: TE DISPARA BOLITAS DE ENERGÍA AL VERTE", "CAMINA, SE SIENTA CERCA TUYO, DISPARA Y SE CAMBIA DE LUGAR"};
    static final int[] PRIZES = {Base.P_CAPSULE, Base.P_LIFE, Base.P_WEAPON}; static final String[] PRIZE_NAMES = {"CÁPSULA", "VIDA", "ARMA"};
    static final int[] COLORS = {0xFF8B5A2B, 0xFFD9B26F, 0xFFB5482E, 0xFF5E8C3A, 0xFF7D8590, 0xFFE8EEF4, 0xFF3E6FB0, 0xFFB04FD8, 0xFFC8955A, 0xFF6B4226, 0xFF1E1E24};
    static final float TOP = 24, BOTTOM = 30;

    final App app; Base b; int tool = PIECE, mat, color, move, enemy, prize; int[] selRoom; boolean confirmDelete, dirty;
    float camX, camY, zoom = .3f; int curX = 2, curY = 6; boolean aHeld;
    final ArrayList<byte[]> undo = new ArrayList<>(), redo = new ArrayList<>(); byte[] before;
    BaseEditor(App a) { app = a; }

    void open(Base base) {
        b = base; tool = PIECE; selRoom = null; confirmDelete = false; dirty = false; fingers.clear(); mode = NONE; undo.clear(); redo.clear();
        int[] r = b.rooms.get(0); camX = r[0] * Base.ROOM_PX + Base.ROOM_PX / 2f; camY = r[1] * Base.ROOM_PY + Base.ROOM_PY / 2f; zoom = 0;
    }
    float fitZoom() { return Math.min((app.W - 8) / (float) Base.ROOM_PX, (app.H - TOP - BOTTOM - 22) / (float) Base.ROOM_PY); }
    float mapTop() { return TOP + 16; }
    float midY() { return mapTop() + (app.H - BOTTOM - mapTop()) / 2; }
    float ux(float x) { return (x - camX) * zoom + app.W / 2f; } float uy(float y) { return (y - camY) * zoom + midY(); }
    float wx(float sx) { return (sx - app.W / 2f) / zoom + camX; } float wy(float sy) { return (sy - midY()) / zoom + camY; }
    boolean inMap(float uy) { return uy > mapTop() && uy < app.H - BOTTOM; }

    // ---------- Editar (deshacer guarda la base entera: es chica)
    void begin() { if (before == null) before = b.save(); }
    void end() {
        if (before == null) return; byte[] now = b.save();
        if (!java.util.Arrays.equals(before, now)) { undo.add(before); while (undo.size() > 40) undo.remove(0); redo.clear(); dirty = true; }
        before = null;
    }
    void restore(ArrayList<byte[]> from, ArrayList<byte[]> to) {
        if (from.isEmpty()) return; to.add(b.save());
        try { Base n = Base.load(from.remove(from.size() - 1)); n.name = b.name; b = n; } catch (Exception e) { } dirty = true;
    }
    void apply(float x, float y) {
        int cx = (int) Math.floor(x / Base.CELL), cy = (int) Math.floor(y / Base.CELL);
        switch (tool) {
            case PIECE: if (MATS[mat] == Base.T_PC) { b.putTile(cx, cy, Base.T_PC, 0, 0); for (int[] d : new int[][]{{1, 0}, {0, 1}, {1, 1}}) b.erase(cx + d[0], cy + d[1]); }
                else b.putTile(cx, cy, MATS[mat], COLORS[color], move); break;
            case ERASE: b.erase(cx, cy); break;
            case ENEMY: b.putThing(cx, cy, ENEMIES[enemy]); break;
            case PRIZE: b.putThing(cx, cy, PRIZES[prize]); break;
            case START: b.putThing(cx, cy, Base.START); break;
            case ROOM: {
                int rx = (int) Math.floor(x / Base.ROOM_PX), ry = (int) Math.floor(y / Base.ROOM_PY);
                if (b.room(rx, ry) != null) selRoom = b.room(rx, ry);
                else if (b.canAdd(rx, ry)) { b.addRoom(rx, ry); selRoom = b.room(rx, ry); }
                else if (b.rooms.size() >= Base.MAX_ROOMS) { app.toast = "MÁXIMO " + Base.MAX_ROOMS + " HABITACIONES"; app.toastT = 120; }
                break; }
            default:
        }
    }
    boolean drags() { return tool == PIECE && MATS[mat] != Base.T_PC || tool == ERASE; }

    // ---------- Gestos: un dedo pone; dos mueven y hacen zoom
    static final int NONE = 0, DRAW = 1, GESTURE = 2;
    final HashMap<Integer, float[]> fingers = new HashMap<>(); int mode = NONE, drawId; float gx, gy, gd, gz, gwx, gwy; long lastCell = Long.MIN_VALUE;
    void pointer(int id, float sx, float sy, boolean down) {
        float x = sx / app.uiScale, y = sy / app.uiScale;
        if (down) {
            boolean isNew = !fingers.containsKey(id); fingers.put(id, new float[]{x, y});
            if (isNew) {
                if (fingers.size() == 1 && inMap(y)) { mode = DRAW; drawId = id; lastCell = Long.MIN_VALUE; begin(); paintAt(x, y); }
                else if (fingers.size() >= 2) { if (mode == DRAW) end(); startGesture(); }
            } else if (mode == DRAW && id == drawId) { if (drags()) paintAt(x, y); }
            else if (mode == GESTURE) gesture();
        } else {
            fingers.remove(id);
            if (mode == DRAW && id == drawId) { end(); mode = NONE; }
            if (fingers.isEmpty()) mode = NONE;
        }
    }
    void paintAt(float x, float y) {
        float mx = wx(x), my = wy(y); long c = Base.key((int) Math.floor(mx / Base.CELL), (int) Math.floor(my / Base.CELL));
        if (c == lastCell) return; lastCell = c; apply(mx, my);
    }
    void startGesture() {
        mode = GESTURE; float[][] f = two(); if (f == null) return;
        gx = (f[0][0] + f[1][0]) / 2; gy = (f[0][1] + f[1][1]) / 2; gd = Math.max(10, dist(f)); gz = zoom; gwx = wx(gx); gwy = wy(gy);
    }
    void gesture() {
        float[][] f = two(); if (f == null) return;
        float mx = (f[0][0] + f[1][0]) / 2, my = (f[0][1] + f[1][1]) / 2, fz = fitZoom();
        zoom = Math.max(fz * .2f, Math.min(fz * 3, gz * dist(f) / gd));
        camX = gwx - (mx - app.W / 2f) / zoom; camY = gwy - (my - midY()) / zoom; // el punto entre los dedos queda bajo los dedos
    }
    float[][] two() { if (fingers.size() < 2) return null; java.util.Iterator<float[]> it = fingers.values().iterator(); return new float[][]{it.next(), it.next()}; }
    static float dist(float[][] f) { return (float) Math.hypot(f[0][0] - f[1][0], f[0][1] - f[1][1]); }

    // ---------- Mando: el cursor va de cuadro en cuadro
    int rep;
    void tick(int held, int press) {
        if (!app.padSeen || !fingers.isEmpty()) return;
        int step = press != 0 ? 1 : ++rep > 12 && rep % 4 == 0 ? 1 : 0; if ((held & (Input.LEFT | Input.RIGHT | Input.UP | Input.DOWN)) == 0) rep = 0;
        if (step > 0) {
            if ((held & Input.LEFT) != 0) curX--; if ((held & Input.RIGHT) != 0) curX++; if ((held & Input.UP) != 0) curY--; if ((held & Input.DOWN) != 0) curY++;
            float x = (curX + .5f) * Base.CELL, y = (curY + .5f) * Base.CELL, vw = (app.W - 20) / zoom / 2, vh = (app.H - TOP - BOTTOM - 30) / zoom / 2;
            if (x < camX - vw) camX = x + vw; if (x > camX + vw) camX = x - vw; if (y < camY - vh) camY = y + vh; if (y > camY + vh) camY = y - vh;
        }
        if ((press & Input.X) != 0) { tool = (tool + 1) % TOOLS; confirmDelete = false; }
        boolean a = (held & Input.A) != 0;
        if (a && (!aHeld || drags() && step > 0)) { begin(); apply((curX + .5f) * Base.CELL, (curY + .5f) * Base.CELL); end(); }
        aHeld = a;
        if ((press & Input.B) != 0) app.leaveBaseEditor(false);
    }

    // ---------- Dibujo (capa de pixel art)
    static int roomTint(int bg) { switch (bg) { case 1: return 0xFFBFE3F5; case 2: return 0xFFC9ECF7; case 3: return 0xFF3E4470; default: return 0xFFC7D2E0; } }
    void draw(Gfx g) {
        float W = app.W, H = app.H; if (zoom <= 0) zoom = fitZoom();
        g.rect(0, 0, W, H, 0xFF101330);
        float cs = Base.CELL * zoom; int tint = roomTint(b.bg), line = b.bg == 3 ? 0xFF50578A : 0xFFAEBBCB;
        for (int[] r : b.rooms) { // habitaciones con su grilla
            float x = ux(r[0] * Base.ROOM_PX), y = uy(r[1] * Base.ROOM_PY), w = Base.ROOM_PX * zoom, h = Base.ROOM_PY * zoom;
            g.rect(x, y, w, h, tint);
            if (cs >= 4) { for (int i = 1; i < Base.RW; i++) g.rect(x + i * cs, y, 1, h, line); for (int j = 1; j < Base.RH; j++) g.rect(x, y + j * cs, w, 1, line); }
        }
        for (java.util.Map.Entry<Long, int[]> e : b.tiles.entrySet()) {
            float x = ux(Base.kx(e.getKey()) * Base.CELL), y = uy(Base.ky(e.getKey()) * Base.CELL); if (x > W || y > H || x + 2 * cs < 0 || y + 2 * cs < 0) continue;
            tile(g, x, y, cs, e.getValue());
        }
        for (java.util.Map.Entry<Long, Integer> e : b.things.entrySet()) {
            float x = ux(Base.kx(e.getKey()) * Base.CELL), y = uy(Base.ky(e.getKey()) * Base.CELL); if (x > W || y > H || x + cs < 0 || y + cs < 0) continue;
            thing(g, x, y, cs, e.getValue());
        }
        for (int[] r : b.rooms) { // bordes: la del jefe en rojo, la elegida en dorado
            float x = ux(r[0] * Base.ROOM_PX), y = uy(r[1] * Base.ROOM_PY), w = Base.ROOM_PX * zoom, h = Base.ROOM_PY * zoom; boolean sel = r == selRoom && tool == ROOM;
            int c = r[2] == 1 ? App.RED : sel ? App.GOLD : App.INK; frame(g, x, y, w, h, c); if (r[2] == 1 || sel) frame(g, x + 1, y + 1, w - 2, h - 2, c);
            if (r[2] == 1) { app.box(g, x + 3, y + 3, 30, 11, App.RED, 0xFFFF8A8F, 0xFF8E1520); app.txt(g, "JEFE", x + 18, y + 12, 8, App.CREAM, 0); }
        }
        if (tool == ROOM) for (int[] r : b.rooms) for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) { // los "+" donde se puede agregar
            int rx = r[0] + d[0], ry = r[1] + d[1]; if (!b.canAdd(rx, ry)) continue;
            float x = ux(rx * Base.ROOM_PX), y = uy(ry * Base.ROOM_PY), w = Base.ROOM_PX * zoom, h = Base.ROOM_PY * zoom;
            g.rect(x + 2, y + 2, w - 4, h - 4, 0x553DDC97); frame(g, x + 2, y + 2, w - 4, h - 4, App.MINT);
            app.txtS(g, "+", x + w / 2, y + h / 2 + 8, Math.max(8, Math.min(24, h / 3)), App.MINT, 0);
        }
        if (app.padSeen) { float x = ux(curX * Base.CELL), y = uy(curY * Base.CELL); frame(g, x - 1, y - 1, cs + 2, cs + 2, App.RED); frame(g, x, y, cs, cs, App.CREAM); }
        bars(g, W, H);
    }
    static void frame(Gfx g, float x, float y, float w, float h, int c) { g.rect(x, y, w, 1, c); g.rect(x, y + h - 1, w, 1, c); g.rect(x, y, 1, h, c); g.rect(x + w - 1, y, 1, h, c); }
    static int matColor(int mat, int color) {
        switch (mat) { case Base.T_PANEL: return 0xFF8214FF; case Base.T_STEEL: return 0xFF3A3F55; case Base.T_CRATE: return 0xFFCB8C3C; case Base.T_METAL: return 0xFF8D99A8; case Base.T_STONE: return 0xFF9C9488; case Base.T_PC: return 0xFF2C323F; default: return color | 0xFF000000; }
    }
    void tile(Gfx g, float x, float y, float cs, int[] v) {
        if (v[0] == Base.T_BACK) { g.rect(x, y, cs, cs, Renderer.tone(v[1] | 0xFF000000, .55f, 0)); if (cs >= 8) { g.line(x, y, x + cs, y + cs, 0x33FFFFFF, 1); } return; } // fondo: oscuro y rayado
        int mat = v[0]; float s = mat == Base.T_PC ? cs * 2 : cs; int c = matColor(mat, v[1]);
        g.rect(x, y, s, s, c);
        if (cs >= 6) {
            if (mat == Base.T_CRATE) { g.line(x + 1, y + 1, x + s - 1, y + s - 1, 0xFF96601F, 1); g.line(x + s - 1, y + 1, x + 1, y + s - 1, 0xFF96601F, 1); }
            else if (mat == Base.T_PC) g.rect(x + s * .15f, y + s * .15f, s * .7f, s * .4f, 0xFF46F08A);
            else if (mat == Base.T_PANEL || mat == Base.T_EARTH) g.rect(x, y, s, Math.max(1, s / 10), Renderer.tone(c, 1.25f, 30));
            frame(g, x, y, s, s, 0x66000000);
        }
        if (v[2] != Base.M_NONE && cs >= 8) moveIcon(g, x + cs / 2, y + cs / 2, v[2], cs);
    }
    /** Marca de las piezas que se mueven: flechas o un círculo. */
    static void moveIcon(Gfx g, float cx, float cy, int move, float cs) {
        float r = Math.max(2, cs * .28f); int c = App.CREAM;
        switch (move) {
            case Base.M_UPDOWN: g.rect(cx - .5f, cy - r, 1, 2 * r, c); g.rect(cx - 1.5f, cy - r + 1, 3, 1, c); g.rect(cx - 1.5f, cy + r - 2, 3, 1, c); break;
            case Base.M_SIDE: g.rect(cx - r, cy - .5f, 2 * r, 1, c); g.rect(cx - r + 1, cy - 1.5f, 1, 3, c); g.rect(cx + r - 2, cy - 1.5f, 1, 3, c); break;
            case Base.M_SPIN: frame(g, cx - r, cy - r, 2 * r, 2 * r, c); g.rect(cx + r - 1, cy - 1, 2, 2, App.GOLD); break;
            default: for (int i = 0; i < 3; i++) g.rect(cx - r + i * r * .8f, cy - .5f, r * .5f, 1, c);
        }
    }
    void thing(Gfx g, float x, float y, float cs, int what) {
        float cx = x + cs / 2, cy = y + cs / 2, r = cs * .4f;
        switch (what) {
            case Base.E_DRONE: g.rect(cx - r, cy - r * .5f, 2 * r, r, 0xFF7D8699); g.rect(cx - r * 1.2f, cy - r * .8f, r * 2.4f, Math.max(1, r * .15f), App.INK); g.rect(cx - 1, cy - 1, 2, 2, App.RED); break;
            case Base.E_TURRET: g.rect(cx - r, cy, 2 * r, r, 0xFF4A5164); g.rect(cx - r * .8f, cy - r * .6f, r * 1.6f, r * .9f, 0xFF7D8699); g.rect(cx - 1, cy - r * .3f, 2, 2, App.RED); break;
            case Base.E_BADPAL: g.rect(cx - r, cy - r * .5f, 2 * r, r, 0xFF5E8C3A); g.rect(cx - r * .5f - 1, cy - r * .2f, 2, 2, App.CREAM); g.rect(cx + r * .5f - 1, cy - r * .2f, 2, 2, App.CREAM); g.rect(cx - r * .6f, cy + r * .5f, 1, r * .5f, App.INK); g.rect(cx + r * .6f, cy + r * .5f, 1, r * .5f, App.INK); break;
            case Base.P_CAPSULE: g.rect(cx - r, cy - r * .3f, r, r * .6f, App.RED); g.rect(cx, cy - r * .3f, r, r * .6f, 0xFFFFD93D); break;
            case Base.P_LIFE: heart(g, cx, cy, r); break;
            case Base.P_WEAPON: g.rect(cx - r, cy - r, 2 * r, 2 * r, 0xFF2E4BA8); frame(g, cx - r, cy - r, 2 * r, 2 * r, App.GOLD); if (cs >= 10) app.txt(g, "A", cx, cy + 3, 8, App.GOLD, 0); break;
            case Base.START: g.rect(cx - r * .7f, cy - r, 1.5f, 2 * r, App.INK); g.rect(cx - r * .6f, cy - r, r * 1.3f, r * .8f, App.MINT); if (cs >= 14) app.txtS(g, "INICIO", cx, y - 2, 8, App.MINT, 0); break;
            default:
        }
    }
    static void heart(Gfx g, float cx, float cy, float r) { g.rect(cx - r, cy - r * .5f, r, r * .6f, App.PINK); g.rect(cx, cy - r * .5f, r, r * .6f, App.PINK); g.rect(cx - r * .7f, cy, r * 1.4f, r * .4f, App.PINK); g.rect(cx - r * .3f, cy + r * .4f, r * .6f, r * .3f, App.PINK); }
    /** Barras: herramientas y acciones arriba; nombre y ayuda debajo; lo propio de cada herramienta abajo. */
    void bars(Gfx g, float W, float H) {
        g.rect(0, 0, W, TOP, 0xE6101330); g.rect(0, TOP - 1, W, 1, App.INK);
        boolean small = W < 330; float aw = small ? 30 : 42, acts = 3 * aw, tw = Math.min(46, (W - acts - 8) / TOOLS);
        for (int i = 0; i < TOOLS; i++) {
            final int t = i; float x = 3 + i * tw; boolean sel = t == tool;
            app.box(g, x, 2, tw - 1, 20, sel ? App.GOLD : App.BLUE, sel ? App.CREAM : App.SKY, sel ? 0xFFB8862A : App.NAVY);
            app.label(g, TOOL_NAMES[i], x, 2, tw - 1, 20, sel ? App.DEEP : App.CREAM, sel ? 0 : App.DEEP);
            app.btn(x, 0, tw, TOP, () -> { tool = t; confirmDelete = false; });
        }
        float ax = W - acts - 2;
        act(g, ax, aw, small ? "JUGAR" : "PROBAR", App.MINT, () -> app.testBase());
        act(g, ax + aw, aw, "SALIR", App.BLUE, () -> app.leaveBaseEditor(false));
        act(g, ax + 2 * aw, aw, confirmDelete ? "¿SEGURO?" : "BORRAR", App.RED, () -> { if (confirmDelete) app.leaveBaseEditor(true); else confirmDelete = true; });
        // nombre (tocar para renombrar), deshacer y la ayuda
        g.rect(0, TOP, W, 16, 0xE6101330); float nw = Math.min(W * .3f, PixelFont.width(b.name, 8) + 10); app.box(g, 2, TOP + 2, nw, 13, App.NAVY, App.BLUE, App.DEEP); app.label(g, b.name, 2, TOP + 2, nw, 13, App.GOLD, 0);
        app.btn(2, TOP + 2, nw, 13, () -> app.renameBase());
        float ux0 = W - 44; app.chip(g, ux0, TOP + 2, 20, 13, "", !undo.isEmpty(), () -> restore(undo, redo)); arrowIcon(g, ux0 + 10, TOP + 8, -1);
        app.chip(g, ux0 + 22, TOP + 2, 20, 13, "", !redo.isEmpty(), () -> restore(redo, undo)); arrowIcon(g, ux0 + 32, TOP + 8, 1);
        String hint = confirmDelete ? "TOCA OTRA VEZ PARA BORRAR LA BASE" : HINT[tool]; float hw = ux0 - nw - 10;
        app.label(g, hint, nw + 6, TOP + 1, hw, 14, confirmDelete ? App.RED : App.CREAM, 0);
        if (app.toastT > 0) { g.rect(0, TOP + 16, W, 12, App.MINT); app.txtS(g, app.toast, W / 2, TOP + 25, 8, App.CREAM, 0); }
        // barra de abajo
        float by = H - BOTTOM; g.rect(0, by, W, BOTTOM, 0xE6101330); g.rect(0, by, W, 1, App.INK);
        switch (tool) {
            case PIECE: {
                float cw = Math.min(44, (W - 6) / MATS.length);
                for (int i = 0; i < MATS.length; i++) { final int k = i; String n = Base.TILE_NAMES[MATS[i]]; boolean sel = i == mat; float x = 3 + i * cw;
                    app.box(g, x, by + 2, cw - 2, 12, sel ? App.GOLD : matColor(MATS[i], COLORS[color]), sel ? App.CREAM : 0x55FFFFFF, App.DEEP);
                    app.label(g, n, x, by + 2, cw - 2, 12, sel ? App.DEEP : App.CREAM, sel ? 0 : App.DEEP); app.btn(x, by + 1, cw, 14, () -> mat = k); }
                float y2 = by + 16; boolean colors = MATS[mat] == Base.T_EARTH || MATS[mat] == Base.T_WOOD || MATS[mat] == Base.T_BACK, moves = Base.movable(MATS[mat]);
                float mw = moves ? 84 : 0;
                if (colors) for (int i = 0; i < COLORS.length; i++) { final int k = i; float x = 3 + i * 13; if (x + 12 > W - mw - 6) break;
                    if (i == color) g.rect(x - 1, y2 - 1, 13, 13, App.GOLD); g.rect(x, y2, 11, 11, COLORS[i]); app.btn(x, y2, 13, 12, () -> color = k); }
                else app.txt(g, "SIN COLOR", 6, y2 + 9, 8, App.MUTE, -1);
                if (moves) app.chip(g, W - mw - 3, y2, mw, 12, "MOVIMIENTO: " + Base.MOVE_NAMES[move], move != 0, () -> move = (move + 1) % Base.MOVES);
                break; }
            case ENEMY: row(g, by, ENEMY_NAMES, enemy, k -> enemy = k); app.label(g, ENEMY_HINT[enemy], 4, by + 17, W - 8, 12, App.MUTE, 0); break;
            case PRIZE: row(g, by, PRIZE_NAMES, prize, k -> prize = k); break;
            case ROOM: {
                String[] l = {"FONDO: " + Base.BG_NAMES[b.bg], selRoom != null && selRoom[2] == 1 ? "QUITAR JEFE" : "SALA DEL JEFE", "QUITAR SALA"};
                Runnable[] a = {() -> { begin(); b.bg = (b.bg + 1) % Base.BG_NAMES.length; end(); },
                    () -> { if (selRoom == null) { app.toast = "PRIMERO TOCA UNA HABITACIÓN"; app.toastT = 100; return; } begin(); b.toggleBoss(selRoom[0], selRoom[1]); end(); },
                    () -> { if (selRoom == null) { app.toast = "PRIMERO TOCA UNA HABITACIÓN"; app.toastT = 100; return; } begin(); if (!b.removeRoom(selRoom[0], selRoom[1])) { app.toast = b.rooms.size() <= 1 ? "LA BASE NECESITA AL MENOS UNA HABITACIÓN" : "NO PUEDE QUEDAR UNA HABITACIÓN SUELTA"; app.toastT = 140; } else selRoom = null; end(); }};
                float cw = (W - 6) / 3; for (int i = 0; i < 3; i++) app.chip(g, 3 + i * cw, by + 3, cw - 3, 12, l[i], i == 1 && selRoom != null && selRoom[2] == 1, a[i]);
                String info = b.rooms.size() + " DE " + Base.MAX_ROOMS + " HABITACIONES · " + (b.canOnline() ? "LISTA PARA JUGAR EN LÍNEA" : "EN LÍNEA: LA SALA DEL JEFE DEBE IR AL FINAL, A LA DERECHA");
                app.label(g, info, 4, by + 17, W - 8, 12, b.canOnline() ? App.MINT : App.MUTE, 0); break; }
            default: app.label(g, HINT[tool], 4, by + 8, W - 8, 14, App.MUTE, 0);
        }
    }
    void row(Gfx g, float by, String[] names, int sel, java.util.function.IntConsumer pick) {
        float cw = (app.W - 6) / names.length;
        for (int i = 0; i < names.length; i++) { final int k = i; app.chip(g, 3 + i * cw, by + 3, cw - 3, 13, names[i], i == sel, () -> pick.accept(k)); }
    }
    void act(Gfx g, float x, float w, String t, int c, Runnable r) {
        app.box(g, x, 2, w - 2, 20, c, 0x88FFFFFF, App.DEEP); app.label(g, t, x, 2, w - 2, 20, App.CREAM, App.DEEP); app.btn(x, 0, w, TOP, r);
    }
    static void arrowIcon(Gfx g, float cx, float cy, int dir) { for (int i = 0; i < 4; i++) g.rect(Math.round(cx + (dir > 0 ? -2 + i : 2 - i)), Math.round(cy - 3 + i), 1, 7 - 2 * i, App.CREAM); }
}
