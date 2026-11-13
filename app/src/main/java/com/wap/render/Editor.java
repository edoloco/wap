package com.wap.render;
import com.wap.core.*;
import java.util.ArrayList;
import java.util.HashMap;
/**
 * Editor de niveles. Con el dedo: un dedo dibuja, dos dedos mueven la vista y el pellizco hace zoom.
 * Con el mando: la cruceta mueve un cursor, A aplica (en las formas: apretar marca el inicio y soltar las dibuja),
 * X cambia de herramienta y B sale.
 * Herramientas de dibujo: pincel, pixel (celdas de una grilla, color parejo), puntos (polígono punto a punto, como
 * en un programa vectorial), línea, cuadrado, círculo, triángulo, balde y borrar. Materiales: tierra, madera, acero
 * (no se rompe), fondo (más claro, no choca, queda detrás de todo) y sobre (más oscuro, no choca, queda encima de todo).
 * Objetos (en su propio panel): bloque, bloque de acero, caja, metal, computador, piedra, ducto y agua.
 */
final class Editor {
    static final int BRUSH = 0, PIXEL = 1, POINTS = 2, LINE = 3, RECT = 4, OVAL = 5, TRI = 6, BUCKET = 7, ERASE = 8,
        PANEL = 9, STEEL = 10, CRATE = 11, METAL = 12, PC = 13, STONE = 14, VENT = 15, WATER = 16, TOOLS = 17, DRAW_TOOLS = 9;
    static final String[] NAMES = {"PINCEL", "PIXEL", "PUNTOS", "LÍNEA", "CUADRADO", "CÍRCULO", "TRIÁNGULO", "BALDE", "BORRAR",
        "BLOQUE", "BLOQUE DE ACERO", "CAJA", "METAL", "COMPUTADOR", "PIEDRA", "DUCTO", "AGUA"};
    static final String[] HINT = {"UN DEDO DIBUJA · DOS DEDOS MUEVEN Y HACEN ZOOM", "PINTA CUADRITOS DE LA GRILLA, COLOR PAREJO", "TOCA PARA PONER PUNTOS · ARRASTRA UN PUNTO PARA MOVERLO · TOCA EL PRIMERO PARA CERRAR",
        "ARRASTRA PARA HACER UNA LÍNEA", "ARRASTRA PARA HACER UN CUADRADO", "ARRASTRA PARA HACER UN CÍRCULO", "ARRASTRA PARA HACER UN TRIÁNGULO",
        "TOCA UNA ZONA PARA PINTARLA O UN HUECO CERRADO PARA LLENARLO", "BORRA LO QUE TOQUES", "BLOQUES MORADOS, SE PUEDEN ROMPER", "NO SE PUEDE ROMPER", "TOCA PARA PONER UNA CAJA", "CAJA DE METAL, MÁS RESISTENTE",
        "TOCA PARA PONER UN COMPUTADOR", "BLOQUE DE PIEDRA, MUY PESADO", "DUCTO DE VENTILACIÓN", "TOCA PARA ELEGIR LA ALTURA DEL AGUA"};
    /** Materiales para pincel, pixel, puntos, formas y balde. */
    static final int[] MATS = {Terrain.EARTH, Terrain.WOOD, Terrain.STEEL, Terrain.BACKDROP, Terrain.OVERLAY};
    static final String[] MAT_NAMES = {"TIERRA", "MADERA", "ACERO", "FONDO", "SOBRE"};
    static final String[] MAT_HINT = {"SE ROMPE, CON VETAS", "SE ROMPE POR TROZOS", "NO SE ROMPE", "NO CHOCA · DETRÁS DE TODO", "NO CHOCA · ENCIMA DE TODO"};
    /** Paleta: los 11 de antes y 29 más. */
    static final int[] COLORS = {0xFF8B5A2B, 0xFFD9B26F, 0xFFB5482E, 0xFF5E8C3A, 0xFF7D8590, 0xFFE8EEF4, 0xFF3E6FB0, 0xFFB04FD8, 0xFFC8955A, 0xFF6B4226, 0xFF1E1E24,
        0xFFFFFFFF, 0xFFB8BEC8, 0xFF4A505A, 0xFF9AA7B8, 0xFFE03C3C, 0xFF8E1F2A, 0xFFFF7A2A, 0xFFFFB347, 0xFFFFD93D, 0xFFF2E6A0,
        0xFFA6D94A, 0xFF3FB34F, 0xFF1F6B3A, 0xFF6E7A3A, 0xFF4FD1A8, 0xFF2BB3C0, 0xFF7FD6FF, 0xFF2850C8, 0xFF1A2A6C, 0xFF6C4FD8,
        0xFF5A2A8C, 0xFFE07AD8, 0xFFFF9EC4, 0xFFC8325F, 0xFFF6D2B0, 0xFFD69A6A, 0xFF8D5A3C, 0xFFA0522D, 0xFF3A2A1E};
    static final int[] SIZES = {4, 9, 18}, CELLS = {4, 8, 16};
    static final String[] ERASE_NAMES = {"BORRA: MAPA", "BORRA: FONDO", "BORRA: SOBRE"};
    final App app; World w; String name; int tool = BRUSH, color, size = 1, mat, eraseLayer; boolean fill = true, dirty, confirmDelete, pixErase;
    /** Panel abierto: 0 ninguno, 1 materiales y colores, 2 objetos. */
    int panel;
    /** Últimos colores usados (índices de COLORS), el más reciente primero. */
    final ArrayList<Integer> recent = new ArrayList<>(java.util.Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    /** Puntos del polígono que se está armando (coordenadas del mapa). */
    final ArrayList<float[]> pts = new ArrayList<>();
    float cx = 900, cy = 600;
    Editor(App a) { app = a; }

    void open(Level l) {
        name = l.name; Wpn[] k = new Wpn[Robot.SLOTS]; java.util.Arrays.fill(k, Wpn.ALL[0]);
        w = new World(1, 1, k, k, l); for (Robot r : w.robots) r.deadT = Integer.MAX_VALUE; // sin personajes
        app.ren.marks = true; app.ren.reset(w); app.ren.freeCam = true; app.ren.zoomMul = .7f; app.ren.camX = 900; app.ren.camY = 600; app.ren.shake = 0;
        dirty = false; confirmDelete = false; mode = NONE; fingers.clear(); pts.clear(); panel = 0; undo.clear(); redo.clear(); editing = false;
    }
    void close() { app.ren.marks = false; app.ren.freeCam = false; app.ren.zoomMul = 1; fingers.clear(); mode = NONE; pts.clear(); panel = 0; }
    Level level() { return Level.of(w, name); }
    int mat() { return MATS[mat]; }
    int col() { return COLORS[color]; }
    void pick(int c) { color = c; recent.remove((Integer) c); recent.add(0, c); while (recent.size() > 12) recent.remove(recent.size() - 1); }
    void setTool(int t) { if (t != tool) pts.clear(); tool = t; confirmDelete = false; }
    boolean shape() { return tool >= LINE && tool <= TRI; }
    boolean paints() { return tool == BRUSH || tool == ERASE || tool == PIXEL; }
    boolean usesMat() { return tool <= BUCKET; }
    /** Color con el que se ve el material elegido (el fondo más claro y el de encima más oscuro). */
    int shown(int c) { return mat == 3 ? Level.backTone(c) : mat == 4 ? Level.frontTone(c) : c; }

    // ---------- Aplicar herramientas en coordenadas del mapa
    void apply(float x, float y) {
        begin(); int ix = Math.round(x), iy = Math.round(y), r = SIZES[size];
        switch (tool) {
            case BRUSH: w.t.brush(ix, iy, r, mat(), col()); break;
            case PIXEL: w.t.cell((int) Math.floor(x), (int) Math.floor(y), CELLS[size], mat(), col(), pixErase); break;
            case ERASE: if (eraseLayer == 0) w.removeProps(ix, iy, r); w.t.wipe(ix, iy, r, eraseLayer == 1 ? Terrain.BACKDROP : eraseLayer == 2 ? Terrain.OVERLAY : 0); break;
            case BUCKET: if (!w.t.bucket(ix, iy, mat(), col())) { drop(); app.toast = "ESE HUECO NO ESTÁ CERRADO: LLEGA AL BORDE"; app.toastT = 120; return; } break;
            case PANEL: case STEEL: w.t.block(ix, iy, tool == STEEL ? Terrain.STEEL : Terrain.PANEL); break;
            case WATER: w.water = Math.max(Terrain.EDGE + 40, Math.min(Terrain.H - Terrain.EDGE - 8, iy)); break;
            case VENT: w.t.vent(ix, iy); break;
            case CRATE: case METAL: case PC: case STONE: w.addProp(w.t.object(ix, iy, tool == CRATE ? Terrain.CRATE : tool == METAL ? Terrain.METAL : tool == PC ? Terrain.COMPUTER : Terrain.STONE)); break;
            default: return;
        }
        dirty = true;
    }
    /** Trazo continuo del pincel, el pixel o la goma: rellena entre el punto anterior y el actual. */
    float lastX = Float.NaN, lastY;
    void strokeTo(float x, float y) {
        if (Float.isNaN(lastX) || tool == PANEL || tool == STEEL) { apply(x, y); lastX = x; lastY = y; return; }
        float step = tool == PIXEL ? Math.max(1, CELLS[size] * .5f) : Math.max(2, SIZES[size] * .5f);
        float dx = x - lastX, dy = y - lastY, d = (float) Math.hypot(dx, dy); int n = Math.max(1, (int) (d / step));
        for (int i = 1; i <= n; i++) apply(lastX + dx * i / n, lastY + dy * i / n);
        lastX = x; lastY = y;
    }
    /** Dibuja la forma entre dos puntos del mapa. */
    void commitShape(float x0, float y0, float x1, float y1) {
        int a = Math.round(x0), b = Math.round(y0), c = Math.round(x1), d = Math.round(y1), r = SIZES[size], m = mat(), col = col();
        if (Math.abs(c - a) + Math.abs(d - b) < 3 || !shape()) return;
        begin();
        switch (tool) {
            case LINE: w.t.line(a, b, c, d, r, m, col); break;
            case RECT: w.t.rect(a, b, c, d, fill, r, m, col); break;
            case OVAL: w.t.oval(a, b, c, d, fill, r, m, col); break;
            case TRI: w.t.tri(a, b, c, d, fill, r, m, col); break;
            default:
        }
        dirty = true; end();
    }
    // ---------- Puntos (polígono)
    /** Agrega un punto; si se toca el primero con 3 o más, cierra la figura y la dibuja. */
    void addPoint(float x, float y) {
        if (pts.size() >= 3 && near(pts.get(0), x, y)) { commitPoints(true); return; }
        if (!pts.isEmpty() && near(pts.get(pts.size() - 1), x, y)) return; // tocar el último otra vez no repite el punto
        if (pts.size() < 200) pts.add(new float[]{x, y});
    }
    /** Dibuja el polígono: relleno (3 puntos o más) o contorno (abierto, salvo que se haya cerrado tocando el primero). */
    void commitPoints(boolean closed) {
        int n = pts.size(); if (n < (fill ? 3 : 2)) { app.toast = fill ? "FALTAN PUNTOS: SON 3 O MÁS" : "FALTAN PUNTOS: SON 2 O MÁS"; app.toastT = 90; return; }
        int[] xy = new int[n * 2]; for (int i = 0; i < n; i++) { xy[2 * i] = Math.round(pts.get(i)[0]); xy[2 * i + 1] = Math.round(pts.get(i)[1]); }
        begin(); w.t.polygon(xy, fill, closed || fill, SIZES[size], mat(), col()); pts.clear(); dirty = true; end();
    }
    // ---------- Deshacer y rehacer: antes de cada cambio se guarda una copia del nivel (comprimida aparte, para no trabar)
    static final int MAX_UNDO = 30;
    final ArrayList<Snap> undo = new ArrayList<>(), redo = new ArrayList<>();
    boolean editing;
    static final java.util.concurrent.ExecutorService PACK = java.util.concurrent.Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "deshacer"); t.setDaemon(true); return t; });
    /** Copia del nivel; se comprime en segundo plano. */
    static final class Snap {
        final java.util.concurrent.Future<byte[]> data;
        Snap(Level l) { data = PACK.submit(l::save); }
        Level level() { try { return Level.load(data.get()); } catch (Exception e) { return null; } }
    }
    /** Antes del primer cambio de un trazo o una acción: guarda cómo estaba. */
    void begin() { if (editing) return; editing = true; undo.add(new Snap(Level.of(w, name))); if (undo.size() > MAX_UNDO) undo.remove(0); redo.clear(); }
    /** Termina el trazo o la acción. */
    void end() { editing = false; }
    /** La acción no cambió nada (balde en un hueco abierto): se olvida la copia. */
    void drop() { if (editing && !undo.isEmpty()) undo.remove(undo.size() - 1); editing = false; }
    void undo() { step(undo, redo); }
    void redo() { step(redo, undo); }
    void step(ArrayList<Snap> from, ArrayList<Snap> to) {
        if (from.isEmpty() || mode == DRAW) return; editing = false;
        Level l = from.remove(from.size() - 1).level(); if (l == null) return;
        to.add(new Snap(Level.of(w, name))); restore(l); dirty = true;
    }
    /** Carga una copia sin mover la vista. */
    void restore(Level l) {
        float x = app.ren.camX, y = app.ren.camY, z = app.ren.zoomMul;
        Wpn[] k = new Wpn[Robot.SLOTS]; java.util.Arrays.fill(k, Wpn.ALL[0]);
        w = new World(1, 1, k, k, l); for (Robot r : w.robots) r.deadT = Integer.MAX_VALUE;
        app.ren.reset(w); app.ren.freeCam = true; app.ren.camX = x; app.ren.camY = y; app.ren.zoomMul = z; app.ren.shake = 0; pts.clear();
    }
    /** Si un punto del mapa está bajo el dedo (unos 10 px de pantalla). */
    boolean near(float[] p, float x, float y) { return Math.hypot(p[0] - x, p[1] - y) * app.ren.zoom < 11 * app.uiScale; }
    int dragPt = -1;

    float wx(float sx) { return app.ren.vx0 + sx / app.ren.zoom; } float wy(float sy) { return app.ren.vy0 + sy / app.ren.zoom; }
    boolean inMap(float sy) { float y = sy / app.uiScale; return y > TOP && y < app.H - BOTTOM; }
    static final float TOP = 24, BOTTOM = 24;

    // ---------- Gestos: un dedo dibuja; con dos se mueve la vista y se hace zoom
    static final int NONE = 0, DRAW = 1, GESTURE = 2;
    final HashMap<Integer, float[]> fingers = new HashMap<>();
    int mode = NONE, drawId; boolean moved; float sx0, sy0, sx1, sy1;
    float gMid0x, gMid0y, gD0, gZ0, gWx, gWy;
    void pointer(int id, float x, float y, boolean down) {
        if (panel != 0) { if (!down) fingers.remove(id); mode = NONE; return; } // con un panel abierto, los toques son para el panel
        if (down) {
            boolean isNew = !fingers.containsKey(id); fingers.put(id, new float[]{x, y});
            if (isNew) {
                if (fingers.size() == 1 && inMap(y)) {
                    mode = DRAW; drawId = id; moved = false; sx0 = sx1 = x; sy0 = sy1 = y; lastX = Float.NaN; dragPt = -1;
                    if (tool == POINTS) for (int i = pts.size() - 1; i >= 0; i--) if (near(pts.get(i), wx(x), wy(y))) { dragPt = i; break; }
                }
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
        if (moved && tool == POINTS && dragPt >= 0 && dragPt < pts.size()) { float[] q = pts.get(dragPt); q[0] = wx(x); q[1] = wy(y); }
    }
    void drawEnd() {
        if (shape()) { if (moved) commitShape(wx(sx0), wy(sy0), wx(sx1), wy(sy1)); }
        else if (paints()) { if (!moved) apply(wx(sx0), wy(sy0)); }
        else if (tool == POINTS) { if (!moved || dragPt < 0) addPoint(wx(sx1), wy(sy1)); }
        else apply(wx(sx1), wy(sy1)); // herramientas de un toque
        lastX = Float.NaN; dragPt = -1; end();
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
        r.zoomMul = Math.max(.22f, Math.min(4f, gZ0 * dist(f) / gD0)); float z = base * r.zoomMul;
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
        if (panel != 0) { if ((press & Input.B) != 0) panel = 0; return; } // en un panel, la cruceta recorre sus botones
        if ((held & Input.LEFT) != 0) cx -= sp; if ((held & Input.RIGHT) != 0) cx += sp;
        if ((held & Input.UP) != 0) cy -= sp; if ((held & Input.DOWN) != 0) cy += sp;
        cx = Math.max(Terrain.EDGE, Math.min(Terrain.W - Terrain.EDGE, cx)); cy = Math.max(Terrain.EDGE, Math.min(Terrain.H - Terrain.EDGE, cy));
        if (dpad) { // la vista sigue al cursor solo mientras se mueve con la cruceta (antes lo hacía siempre y deshacía el zoom con los dedos)
            float vw = app.ren.vwNow(), vh = app.ren.vh, mx = vw * .3f, my = vh * .3f;
            if (cx < app.ren.camX - mx) app.ren.camX = cx + mx; if (cx > app.ren.camX + mx) app.ren.camX = cx - mx;
            if (cy < app.ren.camY - my) app.ren.camY = cy + my; if (cy > app.ren.camY + my) app.ren.camY = cy - my;
        }
        if ((press & Input.X) != 0) setTool((tool + 1) % TOOLS);
        boolean a = (held & Input.A) != 0;
        if (a && !aHeld) { gx0 = cx; gy0 = cy; lastX = Float.NaN; if (tool == POINTS) addPoint(cx, cy); else if (!shape() && !paints()) apply(cx, cy); }
        if (a && paints()) strokeTo(cx, cy);
        if (!a && aHeld && shape()) commitShape(gx0, gy0, cx, cy);
        if (!a && aHeld) end();
        aHeld = a;
        if ((press & Input.B) != 0) { if (tool == POINTS && !pts.isEmpty()) pts.remove(pts.size() - 1); else app.leaveEditor(false); }
    }

    // ---------- Interfaz (capa de pixel art)
    float ux(float x) { return (x - app.ren.vx0) * app.ren.zoom / app.uiScale; } float uy(float y) { return (y - app.ren.vy0) * app.ren.zoom / app.uiScale; }
    void draw(Gfx g) {
        float W = app.W, H = app.H, s = app.uiScale;
        if (tool == PIXEL) grid(g);
        preview(g, s);
        if (tool == POINTS) points(g);
        if (app.padSeen) { // cursor del mando
            int x = Math.round(ux(cx)), y = Math.round(uy(cy)); g.rect(x - 6, y, 13, 1, App.CREAM); g.rect(x, y - 6, 1, 13, App.CREAM); g.rect(x - 1, y - 1, 3, 3, App.RED);
        }
        // barra de herramientas: las de dibujo y un botón con los objetos
        g.rect(0, 0, W, TOP, 0xE6101330); g.rect(0, TOP - 1, W, 1, App.INK);
        boolean small = (W - 3 * 44 - 8) / (DRAW_TOOLS + 1) < 16; float aw = small ? 26 : 44; // pantallas angostas: acciones con ícono, para que quepan las herramientas
        float acts = 3 * aw, iw = Math.min(22, (W - acts - 8) / (DRAW_TOOLS + 1));
        for (int i = 0; i <= DRAW_TOOLS; i++) {
            float x = 3 + i * iw; boolean obj = i == DRAW_TOOLS, sel = obj ? tool >= DRAW_TOOLS || panel == 2 : i == tool; final int t = i;
            app.box(g, x, 2, iw - 1, 20, sel ? App.GOLD : App.BLUE, sel ? App.CREAM : App.SKY, sel ? 0xFFB8862A : App.NAVY);
            icon(g, obj ? (tool >= DRAW_TOOLS ? tool : CRATE) : i, x + (iw - 1) / 2, obj ? 10 : 12);
            if (obj) { g.rect(Math.round(x + (iw - 1) / 2f) - 3, 18, 1, 1, App.CREAM); g.rect(Math.round(x + (iw - 1) / 2f) - 1, 18, 1, 1, App.CREAM); g.rect(Math.round(x + (iw - 1) / 2f) + 1, 18, 1, 1, App.CREAM); }
            app.btn(x, 0, iw, TOP, obj ? () -> panel = panel == 2 ? 0 : 2 : () -> { setTool(t); panel = 0; });
        }
        float ax = W - acts - 2;
        if (small) {
            actionIcon(g, ax, 0, App.MINT, () -> app.testLevel());
            actionIcon(g, ax + aw, 1, App.BLUE, () -> app.leaveEditor(false));
            actionIcon(g, ax + 2 * aw, confirmDelete ? 3 : 2, App.RED, () -> { if (confirmDelete) app.leaveEditor(true); else confirmDelete = true; });
            if (confirmDelete) app.txtS(g, "TOCA OTRA VEZ PARA BORRAR", W - 4, TOP + 12, 8, App.RED, 1);
        } else {
            action(g, ax, "PROBAR", App.MINT, () -> app.testLevel());
            action(g, ax + 44, "SALIR", App.BLUE, () -> app.leaveEditor(false));
            action(g, ax + 88, confirmDelete ? "¿SEGURO?" : "BORRAR", App.RED, () -> { if (confirmDelete) app.leaveEditor(true); else confirmDelete = true; });
        }
        // nombre del nivel (tocar para renombrar) y herramienta actual
        float nw = PixelFont.width(name, 8) + 10;
        app.box(g, 2, TOP + 2, nw, 14, App.NAVY, App.BLUE, App.DEEP); app.txt(g, name, 7, TOP + 12, 8, App.GOLD, -1);
        app.btn(2, TOP + 2, nw, 14, () -> app.renameLevel());
        // deshacer y rehacer, a la derecha bajo la barra
        float ux0 = W - 52; undoBtn(g, ux0, false, !undo.isEmpty()); undoBtn(g, ux0 + 26, true, !redo.isEmpty());
        String tn = NAMES[tool] + (usesMat() ? " · " + MAT_NAMES[mat] : ""); app.txtS(g, tn, Math.max(W / 2, nw + 8 + PixelFont.width(tn, 8) / 2f), TOP + 12, 8, App.CREAM, 0);
        bottom(g, W, H);
        if (panel == 1) matPanel(g, W, H); else if (panel == 2) objPanel(g, W, H);
    }
    /** Barra inferior: material y colores, grosor o tamaño de celda, relleno y lo propio de cada herramienta. */
    void bottom(Gfx g, float W, float H) {
        float by = H - BOTTOM; g.rect(0, by, W, BOTTOM, 0xE6101330); g.rect(0, by, W, 1, App.INK);
        boolean sizes = tool == BRUSH || tool == ERASE || tool == LINE || tool == PIXEL || (shape() || tool == POINTS) && !fill;
        boolean fills = tool == RECT || tool == OVAL || tool == TRI || tool == POINTS;
        boolean narrow = W < 300; int bw = narrow ? 13 : 15, fw = narrow ? 54 : 62;
        float right = 4 + (sizes ? 3 * (bw + 2) + 3 : 0) + (fills ? fw + 4 : 0) + (tool == PIXEL ? 54 : 0) + (tool == POINTS ? 80 : 0) + (tool == WATER ? 76 : 0);
        float x = 4;
        if (usesMat()) {
            int c = shown(col()), mw = narrow ? 46 : 58;
            app.box(g, x, by + 3, mw, 18, c, RobotArt.mixLight(c), RobotArt.mixDark(c)); app.txtS(g, MAT_NAMES[mat], x + mw / 2f, by + 15, 8, App.CREAM, 0);
            app.btn(x, by, mw, BOTTOM, () -> panel = 1); x += mw + 3;
            boolean plus = W - x - right >= 40; int n = (int) Math.max(0, Math.min(recent.size(), Math.floor((W - x - right - (plus ? 30 : 0)) / 12)));
            for (int i = 0; i < n; i++) {
                final int ci = recent.get(i); int cc = shown(COLORS[ci]);
                if (ci == color) app.box(g, x - 1, by + 2, 12, 20, App.GOLD, App.CREAM, 0xFFB8862A);
                app.box(g, x + 1, by + 4, 8, 16, cc, RobotArt.mixLight(cc), RobotArt.mixDark(cc));
                app.btn(x - 1, by, 12, BOTTOM, () -> pick(ci)); x += 12;
            }
            if (plus) { app.box(g, x + 1, by + 3, 22, 18, App.BLUE, App.SKY, App.NAVY); app.txtS(g, "+", x + 12, by + 15, 8, App.CREAM, 0); // más colores
                app.btn(x, by, 24, BOTTOM, () -> panel = 1); x += 28; }
        } else if (tool == ERASE) {
            app.box(g, x, by + 3, 84, 18, eraseLayer == 0 ? App.BLUE : 0xFF4A5A8A, App.SKY, App.NAVY); app.txtS(g, ERASE_NAMES[eraseLayer], x + 42, by + 15, 8, App.CREAM, 0);
            app.btn(x, by, 84, BOTTOM, () -> eraseLayer = (eraseLayer + 1) % 3); x += 88;
        }
        if (sizes) {
            for (int i = 0; i < 3; i++) {
                final int k = i; boolean sel = i == size;
                app.box(g, x, by + 3, bw, 18, sel ? App.GOLD : App.BLUE, sel ? App.CREAM : App.SKY, sel ? 0xFFB8862A : App.NAVY);
                int d = 2 + i * 2; if (tool == PIXEL) { g.rect(Math.round(x + bw / 2f - d / 2f), Math.round(by + 12 - d / 2f), d, d, sel ? App.INK : App.CREAM); g.rect(Math.round(x + bw / 2f - d / 2f) + 1, Math.round(by + 12 - d / 2f) + 1, Math.max(1, d - 2), Math.max(1, d - 2), sel ? App.GOLD : App.BLUE); }
                else g.rect(Math.round(x + bw / 2f - d / 2f), Math.round(by + 12 - d / 2f), d, d, sel ? App.INK : App.CREAM);
                app.btn(x, by, bw, BOTTOM, () -> size = k); x += bw + 2;
            }
            x += 3;
        }
        if (fills) {
            app.box(g, x, by + 3, fw, 18, App.BLUE, App.SKY, App.NAVY); app.txtS(g, fill ? "RELLENO" : "CONTORNO", x + fw / 2f, by + 15, 8, App.CREAM, 0);
            app.btn(x, by, fw, BOTTOM, () -> fill = !fill); x += fw + 4;
        }
        if (tool == PIXEL) {
            app.box(g, x, by + 3, 50, 18, pixErase ? App.RED : App.BLUE, App.SKY, App.NAVY); app.txtS(g, pixErase ? "BORRAR" : "PINTAR", x + 25, by + 15, 8, App.CREAM, 0);
            app.btn(x, by, 50, BOTTOM, () -> pixErase = !pixErase); x += 54;
        }
        if (tool == POINTS) {
            app.box(g, x, by + 3, 36, 18, pts.isEmpty() ? 0xFF3A4370 : App.BLUE, App.SKY, App.NAVY); app.txtS(g, "ATRÁS", x + 18, by + 15, 8, pts.isEmpty() ? App.MUTE : App.CREAM, 0);
            app.btn(x, by, 36, BOTTOM, () -> { if (!pts.isEmpty()) pts.remove(pts.size() - 1); }); x += 38;
            boolean ok = pts.size() >= (fill ? 3 : 2);
            app.box(g, x, by + 3, 40, 18, ok ? App.MINT : 0xFF3A4370, ok ? RobotArt.mixLight(App.MINT) : App.SKY, ok ? RobotArt.mixDark(App.MINT) : App.NAVY); app.txtS(g, "LISTO", x + 20, by + 15, 8, ok ? App.CREAM : App.MUTE, 0);
            app.btn(x, by, 40, BOTTOM, () -> commitPoints(false)); x += 42;
        }
        if (tool == WATER) {
            app.box(g, x, by + 3, 70, 18, w.water < 0 ? App.GOLD : App.BLUE, App.SKY, App.NAVY);
            app.txt(g, "SIN AGUA", x + 35, by + 15, 8, w.water < 0 ? App.INK : App.CREAM, 0);
            app.btn(x, by, 70, BOTTOM, () -> { if (w.water >= 0) { begin(); w.water = -1; dirty = true; end(); } }); x += 76;
        }
        String h = usesMat() && tool != POINTS && tool != PIXEL ? MAT_HINT[mat] : HINT[tool]; if (PixelFont.width(h, 8) < W - x - 8) app.txt(g, h, (x + W) / 2, by + 15, 8, App.MUTE, 0);
    }
    /** Panel de materiales y la paleta completa. */
    void matPanel(Gfx g, float W, float H) {
        app.btn(0, 0, W, H, () -> panel = 0); // tocar fuera lo cierra
        int cols = 10, cs = 16, pw = Math.max(cols * cs, 5 * 46) + 12, ph = 12 + 22 + 10 + 4 * cs + 8 + 22 + 6;
        float px = Math.round((W - pw) / 2f), py = Math.round(Math.max(TOP + 2, (H - ph) / 2f));
        app.box(g, px, py, pw, ph, 0xF0101330, App.BLUE, App.INK); app.btn(px, py, pw, ph, () -> {}); // dentro no cierra
        app.txt(g, "MATERIAL", px + 6, py + 10, 8, App.GOLD, -1);
        for (int i = 0; i < MATS.length; i++) {
            final int k = i; float bx = px + 6 + i * 46; boolean sel = i == mat; int c = i == 3 ? Level.backTone(col()) : i == 4 ? Level.frontTone(col()) : col();
            app.box(g, bx, py + 14, 44, 20, sel ? App.GOLD : c, sel ? App.CREAM : RobotArt.mixLight(c), sel ? 0xFFB8862A : RobotArt.mixDark(c));
            app.txtS(g, MAT_NAMES[i], bx + 22, py + 27, 8, sel ? App.INK : App.CREAM, 0);
            app.btn(bx, py + 14, 44, 20, () -> mat = k);
        }
        app.txt(g, "COLOR", px + 6, py + 44, 8, App.GOLD, -1); app.txt(g, MAT_HINT[mat], px + pw - 6, py + 44, 8, App.MUTE, 1);
        float gx = px + (pw - cols * cs) / 2f, gy = py + 48;
        for (int i = 0; i < COLORS.length; i++) {
            final int k = i; float x = gx + i % cols * cs, y = gy + i / cols * cs; int c = shown(COLORS[i]);
            if (i == color) app.box(g, x, y, cs, cs, App.GOLD, App.CREAM, 0xFFB8862A);
            app.box(g, x + 2, y + 2, cs - 4, cs - 4, c, RobotArt.mixLight(c), RobotArt.mixDark(c));
            app.btn(x, y, cs, cs, () -> { pick(k); panel = 0; });
        }
        float ly = gy + 4 * cs + 6;
        app.box(g, px + pw / 2f - 30, ly, 60, 20, App.MINT, RobotArt.mixLight(App.MINT), RobotArt.mixDark(App.MINT)); app.txtS(g, "LISTO", px + pw / 2f, ly + 13, 8, App.CREAM, 0);
        app.btn(px + pw / 2f - 30, ly, 60, 20, () -> panel = 0);
    }
    /** Panel de objetos: bloques, cajas, computador, ducto y agua. */
    void objPanel(Gfx g, float W, float H) {
        app.btn(0, 0, W, H, () -> panel = 0);
        int cols = W >= 300 ? 4 : 3, rows = (8 + cols - 1) / cols, bw = 70, bh = 34, pw = cols * (bw + 2) - 2 + 12, ph = rows * (bh + 2) - 2 + 26;
        float px = Math.round((W - pw) / 2f), py = TOP + 4;
        app.box(g, px, py, pw, ph, 0xF0101330, App.BLUE, App.INK); app.btn(px, py, pw, ph, () -> {});
        app.txt(g, "OBJETOS", px + 6, py + 10, 8, App.GOLD, -1);
        for (int i = 0; i < 8; i++) {
            final int t = PANEL + i; float x = px + 6 + i % cols * (bw + 2), y = py + 16 + i / cols * (bh + 2); boolean sel = t == tool;
            app.box(g, x, y, bw, bh, sel ? App.GOLD : App.BLUE, sel ? App.CREAM : App.SKY, sel ? 0xFFB8862A : App.NAVY);
            icon(g, t, x + bw / 2f, y + 12);
            String n = t == STEEL ? "ACERO" : NAMES[t]; app.txtS(g, n, x + bw / 2f, y + bh - 5, 8, sel ? App.INK : App.CREAM, 0);
            app.btn(x, y, bw, bh, () -> { setTool(t); panel = 0; });
        }
    }
    /** Grilla del pixel art, solo si las celdas se ven de 3 px o más. */
    void grid(Gfx g) {
        Renderer r = app.ren; float cs = CELLS[size], k = cs * r.zoom / app.uiScale; if (k < 3) return;
        float x0 = (float) Math.floor(r.vx0 / cs) * cs, y0 = (float) Math.floor(r.vy0 / cs) * cs; int c = k >= 6 ? 0x40FFFFFF : 0x26FFFFFF;
        for (float x = x0; x < r.vx0 + r.vw + cs; x += cs) { float u = ux(x); g.rect(Math.round(u), TOP, 1, app.H - TOP - BOTTOM, c); }
        for (float y = y0; y < r.vy0 + r.vh + cs; y += cs) { float v = uy(y); if (v > TOP && v < app.H - BOTTOM) g.rect(0, Math.round(v), app.W, 1, c); }
    }
    /** Puntos y líneas del polígono que se está armando. */
    void points(Gfx g) {
        int n = pts.size(); if (n == 0) return;
        for (int i = 0; i + 1 < n; i++) g.line(ux(pts.get(i)[0]), uy(pts.get(i)[1]), ux(pts.get(i + 1)[0]), uy(pts.get(i + 1)[1]), App.CREAM, 1);
        if (n >= 3 && fill) { float[] a = pts.get(n - 1), b = pts.get(0); float ax = ux(a[0]), ay = uy(a[1]), bx = ux(b[0]), by = uy(b[1]); // cierre en línea punteada
            int seg = (int) Math.max(1, Math.hypot(bx - ax, by - ay) / 4); for (int i = 0; i < seg; i += 2) g.line(ax + (bx - ax) * i / seg, ay + (by - ay) * i / seg, ax + (bx - ax) * (i + 1) / seg, ay + (by - ay) * (i + 1) / seg, 0xAAFFF4D6, 1); }
        for (int i = 0; i < n; i++) { int x = Math.round(ux(pts.get(i)[0])), y = Math.round(uy(pts.get(i)[1])); boolean first = i == 0;
            int d = first && n >= 3 ? 5 : 3; g.rect(x - d / 2 - 1, y - d / 2 - 1, d + 2, d + 2, App.INK); g.rect(x - d / 2, y - d / 2, d, d, first ? App.GOLD : App.CREAM); }
    }
    /** Vista previa de la forma mientras se arrastra. */
    void preview(Gfx g, float s) {
        float x0, y0, x1, y1;
        if (mode == DRAW && shape() && moved) { x0 = sx0 / s; y0 = sy0 / s; x1 = sx1 / s; y1 = sy1 / s; }
        else if (aHeld && shape() && app.padSeen) { x0 = ux(gx0); y0 = uy(gy0); x1 = ux(cx); y1 = uy(cy); }
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
    /** Acción con ícono (pantallas angostas): 0 probar (play), 1 salir y guardar (flecha), 2 borrar (basurero), 3 confirmar (?). */
    void actionIcon(Gfx g, float x, int k, int c, Runnable r) {
        app.box(g, x, 2, 24, 20, c, RobotArt.mixLight(c), RobotArt.mixDark(c)); int cx = Math.round(x + 12), cy = 12, w = App.CREAM;
        switch (k) {
            case 0: for (int j = -4; j <= 4; j++) g.rect(cx - 3, cy + j, 7 - Math.abs(j) * 3 / 2, 1, w); break;
            case 1: g.rect(cx - 4, cy, 9, 1, w); for (int j = 1; j <= 3; j++) { g.rect(cx - 4 + j, cy - j, 1, 1, w); g.rect(cx - 4 + j, cy + j, 1, 1, w); } break;
            case 2: g.rect(cx - 4, cy - 4, 9, 1, w); g.rect(cx - 1, cy - 5, 3, 1, w); g.rect(cx - 3, cy - 2, 7, 7, w); g.rect(cx - 1, cy - 1, 1, 5, c); g.rect(cx + 1, cy - 1, 1, 5, c); break;
            default: app.txtS(g, "?", cx, cy + 3, 8, w, 0);
        }
        app.btn(x, 0, 24, TOP, r);
    }
    static final String[] ARROW = {"..X......", ".XX......", "XXXXXXX..", ".XX....X.", "..X....X.", ".......X.", "...XXXX.."};
    /** Botón con flecha curva: hacia la izquierda deshace, hacia la derecha rehace. */
    void undoBtn(Gfx g, float x, boolean right, boolean on) {
        float y = TOP + 2; int c = on ? App.CREAM : App.MUTE;
        app.box(g, x, y, 24, 16, on ? App.BLUE : 0xFF3A4370, App.SKY, App.NAVY);
        int x0 = Math.round(x + 12) - 4, y0 = Math.round(y + 8) - 4;
        for (int r = 0; r < ARROW.length; r++) for (int q = 0; q < 9; q++) if (ARROW[r].charAt(q) == 'X') g.rect(x0 + (right ? 8 - q : q), y0 + r, 1, 1, c);
        app.btn(x, y, 24, 16, right ? this::redo : this::undo);
    }
    /** Íconos de 14 px para cada herramienta. */
    void icon(Gfx g, int t, float cx, float cy) {
        int x = Math.round(cx), y = Math.round(cy), m = shown(col());
        switch (t) {
            case BRUSH: g.rect(x - 1, y - 6, 3, 7, 0xFFD9B26F); g.rect(x - 2, y + 1, 5, 4, m); g.rect(x - 1, y + 5, 3, 1, m); break;
            case PIXEL: for (int j = 0; j < 3; j++) for (int i = 0; i < 3; i++) g.rect(x - 5 + i * 4, y - 5 + j * 4, 3, 3, (i + j) % 2 == 0 ? m : App.CREAM); break;
            case POINTS: { int[] p = {-5, 4, -1, -5, 5, -1, 2, 5}; for (int i = 0; i < 3; i++) g.line(x + p[2 * i], y + p[2 * i + 1], x + p[2 * i + 2], y + p[2 * i + 3], m, 1);
                for (int i = 0; i < 4; i++) g.rect(x + p[2 * i] - 1, y + p[2 * i + 1] - 1, 3, 3, App.CREAM); break; }
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
