package com.wap.core;
/** Definición de un arma: cargador, tiempo de recarga y comportamiento del proyectil, al estilo de la tabla de armas de Liero. */
public final class Wpn {
    public static final int BULLET = 0, ROCKET = 1, GRENADE = 2, MINE = 3, FLAME = 4, SPIKE = 5, DIRT = 6, ORB = 7, NUKE = 8, SPARK = 9, CRATE = 10, LEMON = 11, PLASMA = 12, GREEN = 13, BLASER = 14, LIGHT = 15, RAY = 16, GAS = 17, WIND = 18, GLOVE = 19, PROTON = 20, TRAP = 21, DRONE = 22, MISSILE = 23;
    /** name: identificador (no cambia; con él se guardan las armas elegidas). label: nombre que se muestra. */
    public String name, desc = "", label;
    /** Arma original de la que viene (para las creadas en armas.txt; decide, por ejemplo, el sonido). */
    public String base;
    /** Quitada del juego con armas.txt: no aparece para elegir ni al azar. */
    public boolean off;
    /** Dron teledirigido: mientras vuela, los controles del dueño lo manejan (ver Shot.droneMove). */
    public boolean drone;
    /** Aspecto (armas.txt): color nuevo (0 = el de siempre), tamaño, si se ve su luz y un dibujo propio que reemplaza al cuerpo. */
    public int tint; public float size = 1; public boolean light = true; public Art[] art;
    /** Una forma de un dibujo propio, en unidades del juego, mirando hacia la derecha (hacia donde vuela). */
    public static final class Art { public float[][] polys; public int fill, stroke; public float sw; public boolean eo; }
    /** Nombres de los aspectos para armas.txt, en el orden de las constantes BULLET..MISSILE. */
    public static final String[] LOOKS = {"BALA", "COHETE", "GRANADA", "MINA", "FUEGO", "PUAS", "TIERRA", "ORBE", "NUKE", "CHISPA", "CAPSULAS", "LIMON", "PLASMA",
        "VERDE", "LASER", "LUZ", "RAYO", "GAS", "VIENTO", "GUANTE", "PROTON", "TRAMPA", "DRON", "MISIL"};
    public static final int MAX_ART_POINTS = 600;
    public int charge, clip = 1, delay = 20, reload = 100, parts = 1, fuse = 0, look = BULLET;
    public float spread, speed = 10, grav, bounce, drag = 1, accel, dmg, knock = 1, recoil, lob, jitter;
    public int radius, dig;          // radio de explosión (0 = sin explosión) y radio de excavación
    public boolean punch, infinite, lasso, trap, beam, pierce, sticky, burn, impact = true, smoke, fan;
    public Wpn splinter; public int nSplinter, dropEvery, hops, addDirt, trigger, maxBounce, stagger, boomStyle; public float angle;
    Wpn(String n, String d) { name = n; desc = d; label = n; base = n; }
    /** Copia completa (mismo comportamiento) con otro nombre: así se crean armas nuevas. */
    Wpn copy(String n) {
        Wpn c = new Wpn(n, desc);
        try { for (java.lang.reflect.Field f : Wpn.class.getDeclaredFields()) if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) { f.setAccessible(true); f.set(c, f.get(this)); } }
        catch (IllegalAccessException e) { throw new IllegalStateException(e); }
        c.name = n; c.label = n; c.base = base; c.off = false; return c;
    }
    Wpn mag(int c, int dl, int rl) { clip = c; delay = dl; reload = rl; return this; }
    Wpn shot(int p, float sp, float v) { parts = p; spread = sp; speed = v; return this; }
    Wpn phys(float g, float b, float dr) { grav = g; bounce = b; drag = dr; return this; }
    Wpn hit(float d, int dg, float k) { dmg = d; dig = dg; knock = k; return this; }
    Wpn boom(int r, float d, int dg, float k) { radius = r; dmg = d; dig = dg; knock = k; return this; }
    Wpn fuse(int f) { fuse = f; return this; }
    Wpn look(int l) { look = l; return this; }
    Wpn rec(float r) { recoil = r; return this; }
    Wpn lob(float deg) { lob = deg; return this; }
    Wpn split(Wpn w, int n) { splinter = w; nSplinter = n; return this; }

    private static Wpn sub(String n) { return new Wpn(n, ""); }
    /** Todas las armas: las de siempre y, al final, las creadas en armas.txt. */
    public static Wpn[] ALL;
    static int BASE_COUNT;
    static {
        Wpn frag = sub("frag").shot(1, 180, 5).phys(.14f, .4f, 1).boom(14, 7, 12, 1.4f).fuse(50).look(GRENADE);
        Wpn shrap = sub("shrap").shot(1, 180, 8).phys(.08f, 0, .98f).hit(6, 5, 1).fuse(34);
        Wpn fire = sub("fire").shot(1, 180, 6).phys(.14f, 0, .96f).hit(1.1f, 0, .1f).fuse(190).look(FLAME); fire.burn = true; fire.impact = false; fire.sticky = true;
        Wpn drop = sub("drop").shot(1, 8, 1).phys(.2f, 0, 1).boom(16, 8, 14, 1.4f).look(GRENADE);
        Wpn chiq = sub("chiq").shot(1, 180, 4.5f).phys(.14f, .35f, 1).boom(24, 16, 20, 2.2f).fuse(55).look(GRENADE).split(frag, 4);
        Wpn chiqBig = sub("chiqb").shot(1, 180, 4.5f).phys(.14f, .35f, 1).boom(24, 16, 20, 2.2f).fuse(55).look(GRENADE).split(frag, 8);
        Wpn hop = sub("");
        Wpn missile = sub("misil").shot(1, 0, 8.5f).boom(8, 7, 6, 1).look(MISSILE); missile.impact = true;
        ALL = new Wpn[] {
            new Wpn("PLASMA GUN", "Ráfaga de disparos de energía").mag(30, 9, 90).shot(1, 0, 9.5f).hit(5, 4, .5f).look(LEMON),
            new Wpn("CANNON PUNCH", "Guante que sale volando y lanza al rival").mag(1, 20, 80).shot(1, 0, 7.5f).hit(4, 0, 0).fuse(48).look(GLOVE),
            new Wpn("PLASMA CANNON", "Comando para cargar, comando para lanzar").mag(1, 12, 30).shot(1, 0, 8).boom(34, 60, 30, 4).look(PLASMA).rec(2),
            new Wpn("DEATH RAY", "Rayo celeste de cinco cargas que atraviesa").mag(1, 30, 200).shot(5, 0, 4.5f).boom(18, 9, 16, 1.6f).fuse(300).look(RAY).rec(1.5f),
            new Wpn("SHOTGUN", "Abanico de perdigones").mag(5, 34, 120).shot(9, 8, 11).phys(.02f, 0, 1).hit(5, 5, .8f).rec(1.6f),
            new Wpn("SUPER SHOTGUN", "Doble cañón, mucho retroceso").mag(2, 26, 150).shot(26, 13, 12).phys(.02f, 0, 1).hit(5, 5, 1).rec(3.2f),
            new Wpn("CHAINGUN", "Ráfaga larga").mag(50, 3, 220).shot(1, 3, 12).phys(.01f, 0, 1).hit(3, 4, .35f).rec(.12f),
            new Wpn("UZI", "Ráfaga corta y rápida").mag(30, 2, 150).shot(1, 6, 11).phys(.02f, 0, 1).hit(2.4f, 3, .25f).rec(.1f),
            new Wpn("RIFLE", "Un tiro certero").mag(1, 10, 95).shot(1, 0, 34).hit(36, 6, 3).rec(1).look(LIGHT),
            new Wpn("WINCHESTER", "Seis tiros de precisión").mag(6, 28, 150).shot(1, 1, 26).hit(20, 5, 2).rec(.8f),
            new Wpn("BAZOOKA", "Cohete que acelera").mag(3, 42, 130).shot(1, 1, 4).boom(36, 30, 32, 3.4f).look(ROCKET).rec(1.5f),
            new Wpn("DOOMSDAY", "Cinco cohetes de una vez").mag(1, 40, 240).shot(5, 13, 4.5f).boom(28, 18, 25, 2.4f).look(ROCKET).rec(3),
            new Wpn("PROTON RAY", "Mantén el disparo: el rayo atrapa al rival y se agota lento").mag(240, 1, 240).shot(1, 0, 0).hit(0, 0, 0).look(PROTON),
            new Wpn("GRENADE", "Rebota y estalla").mag(3, 40, 160).shot(1, 2, 6.8f).phys(.17f, .5f, 1).boom(38, 34, 34, 3.4f).fuse(110).look(GRENADE).lob(22),
            new Wpn("CLUSTER BOMB", "Se parte en diez").mag(2, 40, 210).shot(1, 2, 6.5f).phys(.17f, .35f, 1).boom(22, 12, 20, 2).fuse(70).look(GRENADE).lob(25).split(frag, 10),
            new Wpn("MINI BOMB", "Se parte y se vuelve a partir").mag(1, 40, 280).shot(1, 2, 6).phys(.17f, .35f, 1).boom(26, 14, 22, 2).fuse(75).look(GRENADE).lob(25).split(chiq, 5),
            new Wpn("BIG BOMB", "Como Mini Bomb, con el doble de pedazos").mag(1, 40, 360).shot(1, 2, 6).phys(.17f, .35f, 1).boom(30, 18, 26, 2.4f).fuse(75).look(GRENADE).lob(25).split(chiqBig, 10),
            new Wpn("MINE", "Espera pegada al suelo").mag(3, 26, 200).shot(1, 2, 4.5f).phys(.2f, 0, 1).boom(38, 44, 32, 3.6f).look(MINE).lob(20),
            new Wpn("TRAP", "Parece un bonus de cura").mag(2, 26, 200).shot(1, 2, 4.5f).phys(.2f, 0, 1).boom(32, 36, 26, 3).look(CRATE).lob(20),
            new Wpn("FLAK CANNON", "Revienta en el aire").mag(3, 34, 150).shot(1, 2, 9).boom(14, 8, 12, 1).fuse(34).look(GRENADE).split(shrap, 28),
            new Wpn("NAPALM", "Deja el suelo ardiendo").mag(1, 40, 230).shot(1, 2, 6.5f).phys(.14f, 0, 1).boom(24, 12, 20, 2).look(GRENADE).lob(18).split(fire, 32),
            new Wpn("FLAMER", "Lanzallamas de largo alcance").mag(70, 1, 200).shot(2, 9, 8.5f).phys(-.02f, 0, .97f).hit(1.2f, 3, .06f).fuse(44).look(FLAME),
            new Wpn("ROBOT TRAP", "Cárgala; lista, A con arriba, adelante o abajo elige dónde cae").mag(1, 30, 420).shot(1, 2, 5.5f).phys(.2f, 0, 1).fuse(0).look(TRAP).lob(22),
            new Wpn("LASER", "Rayo continuo").mag(40, 1, 220).hit(1.6f, 2, .05f).look(ORB),
            new Wpn("BOUNCY LASER", "Láser que rebota en diagonal").mag(1, 20, 160).shot(1, 0, 5.5f).phys(0, 1, 1).boom(14, 14, 12, 1.6f).fuse(600).look(BLASER),
            new Wpn("ZIMM", "Rebota en todo y no pierde fuerza").mag(1, 20, 120).shot(1, 0, 9).phys(0, 1, 1).hit(30, 0, 2.4f).fuse(320).look(ORB),
            new Wpn("SPIKEBALLS", "Púas que rebotan").mag(3, 40, 180).shot(6, 18, 6).phys(.16f, .65f, 1).hit(7, 0, .8f).fuse(230).look(SPIKE).lob(14),
            new Wpn("HELLRAIDER", "Chispas enloquecidas").mag(50, 2, 240).shot(1, 25, 7).phys(.05f, .8f, 1).hit(2.4f, 2, .3f).fuse(90).look(SPARK),
            new Wpn("CRACKLER", "Nube de chispas en zigzag").mag(8, 16, 180).shot(1, 3, 7).phys(0, .6f, 1).boom(10, 12, 9, 1).fuse(120).look(GAS),
            new Wpn("GRASSHOPPER", "Salta explotando").mag(3, 40, 180).shot(1, 2, 6.5f).phys(.17f, .9f, 1).boom(24, 16, 20, 2.4f).fuse(0).look(GREEN).lob(30),
            new Wpn("MINI NUKE", "Cráter enorme").mag(1, 40, 300).shot(1, 1, 5).phys(.05f, 0, 1).boom(68, 64, 66, 6).look(NUKE).rec(2),
            new Wpn("BIG NUKE", "Lenta y devastadora").mag(1, 40, 480).shot(1, 1, 2.6f).boom(125, 100, 120, 9).look(NUKE).rec(2),
            new Wpn("FAN", "Desvía balas y empuja; nunca se acaba").mag(1, 1, 0).shot(1, 12, 8).phys(0, 0, .96f).hit(0, 0, .35f).fuse(36).look(WIND),
            new Wpn("DRONE", "Teledirigido: la cruceta lo maneja, A dispara misiles, B lo explota").mag(1, 30, 420).shot(1, 0, 3.2f).boom(34, 45, 26, 3.5f).fuse(600).look(DRONE),
        };
        for (Wpn w : ALL) {
            if (w.name.equals("BAZOOKA") || w.name.equals("DOOMSDAY")) { w.accel = .24f; w.smoke = true; }
            if (w.name.equals("MINE") || w.name.equals("TRAP")) { w.sticky = true; w.trigger = 28; }
            if (w.name.equals("PLASMA CANNON")) w.charge = 75;
            if (w.name.equals("CANNON PUNCH")) { w.punch = true; w.impact = true; }
            if (w.name.equals("DEATH RAY")) { w.stagger = 7; w.boomStyle = 1; w.impact = true; }
            if (w.name.equals("BOUNCY LASER")) { w.maxBounce = 10; w.angle = 40; w.impact = true; w.boomStyle = 2; }
            if (w.name.equals("FAN")) w.infinite = true;
            if (w.name.equals("LASER")) w.beam = true;
            if (w.name.equals("PROTON RAY")) w.lasso = true;
            if (w.name.equals("ROBOT TRAP")) { w.trap = true; w.charge = 150; }
            if (w.name.equals("GRASSHOPPER")) w.hops = 5;
            if (w.name.equals("CRACKLER")) w.jitter = .55f;
            if (w.name.equals("HELLRAIDER")) w.jitter = .2f;
            if (w.name.equals("FAN")) w.fan = true;
            if (w.name.equals("ZIMM")) w.angle = 40;
            if (w.name.equals("ZIMM") || w.name.equals("SPIKEBALLS") || w.name.equals("HELLRAIDER")) w.impact = true;
            if (w.name.equals("DRONE")) { w.drone = true; w.splinter = missile; w.nSplinter = 0; }
        }
    }
    /** Nombres antiguos guardados en el teléfono. */
    static String rename(String n) {
        switch (n) { case "DROP": return "CANNON PUNCH"; case "CHIQUITA BOMB": return "MINI BOMB"; case "BOOBY TRAP": return "TRAP"; case "GAUSS GUN": return "BOUNCY LASER"; case "LARPA": return "PROTON RAY"; case "BLOWTORCH": return "ROBOT TRAP"; default: return n; }
    }
    // ---------- armas.txt: cambiar valores, nombres y quitar armas (Taller de armas) ----------
    /** Valores que se pueden cambiar: clave en armas.txt y límites. */
    public static final String[] KEYS = {"cargador", "cadencia", "recarga", "carga", "proyectiles", "dispersion", "velocidad", "gravedad", "rebote", "roce", "aceleracion",
        "dano", "excavar", "empuje", "explosion", "mecha", "retroceso", "arco", "pedazos", "zigzag", "rebotes", "angulo", "saltos", "sensor", "tamano", "luz"};
    static final float[][] LIMITS = {{1, 999}, {1, 600}, {0, 3000}, {0, 3000}, {1, 60}, {0, 180}, {0, 60}, {-1, 2}, {0, 1.2f}, {.5f, 1.05f}, {0, 2},
        {0, 500}, {0, 200}, {0, 30}, {0, 250}, {0, 3000}, {0, 20}, {-90, 90}, {0, 60}, {0, 2}, {0, 100}, {-90, 90}, {0, 50}, {0, 200}, {.3f, 4}, {0, 1}};
    public float get(String k) {
        switch (k) {
            case "cargador": return clip; case "cadencia": return delay; case "recarga": return reload; case "carga": return charge; case "proyectiles": return parts;
            case "dispersion": return spread; case "velocidad": return speed; case "gravedad": return grav; case "rebote": return bounce; case "roce": return drag; case "aceleracion": return accel;
            case "dano": return dmg; case "excavar": return dig; case "empuje": return knock; case "explosion": return radius; case "mecha": return fuse; case "retroceso": return recoil;
            case "arco": return lob; case "pedazos": return nSplinter; case "zigzag": return jitter; case "rebotes": return maxBounce; case "angulo": return angle; case "saltos": return hops; case "sensor": return trigger;
            case "tamano": return size; case "luz": return light ? 1 : 0;
            default: throw new IllegalArgumentException("valor desconocido: " + k);
        }
    }
    void set(String k, float v) {
        int i = java.util.Arrays.asList(KEYS).indexOf(k); if (i < 0) throw new IllegalArgumentException("valor desconocido: " + k);
        if (Float.isNaN(v)) throw new IllegalArgumentException("número inválido en " + k);
        v = Math.max(LIMITS[i][0], Math.min(LIMITS[i][1], v)); int n = Math.round(v);
        switch (k) {
            case "cargador": clip = n; break; case "cadencia": delay = n; break; case "recarga": reload = n; break; case "carga": charge = n; break; case "proyectiles": parts = n; break;
            case "dispersion": spread = v; break; case "velocidad": speed = v; break; case "gravedad": grav = v; break; case "rebote": bounce = v; break; case "roce": drag = v; break; case "aceleracion": accel = v; break;
            case "dano": dmg = v; break; case "excavar": dig = n; break; case "empuje": knock = v; break; case "explosion": radius = n; break; case "mecha": fuse = n; break; case "retroceso": recoil = v; break;
            case "arco": lob = v; break; case "pedazos": nSplinter = splinter == null ? 0 : n; break; case "zigzag": jitter = v; break; case "rebotes": maxBounce = n; break; case "angulo": angle = v; break;
            case "saltos": hops = n; break; case "sensor": trigger = n; break;
            case "tamano": size = v; break; case "luz": light = v >= .5f; break;
            default:
        }
    }
    private static float[][] DEFAULTS; private static String[][] DEF_TEXT; private static int[] DEF_LOOK;
    private static void snapshot() {
        DEFAULTS = new float[ALL.length][KEYS.length]; DEF_TEXT = new String[ALL.length][];
        BASE_COUNT = ALL.length; DEF_LOOK = new int[ALL.length]; for (int i = 0; i < ALL.length; i++) DEF_LOOK[i] = ALL[i].look;
        for (int i = 0; i < ALL.length; i++) { for (int k = 0; k < KEYS.length; k++) DEFAULTS[i][k] = ALL[i].get(KEYS[k]); DEF_TEXT[i] = new String[]{ALL[i].label, ALL[i].desc}; }
    }
    /** Huella de las armas: 0 si son las de siempre. Se compara en línea para que ambos teléfonos jueguen con las mismas. */
    public static int signature;
    public static String error;
    /** Vuelve a las armas de siempre. */
    public static void reset() {
        if (ALL.length != BASE_COUNT) ALL = java.util.Arrays.copyOf(ALL, BASE_COUNT); // quita las armas creadas
        for (int i = 0; i < ALL.length; i++) { for (int k = 0; k < KEYS.length; k++) ALL[i].set(KEYS[k], DEFAULTS[i][k]); ALL[i].label = DEF_TEXT[i][0]; ALL[i].desc = DEF_TEXT[i][1]; ALL[i].off = false; ALL[i].look = DEF_LOOK[i]; ALL[i].tint = 0; ALL[i].art = null; }
        signature = 0; error = null;
    }
    /**
     * Lee armas.txt. Una instrucción por línea; en los nombres, los espacios van como _ (BIG_NUKE):
     *   arma NOMBRE clave=valor clave=valor ...
     *   nombre NOMBRE texto que se muestra
     *   descripcion NOMBRE texto de ayuda
     *   quitar NOMBRE
     *   nueva NOMBRE_NUEVO NOMBRE_BASE   (crea un arma copiando otra; después se cambia con las líneas de arriba)
     *   arma NOMBRE aspecto=COHETE color=RRGGBB tamano=1.5 luz=0   (cómo se ve el disparo; aspectos en LOOKS)
     *   dibujo NOMBRE  /  forma relleno=AARRGGBB contorno=AARRGGBB grosor=N parimpar=0  /  p x y ...  /  fin   (dibujo propio del disparo)
     * Si hay un error, se deja todo como siempre y se avisa en error.
     */
    public static void load(String text) {
        reset(); if (text == null || text.trim().isEmpty()) return;
        int line = 0; StringBuilder sig = new StringBuilder(); String[] lines = text.split("\n");
        try {
            // primero se crean las armas nuevas, para que las demás líneas las puedan cambiar
            java.util.ArrayList<Wpn> extra = new java.util.ArrayList<>(java.util.Arrays.asList(ALL));
            for (String raw : lines) {
                line++; String l = raw.trim(); if (!l.startsWith("nueva")) continue;
                String[] t = l.split("\\s+"); if (t.length != 3) throw new IllegalArgumentException("se esperaba: nueva NOMBRE BASE");
                String n = t[1].replace('_', ' ').toUpperCase(), b = t[2].replace('_', ' ');
                if (!n.matches("[A-Z0-9ÁÉÍÓÚÑ ]{1,18}")) throw new IllegalArgumentException("nombre inválido: " + t[1]);
                Wpn from = null; for (Wpn w : extra) { if (w.name.equals(n)) throw new IllegalArgumentException("ya existe el arma " + n); if (w.name.equals(b)) from = w; }
                if (from == null) throw new IllegalArgumentException("no existe el arma base " + t[2]);
                if (extra.size() >= BASE_COUNT + 40) throw new IllegalArgumentException("demasiadas armas nuevas");
                extra.add(from.copy(n)); sig.append(l).append('\n');
            }
            ALL = extra.toArray(new Wpn[0]); line = 0;
            while (line < lines.length) { // por índice: "dibujo" consume sus propias líneas hasta "fin"
                String raw = lines[line++]; String l = raw.trim(); if (l.isEmpty() || l.startsWith("#") || l.startsWith("nueva")) continue;
                String[] t = l.split("\\s+", 3); if (t.length < 2) throw new IllegalArgumentException("falta el nombre del arma");
                int i = find(t[1].replace('_', ' ')); if (i < 0) throw new IllegalArgumentException("no existe el arma " + t[1]);
                Wpn w = ALL[i]; sig.append(l).append('\n');
                switch (t[0]) {
                    case "arma": if (t.length > 2) for (String kv : t[2].trim().split("\\s+")) { String[] p = kv.split("=", 2); if (p.length != 2) throw new IllegalArgumentException("se esperaba clave=valor: " + kv);
                        if (p[0].equals("aspecto")) { int lk = java.util.Arrays.asList(LOOKS).indexOf(p[1].toUpperCase()); if (lk < 0) throw new IllegalArgumentException("aspecto desconocido: " + p[1]); w.look = lk; }
                        else if (p[0].equals("color")) { if (!p[1].matches("[0-9A-Fa-f]{6}")) throw new IllegalArgumentException("color inválido: " + p[1]); w.tint = 0xFF000000 | Integer.parseInt(p[1], 16); }
                        else w.set(p[0], Float.parseFloat(p[1])); } break;
                    case "dibujo": {
                        java.util.ArrayList<Art> shapes = new java.util.ArrayList<>(); Art cur = null; java.util.ArrayList<float[]> ps = new java.util.ArrayList<>(); int pts = 0;
                        while (true) {
                            line++; if (line > lines.length) throw new IllegalArgumentException("falta 'fin' en el dibujo de " + t[1]);
                            String d = lines[line - 1].trim(); if (d.isEmpty() || d.startsWith("#")) continue;
                            if (d.equals("fin")) break;
                            String[] q = d.split("\\s+");
                            if (q[0].equals("forma")) {
                                if (cur != null) cur.polys = ps.toArray(new float[0][]); ps = new java.util.ArrayList<>(); cur = new Art(); cur.sw = .5f; shapes.add(cur);
                                for (int k = 1; k < q.length; k++) { String[] kv = q[k].split("=", 2); if (kv.length < 2) continue;
                                    switch (kv[0]) { case "relleno": cur.fill = (int) Long.parseLong(kv[1], 16); break; case "contorno": cur.stroke = (int) Long.parseLong(kv[1], 16); break;
                                        case "grosor": cur.sw = Float.parseFloat(kv[1]); break; case "parimpar": cur.eo = kv[1].equals("1"); break; default: } }
                            } else if (q[0].equals("p")) {
                                if (cur == null) throw new IllegalArgumentException("contorno sin forma en el dibujo de " + t[1]);
                                float[] p = new float[q.length - 1]; for (int k = 1; k < q.length; k++) { p[k - 1] = Float.parseFloat(q[k]); if (Math.abs(p[k - 1]) > 200) throw new IllegalArgumentException("dibujo demasiado grande"); }
                                if (p.length >= 6 && p.length % 2 == 0) { ps.add(p); pts += p.length / 2; }
                            } else throw new IllegalArgumentException("se esperaba forma, p o fin en el dibujo de " + t[1]);
                        }
                        if (cur != null) cur.polys = ps.toArray(new float[0][]);
                        if (pts > MAX_ART_POINTS) throw new IllegalArgumentException("el dibujo de " + t[1] + " tiene demasiados puntos (" + pts + ", máximo " + MAX_ART_POINTS + ")");
                        shapes.removeIf(a -> a.polys == null || a.polys.length == 0); w.art = shapes.isEmpty() ? null : shapes.toArray(new Art[0]); break; }
                    case "nombre": if (t.length > 2) w.label = t[2].trim().toUpperCase(); break;
                    case "descripcion": w.desc = t.length > 2 ? t[2].trim() : ""; break;
                    case "quitar": w.off = true; break;
                    default: throw new IllegalArgumentException("instrucción desconocida: " + t[0]);
                }
            }
            if (enabled().length == 0) throw new IllegalArgumentException("no puedes quitar todas las armas");
        } catch (RuntimeException e) { reset(); error = "línea " + line + ": " + e.getMessage(); return; }
        signature = sig.length() == 0 ? 0 : sig.toString().hashCode() | 1;
    }
    private static int find(String n) { for (int i = 0; i < ALL.length; i++) if (ALL[i].name.equals(n)) return i; return -1; }
    /** Índices de las armas que se pueden elegir. */
    public static int[] enabled() { int n = 0; for (Wpn w : ALL) if (!w.off) n++; int[] r = new int[n]; n = 0; for (int i = 0; i < ALL.length; i++) if (!ALL[i].off) r[n++] = i; return r; }
    /** Si el arma i fue quitada, la primera que se puede elegir. */
    public static int usable(int i) { if (i >= 0 && i < ALL.length && !ALL[i].off) return i; int[] e = enabled(); return e.length > 0 ? e[0] : 0; }
    public static int index(String n) { n = rename(n); for (int i = 0; i < ALL.length; i++) if (ALL[i].name.equals(n)) return i; return 0; }
    static { snapshot(); } // al final: KEYS y LIMITS ya existen
}
