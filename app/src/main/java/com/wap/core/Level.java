package com.wap.core;
import java.io.*; import java.util.ArrayList; import java.util.zip.*;
/**
 * Nivel guardado: el mapa píxel a píxel (material y color), los objetos con resistencia y la altura del agua.
 * Se guarda comprimido (unas decenas de KB) y se puede mandar al otro teléfono antes de una partida en línea.
 */
public final class Level {
    static final int MAGIC = 0x5741504E, VERSION = 2; // "WAPN"; la 2 agrega las capas de fondo y sobre
    public String name = "";
    /** Altura de la superficie del agua en el mapa; -1 si no hay agua. */
    public int water = -1;
    public byte[] m = new byte[Terrain.W * Terrain.H]; public int[] tex = new int[Terrain.W * Terrain.H];
    public final ArrayList<int[]> props = new ArrayList<>();
    /** Capas que no chocan (ver Terrain.back y front); null si no hay. */
    public int[] back, front;

    /** Copia el estado actual de un mapa (los objetos, en su posición actual). */
    public static Level of(World w, String name) {
        Level l = new Level(); l.name = name; l.water = w.water;
        System.arraycopy(w.t.m, 0, l.m, 0, l.m.length);
        for (int i = 0; i < l.tex.length; i++) l.tex[i] = l.m[i] == 0 ? 0 : w.t.tex[i];
        for (Prop p : w.props) if (!p.gone) l.props.add(new int[]{p.x, p.y, p.w, p.h, p.kind});
        l.back = used(w.t.back); l.front = used(w.t.front);
        return l;
    }
    /** Pone el nivel en un mapa. El marco indestructible siempre queda. */
    void apply(Terrain t) {
        System.arraycopy(m, 0, t.m, 0, m.length); System.arraycopy(tex, 0, t.tex, 0, tex.length);
        t.props.clear(); for (int[] q : props) t.props.add(q.clone());
        t.back = back == null ? null : back.clone(); t.front = front == null ? null : front.clone();
        t.frame(); java.util.Arrays.fill(t.dirty, true);
    }
    /** Copia de una capa, o null si está vacía. */
    static int[] used(int[] a) { if (a == null) return null; for (int v : a) if (v != 0) return a.clone(); return null; }
    public byte[] save() {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream(); DataOutputStream o = new DataOutputStream(b);
            o.writeInt(MAGIC); o.writeInt(VERSION); o.writeUTF(name); o.writeInt(water); o.writeInt(props.size());
            for (int[] q : props) for (int v : q) o.writeInt(v);
            DataOutputStream z = new DataOutputStream(new BufferedOutputStream(new DeflaterOutputStream(o, new Deflater(9)), 65536));
            z.write(m); for (int i = 0; i < tex.length; i++) z.writeInt(m[i] == 0 ? 0 : tex[i]);
            for (int[] a : new int[][]{back, front}) { z.writeBoolean(a != null); if (a != null) for (int v : a) z.writeInt(v); }
            z.close(); return b.toByteArray();
        } catch (IOException e) { throw new RuntimeException(e); }
    }
    /** Solo el nombre, sin descomprimir el mapa (para las listas). */
    public static String peekName(byte[] data) {
        try { DataInputStream i = new DataInputStream(new ByteArrayInputStream(data)); if (i.readInt() != MAGIC) return null; i.readInt(); return i.readUTF(); } catch (IOException e) { return null; }
    }
    /** Captura chica del nivel (ancho x alto en píxeles): el color de lo que hay en cada punto, con el agua en azul. */
    public int[] snapshot(int tw, int th) {
        int[] t = new int[tw * th];
        for (int ty = 0; ty < th; ty++) for (int tx = 0; tx < tw; tx++) {
            int c = 0xFF20264A, cb = 0, cf = 0;
            for (int sy = 0; sy < 3 && c == 0xFF20264A; sy++) for (int sx = 0; sx < 3; sx++) {
                int x = tx * Terrain.W / tw + sx * Terrain.W / tw / 3, y = ty * Terrain.H / th + sy * Terrain.H / th / 3, q = y * Terrain.W + x, k = m[q];
                if (front != null && front[q] != 0 && cf == 0) cf = front[q]; if (back != null && back[q] != 0 && cb == 0) cb = back[q];
                if (k != 0 && k != Terrain.DEBRIS && k != Terrain.DECOR) { c = tex[q] | 0xFF000000; if (c == 0xFF000000) c = 0xFF8214FF; break; }
            }
            if (c == 0xFF20264A && cb != 0) c = backTone(cb); if (cf != 0) c = frontTone(cf);
            if (water >= 0 && ty * Terrain.H / th > water) c = 0xFF000000 | ((c >> 16 & 255) * 6 / 10 + 0x18) << 16 | ((c >> 8 & 255) * 6 / 10 + 0x38) << 8 | (c & 255) * 6 / 10 + 0x66;
            t[ty * tw + tx] = c;
        }
        return t;
    }
    /** Cómo se ve la capa de fondo (más clara) y la de encima (más oscura). */
    public static int backTone(int c) { int r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255; return 0xFF000000 | (r + (0xDC - r) * 45 / 100) << 16 | (g + (0xE4 - g) * 45 / 100) << 8 | b + (0xEE - b) * 45 / 100; }
    public static int frontTone(int c) { return 0xFF000000 | (c >> 16 & 255) * 55 / 100 << 16 | (c >> 8 & 255) * 55 / 100 << 8 | (c & 255) * 55 / 100; }
    public static Level load(byte[] data) throws IOException {
        DataInputStream i = new DataInputStream(new ByteArrayInputStream(data));
        if (i.readInt() != MAGIC) throw new IOException("no es un nivel de WAP");
        int ver = i.readInt(); if (ver > VERSION) throw new IOException("nivel de una versión más nueva");
        Level l = new Level(); l.name = i.readUTF(); l.water = i.readInt(); int n = i.readInt();
        if (n < 0 || n > 5000) throw new IOException("nivel dañado");
        for (int k = 0; k < n; k++) { int[] q = new int[5]; for (int j = 0; j < 5; j++) q[j] = i.readInt(); if (Terrain.isProp(q[4])) l.props.add(q); }
        DataInputStream z = new DataInputStream(new BufferedInputStream(new InflaterInputStream(i), 65536));
        z.readFully(l.m); for (int k = 0; k < l.tex.length; k++) l.tex[k] = z.readInt();
        if (ver >= 2) for (int a = 0; a < 2; a++) if (z.readBoolean()) { int[] v = new int[l.m.length]; for (int k = 0; k < v.length; k++) v[k] = z.readInt(); if (a == 0) l.back = v; else l.front = v; }
        return l;
    }
    /** Un nivel vacío: solo el marco y un piso. */
    public static Level empty(String name) {
        Level l = new Level(); l.name = name; Terrain t = new Terrain(); t.frame(); t.floor();
        System.arraycopy(t.m, 0, l.m, 0, l.m.length); System.arraycopy(t.tex, 0, l.tex, 0, l.tex.length); return l;
    }
    /** Un nivel aleatorio como los de siempre, para usarlo de punto de partida. */
    public static Level random(int seed, String name) {
        Wpn[] a = new Wpn[Robot.SLOTS]; java.util.Arrays.fill(a, Wpn.ALL[0]);
        return of(new World(seed, 1, a, a), name);
    }
}
