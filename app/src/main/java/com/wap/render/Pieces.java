package com.wap.render;
import java.util.ArrayList; import java.util.HashMap;
/**
 * Piezas del personaje dibujadas por el usuario en el editor de piezas (archivo personaje.txt). Son vectores: se dibujan
 * igual que el personaje de siempre, sin imágenes. Cada pieza está en las coordenadas del hueso que la mueve, así que
 * sirve en todas las poses. Una pieza que no esté en el archivo se dibuja como siempre.
 *
 * Formato (una instrucción por línea):
 *   pieza NOMBRE
 *   forma relleno=AARRGGBB contorno=AARRGGBB grosor=0.75 parimpar=0 traje=1
 *   p x y x y x y ...        (un contorno cerrado de la forma anterior)
 *   pose NOMBRE fx fy bx by a1 a2 b1 b2 pel lean   (una pose de las animaciones; ver RobotArt.POSE_NAMES)
 */
public final class Pieces {
    static final class Sh { float[][] polys; int fill, stroke; float sw; boolean eo, suit; }
    static final HashMap<String, Sh[]> map = new HashMap<>();
    /** Límite de puntos: un archivo más pesado se ignora para no hacer lento el juego (sobre todo en línea). */
    public static final int MAX_POINTS = 6000;
    public static int points, pieces, poses; public static String error;

    public static void clear() { map.clear(); points = 0; pieces = 0; poses = 0; error = null; RobotArt.resetPoses(); }
    public static void load(String text) {
        clear(); if (text == null) return;
        HashMap<String, float[]> pz = new HashMap<>(); HashMap<String, ArrayList<Sh>> m = new HashMap<>(); ArrayList<Sh> cur = null; Sh sh = null; ArrayList<float[]> ps = new ArrayList<>(); int pts = 0, line = 0;
        try {
            for (String raw : text.split("\n")) {
                line++; String l = raw.trim(); if (l.isEmpty() || l.startsWith("#")) continue;
                String[] t = l.split("\\s+");
                switch (t[0]) {
                    case "pieza": close(sh, ps); sh = null; ps = new ArrayList<>(); cur = m.computeIfAbsent(t[1], k -> new ArrayList<>()); break;
                    case "forma": {
                        close(sh, ps); ps = new ArrayList<>(); if (cur == null) throw new IllegalArgumentException("forma antes de pieza");
                        sh = new Sh(); sh.sw = .75f;
                        for (int i = 1; i < t.length; i++) { String[] kv = t[i].split("=", 2); if (kv.length < 2) continue;
                            switch (kv[0]) {
                                case "relleno": sh.fill = (int) Long.parseLong(kv[1], 16); break;
                                case "contorno": sh.stroke = (int) Long.parseLong(kv[1], 16); break;
                                case "grosor": sh.sw = Float.parseFloat(kv[1]); break;
                                case "parimpar": sh.eo = kv[1].equals("1"); break;
                                case "traje": sh.suit = kv[1].equals("1"); break;
                                default:
                            } }
                        cur.add(sh); break; }
                    case "p": {
                        if (sh == null) throw new IllegalArgumentException("contorno sin forma");
                        float[] p = new float[t.length - 1]; for (int i = 1; i < t.length; i++) p[i - 1] = Float.parseFloat(t[i]);
                        if (p.length >= 4 && p.length % 2 == 0) { ps.add(p); pts += p.length / 2; } break; }
                    case "pose": {
                        if (t.length != 12 || RobotArt.poseValues(t[1]) == null) throw new IllegalArgumentException("pose mal escrita");
                        float[] f = new float[10]; for (int i = 0; i < 10; i++) { f[i] = Float.parseFloat(t[i + 2]); if (Float.isNaN(f[i]) || Math.abs(f[i]) > 1000) throw new IllegalArgumentException("número fuera de rango"); }
                        pz.put(t[1], f); break; }
                    default:
                }
            }
            close(sh, ps);
        } catch (RuntimeException e) { error = "línea " + line + ": " + e.getMessage(); return; }
        if (pts > MAX_POINTS) { error = "demasiados puntos (" + pts + ", máximo " + MAX_POINTS + ")"; return; }
        for (java.util.Map.Entry<String, ArrayList<Sh>> e : m.entrySet()) { ArrayList<Sh> a = e.getValue(); a.removeIf(x -> x.polys == null || x.polys.length == 0); if (!a.isEmpty()) map.put(e.getKey(), a.toArray(new Sh[0])); }
        for (java.util.Map.Entry<String, float[]> e : pz.entrySet()) RobotArt.setPose(e.getKey(), e.getValue());
        points = pts; pieces = map.size(); poses = pz.size();
    }
    private static void close(Sh sh, ArrayList<float[]> ps) { if (sh != null && sh.polys == null) sh.polys = ps.toArray(new float[0][]); }
    /** Dibuja la pieza del usuario; false si no hay, para usar la de siempre. */
    static boolean draw(Gfx g, String n, int c, boolean lit) {
        Sh[] a = map.get(n); if (a == null) return false;
        for (Sh s : a) {
            int f = s.suit && s.fill != 0 ? s.fill & 0xFF000000 | c & 0xFFFFFF : s.fill;
            if (lit && f != 0) f = light(f);
            g.shape(s.polys, f, s.eo, s.stroke, s.stroke == 0 ? 0 : s.sw);
        }
        return true;
    }
    /** Destello del disparo: aclara los rellenos que no son casi negros. */
    static int light(int c) {
        int r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255; if (Math.max(r, Math.max(g, b)) < 60) return c;
        return c & 0xFF000000 | (r + (255 - r) * 2 / 5) << 16 | (g + (255 - g) * 2 / 5) << 8 | b + (255 - b) * 2 / 5;
    }
}
