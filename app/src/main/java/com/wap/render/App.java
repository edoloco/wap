package com.wap.render;
import com.wap.core.*;
import com.wap.net.*;
import java.util.ArrayList; import java.util.List;
/** Pantallas y flujo de la aplicación. No conoce Android; recibe entradas y dibuja por Gfx. */
public final class App {
    public static final int TITLE = 0, ARMS = 1, PLAY = 2, PAUSE = 3, OVER = 4, PAINT = 5, LOBBY = 6;
    /** Servicios del teléfono para jugar en red. */
    public interface Device { String name(); void listen(boolean on); }
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
        try { level = Integer.parseInt(st.get("level", "1")); livesIx = Integer.parseInt(st.get("lives", "2")); } catch (Exception e) { }
    }
    public void seed(int s) { seed = s; }
    public void start() {
        Wpn[] a = new Wpn[SLOTS], b = new Wpn[SLOTS]; seed = seed * 1103515245 + 12345;
        Rng r = new Rng(seed ^ 0x5A5A);
        for (int i = 0; i < SLOTS; i++) { a[i] = Wpn.ALL[slots[i]]; b[i] = Wpn.ALL[r.i(Wpn.ALL.length)]; }
        RobotArt.COLOR[0] = myColor; RobotArt.COLOR[1] = near(myColor, 0xFF1266FF) ? 0xFFFF0000 : 0xFF1266FF; me = 0; online = false;
        w = new World(seed, LIVES[livesIx] , a, b); bot = new Bot(seed + 1, 1, SKILL[level]); ren.reset(w); cd = 90; screen = PLAY; overT = 0;
        pad.clear();
    }
    static boolean near(int a, int b) {
        int r = (a >> 16 & 255) - (b >> 16 & 255), g = (a >> 8 & 255) - (b >> 8 & 255), l = (a & 255) - (b & 255);
        return r * r + g * g + l * l < 14000;
    }
    // ---------- Partida en red local
    Link.Hello hello(int seedV, int lives) {
        Link.Hello h = new Link.Hello(); h.rules = World.RULES; h.seed = seedV; h.lives = lives; h.color = myColor; h.slots = slots.clone();
        h.name = dev != null ? dev.name() : "Sala WAP"; return h;
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
        final String name = dev != null ? dev.name() : "Sala WAP";
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
        w = new World(h.seed, h.lives, a, b); bot = null; link = l; l.startReader(); ls = new Lockstep(l, me); online = true;
        ren.reset(w); cd = 90; screen = PLAY; overT = 0; pad.clear();
    }
    void endOnline(String msg) {
        if (link != null) link.close(); link = null; ls = null; online = false;
        if (msg != null) { toast = msg; toastT = 300; }
        go(TITLE);
    }
    /** Al cerrar la app. */
    public void quit() { if (online) endOnline(null); closeLobby(); }
    public void back() {
        if (screen == LOBBY) { closeLobby(); go(TITLE); return; }
        if (screen == OVER && online) { endOnline(null); return; }
        if (screen == PAINT) { save(); go(TITLE); return; }
        if (screen == PLAY) screen = PAUSE; else if (screen == PAUSE) screen = PLAY; else if (screen == ARMS) { save(); go(TITLE); } else if (screen == OVER) go(TITLE);
    }
    public void pauseNow() { if (screen == PLAY) screen = PAUSE; }
    void save() { st.put("traje", String.format("%06X", myColor & 0xFFFFFF)); for (int i = 0; i < SLOTS; i++) st.put(key(i), Wpn.ALL[slots[i]].name); st.put("level", "" + level); st.put("lives", "" + livesIx); }
    void go(int s) { screen = s; focus = 0; pad.clear(); }
    /** Toques del mando en pantalla, convertidos a la capa de interfaz. */
    public void pointer(int id, float x, float y, boolean down) { pad.pointer(id, x / uiScale, y / uiScale, down); }
    public boolean tap(float x, float y) {
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
            for (int n = 0; n < 3; n++) {
                ls.pump(w, mm);
                if (!ls.step(w)) break;
                ren.tick(w); sounds(); ren.consume(w, false);
                if (!ls.behind(w)) break;
            }
            if (w.finished && screen != OVER && ++overT > 110) go(OVER);
            if (screen == OVER) navList(press);
            return;
        }
        if (screen == PLAY) {
            if (cd > 0) cd--;
            else {
                in[0] = m; in[1] = bot.think(w); w.step(in); ren.tick(w);
                sounds();
                ren.consume(w, false);
                if (w.finished && ++overT > 110) go(OVER);
            }
        } else if (screen == ARMS) navArms(press);
        else if (screen == PAINT) navPaint(press, keys);
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
            case Fx.DEATH: au.play(Audio.DEATH); break;
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
    void navPaint(int p, int held) {
        if ((p & Input.DOWN) != 0) paintRow = (paintRow + 1) % 3;
        if ((p & Input.UP) != 0) paintRow = (paintRow + 2) % 3;
        if ((held & Input.LEFT) != 0) channel(paintRow, channel(paintRow) - 3);
        if ((held & Input.RIGHT) != 0) channel(paintRow, channel(paintRow) + 3);
        if ((p & (Input.A | Input.B)) != 0) back();
    }
    /** Arrastre sobre las barras de color. */
    public void drag(float x, float y) {
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
        g.rect(X + 1, Y, Wd - 2, Hd, INK); g.rect(X, Y + 1, Wd, Hd - 2, INK);
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
        txtS(g, "COLOR DEL ROBOT", sliderX(), 28 * u, 20 * u, GOLD, -1);
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
        if ((p & Input.A) != 0) { slots[selSlot] = cur; selSlot = (selSlot + 1) % SLOTS; au.play(Audio.PICK); }
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
        if (game) ren.drawWorld(g, w, me); else g.clear(DEEP);
        g.beginPixels((g.w() + s - 1) / s, (g.h() + s - 1) / s); // toda la interfaz va en pixel art
        W = (g.w() + s - 1) / s; H = (g.h() + s - 1) / s; u = H / 360f; pad.layout(W, H); btns.clear(); acts.clear(); heroV = null;
        switch (screen) {
            case TITLE: title(g); break;
            case ARMS: arms(g); break;
            case PAINT: paint(g); break;
            case LOBBY: lobby(g); break;
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
                    else menu(g, W / 2 - 110 * u, H * .4f, 220 * u, new String[]{"SEGUIR", "VOLVER AL MENÚ"}, new Runnable[]{() -> screen = PLAY, () -> go(TITLE)});
                } else {
                    veil(g); boolean win = w.winner == me;
                    txtS(g, win ? "¡GANASTE!" : "PERDISTE", W / 2, H * .28f, 48 * u, win ? GOLD : RED, 0);
                    txt(g, "BAJAS " + w.robots[me].kills + "   MUERTES " + w.robots[me].deaths, W / 2, H * .37f, 14 * u, CREAM, 0);
                    if (online) menu(g, W / 2 - 110 * u, H * .44f, 220 * u, new String[]{"VOLVER AL MENÚ"}, new Runnable[]{() -> endOnline(null)});
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
    void menu(Gfx g, float x, float y, float w, String[] l, Runnable[] a) {
        float h = (l.length > 6 ? 31 : l.length > 5 ? 34 : l.length > 4 ? 37 : 40) * u, gap = (l.length > 6 ? 5 : l.length > 4 ? 6 : 9) * u;
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
        float x = W * .5f, wd = W * .44f;
        txtS(g, "WAP", x, H * .17f, 48 * u, GOLD, -1);
        String[] l = {"JUGAR", "EN LÍNEA (RED LOCAL)", "ELEGIR 4 ARMAS", "COLOR DEL ROBOT", "VIDAS  " + LIVES[livesIx], "RIVAL  " + LEVELS[level]};
        Runnable[] a = {this::start, this::openLobby, () -> go(ARMS), () -> go(PAINT), () -> { livesIx = (livesIx + 1) % LIVES.length; save(); }, () -> { level = (level + 1) % 3; save(); }};
        if (up != null) { l = new String[]{l[0], l[1], l[2], l[3], l[4], l[5], up.label()}; a = new Runnable[]{a[0], a[1], a[2], a[3], a[4], a[5], () -> up.act()}; txt(g, up.version(), W - 4, H - 3, 8, MUTE, 1); }
        menu(g, x, H * .21f, wd, l, a);
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
        txtS(g, "TUS 4 ARMAS", m, y + 16 * u, 15 * u, GOLD, -1); y += 24 * u;
        for (int i = 0; i < SLOTS; i++) {
            boolean sel = i == selSlot; final int ix = i;
            if (sel) box(g, m, y, cw, row - 5 * u, RED, 0xFFFF8A8F, 0xFF8E1520); else box(g, m, y, cw, row - 5 * u, BLUE, SKY, NAVY);
            combo(g, i, m + 6 * u, y + 17 * u, 14 * u, sel ? GOLD : CREAM);
            txtS(g, Wpn.ALL[slots[i]].name, m + 46 * u, y + 23 * u, 8, CREAM, -1);
            btn(m, y, cw, row - 5 * u, () -> selSlot = ix); y += row;
        }
        float bx = m, by = H - 48 * u;
        box(g, bx, by, cw, 38 * u, MINT, 0xFF9CF5C9, 0xFF1F8A5B); txtS(g, "LISTO", bx + cw / 2, by + 24 * u, 16 * u, CREAM, 0);
        btn(bx, by, cw, 38 * u, () -> { save(); go(TITLE); });
        float gx = m + cw + 10 * u, gw = (W - gx - m) / COLS, gh = 28 * u, gy = 8 * u; int n = Wpn.ALL.length;
        for (int i = 0; i < n; i++) {
            final int ix = i; float x = gx + i % COLS * gw, yy = gy + i / COLS * gh;
            boolean c = i == cur, eq = false; for (int s : slots) eq |= s == i;
            if (c) box(g, x, yy, gw - u, gh - 2 * u, GOLD, CREAM, 0xFFB8862A);
            else if (eq) box(g, x, yy, gw - u, gh - 2 * u, PURPLE, 0xFFB46BFF, 0xFF4A00A8);
            else box(g, x, yy, gw - u, gh - 2 * u, NAVY, BLUE, DEEP);
            txt(g, Wpn.ALL[i].name, x + gw / 2, yy + gh / 2 + 3, 8, c ? DEEP : CREAM, 0);
            btn(x, yy, gw, gh, () -> { cur = ix; slots[selSlot] = ix; selSlot = (selSlot + 1) % SLOTS; });
        }
        txt(g, Wpn.ALL[cur].desc, gx + 2, H - 3, 8, SKY, -1);
    }
    void hud(Gfx g) {
        Robot me = w.robots[this.me], e = w.robots[1 - this.me];
        float bw = 168 * u; bar(g, 4, 4, bw, me, this.me); bar(g, W - 4 - bw, 4, bw, e, 1 - this.me);
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
