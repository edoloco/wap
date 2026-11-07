package com.wap.core;
/** Definición de un arma: cargador, tiempo de recarga y comportamiento del proyectil, al estilo de la tabla de armas de Liero. */
public final class Wpn {
    public static final int BULLET = 0, ROCKET = 1, GRENADE = 2, MINE = 3, FLAME = 4, SPIKE = 5, DIRT = 6, ORB = 7, NUKE = 8, SPARK = 9, CRATE = 10, LEMON = 11, PLASMA = 12, GREEN = 13, BLASER = 14, LIGHT = 15, RAY = 16, GAS = 17, WIND = 18, GLOVE = 19;
    public String name, desc = "";
    public int charge, clip = 1, delay = 20, reload = 100, parts = 1, fuse = 0, look = BULLET;
    public float spread, speed = 10, grav, bounce, drag = 1, accel, dmg, knock = 1, recoil, lob, jitter;
    public int radius, dig;          // radio de explosión (0 = sin explosión) y radio de excavación
    public boolean punch, infinite, beam, pierce, sticky, burn, impact = true, smoke, fan;
    public Wpn splinter; public int nSplinter, dropEvery, hops, addDirt, trigger, maxBounce, stagger, boomStyle; public float angle;
    Wpn(String n, String d) { name = n; desc = d; }
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
    public static final Wpn[] ALL;
    static {
        Wpn frag = sub("frag").shot(1, 180, 5).phys(.14f, .4f, 1).boom(14, 7, 12, 1.4f).fuse(50).look(GRENADE);
        Wpn shrap = sub("shrap").shot(1, 180, 8).phys(.08f, 0, .98f).hit(6, 5, 1).fuse(34);
        Wpn fire = sub("fire").shot(1, 180, 6).phys(.14f, 0, .96f).hit(1.1f, 0, .1f).fuse(190).look(FLAME); fire.burn = true; fire.impact = false; fire.sticky = true;
        Wpn drop = sub("drop").shot(1, 8, 1).phys(.2f, 0, 1).boom(16, 8, 14, 1.4f).look(GRENADE);
        Wpn chiq = sub("chiq").shot(1, 180, 4.5f).phys(.14f, .35f, 1).boom(24, 16, 20, 2.2f).fuse(55).look(GRENADE).split(frag, 4);
        Wpn chiqBig = sub("chiqb").shot(1, 180, 4.5f).phys(.14f, .35f, 1).boom(24, 16, 20, 2.2f).fuse(55).look(GRENADE).split(frag, 8);
        Wpn hop = sub("");
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
            new Wpn("LARPA", "Cohete que suelta bombas").mag(3, 50, 170).shot(1, 0, 3.6f).boom(26, 14, 22, 2.4f).look(ROCKET),
            new Wpn("GRENADE", "Rebota y estalla").mag(3, 40, 160).shot(1, 2, 6.8f).phys(.17f, .5f, 1).boom(38, 34, 34, 3.4f).fuse(110).look(GRENADE).lob(22),
            new Wpn("CLUSTER BOMB", "Se parte en diez").mag(2, 40, 210).shot(1, 2, 6.5f).phys(.17f, .35f, 1).boom(22, 12, 20, 2).fuse(70).look(GRENADE).lob(25).split(frag, 10),
            new Wpn("MINI BOMB", "Se parte y se vuelve a partir").mag(1, 40, 280).shot(1, 2, 6).phys(.17f, .35f, 1).boom(26, 14, 22, 2).fuse(75).look(GRENADE).lob(25).split(chiq, 5),
            new Wpn("BIG BOMB", "Como Mini Bomb, con el doble de pedazos").mag(1, 40, 360).shot(1, 2, 6).phys(.17f, .35f, 1).boom(30, 18, 26, 2.4f).fuse(75).look(GRENADE).lob(25).split(chiqBig, 10),
            new Wpn("MINE", "Espera pegada al suelo").mag(3, 26, 200).shot(1, 2, 4.5f).phys(.2f, 0, 1).boom(38, 44, 32, 3.6f).look(MINE).lob(20),
            new Wpn("TRAP", "Parece un bonus de cura").mag(2, 26, 200).shot(1, 2, 4.5f).phys(.2f, 0, 1).boom(32, 36, 26, 3).look(CRATE).lob(20),
            new Wpn("FLAK CANNON", "Revienta en el aire").mag(3, 34, 150).shot(1, 2, 9).boom(14, 8, 12, 1).fuse(34).look(GRENADE).split(shrap, 28),
            new Wpn("NAPALM", "Deja el suelo ardiendo").mag(1, 40, 230).shot(1, 2, 6.5f).phys(.14f, 0, 1).boom(24, 12, 20, 2).look(GRENADE).lob(18).split(fire, 32),
            new Wpn("FLAMER", "Lanzallamas de largo alcance").mag(70, 1, 200).shot(2, 9, 8.5f).phys(-.02f, 0, .97f).hit(1.2f, 3, .06f).fuse(44).look(FLAME),
            new Wpn("BLOWTORCH", "Soplete: cava rápido").mag(90, 1, 150).shot(1, 5, 8).phys(0, 0, .94f).hit(1.3f, 6, .05f).fuse(26).look(FLAME),
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
        };
        for (Wpn w : ALL) {
            if (w.name.equals("BAZOOKA") || w.name.equals("DOOMSDAY") || w.name.equals("LARPA")) { w.accel = .24f; w.smoke = true; }
            if (w.name.equals("MINE") || w.name.equals("TRAP")) { w.sticky = true; w.trigger = 28; }
            if (w.name.equals("PLASMA CANNON")) w.charge = 75;
            if (w.name.equals("CANNON PUNCH")) { w.punch = true; w.impact = true; }
            if (w.name.equals("DEATH RAY")) { w.stagger = 7; w.boomStyle = 1; w.impact = true; }
            if (w.name.equals("BOUNCY LASER")) { w.maxBounce = 10; w.angle = 40; w.impact = true; w.boomStyle = 2; }
            if (w.name.equals("FAN")) w.infinite = true;
            if (w.name.equals("LASER")) w.beam = true;
            if (w.name.equals("LARPA")) { w.splinter = drop; w.dropEvery = 16; }
            if (w.name.equals("GRASSHOPPER")) w.hops = 5;
            if (w.name.equals("CRACKLER")) w.jitter = .55f;
            if (w.name.equals("HELLRAIDER")) w.jitter = .2f;
            if (w.name.equals("FAN")) w.fan = true;
            if (w.name.equals("ZIMM")) w.angle = 40;
            if (w.name.equals("ZIMM") || w.name.equals("SPIKEBALLS") || w.name.equals("HELLRAIDER")) w.impact = true;
        }
    }
    /** Nombres antiguos guardados en el teléfono. */
    static String rename(String n) {
        switch (n) { case "DROP": return "CANNON PUNCH"; case "CHIQUITA BOMB": return "MINI BOMB"; case "BOOBY TRAP": return "TRAP"; case "GAUSS GUN": return "BOUNCY LASER"; default: return n; }
    }
    public static int index(String n) { n = rename(n); for (int i = 0; i < ALL.length; i++) if (ALL[i].name.equals(n)) return i; return 0; }
}
