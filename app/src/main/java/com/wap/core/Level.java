package com.wap.core;
import java.io.*; import java.util.ArrayList; import java.util.zip.*;
/**
 * Nivel guardado: el mapa píxel a píxel (material y color), los objetos con resistencia y la altura del agua.
 * Se guarda comprimido (unas decenas de KB) y se puede mandar al otro teléfono antes de una partida en línea.
 */
public final class Level {
    static final int MAGIC = 0x5741504E, VERSION = 1; // "WAPN"
    public String name = "";
    /** Altura de la superficie del agua en el mapa; -1 si no hay agua. */
    public int water = -1;
    public byte[] m = new byte[Terrain.W * Terrain.H]; public int[] tex = new int[Terrain.W * Terrain.H];
    public final ArrayList<int[]> props = new ArrayList<>();

    /** Copia el estado actual de un mapa (los objetos, en su posición actual). */
    public static Level of(World w, String name) {
        Level l = new Level(); l.name = name; l.water = w.water;
        System.arraycopy(w.t.m, 0, l.m, 0, l.m.length);
        for (int i = 0; i < l.tex.length; i++) l.tex[i] = l.m[i] == 0 ? 0 : w.t.tex[i];
        for (Prop p : w.props) if (!p.gone) l.props.add(new int[]{p.x, p.y, p.w, p.h, p.kind});
        return l;
    }
    /** Pone el nivel en un mapa. El marco indestructible siempre queda. */
    void apply(Terrain t) {
        System.arraycopy(m, 0, t.m, 0, m.length); System.arraycopy(tex, 0, t.tex, 0, tex.length);
        t.props.clear(); for (int[] q : props) t.props.add(q.clone());
        t.frame(); java.util.Arrays.fill(t.dirty, true);
    }
    public byte[] save() {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream(); DataOutputStream o = new DataOutputStream(b);
            o.writeInt(MAGIC); o.writeInt(VERSION); o.writeUTF(name); o.writeInt(water); o.writeInt(props.size());
            for (int[] q : props) for (int v : q) o.writeInt(v);
            DataOutputStream z = new DataOutputStream(new DeflaterOutputStream(o, new Deflater(9)));
            z.write(m); for (int i = 0; i < tex.length; i++) z.writeInt(m[i] == 0 ? 0 : tex[i]);
            z.close(); return b.toByteArray();
        } catch (IOException e) { throw new RuntimeException(e); }
    }
    public static Level load(byte[] data) throws IOException {
        DataInputStream i = new DataInputStream(new ByteArrayInputStream(data));
        if (i.readInt() != MAGIC) throw new IOException("no es un nivel de WAP");
        if (i.readInt() > VERSION) throw new IOException("nivel de una versión más nueva");
        Level l = new Level(); l.name = i.readUTF(); l.water = i.readInt(); int n = i.readInt();
        if (n < 0 || n > 5000) throw new IOException("nivel dañado");
        for (int k = 0; k < n; k++) { int[] q = new int[5]; for (int j = 0; j < 5; j++) q[j] = i.readInt(); if (Terrain.isProp(q[4])) l.props.add(q); }
        DataInputStream z = new DataInputStream(new InflaterInputStream(i));
        z.readFully(l.m); for (int k = 0; k < l.tex.length; k++) l.tex[k] = z.readInt();
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
