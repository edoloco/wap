package com.wap.render;
import com.wap.core.*;
import java.util.ArrayList;
/** Pantallas y flujo de la aplicación. No conoce Android; recibe entradas y dibuja por Gfx. */
public final class App {
    public static final int TITLE = 0, ARMS = 1, PLAY = 2, PAUSE = 3, OVER = 4, PAINT = 5;
    static final int[] SWATCH = {0xFF5E5650, 0xFFFF0000, 0xFFFF7A00, 0xFFFFD200, 0xFF19C24A, 0xFF00B8D4, 0xFF1266FF, 0xFF8214FF, 0xFFFF2FA8, 0xFF2A2A2A};
    static final int RED = 0xFFFF0000, INK = RobotArt.INK, WHITE = 0xFFFFFFFF, MUTE = 0xFF6B7280, LINE = 0xFFCBD2DC;
    static final String[] LEVELS = {"FÁCIL", "NORMAL", "DIFÍCIL"}; static final float[] SKILL = {.45f, .9f, 1.6f};
    static final int[] LIVES = {3, 5, 10, 15, 30};
    static final String[] DEF = {"GRENADE", "BAZOOKA", "PLASMA CANNON", "PLASMA GUN"};
    static final int SLOTS = Robot.SLOTS;
    static String key(int i) { return "arma" + (i == 3 ? 4 : i); } // conserva lo elegido en versiones anteriores
    public int screen = TITLE, keys, last, focus; public boolean padSeen, startKey; public Updater up;
    final Store st; final Audio au; public final TouchPad pad = new TouchPad(); public final Renderer ren = new Renderer();
    public World w; Bot bot; float W = 640, H = 360, u = 1; int cd; int paintRow, level = 1, livesIx = 2, selSlot = 3, cur, seed = 12345, overT; long frames;
    final int[] slots = new int[SLOTS]; final int[] in = new int[2];
    final ArrayList<float[]> btns = new ArrayList<>(); final ArrayList<Runnable> acts = new ArrayList<>();
    final RobotArt.View hero = RobotArt.poster();

    public App(Store st, Audio au) {
        this.st = st; this.au = au;
        for (int i = 0; i < SLOTS; i++) { slots[i] = Wpn.index(st.get(key(i), DEF[i])); }
        try { RobotArt.COLOR[0] = 0xFF000000 | Integer.parseInt(st.get("traje", "5E5650"), 16); } catch (Exception e) { }
        try { level = Integer.parseInt(st.get("level", "1")); livesIx = Integer.parseInt(st.get("lives", "2")); } catch (Exception e) { }
    }
    public void seed(int s) { seed = s; }
    public void start() {
        Wpn[] a = new Wpn[SLOTS], b = new Wpn[SLOTS]; seed = seed * 1103515245 + 12345;
        Rng r = new Rng(seed ^ 0x5A5A);
        for (int i = 0; i < SLOTS; i++) { a[i] = Wpn.ALL[slots[i]]; b[i] = Wpn.ALL[r.i(Wpn.ALL.length)]; }
        RobotArt.COLOR[1] = near(RobotArt.COLOR[0], 0xFF1266FF) ? 0xFFFF0000 : 0xFF1266FF;
        w = new World(seed, LIVES[livesIx] , a, b); bot = new Bot(seed + 1, 1, SKILL[level]); ren.reset(w); cd = 90; screen = PLAY; overT = 0;
        pad.clear();
    }
    static boolean near(int a, int b) {
        int r = (a >> 16 & 255) - (b >> 16 & 255), g = (a >> 8 & 255) - (b >> 8 & 255), l = (a & 255) - (b & 255);
        return r * r + g * g + l * l < 14000;
    }
    public void back() {
        if (screen == PAINT) { save(); go(TITLE); return; }
        if (screen == PLAY) screen = PAUSE; else if (screen == PAUSE) screen = PLAY; else if (screen == ARMS) { save(); go(TITLE); } else if (screen == OVER) go(TITLE);
    }
    public void pauseNow() { if (screen == PLAY) screen = PAUSE; }
    void save() { st.put("traje", String.format("%06X", RobotArt.COLOR[0] & 0xFFFFFF)); for (int i = 0; i < SLOTS; i++) st.put(key(i), Wpn.ALL[slots[i]].name); st.put("level", "" + level); st.put("lives", "" + livesIx); }
    void go(int s) { screen = s; focus = 0; pad.clear(); }
    public boolean tap(float x, float y) {
        for (int i = btns.size() - 1; i >= 0; i--) { float[] b = btns.get(i); if (x >= b[0] && y >= b[1] && x <= b[2] && y <= b[3]) { au.play(Audio.MENU); acts.get(i).run(); return true; } }
        return false;
    }
    public void tick() {
        frames++;
        int m = keys | (screen == PLAY ? pad.mask() : 0), press = keys & ~last; last = keys;
        if (startKey) { startKey = false; if (screen == PLAY || screen == PAUSE) back(); else if (screen == TITLE) start(); }
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
            case Fx.FLASH: au.play(Audio.SHOT); break;
            case Fx.ARMOR: au.play(Audio.ARMOR); break;
            case Fx.HIT: au.play(Audio.HURT); break;
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
    void channel(int ch, int v) { int s = (2 - ch) * 8, c = RobotArt.COLOR[0]; RobotArt.COLOR[0] = c & ~(255 << s) | Math.max(0, Math.min(255, v)) << s; }
    int channel(int ch) { return RobotArt.COLOR[0] >> (2 - ch) * 8 & 255; }
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
        for (int i = 0; i < 3; i++) {
            float ty = sliderY(i);
            if (Math.abs(y - ty) < 22 * u && x > sliderX() - 16 * u && x < sliderX() + sliderW() + 16 * u) { paintRow = i; channel(i, (int) ((x - sliderX()) / sliderW() * 255)); }
        }
    }
    float sliderX() { return W * .5f + 24 * u; } float sliderW() { return W * .5f - 60 * u; } float sliderY(int i) { return 66 * u + i * 56 * u; }
    /** Personaje grande en pixel art, para menús. */
    void hero(Gfx g, RobotArt.View v, float cx, float by) {
        int sc = Math.max(2, (int) (H * .78f / 56)), lw = (int) W / sc + 1, lh = (int) H / sc + 1;
        g.beginPixels(lw, lh); g.save(); g.translate(Math.round(cx / sc), Math.round(by / sc));
        RobotArt.drawPose(g, v, (1 << Robot.PIECES) - 1, RobotArt.COLOR[0], 0); g.restore(); g.endPixels(0, 0, sc);
    }
    void paint(Gfx g) {
        g.clear(WHITE);
        RobotArt.View v = new RobotArt.View();
        hero(g, v, W * .24f, H * .93f);
        g.text("COLOR DEL ROBOT", sliderX(), 28 * u, 20 * u, INK, -1);
        String[] n = {"ROJO", "VERDE", "AZUL"}; int[] tone = {0xFFFF0000, 0xFF19A84A, 0xFF1266FF};
        for (int i = 0; i < 3; i++) {
            float y = sliderY(i), x = sliderX(), w = sliderW(), k = channel(i) / 255f;
            g.text(n[i], x, y - 11 * u, 11 * u, INK, -1); g.text("" + channel(i), x + w, y - 11 * u, 11 * u, INK, 1);
            g.rrect(x, y - 5 * u, w, 10 * u, 2 * u, 0xFFE4E7EC, i == paintRow && padSeen ? INK : LINE, 1.4f * u);
            g.rrect(x, y - 5 * u, Math.max(4 * u, w * k), 10 * u, 2 * u, tone[i], 0, 0);
            g.circle(x + w * k, y, 11 * u, WHITE, INK, 2 * u);
        }
        float sz = (sliderW() + 8 * u) / SWATCH.length, y = sliderY(2) + 26 * u;
        for (int i = 0; i < SWATCH.length; i++) {
            final int c = SWATCH[i]; float x = sliderX() + i * sz;
            g.rrect(x, y, sz - 5 * u, 28 * u, 2 * u, c, INK, RobotArt.COLOR[0] == c ? 2.6f * u : 1 * u);
            btn(x, y, sz - 5 * u, 28 * u, () -> { RobotArt.COLOR[0] = c; save(); });
        }
        float by = H - 46 * u;
        g.rrect(sliderX(), by, sliderW(), 36 * u, 3 * u, INK, INK, 1); g.text("LISTO", sliderX() + sliderW() / 2, by + 24 * u, 15 * u, WHITE, 0);
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
        g.text(b, tx, cy + s * .36f, s, c, -1);
    }
    public void draw(Gfx g) {
        W = g.w(); H = g.h(); u = H / 360f; pad.layout(W, H); btns.clear(); acts.clear();
        switch (screen) {
            case TITLE: title(g); break;
            case ARMS: arms(g); break;
            case PAINT: paint(g); break;
            default:
                ren.drawWorld(g, w, 0); hud(g);
                if (screen == PLAY) {
                    if (cd > 0) { g.text(cd > 60 ? "3" : cd > 30 ? "2" : "1", W / 2, H * .42f, 70 * u, RED, 0); }
                    pad.draw(g, pad.mask()); float x = W / 2 - 18 * u, y = H - 40 * u;
                    if (pad.visible) { g.rrect(x, y, 36 * u, 28 * u, 5 * u, 0x44FFFFFF, 0xAA141414, 1.5f * u); g.rect(x + 12 * u, y + 7 * u, 4 * u, 14 * u, 0xAA141414); g.rect(x + 20 * u, y + 7 * u, 4 * u, 14 * u, 0xAA141414); btn(x - 8 * u, y - 8 * u, 52 * u, 44 * u, () -> screen = PAUSE); }
                } else if (screen == PAUSE) {
                    veil(g); g.text("PAUSA", W / 2, H * .3f, 42 * u, WHITE, 0);
                    menu(g, W / 2 - 100 * u, H * .4f, 200 * u, new String[]{"SEGUIR", "VOLVER AL MENÚ"}, new Runnable[]{() -> screen = PLAY, () -> go(TITLE)});
                } else {
                    veil(g); boolean win = w.winner == 0;
                    g.text(win ? "GANASTE" : "PERDISTE", W / 2, H * .28f, 50 * u, win ? WHITE : RED, 0);
                    g.text("Bajas " + w.robots[0].kills + "   Muertes " + w.robots[0].deaths, W / 2, H * .37f, 15 * u, WHITE, 0);
                    menu(g, W / 2 - 100 * u, H * .44f, 200 * u, new String[]{"REVANCHA", "VOLVER AL MENÚ"}, new Runnable[]{this::start, () -> go(TITLE)});
                }
        }
    }
    void veil(Gfx g) { g.rect(0, 0, W, H, 0xB8141414); }
    void menu(Gfx g, float x, float y, float w, String[] l, Runnable[] a) {
        float h = (l.length > 5 ? 34 : l.length > 4 ? 37 : 40) * u, gap = (l.length > 4 ? 5 : 8) * u;
        for (int i = 0; i < l.length; i++) {
            boolean f = i == focus && !pad.visible || i == focus && padSeen;
            g.rrect(x, y, w, h, 3 * u, f ? RED : WHITE, INK, 2 * u);
            g.text(l[i], x + w / 2, y + h * .66f, 16 * u, f ? WHITE : INK, 0);
            btn(x, y, w, h, a[i]); y += h + gap;
        }
    }
    void title(Gfx g) {
        g.clear(WHITE);
        int G = (int) (32 * u); for (int x = 0; x < W; x += G) g.line(x, 0, x, H, Renderer.GRID, 1); for (int y = 0; y < H; y += G) g.line(0, y, W, y, Renderer.GRID, 1);
        hero.lean = frames / 30 % 2 == 0 ? 4 : 6;
        hero(g, hero, W * .25f, H * .92f);
        float x = W * .5f, wd = W * .42f;
        g.text("WAP", x - 3 * u, H * .19f, 58 * u, INK, -1);
        String[] l = {"JUGAR", "ELEGIR 4 ARMAS", "COLOR DEL ROBOT", "VIDAS  " + LIVES[livesIx], "RIVAL  " + LEVELS[level]};
        Runnable[] a = {this::start, () -> go(ARMS), () -> go(PAINT), () -> { livesIx = (livesIx + 1) % LIVES.length; save(); }, () -> { level = (level + 1) % 3; save(); }};
        if (up != null) { l = new String[]{l[0], l[1], l[2], l[3], l[4], up.label()}; a = new Runnable[]{a[0], a[1], a[2], a[3], a[4], () -> up.act()}; g.text(up.version(), W - 8 * u, H - 7 * u, 10 * u, MUTE, 1); }
        menu(g, x, H * .22f, wd, l, a);
    }
    void arms(Gfx g) {
        g.clear(WHITE);
        float m = 12 * u, cw = 190 * u, row = 44 * u, y = 10 * u;
        g.text("TUS 4 ARMAS", m, y + 15 * u, 15 * u, INK, -1); y += 24 * u;
        for (int i = 0; i < SLOTS; i++) {
            boolean sel = i == selSlot; final int ix = i;
            g.rrect(m, y, cw, row - 5 * u, 3 * u, sel ? RED : WHITE, INK, 1.6f * u);
            int c = sel ? WHITE : INK;
            combo(g, i, m + 8 * u, y + 19 * u, 14 * u, c);
            g.text(Wpn.ALL[slots[i]].name, m + 44 * u, y + 24 * u, 13 * u, c, -1);
            btn(m, y, cw, row - 5 * u, () -> selSlot = ix); y += row;
        }
        float bx = m, by = H - 48 * u;
        g.rrect(bx, by, cw, 38 * u, 3 * u, INK, INK, 1); g.text("LISTO", bx + cw / 2, by + 25 * u, 15 * u, WHITE, 0);
        btn(bx, by, cw, 38 * u, () -> { save(); go(TITLE); });
        float gx = m + cw + 12 * u, gw = (W - gx - m) / COLS, gh = 29 * u, gy = 8 * u; int n = Wpn.ALL.length;
        for (int i = 0; i < n; i++) {
            final int ix = i; float x = gx + i % COLS * gw, yy = gy + i / COLS * gh;
            boolean c = i == cur, eq = false; for (int s : slots) eq |= s == i;
            g.rrect(x + u, yy + u, gw - 2 * u, gh - 3 * u, 2 * u, eq ? 0xFFFFE3E3 : WHITE, c ? RED : LINE, c ? 2.2f * u : 1 * u);
            g.text(Wpn.ALL[i].name, x + gw / 2, yy + 17 * u, 10.5f * u, INK, 0);
            btn(x, yy, gw, gh, () -> { cur = ix; slots[selSlot] = ix; selSlot = (selSlot + 1) % SLOTS; });
        }
        g.text(Wpn.ALL[cur].desc, gx + 4 * u, H - 10 * u, 12 * u, MUTE, -1);
    }
    void hud(Gfx g) {
        Robot me = w.robots[0], e = w.robots[1];
        bar(g, 10 * u, 8 * u, me, 0, false); bar(g, W - 134 * u, 8 * u, e, 1, true);
        int n = SLOTS; float cw = 72 * u, tot = cw * n, x = W / 2 - tot / 2;
        for (int i = 0; i < n; i++) {
            float y = 7 * u, h = 30 * u, cx = x + i * cw; Wpn wp = me.loadout[i];
            g.rrect(cx + u, y, cw - 2 * u, h, 2 * u, 0xE6FFFFFF, INK, 1.2f * u);
            combo(g, i, cx + 5 * u, y + 10 * u, 10 * u, INK);
            float frac = me.reload[i] > 0 ? 1 - me.reload[i] / (float) wp.reload : me.ammo[i] / (float) wp.clip;
            int c = me.reload[i] > 0 ? 0xFF9AA3B2 : RED;
            if (wp.charge > 0 && me.reload[i] == 0) { frac = me.charge[i] / (float) wp.charge; c = 0xFFE0A800; }
            g.rect(cx + 5 * u, y + 23 * u, (cw - 10 * u) * Math.max(0, frac), 3.5f * u, c);
            String nm = wp.name.length() > 9 ? wp.name.substring(0, 9) : wp.name;
            g.text(nm, cx + 5 * u, y + 20 * u, 7.5f * u, INK, -1);
        }
    }
    void bar(Gfx g, float x, float y, Robot r, int id, boolean right) {
        float w = 124 * u, h = 30 * u;
        g.rrect(x, y - u, w, h, 2 * u, 0xE6FFFFFF, INK, 1.2f * u);
        float k = r.hp / 100f; g.rect(x + 3 * u, y + 2 * u, (w - 6 * u) * Math.max(0, k), 9 * u, RobotArt.COLOR[id]);
        g.text("VIDAS " + Math.max(0, r.lives) + "   BAJAS " + r.kills, x + 4 * u, y + 23 * u, 10 * u, INK, -1);
    }
}
