package com.wap.core;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
/**
 * BASES: un plataformero simple armado por el usuario en el editor de bases.
 * Todo va en una grilla de cuadros de 32 px. La base se arma con habitaciones (contenedores) de una pantalla
 * (16 x 9 cuadros) pegadas unas a otras, como en el editor de Mega Man Powered Up. En los cuadros van piezas de
 * material (algunas se mueven), enemigos, premios y el punto de inicio. Una habitación puede ser la del jefe.
 * Lo que no es habitación es pared, salvo bajo una habitación sin otra debajo: ahí hay precipicio.
 */
public final class Base {
    public static final int MAGIC = 0x57415042, VERSION = 1; // "WAPB"
    public static final int CELL = 32, RW = 16, RH = 9, ROOM_PX = RW * CELL, ROOM_PY = RH * CELL, MAX_ROOMS = 40;
    /** Materiales de las piezas (los del editor de niveles, sin fondo ni sobre). */
    public static final int T_EARTH = 1, T_WOOD = 2, T_PANEL = 3, T_STEEL = 4, T_CRATE = 5, T_METAL = 6, T_STONE = 7, T_PC = 8, TILE_KINDS = 9;
    public static final String[] TILE_NAMES = {"", "TIERRA", "MADERA", "BLOQUE", "ACERO", "CAJA", "METAL", "PIEDRA", "COMPUTADOR"};
    /** Movimiento de una pieza: fija, sube y baja 2 cuadros, de lado a lado 2 cuadros, gira en círculo, aparece y desaparece. */
    public static final int M_NONE = 0, M_UPDOWN = 1, M_SIDE = 2, M_SPIN = 3, M_BLINK = 4, MOVES = 5;
    public static final String[] MOVE_NAMES = {"FIJA", "SUBE Y BAJA", "LADO A LADO", "GIRA", "APARECE"};
    /** Enemigos, premios y el inicio (lo que va en "things"). */
    public static final int E_DRONE = 1, E_TURRET = 2, E_BADPAL = 3, P_CAPSULE = 10, P_LIFE = 11, P_WEAPON = 12, START = 20;
    public static final String[] BG_NAMES = {"LABORATORIO", "BOSQUE", "PLAYA", "CIUDAD"};

    public String name = ""; public int bg;
    /** Habitaciones: {rx, ry, jefe (0/1)} en unidades de habitación. */
    public final ArrayList<int[]> rooms = new ArrayList<>();
    /** Piezas por cuadro: {material, color, movimiento}. */
    public final HashMap<Long, int[]> tiles = new HashMap<>();
    /** Enemigos, premios e inicio por cuadro. */
    public final HashMap<Long, Integer> things = new HashMap<>();

    public static long key(int cx, int cy) { return (long) cx << 32 | cy & 0xFFFFFFFFL; }
    public static int kx(long k) { return (int) (k >> 32); }
    public static int ky(long k) { return (int) k; }
    public static boolean movable(int mat) { return mat == T_EARTH || mat == T_WOOD || mat == T_PANEL || mat == T_STEEL; }

    /** Base nueva: la "mesa base", una habitación con piso de tierra y el inicio. */
    public static Base empty(String name) {
        Base b = new Base(); b.name = name; b.rooms.add(new int[]{0, 0, 0});
        for (int x = 0; x < RW; x++) b.tiles.put(key(x, RH - 1), new int[]{T_EARTH, 0xFF8B5A2B, M_NONE});
        b.things.put(key(2, RH - 2), START);
        return b;
    }
    public int[] room(int rx, int ry) { for (int[] r : rooms) if (r[0] == rx && r[1] == ry) return r; return null; }
    public int[] roomOfCell(int cx, int cy) { return room(Math.floorDiv(cx, RW), Math.floorDiv(cy, RH)); }
    /** Se puede agregar una habitación ahí: libre y pegada a otra. */
    public boolean canAdd(int rx, int ry) {
        if (rooms.size() >= MAX_ROOMS || room(rx, ry) != null) return false;
        return room(rx - 1, ry) != null || room(rx + 1, ry) != null || room(rx, ry - 1) != null || room(rx, ry + 1) != null;
    }
    public boolean addRoom(int rx, int ry) { if (!canAdd(rx, ry)) return false; rooms.add(new int[]{rx, ry, 0}); return true; }
    /** Quita una habitación y lo que tiene (nunca la última; no deja habitaciones sueltas). */
    public boolean removeRoom(int rx, int ry) {
        int[] r = room(rx, ry); if (r == null || rooms.size() <= 1) return false;
        rooms.remove(r);
        if (!connected()) { rooms.add(r); return false; }
        tiles.keySet().removeIf(k -> Math.floorDiv(kx(k), RW) == rx && Math.floorDiv(ky(k), RH) == ry);
        boolean hadStart = startCell() == null;
        things.keySet().removeIf(k -> Math.floorDiv(kx(k), RW) == rx && Math.floorDiv(ky(k), RH) == ry);
        if (startCell() == null && !hadStart) { int[] f = rooms.get(0); things.put(key(f[0] * RW + 2, f[1] * RH + RH - 2), START); }
        return true;
    }
    boolean connected() {
        if (rooms.isEmpty()) return true;
        java.util.HashSet<int[]> seen = new java.util.HashSet<>(); ArrayList<int[]> q = new ArrayList<>(); q.add(rooms.get(0)); seen.add(rooms.get(0));
        while (!q.isEmpty()) { int[] r = q.remove(q.size() - 1); for (int[] o : rooms) if (!seen.contains(o) && Math.abs(o[0] - r[0]) + Math.abs(o[1] - r[1]) == 1) { seen.add(o); q.add(o); } }
        return seen.size() == rooms.size();
    }
    /** Marca la habitación del jefe (solo una). Tocar la misma la desmarca. */
    public void toggleBoss(int rx, int ry) { int[] r = room(rx, ry); if (r == null) return; boolean on = r[2] == 0; for (int[] o : rooms) o[2] = 0; r[2] = on ? 1 : 0; if (on) bossFrame(r); }
    /** La sala del jefe parte con ribetes de acero en sus bordes, menos en los lados que se conectan con la base. */
    public void bossFrame(int[] r) {
        boolean L = room(r[0] - 1, r[1]) != null, Rr = room(r[0] + 1, r[1]) != null, U = room(r[0], r[1] - 1) != null, D = room(r[0], r[1] + 1) != null;
        int x0 = r[0] * RW, y0 = r[1] * RH;
        for (int i = 0; i < RW; i++) for (int j = 0; j < RH; j++) {
            boolean edge = i == 0 && !L || i == RW - 1 && !Rr || j == 0 && !U || j == RH - 1 && !D;
            if (!edge) continue; long k = key(x0 + i, y0 + j); Integer th = things.get(k);
            if (th != null && th == START) continue; things.remove(k); tiles.put(k, new int[]{T_STEEL, 0, M_NONE});
        }
    }
    public int[] bossRoom() { for (int[] r : rooms) if (r[2] == 1) return r; return null; }
    /** En línea hace falta la sala del jefe al final (ninguna habitación más a la derecha): ahí se juntan los dos. */
    public boolean canOnline() { int[] b = bossRoom(); if (b == null) return false; for (int[] r : rooms) if (r[0] > b[0]) return false; return true; }
    public long[] startCell() { for (Map.Entry<Long, Integer> e : things.entrySet()) if (e.getValue() == START) return new long[]{e.getKey()}; return null; }
    /** Pone algo en un cuadro (dentro de una habitación). El inicio es uno solo. */
    public void putThing(int cx, int cy, int what) {
        if (roomOfCell(cx, cy) == null) return;
        if (what == START) things.values().removeIf(v -> v == START);
        things.put(key(cx, cy), what); tiles.remove(key(cx, cy));
    }
    public void putTile(int cx, int cy, int mat, int color, int move) {
        if (roomOfCell(cx, cy) == null) return;
        Integer th = things.get(key(cx, cy)); if (th != null && th == START) return; // no tapar el inicio
        things.remove(key(cx, cy)); tiles.put(key(cx, cy), new int[]{mat, color, movable(mat) ? move : M_NONE});
    }
    public void erase(int cx, int cy) { tiles.remove(key(cx, cy)); Integer th = things.get(key(cx, cy)); if (th != null && th != START) things.remove(key(cx, cy)); }

    /** Miniatura para las listas: las habitaciones con sus piezas, ajustadas al cuadro. */
    public int[] snapshot(int w, int h) {
        int[] out = new int[w * h]; int bgc = bg == 1 ? 0xFF8EC9F0 : bg == 2 ? 0xFF7FD0F5 : bg == 3 ? 0xFF2B3A66 : 0xFFC7D2E0;
        java.util.Arrays.fill(out, 0xFF1B1F4A);
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (int[] r : rooms) { minX = Math.min(minX, r[0]); minY = Math.min(minY, r[1]); maxX = Math.max(maxX, r[0]); maxY = Math.max(maxY, r[1]); }
        int cw = (maxX - minX + 1) * RW, ch = (maxY - minY + 1) * RH; float k = Math.min(w / (float) cw, h / (float) ch);
        float ox = (w - cw * k) / 2, oy = (h - ch * k) / 2;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int cx = (int) Math.floor((x - ox) / k) + minX * RW, cy = (int) Math.floor((y - oy) / k) + minY * RH;
            int[] room = roomOfCell(cx, cy); if (room == null || x < ox || y < oy) continue;
            int c = room[2] == 1 ? 0xFFE8A0A0 : bgc; int[] t = tiles.get(key(cx, cy));
            if (t != null) c = t[0] == T_PANEL ? 0xFF8214FF : t[0] == T_STEEL ? 0xFF3A3F55 : t[0] >= T_CRATE ? 0xFFCB8C3C : t[1] | 0xFF000000;
            Integer th = things.get(key(cx, cy)); if (th != null) c = th == START ? 0xFF3DDC97 : th < P_CAPSULE ? 0xFFE8323C : 0xFFFFC83D;
            out[y * w + x] = c;
        }
        return out;
    }
    // ---------- Archivo
    public byte[] save() {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream(); DataOutputStream o = new DataOutputStream(new java.util.zip.DeflaterOutputStream(b));
            o.writeInt(MAGIC); o.writeInt(VERSION); o.writeUTF(name); o.writeByte(bg);
            o.writeShort(rooms.size()); for (int[] r : rooms) { o.writeShort(r[0]); o.writeShort(r[1]); o.writeByte(r[2]); }
            ArrayList<Long> tk = new ArrayList<>(tiles.keySet()); java.util.Collections.sort(tk); // orden fijo: el mismo archivo en los dos teléfonos
            o.writeInt(tk.size()); for (long k : tk) { int[] v = tiles.get(k); o.writeShort(kx(k)); o.writeShort(ky(k)); o.writeByte(v[0]); o.writeInt(v[1]); o.writeByte(v[2]); }
            ArrayList<Long> hk = new ArrayList<>(things.keySet()); java.util.Collections.sort(hk);
            o.writeInt(hk.size()); for (long k : hk) { o.writeShort(kx(k)); o.writeShort(ky(k)); o.writeByte(things.get(k)); }
            o.close(); return b.toByteArray();
        } catch (IOException e) { throw new RuntimeException(e); }
    }
    public static boolean isBase(byte[] data) {
        try (DataInputStream i = new DataInputStream(new java.util.zip.InflaterInputStream(new ByteArrayInputStream(data)))) { return i.readInt() == MAGIC; } catch (Exception e) { return false; }
    }
    public static Base load(byte[] data) throws IOException {
        DataInputStream i = new DataInputStream(new java.util.zip.InflaterInputStream(new ByteArrayInputStream(data)));
        if (i.readInt() != MAGIC) throw new IOException("no es una base"); int v = i.readInt(); if (v > VERSION) throw new IOException("base de una versión más nueva");
        Base b = new Base(); b.name = i.readUTF(); b.bg = i.readUnsignedByte() % BG_NAMES.length;
        int n = i.readUnsignedShort(); if (n > MAX_ROOMS * 2) throw new IOException("base inválida");
        for (int k = 0; k < n; k++) b.rooms.add(new int[]{i.readShort(), i.readShort(), i.readUnsignedByte()});
        int nt = i.readInt(); if (nt < 0 || nt > n * RW * RH) throw new IOException("base inválida");
        for (int k = 0; k < nt; k++) { int x = i.readShort(), y = i.readShort(); b.tiles.put(key(x, y), new int[]{i.readUnsignedByte(), i.readInt(), i.readUnsignedByte()}); }
        int nh = i.readInt(); if (nh < 0 || nh > n * RW * RH) throw new IOException("base inválida");
        for (int k = 0; k < nh; k++) { int x = i.readShort(), y = i.readShort(); b.things.put(key(x, y), i.readUnsignedByte()); }
        if (b.rooms.isEmpty()) b.rooms.add(new int[]{0, 0, 0});
        return b;
    }

    // ---------- Armar el mapa para jugar
    /** Margen de pared alrededor (en cuadros); bajo las habitaciones sin piso, ahí está el precipicio. */
    public static final int PAD = 3;
    /** Habitaciones a jugar: con mirror, se agrega la base reflejada al otro lado de la sala del jefe (jugador 2). */
    ArrayList<int[]> playRooms(boolean mirror) {
        ArrayList<int[]> out = new ArrayList<>(); for (int[] r : rooms) out.add(new int[]{r[0], r[1], r[2], 0});
        int[] b = bossRoom();
        if (mirror && b != null) for (int[] r : rooms) if (r != b) out.add(new int[]{2 * b[0] - r[0], r[1], 0, 1}); // {rx, ry, jefe, reflejada}
        return out;
    }
    /** Cuadro reflejado (alrededor del centro de la sala del jefe). */
    int mirrorX(int cx) { int[] b = bossRoom(); return 2 * b[0] * RW + RW - 1 - cx; }
    Terrain build(boolean mirror, BaseRun run) {
        ArrayList<int[]> rs = playRooms(mirror);
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (int[] r : rs) { minX = Math.min(minX, r[0]); minY = Math.min(minY, r[1]); maxX = Math.max(maxX, r[0]); maxY = Math.max(maxY, r[1]); }
        int ox = PAD * CELL - minX * ROOM_PX, oy = PAD * CELL - minY * ROOM_PY; // píxel de la habitación (0, 0)
        int w = (maxX - minX + 1) * ROOM_PX + 2 * PAD * CELL, h = (maxY - minY + 1) * ROOM_PY + 2 * PAD * CELL;
        Terrain t = new Terrain(w, h, run); run.ox = ox; run.oy = oy; // por páginas: el mapa se arma por partes al acercarse
        for (int[] r : rs) {
            int x0 = ox + r[0] * ROOM_PX, y0 = oy + r[1] * ROOM_PY; boolean below = false;
            for (int[] o : rs) if (o[0] == r[0] && o[1] == r[1] + 1) below = true;
            run.rooms.add(new int[]{x0, y0, ROOM_PX, ROOM_PY, r[2], r[3], below ? 0 : 1}); // [6]: sin habitación abajo (precipicio)
        }
        for (Map.Entry<Long, int[]> e : tiles.entrySet()) {
            int cx = kx(e.getKey()), cy = ky(e.getKey()); int[] v = e.getValue();
            int[] home = roomOfCell(cx, cy); if (home == null) continue;
            boolean half = mirror && home[2] == 1 && Math.floorMod(cx, RW) >= RW / 2; // en línea la sala del jefe es simétrica: su mitad izquierda se refleja (así se abre al lado del jugador 2)
            if (half) continue;
            piece(t, run, ox + cx * CELL, oy + cy * CELL, v, false);
            if (mirror && bossRoom() != null) piece(t, run, ox + (mirrorX(cx) - (v[0] == T_PC ? 1 : 0)) * CELL, oy + cy * CELL, v, true); // el computador mide 2 cuadros
        }
        for (Map.Entry<Long, Integer> e : things.entrySet()) {
            int cx = kx(e.getKey()), cy = ky(e.getKey()); int what = e.getValue();
            int[] home = roomOfCell(cx, cy); if (home == null) continue;
            if (mirror && home[2] == 1 && (Math.floorMod(cx, RW) >= RW / 2 || what == START)) continue;
            run.thing(what, ox + cx * CELL + CELL / 2f, oy + cy * CELL + CELL, false);
            if (mirror && bossRoom() != null) run.thing(what, ox + mirrorX(cx) * CELL + CELL / 2f, oy + cy * CELL + CELL, true);
        }
        t.frostSink = (x, y) -> { run.frost.add(key(x / CELL, y / CELL)); return 0; };
        return t;
    }
    /** Anota una pieza: las fijas van a la grilla (de ahí se arman las páginas y se dibujan), las que se mueven aparte y los objetos como objetos. */
    void piece(Terrain t, BaseRun run, int x, int y, int[] v, boolean mirrored) {
        int mat = v[0], color = v[1] | 0xFF000000, move = v[2];
        if (move != M_NONE && movable(mat)) { run.kin.add(new BaseRun.Kin(x, y, mat, color, move, mirrored)); return; }
        switch (mat) {
            case T_CRATE: t.props.add(new int[]{x, y, CELL, CELL, Terrain.CRATE}); break;
            case T_METAL: t.props.add(new int[]{x, y, CELL, CELL, Terrain.METAL}); break;
            case T_STONE: t.props.add(new int[]{x, y, CELL, CELL, Terrain.STONE}); break;
            case T_PC: t.props.add(new int[]{x, y, 2 * CELL, 2 * CELL, Terrain.COMPUTER}); break;
            default: run.cells.put(key(x / CELL, y / CELL), new int[]{mat, color});
        }
    }
    /** Material del mapa para cada pieza fija. */
    static byte matOf(int mat) { return mat == T_EARTH ? Terrain.EARTH : mat == T_WOOD ? Terrain.WOOD : mat == T_PANEL ? Terrain.PANEL : Terrain.STEEL; }
}
