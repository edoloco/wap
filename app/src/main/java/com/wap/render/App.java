package com.wap.render;
import com.wap.core.*;
import com.wap.net.*;
import java.util.ArrayList; import java.util.List;
/** Pantallas y flujo de la aplicación. No conoce Android; recibe entradas y dibuja por Gfx. */
public final class App {
    public static final int TITLE = 0, ARMS = 1, PLAY = 2, PAUSE = 3, OVER = 4, PAINT = 5, LOBBY = 6, EDIT = 7, LEVEL_LIST = 8, RIVALS = 9, LEVEL_SEL = 10;
    /** Servicios del teléfono para jugar en red. */
    public interface Device { String name(); void listen(boolean on);
        /** Pide un texto con el teclado del teléfono; done recibe el texto o null si se cancela. */
        default void askText(String title, String current, int max, java.util.function.Consumer<String> done) {}
        /** WiFi en modo de baja latencia durante la partida en línea (sin ahorro de energía). */
        default void lowLatency(boolean on) {}
        /** Cierra el juego. */
        default void exit() {} }
    static final int[] SWATCH = {0xFF5E5650, 0xFFFF0000, 0xFFFF7A00, 0xFFFFD200, 0xFF19C24A, 0xFF00B8D4, 0xFF1266FF, 0xFF8214FF, 0xFFFF2FA8, 0xFF2A2A2A};
    static final int RED = 0xFFE8323C, INK = RobotArt.INK, WHITE = 0xFFFFFFFF, MUTE = 0xFF9AA6D6, LINE = 0xFFCBD2DC;
    /** Paleta de la interfaz en pixel art. */
    static final int NAVY = 0xFF1B1F4A, BLUE = 0xFF2E4BA8, SKY = 0xFF5FA8FF, GOLD = 0xFFFFC83D, CREAM = 0xFFFFF4D6, MINT = 0xFF3DDC97, PINK = 0xFFFF6FA0, DEEP = 0xFF101330, PURPLE = 0xFF8214FF;
    float uiScale = 1;
    static final String[] LEVELS = {"FÁCIL", "NORMAL", "DIFÍCIL"}; static final float[] SKILL = {.45f, .9f, 1.6f};
    static final int[] LIVES = {3, 5, 10, 15, 30};
    static final String[] DEF = {"GRENADE", "BAZOOKA", "PLASMA CANNON", "PLASMA GUN"};
    static final int SLOTS = Robot.SLOTS;
    static String key(int i) { return "arma" + (i == 3 ? 4 : i); } // conserva lo elegido en versiones anteriores
    public int screen = TITLE, keys, last, focus; public boolean padSeen, startKey; public Updater up; public Device dev;
    String myName = ""; String[] names = {"PLAYER 1", "PLAYER 2"};
    /** Niveles guardados: archivos de la app. levelName es el elegido para jugar ("" = aleatorio). */
    public Files files = new Files.Memory(); String levelName = ""; final Editor editor = new Editor(this);
    /** Nivel que se está probando desde el editor (al terminar se vuelve al editor). */
    String testing; Level testLevel;
    public String levelName() { return levelName; }
    /** Nivel recibido del otro teléfono en una partida en línea, para ofrecer conservarlo al final. */
    Level received; boolean receivedSaved;
    // ---------- Rivales (solo contra la máquina): cantidad, modo, y por rival dificultad, armas y color o equipo
    static final int MAX_RIVALS = 4;
    static final int[] RIVAL_COLORS = {0xFF1266FF, 0xFFE0302A, 0xFF2BAA4A, 0xFFE8B21E, 0xFFB04FD8, 0xFFFF7A1A, 0xFF23C8C8, 0xFFEDEDED, 0xFF2A2A2A};
    static final int[] TEAM_COLORS = {0xFFE0302A, 0xFF1266FF, 0xFF2BAA4A, 0xFFE8B21E}; static final String[] TEAM_NAMES = {"ROJO", "AZUL", "VERDE", "AMARILLO"};
    int rivals = 1, myTeam; boolean teams;
    final int[] rLevel = {1, 1, 1, 1}, rColor = {0, 1, 2, 3}, rTeam = {1, 1, 1, 1}; final boolean[] rRandom = {true, true, true, true};
    final int[][] rSlots = new int[MAX_RIVALS][SLOTS];
    /** Para la pantalla de armas: -1 son las tuyas; 0..3, las de ese rival. */
    int armsFor = -1;
    int[] eslots() { return armsFor < 0 ? slots : rSlots[armsFor]; }
    Bot[] bots = new Bot[0];
    void loadRivals() {
        try {
            rivals = Math.max(1, Math.min(MAX_RIVALS, Integer.parseInt(st.get("rivales", "1")))); teams = st.get("equipos", "0").equals("1"); myTeam = Integer.parseInt(st.get("miequipo", "0")) & 3;
            for (int i = 0; i < MAX_RIVALS; i++) {
                String[] p = st.get("rival" + i, "").split(",");
                if (p.length < 8) { rLevel[i] = i == 0 ? level : 1; for (int k = 0; k < SLOTS; k++) rSlots[i][k] = Wpn.index(DEF[k]); continue; }
                rLevel[i] = Integer.parseInt(p[0]) % 3; rColor[i] = Integer.parseInt(p[1]) % RIVAL_COLORS.length; rTeam[i] = Integer.parseInt(p[2]) & 3; rRandom[i] = p[3].equals("1");
                for (int k = 0; k < SLOTS; k++) rSlots[i][k] = Math.floorMod(Integer.parseInt(p[4 + k]), Wpn.ALL.length);
            }
        } catch (Exception e) { }
    }
    void saveRivals() {
        st.put("rivales", "" + rivals); st.put("equipos", teams ? "1" : "0"); st.put("miequipo", "" + myTeam);
        for (int i = 0; i < MAX_RIVALS; i++) { StringBuilder b = new StringBuilder(rLevel[i] + "," + rColor[i] + "," + rTeam[i] + "," + (rRandom[i] ? 1 : 0)); for (int k = 0; k < SLOTS; k++) b.append(',').append(rSlots[i][k]); st.put("rival" + i, b.toString()); }
    }
    int myColor = 0xFF5E5650, me; boolean online, hosting; Lockstep ls; Link link; Lan.Browser browser; Lan.Beacon beacon; java.net.ServerSocket server;
    volatile Link pending; volatile Link.Hello pendHost, pendGuest; volatile String netMsg = ""; String toast = ""; int toastT; volatile boolean busy;
    final Store st; final Audio au; public final TouchPad pad = new TouchPad(); public final Renderer ren = new Renderer();
    public World w; Bot bot; float W = 640, H = 360, u = 1; int cd; int paintRow, level = 1, livesIx = 2, selSlot = 3, cur, seed = 12345, overT; long frames;
    final int[] slots = new int[SLOTS]; final int[] in = new int[2];
    final ArrayList<float[]> btns = new ArrayList<>(); final ArrayList<Runnable> acts = new ArrayList<>();
    final RobotArt.View hero = RobotArt.poster();

    public App(Store st, Audio au) {
        this.st = st; this.au = au;
        for (int i = 0; i < SLOTS; i++) { slots[i] = Wpn.index(st.get(key(i), DEF[i])); }
        try { myColor = 0xFF000000 | Integer.parseInt(st.get("traje", "5E5650"), 16); } catch (Exception e) { }
        myName = clean(st.get("nombre", "")); levelName = st.get("nivel", "");
        try { level = Integer.parseInt(st.get("level", "1")); livesIx = Integer.parseInt(st.get("lives", "2")); } catch (Exception e) { }
        loadRivals();
    }
    public void seed(int s) { seed = s; }
    public void start() {
        seed = seed * 1103515245 + 12345; Rng r = new Rng(seed ^ 0x5A5A);
        int n = 1 + rivals; Wpn[][] loads = new Wpn[n][SLOTS]; int[] team = new int[n];
        for (int k = 0; k < SLOTS; k++) loads[0][k] = Wpn.ALL[slots[k]];
        for (int i = 1; i < n; i++) for (int k = 0; k < SLOTS; k++) loads[i][k] = Wpn.ALL[rRandom[i - 1] ? r.i(Wpn.ALL.length) : rSlots[i - 1][k]];
        boolean oneTeam = true; for (int i = 0; i < rivals; i++) oneTeam &= rTeam[i] == myTeam;
        for (int i = 0; i < n; i++) team[i] = !teams ? i : i == 0 ? myTeam : oneTeam ? (myTeam + 1) % 4 : rTeam[i - 1]; // si todos quedan en tu equipo, los rivales pasan al siguiente
        for (int i = 0; i < n; i++) RobotArt.COLOR[i] = teams ? TEAM_COLORS[team[i]] : i == 0 ? myColor : RIVAL_COLORS[rColor[i - 1]];
        if (!teams) for (int i = 1; i < n; i++) { // si un rival se parece a ti o a otro, toma un color libre
            boolean clash = near(RobotArt.COLOR[i], myColor); for (int j = 1; j < i; j++) clash |= RobotArt.COLOR[j] == RobotArt.COLOR[i];
            if (!clash) continue;
            for (int c : RIVAL_COLORS) { boolean ok = !near(c, myColor); for (int j = 0; j < i; j++) ok &= !near(c, RobotArt.COLOR[j]); if (ok) { RobotArt.COLOR[i] = c; break; } }
        }
        me = 0; online = false;
        Level lv = testing != null ? testLevel : levelName.isEmpty() ? null : loadLevel(levelName);
        if (lv == null && testing == null && !levelName.isEmpty()) { levelName = ""; save(); } // el nivel elegido ya no existe
        w = new World(seed, LIVES[livesIx], loads, team, lv); ren.reset(w); cd = 90; screen = PLAY; overT = 0;
        bots = new Bot[n]; for (int i = 1; i < n; i++) bots[i] = new Bot(seed + i, i, SKILL[rLevel[i - 1]]);
        String[] nm = new String[n]; nm[0] = or(myName, "PLAYER 1"); for (int i = 1; i < n; i++) nm[i] = "PLAYER " + (i + 1);
        setNames(nm, n > 2); ren.teams = teams;
        pad.clear();
    }
    // ---------- Niveles guardados
    static String file(String name) { return name.replaceAll("[^A-Z0-9ÁÉÍÓÚÑÜ _-]", "_") + ".wapn"; }
    /** Nombres de los niveles guardados: se leen una vez (solo la cabecera de cada archivo) y se guardan en memoria. */
    List<String> namesCache;
    void levelsChanged() { namesCache = null; }
    public List<String> levelNames() {
        if (namesCache == null) {
            List<String> l = new ArrayList<>();
            for (String f : files.list()) if (f.endsWith(".wapn")) { byte[] d = files.read(f); String n = d == null ? null : Level.peekName(d); if (n != null) l.add(n); }
            java.util.Collections.sort(l); namesCache = l;
        }
        return new ArrayList<>(namesCache);
    }
    Level loadFile(String f) { try { byte[] d = files.read(f); return d == null ? null : Level.load(d); } catch (Exception e) { return null; } }
    Level loadLevel(String name) { return loadFile(file(name)); }
    /** Guarda el nivel y su captura chica (archivo .wapt), que es lo que muestra SELECCIONA NIVEL. */
    void saveLevel(Level l) {
        files.write(file(l.name), l.save()); forgetThumb(l.name); levelsChanged();
        int[] t = l.snapshot(THUMB, THUMB_H); byte[] b = new byte[t.length * 4];
        for (int i = 0; i < t.length; i++) { b[i * 4] = (byte) (t[i] >> 24); b[i * 4 + 1] = (byte) (t[i] >> 16); b[i * 4 + 2] = (byte) (t[i] >> 8); b[i * 4 + 3] = (byte) t[i]; }
        files.write(thumbFile(l.name), b);
    }
    static String thumbFile(String name) { return file(name).replace(".wapn", ".wapt"); }
    void deleteLevel(String name) { files.delete(file(name)); files.delete(thumbFile(name)); forgetThumb(name); levelsChanged(); }
    String unusedName(String base) { List<String> l = levelNames(); for (int i = 1; ; i++) { String n = base + " " + i; if (!l.contains(n)) return n; } }
    public void openEditor(Level l) { editor.open(l); go(EDIT); }
    /** Sale del editor: guarda (o borra) el nivel y vuelve a la lista. */
    public void leaveEditor(boolean delete) {
        if (delete) { deleteLevel(editor.name); if (editor.name.equals(levelName)) { levelName = ""; save(); } toast = "NIVEL BORRADO"; }
        else { saveLevel(editor.level()); toast = "NIVEL GUARDADO"; }
        toastT = 120; editor.close(); testing = null; go(LEVEL_LIST);
    }
    /** Prueba el nivel contra el rival; al terminar se vuelve al editor. */
    void testLevel() { Level l = editor.level(); saveLevel(l); testLevel = l; testing = l.name; editor.close(); start(); }
    void backToEditor() { Level l = testLevel; testing = null; testLevel = null; openEditor(l); }
    void renameLevel() {
        if (dev == null) return; final String old = editor.name;
        dev.askText("Nombre del nivel", old, NAME_MAX, n -> {
            if (n == null) return; n = clean(n); if (n.isEmpty() || n.equals(old)) return;
            if (levelNames().contains(n)) { toast = "YA EXISTE UN NIVEL CON ESE NOMBRE"; toastT = 150; return; }
            deleteLevel(old); editor.name = n; saveLevel(editor.level()); if (old.equals(levelName)) { levelName = n; save(); }
        });
    }
    /** Cambia el nivel para jugar: aleatorio y después los guardados. */
    public void nextLevel() {
        List<String> l = levelNames(); l.add(0, "");
        int i = l.indexOf(levelName); levelName = l.get((i + 1) % l.size()); save();
    }
    static final String NEW_EMPTY = "\u0001VACIO", NEW_RANDOM = "\u0001AZAR";
    /** EDITOR DE NIVELES: los mismos cuadritos con miniatura; los dos primeros crean un nivel nuevo. */
    void levels(Gfx g) {
        List<String> l = levelNames(); l.add(0, NEW_RANDOM); l.add(0, NEW_EMPTY);
        levelPage = grid(g, "EDITOR DE NIVELES", l, levelPage, null, n -> {
            if (n.equals(NEW_EMPTY)) openEditor(Level.empty(unusedName("NIVEL")));
            else if (n.equals(NEW_RANDOM)) { seed = seed * 1103515245 + 12345; openEditor(Level.random(seed, unusedName("NIVEL"))); }
            else { Level v = loadLevel(n); if (v != null) openEditor(v); }
        }, (l.size() - 2) + (l.size() - 2 == 1 ? " NIVEL GUARDADO" : " NIVELES GUARDADOS"));
        if (toastT > 0) { g.rect(0, 22, W, 14, MINT); txtS(g, toast, W / 2, 33, 8, CREAM, 0); }
    }
    int levelPage, selPage;
    /** Con mando, marca el botón elegido en pantallas con muchos botones. */
    void focusBox(Gfx g) {
        if (!padSeen || btns.isEmpty()) return; focus = Math.floorMod(focus, btns.size()); float[] b = btns.get(focus);
        int x = Math.round(b[0]) - 1, y = Math.round(b[1]) - 1, w2 = Math.round(b[2] - b[0]) + 2, h2 = Math.round(b[3] - b[1]) + 2;
        g.rect(x, y, w2, 1, GOLD); g.rect(x, y + h2 - 1, w2, 1, GOLD); g.rect(x, y, 1, h2, GOLD); g.rect(x + w2 - 1, y, 1, h2, GOLD);
    }
    /** Botón chico de texto. */
    void chip(Gfx g, float x, float y, float w, float h, String t, boolean on, Runnable r) {
        box(g, x, y, w, h, on ? GOLD : BLUE, on ? CREAM : SKY, on ? 0xFFB8862A : NAVY); txtS(g, t, x + w / 2, y + h / 2 + 3.5f, 8, on ? DEEP : CREAM, 0); btn(x, y, w, h, r);
    }
    void swatch(Gfx g, float x, float y, float w, float h, int c, String t, Runnable r) {
        box(g, x, y, w, h, c, RobotArt.mixLight(c), RobotArt.mixDark(c)); if (t != null) txtS(g, t, x + w / 2, y + h / 2 + 3.5f, 8, CREAM, 0); btn(x, y, w, h, r);
    }
    /** Pantalla RIVALES: cuántos, modo (todos contra todos o equipos) y por rival dificultad, armas y color o equipo. */
    void rivalsScreen(Gfx g) {
        sky(g);
        float cw = Math.min(W - 8, 400), x0 = (W - cw) / 2, y = 4;
        txtS(g, "RIVALES", W / 2, y + 13, 16, GOLD, 0); y += 22;
        txt(g, "CUÁNTOS", x0, y + 12, 8, CREAM, -1);
        for (int i = 1; i <= MAX_RIVALS; i++) { final int k = i; chip(g, x0 + 50 + (i - 1) * 20, y, 18, 17, "" + i, rivals == i, () -> rivals = k); }
        chip(g, x0 + 140, y, cw - 140, 17, teams ? "MODO  EQUIPOS" : "MODO  TODOS CONTRA TODOS", false, () -> teams = !teams); y += 21;
        if (teams) {
            txt(g, "TU EQUIPO", x0, y + 12, 8, CREAM, -1);
            swatch(g, x0 + 60, y, 80, 17, TEAM_COLORS[myTeam], TEAM_NAMES[myTeam], () -> myTeam = (myTeam + 1) % 4);
            txt(g, "LOS DEL MISMO EQUIPO NO SE HACEN DAÑO", x0 + 148, y + 12, 8, MUTE, -1); y += 21;
        }
        for (int i = 0; i < rivals; i++) {
            final int k = i; float rh = 21;
            box(g, x0, y, cw, rh, 0xE61B1F4A, BLUE, DEEP);
            int col = teams ? TEAM_COLORS[rTeam[i]] : RIVAL_COLORS[rColor[i]];
            swatch(g, x0 + 3, y + 3, teams ? 62 : 18, rh - 6, col, teams ? TEAM_NAMES[rTeam[i]] : null, () -> { if (teams) rTeam[k] = (rTeam[k] + 1) % 4; else rColor[k] = (rColor[k] + 1) % RIVAL_COLORS.length; });
            float lx = x0 + (teams ? 70 : 26);
            txt(g, "PLAYER " + (i + 2), lx, y + 14, 8, GOLD, -1);
            float bx = lx + 56, bw2 = (x0 + cw - 3 - bx) / 2 - 2;
            chip(g, bx, y + 3, bw2, rh - 6, LEVELS[rLevel[i]], false, () -> rLevel[k] = (rLevel[k] + 1) % 3);
            chip(g, bx + bw2 + 4, y + 3, bw2, rh - 6, rRandom[i] ? "ARMAS AL AZAR" : "ARMAS ELEGIDAS", false, () -> { armsFor = k; selSlot = 0; go(ARMS); });
            y += rh + 2;
        }
        float by = H - 22;
        box(g, W / 2 - 60, by, 120, 19, MINT, 0xFF9CF5C9, 0xFF1F8A5B); txtS(g, "LISTO", W / 2, by + 13, 8, CREAM, 0);
        btn(W / 2 - 60, by, 120, 19, () -> { saveRivals(); go(TITLE); });
    }
    // ---------- SELECCIONA NIVEL: cuadritos con miniatura y nombre
    static final int THUMB = 80, THUMB_H = 44;
    final java.util.HashMap<String, int[]> thumbs = new java.util.HashMap<>(); final java.util.HashMap<String, Integer> thumbIds = new java.util.HashMap<>(); final java.util.HashSet<Integer> thumbUp = new java.util.HashSet<>();
    void forgetThumb(String name) { thumbs.remove(name); Integer id = thumbIds.get(name); if (id != null) thumbUp.remove(id); }
    int[] thumb(String name) {
        int[] t = thumbs.get(name); if (t != null) return t;
        t = new int[THUMB * THUMB]; int[] snap = null;
        byte[] b = name.isEmpty() || name.startsWith("\u0001") ? null : files.read(thumbFile(name)); // la captura guardada junto al nivel
        if (b != null && b.length == THUMB * THUMB_H * 4) { snap = new int[THUMB * THUMB_H]; for (int i = 0; i < snap.length; i++) snap[i] = (b[i * 4] & 255) << 24 | (b[i * 4 + 1] & 255) << 16 | (b[i * 4 + 2] & 255) << 8 | b[i * 4 + 3] & 255; }
        if (snap == null) { // niveles guardados antes de esta versión (o el aleatorio): se hace la captura una vez
            Level l = name.isEmpty() ? Level.random(4242, "") : name.equals(NEW_EMPTY) ? Level.empty("") : loadLevel(name);
            if (l != null) { if (!name.isEmpty() && !name.startsWith("\u0001")) saveLevel(l); snap = l.snapshot(THUMB, THUMB_H); }
        }
        if (snap != null) System.arraycopy(snap, 0, t, 0, snap.length);
        thumbs.put(name, t); if (!thumbIds.containsKey(name)) thumbIds.put(name, 1_000_000 + thumbIds.size()); thumbUp.remove(thumbIds.get(name));
        return t;
    }
    void levelSel(Gfx g) {
        List<String> l = levelNames(); l.add(0, "");
        selPage = grid(g, "SELECCIONA NIVEL", l, selPage, levelName, n -> { levelName = n; save(); go(TITLE); }, l.size() - 1 == 0 ? "AÚN NO HAY NIVELES GUARDADOS" : (l.size() - 1) + " GUARDADOS + ALEATORIO");
    }
    int gridCols() { return Math.max(3, Math.min(5, (int) ((W - 12) / 96))); }
    int pageOf(String name) { List<String> l = levelNames(); l.add(0, ""); int i = Math.max(0, l.indexOf(name)); return i / (gridCols() * 2); }
    /**
     * Cuadritos con miniatura y nombre, por páginas (2 filas). Se cambia de página con las flechas, tocando los puntitos
     * de abajo o deslizando el dedo a los lados. Devuelve la página actual.
     */
    int grid(Gfx g, String title, List<String> items, int page, String selected, java.util.function.Consumer<String> pick, String info) {
        sky(g);
        txtS(g, title, W / 2, 15, 16, GOLD, 0);
        int cols = gridCols(), per = cols * 2, pages = Math.max(1, (items.size() + per - 1) / per); page = Math.max(0, Math.min(page, pages - 1)); gridPages = pages;
        float cw = Math.min(96, (W - 12) / cols), ch = 62, gw = cw * cols, x0 = (W - gw) / 2, y0 = 32;
        txt(g, info + (pages > 1 ? " · DESLIZA PARA VER MÁS" : ""), W / 2, 28, 8, MUTE, 0);
        for (int k = 0; k < per; k++) {
            int i = page * per + k; if (i >= items.size()) break;
            final String n = items.get(i); float x = x0 + k % cols * cw, y = y0 + k / cols * (ch + 4); boolean sel = n.equals(selected), make = n.startsWith("\u0001");
            box(g, x + 2, y, cw - 4, ch, sel ? GOLD : make ? MINT : BLUE, sel ? CREAM : make ? 0xFF9CF5C9 : SKY, sel ? 0xFFB8862A : make ? 0xFF1F8A5B : NAVY);
            int[] t = thumb(n.equals(NEW_RANDOM) ? "" : n); int id = thumbIds.get(n.equals(NEW_RANDOM) ? "" : n); boolean fresh = thumbUp.add(id);
            float tx = Math.round(x + cw / 2 - THUMB / 2f), ty = y + 4;
            g.rect(tx - 1, ty - 1, THUMB + 2, THUMB_H + 2, INK); g.tile(id, t, THUMB, fresh, tx, ty);
            if (n.isEmpty()) txtS(g, "?", x + cw / 2, ty + 32, 24, GOLD, 0);
            if (make) { g.rect(tx, ty, THUMB, THUMB_H, 0x88101330); txtS(g, "+", x + cw / 2, ty + 32, 24, MINT, 0); }
            String shown = n.isEmpty() ? "ALEATORIO" : n.equals(NEW_EMPTY) ? "NUEVO VACÍO" : n.equals(NEW_RANDOM) ? "NUEVO AL AZAR" : n;
            int fit = (int) ((cw - 8) / 6); if (shown.length() > fit) shown = shown.substring(0, Math.max(3, fit - 1)) + ".";
            txtS(g, shown, x + cw / 2, y + ch - 7, 8, sel ? DEEP : CREAM, 0);
            btn(x + 2, y, cw - 4, ch, () -> pick.accept(n));
        }
        // navegación: flechas, puntitos por página (tocables) y deslizar
        float by = H - 21; final int pg = page;
        if (pages > 1) {
            arrow(g, 6, by, 34, 18, -1, () -> gridGo(-1));
            if (pages <= 12) { float dx = 10, sx = W / 2 - 40 - pages * dx / 2; for (int p = 0; p < pages; p++) { final int q = p; g.rect(Math.round(sx + p * dx), Math.round(by + 6), 6, 6, p == pg ? GOLD : 0xFF4A5590); btn(sx + p * dx - 2, by, dx, 18, () -> gridSet(q)); } }
            else txt(g, (page + 1) + " / " + pages, W / 2 - 40, by + 12, 8, CREAM, 0);
            arrow(g, W - 40 - 96, by, 34, 18, 1, () -> gridGo(1));
        }
        chip(g, W - 98, by, 92, 18, "VOLVER", false, () -> go(TITLE));
        return page;
    }
    int gridPages = 1;
    /** Botón con flecha de pixel art (la fuente no tiene < ni >). */
    void arrow(Gfx g, float x, float y, float w, float h, int dir, Runnable r) {
        chip(g, x, y, w, h, "", false, r);
        int cx = Math.round(x + w / 2), cy = Math.round(y + h / 2 - 1);
        for (int i = 0; i < 5; i++) g.rect(cx + (dir > 0 ? -2 + i : 2 - i), cy - 4 + i, 1, 9 - 2 * i, CREAM);
    }
    void gridSet(int p) { if (screen == LEVEL_SEL) selPage = p; else levelPage = p; focus = 0; }
    void gridGo(int d) { int p = (screen == LEVEL_SEL ? selPage : levelPage) + d; gridSet(Math.floorMod(p, Math.max(1, gridPages))); au.play(Audio.MENU); }
    // ---------- Deslizar el dedo en las pantallas de cuadritos: el toque se decide al soltar
    float swX0, swX, swTapX, swTapY; boolean swOn;
    boolean gridScreen() { return screen == LEVEL_SEL || screen == LEVEL_LIST; }
    /** Para las pruebas: elegir herramienta y aplicarla en un punto del mapa. */
    public void editorTool(int t) { editor.tool = t; }
    public void setRivals(int n, boolean t) { rivals = n; teams = t; }
    public int selPageNow() { return selPage; }
    public float[] editorWorldAt(float sx, float sy) { return new float[]{editor.wx(sx), editor.wy(sy)}; }
    /** Para las pruebas: deslizar un dedo en pantalla (coordenadas de la pantalla). */
    public void swipe(float x0, float y0, float x1, float y1) { tap(x0, y0); drag(x0, y0); drag(x1, y1); up(); }
    public void saveTestLevel(String n) { Level l = Level.random(n.hashCode(), n); if (n.equals("PLAYA")) l.water = 650; saveLevel(l); }
    public int editorSolidCount() { int c = 0; for (int i = 0; i < editor.w.t.m.length; i += 7) { int k = editor.w.t.m[i]; if (k == Terrain.EARTH || k == Terrain.WOOD) c++; } return c * 7; }
    public void editorApply(int x, int y) { editor.apply(x, y); }
    static final int NAME_MAX = 12;
    /** Nombre en mayúsculas, sin saltos de línea y con largo máximo. */
    static String clean(String n) { if (n == null) return ""; n = n.replaceAll("[\\r\\n\\t|]", " ").trim().toUpperCase(); return n.length() > NAME_MAX ? n.substring(0, NAME_MAX) : n; }
    static String or(String n, String d) { n = clean(n); return n.isEmpty() ? d : n; }
    void setNames(String a, String b, boolean show) { setNames(new String[]{a, b}, show); }
    void setNames(String[] n, boolean show) { names = n; ren.names = names; ren.showNames = show; ren.feedClear(); }
    static boolean near(int a, int b) {
        int r = (a >> 16 & 255) - (b >> 16 & 255), g = (a >> 8 & 255) - (b >> 8 & 255), l = (a & 255) - (b & 255);
        return r * r + g * g + l * l < 14000;
    }
    // ---------- Partida en red local
    Link.Hello hello(int seedV, int lives) {
        Link.Hello h = new Link.Hello(); h.rules = World.RULES; h.seed = seedV; h.lives = lives; h.color = myColor; h.slots = slots.clone();
        h.name = myName;
        if (seedV != 0 && !levelName.isEmpty()) { Level lv = loadLevel(levelName); if (lv != null) h.level = lv.save(); } // el anfitrión manda su nivel
        return h;
    }
    void openLobby() {
        go(LOBBY); netMsg = "Busca una sala o crea la tuya. Ambos teléfonos deben estar en la misma red Wi-Fi."; if (dev != null) dev.listen(true);
        try { if (browser == null) browser = new Lan.Browser(); } catch (Exception e) { netMsg = "No se pueden buscar salas en esta red. Igual puedes crear una."; }
    }
    /** Solo para pruebas de escritorio. */
    public void openLobbyForTest() { openLobby(); }
    void closeLobby() {
        if (browser != null) { browser.close(); browser = null; } if (beacon != null) { beacon.close(); beacon = null; }
        if (server != null) { try { server.close(); } catch (Exception e) { } server = null; }
        hosting = false; busy = false; if (dev != null) dev.listen(false);
    }
    void hostRoom() {
        if (busy) return; busy = true; hosting = true; if (browser != null) { browser.close(); browser = null; }
        final String name = !myName.isEmpty() ? "Sala de " + myName : dev != null ? dev.name() : "Sala WAP";
        new Thread(() -> {
            try {
                java.net.ServerSocket ss; try { ss = new java.net.ServerSocket(Link.PORT); } catch (Exception e) { ss = new java.net.ServerSocket(0); }
                server = ss; beacon = new Lan.Beacon(name, ss.getLocalPort(), World.RULES);
                netMsg = name + " está abierta. Esperando que el otro teléfono se una...";
                Link l = Link.accept(ss);
                seed = seed * 1103515245 + 12345; Link.Hello mine = hello(seed, LIVES[livesIx]); l.send(mine);
                Link.Hello peer = l.readHello();
                if (peer.rules != World.RULES) { l.close(); netMsg = "El otro teléfono tiene otra versión. Actualicen ambos y vuelvan a intentar."; busy = false; hosting = false; return; }
                pendHost = mine; pendGuest = peer; pending = l;
            } catch (Exception e) { if (hosting) netMsg = "No se pudo abrir la sala en esta red."; busy = false; hosting = false; }
        }, "sala").start();
    }
    void joinRoom(Lan.Room r) {
        if (busy) return;
        if (r.rules != World.RULES) { netMsg = "Esa sala usa otra versión del juego. Actualicen ambos teléfonos."; return; }
        busy = true; netMsg = "Conectando con " + r.name + "...";
        new Thread(() -> {
            try {
                Link l = Link.connect(r.ip, r.port); Link.Hello host = l.readHello(); Link.Hello mine = hello(0, 0); l.send(mine);
                if (host.rules != World.RULES) { l.close(); netMsg = "Esa sala usa otra versión del juego."; busy = false; return; }
                pendHost = host; pendGuest = mine; pending = l;
            } catch (Exception e) { netMsg = "No se pudo conectar con " + r.name + ". Revisa que estén en la misma red."; busy = false; }
        }, "unirse").start();
    }
    void startOnline() {
        Link l = pending; pending = null; Link.Hello h = pendHost, c = pendGuest; me = hosting ? 0 : 1; closeLobby();
        Wpn[] a = new Wpn[SLOTS], b = new Wpn[SLOTS];
        for (int i = 0; i < SLOTS; i++) { a[i] = Wpn.ALL[Math.floorMod(h.slots[i], Wpn.ALL.length)]; b[i] = Wpn.ALL[Math.floorMod(c.slots[i], Wpn.ALL.length)]; }
        int peer = me == 0 ? c.color : h.color; if (near(peer, myColor)) peer = near(myColor, 0xFF1266FF) ? 0xFFFF0000 : 0xFF1266FF;
        RobotArt.COLOR[me] = myColor; RobotArt.COLOR[1 - me] = peer;
        Level lv = null; received = null; receivedSaved = false;
        if (h.level.length > 0) try { lv = Level.load(h.level); } catch (Exception e) { endOnline("El nivel del otro teléfono no se pudo abrir."); return; }
        if (lv != null && me == 1) { received = lv; receivedSaved = levelNames().contains(lv.name) && java.util.Arrays.equals(loadLevel(lv.name).save(), h.level); } // se descarga solo para esta partida
        w = new World(h.seed, h.lives, a, b, lv); bot = null; link = l; l.startReader(); ls = new Lockstep(l, me); online = true;
        ren.reset(w); cd = 90; screen = PLAY; overT = 0; pad.clear();
        String hn = or(h.name, "PLAYER 1"), cn = or(c.name, "PLAYER 2"); if (hn.equals(cn)) cn = cn.length() < NAME_MAX - 1 ? cn + " 2" : "PLAYER 2";
        setNames(hn, cn, true); // en línea se ve el nombre de cada uno sobre su personaje
        if (dev != null) dev.lowLatency(true);
        ren.teams = false;
    }
    /** Guarda el nivel recibido (con otro nombre si ya hay uno igual). */
    void keepReceived() {
        Level l = received; if (l == null) return;
        if (levelNames().contains(l.name)) l.name = unusedName(l.name.length() > NAME_MAX - 3 ? l.name.substring(0, NAME_MAX - 3) : l.name);
        saveLevel(l); receivedSaved = true; toast = "NIVEL GUARDADO: " + l.name; endOnline(null); toastT = 200;
    }
    void endOnline(String msg) {
        if (link != null) link.close(); link = null; ls = null; online = false; if (dev != null) dev.lowLatency(false);
        if (msg != null) { toast = msg; toastT = 300; }
        go(TITLE);
    }
    /** Al cerrar la app. */
    public void quit() { if (online) endOnline(null); closeLobby(); }
    public void back() {
        if (screen == LOBBY) { closeLobby(); go(TITLE); return; }
        if (screen == OVER && online) { endOnline(null); return; }
        if (screen == PAINT) { save(); go(TITLE); return; }
        if (screen == EDIT) { leaveEditor(false); return; }
        if (screen == LEVEL_LIST || screen == LEVEL_SEL) { go(TITLE); return; }
        if (screen == RIVALS) { saveRivals(); go(TITLE); return; }
        if (testing != null && (screen == OVER || screen == PAUSE)) { backToEditor(); return; }
        if (screen == PLAY) screen = PAUSE; else if (screen == PAUSE) screen = PLAY; else if (screen == ARMS) { save(); saveRivals(); go(armsFor >= 0 ? RIVALS : TITLE); } else if (screen == OVER) go(TITLE);
    }
    public void pauseNow() { if (screen == PLAY) screen = PAUSE; }
    void save() { st.put("traje", String.format("%06X", myColor & 0xFFFFFF)); st.put("nombre", myName); st.put("nivel", levelName); for (int i = 0; i < SLOTS; i++) st.put(key(i), Wpn.ALL[slots[i]].name); st.put("level", "" + level); st.put("lives", "" + livesIx); }
    public void go(int s) { screen = s; focus = 0; pad.clear(); }
    /** Toques del mando en pantalla, convertidos a la capa de interfaz. */
    public void pointer(int id, float x, float y, boolean down) { if (screen == EDIT) editor.pointer(id, x, y, down); else pad.pointer(id, x / uiScale, y / uiScale, down); }
    public boolean tap(float x, float y) {
        if (gridScreen()) { swOn = true; swX0 = swX = x; swTapX = x / uiScale; swTapY = y / uiScale; return true; }
        return tapNow(x, y);
    }
    boolean tapNow(float x, float y) {
        x /= uiScale; y /= uiScale;
        for (int i = btns.size() - 1; i >= 0; i--) { float[] b = btns.get(i); if (x >= b[0] && y >= b[1] && x <= b[2] && y <= b[3]) { au.play(Audio.MENU); acts.get(i).run(); return true; } }
        return false;
    }
    public void tick() {
        frames++;
        int m = keys | (screen == PLAY ? pad.mask() : 0), press = keys & ~last; last = keys;
        if (startKey) { startKey = false; if (screen == PLAY || screen == PAUSE) back(); else if (screen == TITLE) start(); }
        if (pending != null) startOnline();
        if (toastT > 0) toastT--;
        if (online && (screen == PLAY || screen == PAUSE || screen == OVER)) {
            if (link.closed && !w.finished) { endOnline(link.error != null ? link.error : "El otro jugador salió de la partida."); return; }
            if (cd > 0) { cd--; return; }
            int mm = screen == PLAY ? m : 0;
            ls.advance(w, mm, () -> { ren.tick(w); sounds(); ren.consume(w, false); });
            if (w.finished && screen != OVER && ++overT > 110) go(OVER);
            if (screen == OVER) navList(press);
            return;
        }
        if (screen == PLAY) {
            if (cd > 0) cd--;
            else {
                int[] ins = new int[w.robots.length]; ins[0] = m; for (int i = 1; i < ins.length; i++) ins[i] = bots[i].think(w);
                w.step(ins); ren.tick(w);
                sounds();
                ren.consume(w, false);
                if (w.finished && ++overT > 110) go(OVER);
            }
        } else if (screen == ARMS) navArms(press);
        else if (screen == PAINT) navPaint(press, keys);
        else if (screen == EDIT) editor.tick(keys, press);
        else navList(press);
    }
    void sounds() {
        for (Fx f : w.fx) switch (f.type) {
            case Fx.EXPLODE: au.play(f.a > 50 ? Audio.BIG : Audio.BOOM); break;
            case Fx.FLASH: { String n = Wpn.ALL[(int) f.b].name; au.play(n.endsWith("SHOTGUN") ? Audio.BLAST : Audio.SHOT); break; }
            case Fx.ARMOR: au.play(Audio.ARMOR); break;
            case Fx.HIT: au.play(Audio.HURT); break;
            case Fx.STOMP: case Fx.SLAM: au.play(Audio.BOOM); break;
            case Fx.GRAB: au.play(Audio.HOOK); break;
            case Fx.JUMP: if (f.a == 0) au.play(Audio.JUMP); break;
            case Fx.HOOK: au.play(Audio.HOOK); break;
            case Fx.PICK: au.play(Audio.PICK); break;
            case Fx.DEATH: { au.play(Audio.DEATH); int d = (int) f.a, k = (int) f.b; // aviso arriba de la pantalla
                ren.feed(k == d || k < 0 ? names[d] + " SE BANEÓ DE LA VIDA" : names[d] + " FUE ELIMINADO POR " + names[k], RobotArt.COLOR[d]); break; }
            case Fx.SPLASH: au.play(Audio.JUMP); break;
            case Fx.LASSO: au.play(f.a == 1 ? Audio.HOOK : Audio.JUMP); break;
            case Fx.TRAP: au.play(f.a > 0 ? Audio.BIG : Audio.PICK); break;
            default:
        }
    }
    void navList(int p) {
        int n = acts.size(); if (n == 0) return;
        if ((p & (Input.DOWN | Input.RIGHT)) != 0) { focus = (focus + 1) % n; au.play(Audio.MENU); }
        if ((p & (Input.UP | Input.LEFT)) != 0) { focus = (focus + n - 1) % n; au.play(Audio.MENU); }
        if ((p & Input.A) != 0 && focus < n) { au.play(Audio.MENU); acts.get(focus).run(); }
        else if ((p & Input.B) != 0) back();
    }
    void channel(int ch, int v) { int s = (2 - ch) * 8, c = myColor; myColor = c & ~(255 << s) | Math.max(0, Math.min(255, v)) << s; }
    int channel(int ch) { return myColor >> (2 - ch) * 8 & 255; }
    void askName() {
        if (dev == null) return;
        dev.askText("Tu nombre", myName, NAME_MAX, n -> { if (n != null) { myName = clean(n); save(); } });
    }
    void navPaint(int p, int held) {
        if ((p & Input.DOWN) != 0) paintRow = (paintRow + 1) % 4;
        if ((p & Input.UP) != 0) paintRow = (paintRow + 3) % 4;
        if (paintRow == 3) { if ((p & Input.A) != 0) askName(); else if ((p & Input.B) != 0) back(); return; }
        if ((held & Input.LEFT) != 0) channel(paintRow, channel(paintRow) - 3);
        if ((held & Input.RIGHT) != 0) channel(paintRow, channel(paintRow) + 3);
        if ((p & (Input.A | Input.B)) != 0) back();
    }
    /** Arrastre sobre las barras de color. */
    /** Dedo que toca, se mueve o se levanta (el editor dibuja con esto). */
    public void down(float x, float y) { }
    public void up() {
        if (!swOn) return; swOn = false;
        float dx = (swX - swX0) / uiScale;
        if (gridScreen() && Math.abs(dx) > 30) gridGo(dx < 0 ? 1 : -1); // deslizar: página siguiente o anterior
        else tapNow(swTapX * uiScale, swTapY * uiScale);
    }
    public void drag(float x, float y) {
        if (swOn) { swX = x; return; }
        if (screen == EDIT) return; // el editor usa los gestos de pointer()
        if (screen != PAINT) return;
        x /= uiScale; y /= uiScale;
        for (int i = 0; i < 3; i++) {
            float ty = sliderY(i);
            if (Math.abs(y - ty) < 22 * u && x > sliderX() - 16 * u && x < sliderX() + sliderW() + 16 * u) { paintRow = i; channel(i, (int) ((x - sliderX()) / sliderW() * 255)); }
        }
    }
    float sliderX() { return W * .5f + 24 * u; } float sliderW() { return W * .5f - 60 * u; } float sliderY(int i) { return 66 * u + i * 56 * u; }
    /** Personaje grande en pixel art, para menús. */
    RobotArt.View heroV; float heroX, heroY;
    /** El personaje del menú va suavizado, como en el juego: se dibuja después de la capa de interfaz. */
    void hero(Gfx g, RobotArt.View v, float cx, float by) { heroV = v; heroX = cx; heroY = by; }
    // ---------- Piezas de interfaz en pixel art
    void txt(Gfx g, String s, float x, float y, float size, int c, int align) { PixelFont.draw(g, s, x, y, size, c, align, 0); }
    void txtS(Gfx g, String s, float x, float y, float size, int c, int align) { PixelFont.draw(g, s, x, y, size, c, align, DEEP); }
    /** Caja con esquinas recortadas, brillo arriba y sombra abajo. */
    void box(Gfx g, float x, float y, float w, float h, int fill, int light, int dark) {
        int X = Math.round(x), Y = Math.round(y), Wd = Math.round(w), Hd = Math.round(h);
        g.rect(X, Y, Wd, Hd, INK); // rectángulo con esquinas rectas
        g.rect(X + 1, Y + 1, Wd - 2, Hd - 2, fill); g.rect(X + 2, Y + 1, Wd - 4, 1, light); g.rect(X + 1, Y + Hd - 3, Wd - 2, 2, dark);
    }
    /** Fondo de los menús: cielo nocturno en bandas, con estrellas que titilan. */
    void sky(Gfx g) {
        int[] band = {0xFF101330, 0xFF15193D, 0xFF1B1F4A, 0xFF212863, 0xFF283180, 0xFF2E3B97, 0xFF33459F};
        int bh = (int) Math.ceil(H / band.length);
        for (int i = 0; i < band.length; i++) {
            g.rect(0, i * bh, W, bh, band[i]);
            if (i > 0) for (int x = (i & 1); x < W; x += 2) g.rect(x, i * bh, 1, 1, band[i - 1]);
        }
        for (int i = 0; i < 26; i++) {
            int h = i * 2654435761L > 0 ? (int) (i * 2654435761L >>> 5) : i * 977; int x = Math.floorMod(h, (int) W), y = Math.floorMod(h >> 11, (int) (H * .6f));
            if ((frames / 20 + i) % 7 != 0) g.rect(x, y, 1, 1, i % 3 == 0 ? GOLD : CREAM);
        }
    }
    /** Plataforma del juego, para el suelo de los menús. */
    void floor(Gfx g, float x, float y, float w) {
        int X = Math.round(x), Y = Math.round(y), Wd = Math.round(w);
        g.rect(X, Y, Wd, (int) H - Y, INK); g.rect(X + 1, Y + 1, Wd - 2, (int) H - Y, PURPLE); g.rect(X + 1, Y + 1, Wd - 2, 1, 0xFFB46BFF);
        for (int k = X + 32; k < X + Wd; k += 32) g.rect(k, Y + 1, 1, (int) H - Y, INK);
    }
    void paint(Gfx g) {
        sky(g); floor(g, 0, H * .93f, W * .48f);
        RobotArt.View v = new RobotArt.View();
        hero(g, v, W * .24f, H * .93f);
        txtS(g, "PERSONALIZAR", sliderX(), 28 * u, 20 * u, GOLD, -1);
        String[] n = {"ROJO", "VERDE", "AZUL"}; int[] tone = {0xFFE8323C, 0xFF3DDC97, 0xFF5FA8FF};
        for (int i = 0; i < 3; i++) {
            float y = sliderY(i), x = sliderX(), w = sliderW(), k = channel(i) / 255f;
            txt(g, n[i], x, y - 10 * u, 14 * u, CREAM, -1); txt(g, "" + channel(i), x + w, y - 10 * u, 14 * u, i == paintRow && padSeen ? GOLD : CREAM, 1);
            box(g, x, y - 6 * u, w, 12 * u, DEEP, NAVY, DEEP);
            g.rect(Math.round(x + 1), Math.round(y - 6 * u + 1), Math.round(Math.max(1, (w - 2) * k)), Math.round(12 * u - 4), tone[i]);
            int kx = Math.round(x + w * k), ky = Math.round(y);
            g.rect(kx - 3, ky - 6, 7, 12, INK); g.rect(kx - 2, ky - 5, 5, 10, CREAM); g.rect(kx - 2, ky + 3, 5, 2, 0xFFC9B98F);
        }
        float sz = (sliderW() + 8 * u) / SWATCH.length, y = sliderY(2) + 24 * u;
        for (int i = 0; i < SWATCH.length; i++) {
            final int c = SWATCH[i]; float x = sliderX() + i * sz;
            if (myColor == c) box(g, x - 2, y - 2, sz - 5 * u + 4, 28 * u + 4, GOLD, CREAM, 0xFFB8862A);
            box(g, x, y, sz - 5 * u, 28 * u, c, RobotArt.mixLight(c), RobotArt.mixDark(c));
            btn(x, y, sz - 5 * u, 28 * u, () -> { myColor = c; save(); });
        }
        float ny = y + 40 * u, nh = 30 * u; boolean sel = paintRow == 3 && padSeen; // nombre: se ve sobre el personaje al jugar en línea
        txt(g, "NOMBRE", sliderX(), ny + nh / 2 + 5 * u, 14 * u, CREAM, -1);
        float lbl = PixelFont.width("NOMBRE", 14 * u) + 8 * u, nx = sliderX() + lbl, nw = sliderW() - lbl;
        box(g, nx, ny, nw, nh, sel ? 0xFF2E4BA8 : DEEP, NAVY, DEEP);
        txt(g, myName.isEmpty() ? "TOCA PARA ESCRIBIR" : myName + (frames / 30 % 2 == 0 && sel ? "_" : ""), nx + 6 * u, ny + nh / 2 + 5 * u, myName.isEmpty() ? 10 * u : 14 * u, myName.isEmpty() ? MUTE : GOLD, -1);
        btn(nx, ny, nw, nh, this::askName);
        float by = H - 46 * u;
        box(g, sliderX(), by, sliderW(), 36 * u, MINT, 0xFF9CF5C9, 0xFF1F8A5B); txtS(g, "LISTO", sliderX() + sliderW() / 2, by + 24 * u, 16 * u, CREAM, 0);
        btn(sliderX(), by, sliderW(), 36 * u, this::back);
    }
    static final int COLS = 3, ROWS = 10;
    void navArms(int p) {
        int n = Wpn.ALL.length;
        if ((p & Input.RIGHT) != 0) cur = (cur + 1) % n;
        if ((p & Input.LEFT) != 0) cur = (cur + n - 1) % n;
        if ((p & Input.DOWN) != 0) cur = Math.min(n - 1, cur + COLS);
        if ((p & Input.UP) != 0) cur = Math.max(0, cur - COLS);
        if ((p & Input.A) != 0) { eslots()[selSlot] = cur; selSlot = (selSlot + 1) % SLOTS; au.play(Audio.PICK); }
        if ((p & Input.B) != 0) back();
    }
    void btn(float x, float y, float w, float h, Runnable r) { btns.add(new float[]{x, y, x + w, y + h}); acts.add(r); }

    /** Etiqueta de la combinación, dibujada con triángulos para no depender de la fuente. */
    public static void combo(Gfx g, int slot, float x, float cy, float s, int c) {
        int dir = slot == 0 ? 2 : slot == 2 ? 0 : slot == 3 ? -1 : 1;
        String b = "A";
        float q = s * .42f;
        if (dir == 0) g.poly(new float[]{x + q, cy - q, x + 2 * q, cy + q * .7f, x, cy + q * .7f}, c, 0, 0);
        if (dir == 2) g.poly(new float[]{x + q, cy + q, x + 2 * q, cy - q * .7f, x, cy - q * .7f}, c, 0, 0);
        if (dir == 1) { g.poly(new float[]{x, cy, x + q, cy - q, x + q, cy + q}, c, 0, 0); g.poly(new float[]{x + 2.8f * q, cy, x + 1.8f * q, cy - q, x + 1.8f * q, cy + q}, c, 0, 0); }
        float tx = dir < 0 ? x : x + (dir == 1 ? 3.3f : 2.5f) * q;
        PixelFont.draw(g, b, tx, cy + 3.5f * PixelFont.scale(s), s, c, -1, 0);
    }
    public void draw(Gfx g) {
        int s = Math.max(1, Math.round(g.h() / 190f)); uiScale = s;
        boolean game = screen == PLAY || screen == PAUSE || screen == OVER;
        if (game) ren.drawWorld(g, w, me); else if (screen == EDIT) ren.drawWorld(g, editor.w, 0); else g.clear(DEEP);
        g.beginPixels((g.w() + s - 1) / s, (g.h() + s - 1) / s); // toda la interfaz va en pixel art
        W = (g.w() + s - 1) / s; H = (g.h() + s - 1) / s; u = H / 360f; pad.layout(W, H); btns.clear(); acts.clear(); heroV = null;
        switch (screen) {
            case TITLE: title(g); break;
            case ARMS: arms(g); break;
            case PAINT: paint(g); break;
            case LOBBY: lobby(g); break;
            case EDIT: editor.draw(g); if (toastT > 0) { g.rect(0, 26, W, 14, MINT); txtS(g, toast, W / 2, 37, 8, CREAM, 0); } break;
            case LEVEL_LIST: levels(g); break;
            case RIVALS: rivalsScreen(g); focusBox(g); break;
            case LEVEL_SEL: levelSel(g); focusBox(g); break;
            default:
                ren.overlay(g, s); hud(g);
                if (online && ls.stall > 45) txtS(g, "ESPERANDO AL OTRO TELÉFONO...", W / 2, H * .5f, 16 * u, CREAM, 0);
                if (online && ls.desync) txtS(g, "PARTIDA DESINCRONIZADA", W / 2, H - 8 * u, 12 * u, RED, 0);
                if (screen == PLAY) {
                    if (cd > 0) txtS(g, cd > 60 ? "3" : cd > 30 ? "2" : "1", W / 2, H * .42f, 64 * u, GOLD, 0);
                    pad.draw(g, pad.mask()); float x = W / 2 - 18 * u, y = H - 40 * u;
                    if (pad.visible) {
                        box(g, x, y, 36 * u, 28 * u, 0xCC1B1F4A, 0x55FFFFFF, DEEP);
                        g.rect(Math.round(x + 12 * u), Math.round(y + 7 * u), Math.max(1, Math.round(4 * u)), Math.round(13 * u), CREAM); g.rect(Math.round(x + 20 * u), Math.round(y + 7 * u), Math.max(1, Math.round(4 * u)), Math.round(13 * u), CREAM);
                        btn(x - 8 * u, y - 8 * u, 52 * u, 44 * u, () -> screen = PAUSE);
                    }
                } else if (screen == PAUSE) {
                    veil(g); txtS(g, "PAUSA", W / 2, H * .3f, 40 * u, GOLD, 0);
                    if (online) { txt(g, "LA PARTIDA SIGUE MIENTRAS ESTÁS AQUÍ", W / 2, H * .37f, 12 * u, CREAM, 0); menu(g, W / 2 - 120 * u, H * .42f, 240 * u, new String[]{"SEGUIR", "SALIR DE LA PARTIDA"}, new Runnable[]{() -> screen = PLAY, () -> endOnline(null)}); }
                    else if (testing != null) menu(g, W / 2 - 110 * u, H * .4f, 220 * u, new String[]{"SEGUIR", "VOLVER AL EDITOR"}, new Runnable[]{() -> screen = PLAY, this::backToEditor});
                    else menu(g, W / 2 - 110 * u, H * .4f, 220 * u, new String[]{"SEGUIR", "VOLVER AL MENÚ"}, new Runnable[]{() -> screen = PLAY, () -> go(TITLE)});
                } else {
                    veil(g); boolean win = w.winnerTeam == w.robots[me].team;
                    txtS(g, win ? "¡GANASTE!" : "PERDISTE", W / 2, H * .28f, 48 * u, win ? GOLD : RED, 0);
                    txt(g, "BAJAS " + w.robots[me].kills + "   MUERTES " + w.robots[me].deaths, W / 2, H * .37f, 14 * u, CREAM, 0);
                    if (online && received != null && !receivedSaved) { // nivel del otro teléfono: ¿conservarlo?
                        txt(g, "¿QUIERES CONSERVAR EL NIVEL \"" + received.name + "\"?", W / 2, H * .44f, 12 * u, GOLD, 0);
                        menu(g, W / 2 - 110 * u, H * .48f, 220 * u, new String[]{"SÍ, GUARDARLO", "NO, VOLVER AL MENÚ"}, new Runnable[]{this::keepReceived, () -> endOnline(null)});
                    }
                    else if (online) menu(g, W / 2 - 110 * u, H * .44f, 220 * u, new String[]{"VOLVER AL MENÚ"}, new Runnable[]{() -> endOnline(null)});
                    else if (testing != null) menu(g, W / 2 - 110 * u, H * .44f, 220 * u, new String[]{"VOLVER AL EDITOR", "JUGAR OTRA VEZ"}, new Runnable[]{this::backToEditor, this::start});
                    else menu(g, W / 2 - 110 * u, H * .44f, 220 * u, new String[]{"REVANCHA", "VOLVER AL MENÚ"}, new Runnable[]{this::start, () -> go(TITLE)});
                }
        }
        g.endPixels(0, 0, s);
        if (heroV != null) { float k = H * s * .78f / 52; g.save(); g.translate(heroX * s, heroY * s); g.scale(k, k); RobotArt.drawPose(g, heroV, (1 << Robot.PIECES) - 1, myColor, 0); g.restore(); }
    }
    /** Texto largo partido en líneas que caben en el ancho. */
    void para(Gfx g, String t, float cx, float y, float maxW, int c) {
        StringBuilder line = new StringBuilder();
        for (String word : t.split(" ")) {
            String next = line.length() == 0 ? word : line + " " + word;
            if (PixelFont.width(next, 8) > maxW && line.length() > 0) { txt(g, line.toString(), cx, y, 8, c, 0); y += 10; line = new StringBuilder(word); }
            else line = new StringBuilder(next);
        }
        txt(g, line.toString(), cx, y, 8, c, 0);
    }
    void veil(Gfx g) { g.rect(0, 0, W, H, 0xC0101330); }
    /** Botón grueso con el texto centrado; si no cabe en una línea se parte en dos. */
    void fat(Gfx g, float x, float y, float w, float h, String label, int index, int fill, Runnable act) {
        boolean f = index == focus && padSeen;
        if (f) box(g, x, y, w, h, GOLD, CREAM, 0xFFB8862A); else box(g, x, y, w, h, fill, RobotArt.mixLight(fill), RobotArt.mixDark(fill));
        List<String> lines = new ArrayList<>(); StringBuilder cur = new StringBuilder(); float max = w - 10;
        for (String part : label.split("\n")) { // un salto de línea manual manda; si no, se parte por palabras
            cur = new StringBuilder();
            for (String word : part.trim().split(" +")) {
                String next = cur.length() == 0 ? word : cur + " " + word;
                if (PixelFont.width(next, 8) > max && cur.length() > 0) { lines.add(cur.toString()); cur = new StringBuilder(word); } else cur = new StringBuilder(next);
            }
            lines.add(cur.toString());
        }
        float lh = 10, y0 = y + (h - 2) / 2 - (lines.size() * lh) / 2 + 8;
        for (int i = 0; i < lines.size(); i++) txtS(g, lines.get(i), x + w / 2, y0 + i * lh, 8, f ? DEEP : CREAM, 0);
        btn(x, y, w, h, act);
    }
    void menu(Gfx g, float x, float y, float w, String[] l, Runnable[] a) {
        float h = (l.length > 9 ? 22 : l.length > 8 ? 24 : l.length > 7 ? 26 : l.length > 6 ? 31 : l.length > 5 ? 34 : l.length > 4 ? 37 : 40) * u, gap = (l.length > 8 ? 3 : l.length > 7 ? 4 : l.length > 6 ? 5 : l.length > 4 ? 6 : 9) * u;
        for (int i = 0; i < l.length; i++) {
            boolean f = i == focus && padSeen;
            if (f) box(g, x, y, w, h, RED, 0xFFFF8A8F, 0xFF8E1520); else box(g, x, y, w, h, BLUE, SKY, NAVY);
            txtS(g, l[i], x + w / 2, y + h / 2 + 3.5f * PixelFont.scale(15 * u) - 1, 15 * u, f ? GOLD : CREAM, 0);
            btn(x, y, w, h, a[i]); y += h + gap;
        }
    }
    void title(Gfx g) {
        sky(g); floor(g, 0, H * .92f, W * .46f);
        hero.lean = frames / 30 % 2 == 0 ? 4 : 6;
        hero(g, hero, W * .25f, H * .92f);
        float x = W * .47f, wd = W - x - 6;
        txtS(g, "WAP", x, H * .15f, 48 * u, GOLD, -1);
        List<String> l = new ArrayList<>(java.util.Arrays.asList("JUGAR", "EN LÍNEA\nRED LOCAL", "SELECCIONA NIVEL", "EDITOR DE NIVELES", "RIVALES", "ELEGIR 4 ARMAS", "PERSONALIZAR", "VIDAS  " + LIVES[livesIx]));
        List<Runnable> a = new ArrayList<>(java.util.Arrays.asList(this::start, this::openLobby, () -> { selPage = pageOf(levelName); go(LEVEL_SEL); }, () -> { levelPage = 0; go(LEVEL_LIST); }, () -> go(RIVALS), () -> { armsFor = -1; go(ARMS); }, () -> go(PAINT),
            () -> { livesIx = (livesIx + 1) % LIVES.length; save(); }));
        if (up != null) { l.add(up.label()); a.add(() -> up.act()); txt(g, up.version(), W - 4, H - 3, 8, MUTE, 1); }
        l.add("SALIR DEL JUEGO"); a.add(() -> { save(); if (dev != null) dev.exit(); });
        // botones gorditos: JUGAR ocupa toda la fila; el resto va de a dos, con el texto en dos líneas si no cabe
        int rest = l.size() - 1, rows = 1 + (rest + 1) / 2; float top = H * .19f, gap = 4, rh = Math.min(32, (H - 5 - top - gap * (rows - 1)) / rows), cw = (wd - gap) / 2;
        fat(g, x, top, wd, rh, l.get(0), 0, RED, a.get(0));
        for (int i = 1; i < l.size(); i++) {
            int r = (i - 1) / 2, c = (i - 1) % 2; boolean last = i == l.size() - 1 && c == 0;
            fat(g, x + c * (cw + gap), top + (r + 1) * (rh + gap), last ? wd : cw, rh, l.get(i), i, l.get(i).startsWith("SALIR") ? 0xFF4A5590 : BLUE, a.get(i));
        }
        if (toastT > 0) { g.rect(0, H - 16, W, 16, RED); txtS(g, toast, W / 2, H - 4, 8, CREAM, 0); }
    }
    void lobby(Gfx g) {
        sky(g);
        txtS(g, "EN LÍNEA · RED LOCAL", W / 2, 34 * u, 24 * u, GOLD, 0);
        para(g, netMsg, W / 2, 52 * u, W - 20, CREAM);
        List<String> l = new ArrayList<>(); List<Runnable> a = new ArrayList<>();
        if (hosting) { l.add("CANCELAR SALA"); a.add(() -> { closeLobby(); openLobby(); }); }
        else {
            l.add("CREAR SALA"); a.add(this::hostRoom);
            List<Lan.Room> rooms = browser != null ? browser.rooms() : new ArrayList<>();
            if (rooms.isEmpty() && !busy) txt(g, "BUSCANDO SALAS EN TU RED" + "...".substring(0, (int) (frames / 20 % 4)), W / 2, H - 10 * u, 8, MUTE, 0);
            for (int i = 0; i < Math.min(4, rooms.size()); i++) { Lan.Room r = rooms.get(i); l.add("UNIRSE A " + r.name.toUpperCase() + (r.rules != World.RULES ? " (OTRA VERSIÓN)" : "")); a.add(() -> joinRoom(r)); }
        }
        l.add("VOLVER"); a.add(() -> { closeLobby(); go(TITLE); });
        menu(g, W / 2 - 170 * u, 84 * u, 340 * u, l.toArray(new String[0]), a.toArray(new Runnable[0]));
    }
    void arms(Gfx g) {
        sky(g);
        float m = 12 * u, cw = 200 * u, row = 44 * u, y = 10 * u;
        txtS(g, armsFor < 0 ? "TUS 4 ARMAS" : "ARMAS DE " + "PLAYER " + (armsFor + 2), m, y + 16 * u, 15 * u, GOLD, -1); y += 24 * u;
        for (int i = 0; i < SLOTS; i++) {
            boolean sel = i == selSlot; final int ix = i;
            if (sel) box(g, m, y, cw, row - 5 * u, RED, 0xFFFF8A8F, 0xFF8E1520); else box(g, m, y, cw, row - 5 * u, BLUE, SKY, NAVY);
            combo(g, i, m + 6 * u, y + 17 * u, 14 * u, sel ? GOLD : CREAM);
            txtS(g, Wpn.ALL[eslots()[i]].name, m + 46 * u, y + 23 * u, 8, CREAM, -1);
            btn(m, y, cw, row - 5 * u, () -> selSlot = ix); y += row;
        }
        float bx = m, by = H - 48 * u;
        box(g, bx, by, cw, 38 * u, MINT, 0xFF9CF5C9, 0xFF1F8A5B); txtS(g, "LISTO", bx + cw / 2, by + 24 * u, 16 * u, CREAM, 0);
        if (armsFor >= 0) { // rival: puede volver a armas al azar
            float hw = cw / 2 - 2 * u; box(g, bx, by, hw, 38 * u, MINT, 0xFF9CF5C9, 0xFF1F8A5B); txtS(g, "LISTO", bx + hw / 2, by + 24 * u, 12 * u, CREAM, 0);
            btn(bx, by, hw, 38 * u, () -> { rRandom[armsFor] = false; saveRivals(); go(RIVALS); });
            box(g, bx + hw + 4 * u, by, hw, 38 * u, PURPLE, 0xFFB46BFF, 0xFF4A00A8); txtS(g, "AL AZAR", bx + hw * 1.5f + 4 * u, by + 24 * u, 12 * u, CREAM, 0);
            btn(bx + hw + 4 * u, by, hw, 38 * u, () -> { rRandom[armsFor] = true; saveRivals(); go(RIVALS); });
        } else btn(bx, by, cw, 38 * u, () -> { save(); go(TITLE); });
        float gx = m + cw + 10 * u, gw = (W - gx - m) / COLS, gh = 28 * u, gy = 8 * u; int n = Wpn.ALL.length;
        for (int i = 0; i < n; i++) {
            final int ix = i; float x = gx + i % COLS * gw, yy = gy + i / COLS * gh;
            boolean c = i == cur, eq = false; for (int s : eslots()) eq |= s == i;
            if (c) box(g, x, yy, gw - u, gh - 2 * u, GOLD, CREAM, 0xFFB8862A);
            else if (eq) box(g, x, yy, gw - u, gh - 2 * u, PURPLE, 0xFFB46BFF, 0xFF4A00A8);
            else box(g, x, yy, gw - u, gh - 2 * u, NAVY, BLUE, DEEP);
            txt(g, Wpn.ALL[i].name, x + gw / 2, yy + gh / 2 + 3, 8, c ? DEEP : CREAM, 0);
            btn(x, yy, gw, gh, () -> { cur = ix; eslots()[selSlot] = ix; selSlot = (selSlot + 1) % SLOTS; });
        }
        txt(g, Wpn.ALL[cur].desc, gx + 2, H - 3, 8, SKY, -1);
    }
    void hud(Gfx g) {
        Robot me = w.robots[this.me];
        float bw = 168 * u; bar(g, 4, 4, bw, me, this.me);
        if (w.robots.length == 2) bar(g, W - 4 - bw, 4, bw, w.robots[1 - this.me], 1 - this.me);
        else { float y = 4; for (Robot o : w.robots) if (o != me) { mini(g, W - 4 - bw, y, bw, o); y += 15; } } // varios rivales: barras chicas
        if (online && ls != null) { // calidad de la conexión, para saber si la lentitud es de la red
            float p = ls.ping(); int c = p < 0 ? MUTE : p < 60 ? MINT : p < 120 ? GOLD : RED;
            txt(g, p < 0 ? "RED ..." : "RED " + Math.round(p) + " MS", 6, 42, 8, c, -1);
        }
        int n = SLOTS; float cw = Math.min(112 * u, (W - 2 * bw - 16) / n), x = W / 2 - cw * n / 2;
        for (int i = 0; i < n; i++) {
            float y = 4, h = 27, cx = x + i * cw; Wpn wp = me.loadout[i];
            box(g, cx + 1, y, cw - 2, h, 0xE61B1F4A, BLUE, DEEP);
            combo(g, i, cx + 4, y + 6, 8, GOLD);
            int fit = Math.max(3, (int) ((cw - 6) / 6)); String nm = wp.name;
            if (nm.length() > fit && nm.indexOf(' ') > 0) nm = nm.charAt(0) + "." + nm.substring(nm.indexOf(' ') + 1); // PLASMA CANNON → P.CANNON
            if (nm.length() > fit) nm = nm.substring(0, fit);
            txt(g, nm, cx + 4, y + 17, 8, CREAM, -1);
            float frac = me.reload[i] > 0 ? 1 - me.reload[i] / (float) wp.reload : me.ammo[i] / (float) wp.clip;
            int c = me.reload[i] > 0 ? 0xFF6B7398 : frac > .5f ? MINT : frac > .2f ? GOLD : RED;
            if (wp.charge > 0 && me.reload[i] == 0) { frac = me.charge[i] / (float) wp.charge; c = frac >= 1 && frames / 4 % 2 == 0 ? CREAM : SKY; }
            int bx = Math.round(cx + 4), by = Math.round(y + h - 7), bwd = Math.round(cw - 10);
            g.rect(bx, by, bwd, 3, DEEP); g.rect(bx, by, Math.round(bwd * Math.max(0, Math.min(1, frac))), 3, c);
        }
    }
    /** Barra chica de un rival: color, vida en segmentos y vidas que le quedan. */
    void mini(Gfx g, float x, float y, float wd, Robot r) {
        int col = RobotArt.COLOR[r.id]; box(g, x, y, wd, 13, 0xE61B1F4A, r.team == w.robots[me].team ? MINT : BLUE, DEEP);
        g.rect(Math.round(x + 3), Math.round(y + 3), 7, 7, col);
        int seg = 10, sx = Math.round(x + 13), sw = Math.max(2, Math.round((wd - 46) / seg)), full = (int) Math.ceil(Math.max(0, r.hp) / 10f);
        for (int i = 0; i < seg; i++) g.rect(sx + i * sw, Math.round(y + 4), sw - 1, 5, r.lives < 0 ? DEEP : i < full ? col : DEEP);
        txt(g, r.lives < 0 ? "FUERA" : "x" + r.lives, Math.round(x + wd - 4), Math.round(y + 10), 8, r.lives < 0 ? MUTE : CREAM, 1);
    }
    void bar(Gfx g, float x, float y, float w, Robot r, int id) {
        float h = 27; box(g, x, y, w, h, 0xE61B1F4A, BLUE, DEEP);
        int seg = 10, sx = Math.round(x + 4), sy = Math.round(y + 4), sw = Math.max(2, Math.round((w - 8) / seg)), full = (int) Math.ceil(Math.max(0, r.hp) / 10f), col = RobotArt.COLOR[id];
        for (int i = 0; i < seg; i++) {
            g.rect(sx + i * sw, sy, sw - 1, 6, i < full ? col : DEEP);
            if (i < full) g.rect(sx + i * sw, sy, sw - 1, 1, RobotArt.mixLight(col));
        }
        int hx = sx, hy = Math.round(y + h - 9);
        g.rect(hx, hy, 2, 1, PINK); g.rect(hx + 3, hy, 2, 1, PINK); g.rect(hx, hy + 1, 5, 2, PINK); g.rect(hx + 1, hy + 3, 3, 1, PINK); g.rect(hx + 2, hy + 4, 1, 1, PINK);
        txt(g, "x" + Math.max(0, r.lives), hx + 7, hy + 5, 8, CREAM, -1);
        txt(g, "BAJAS " + r.kills, Math.round(x + w - 4), hy + 5, 8, GOLD, 1);
    }
}
