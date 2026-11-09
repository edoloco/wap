import com.wap.render.*; import java.io.*; import java.nio.charset.StandardCharsets; import java.nio.file.*; import java.util.*;
/**
 * Exporta las animaciones del personaje como archivos Lottie (.json) para editarlas a mano en Glaxnimate.
 * Usa el dibujo del usuario (arte.txt, del SVG) movido por el esqueleto en cada pose; cada pieza es una capa con
 * cuadros clave de posición, giro, escala y visibilidad.
 * Uso: java Lottie arte.txt carpeta_salida
 * Convención del lienzo: 640 x 720 px, 10 px por unidad del juego, pies en (320, 640), mirando a la derecha.
 */
public class Lottie {
    static final int W = 640, H = 720, OX = 320, OY = 640, PX = 10, SUIT = 0x5F574E;

    /** Registra cada dibujo con su matriz y lo convierte en formas de Lottie en coordenadas locales. */
    static class Rec implements Gfx {
        static final class Item { String layer; double[] m; List<String> shapes = new ArrayList<>(); }
        final List<Item> items = new ArrayList<>(); double[] m = {1, 0, 0, 1, 0, 0}; final ArrayDeque<double[]> st = new ArrayDeque<>();
        String layer = "extra"; Item cur;
        public int w() { return W; } public int h() { return H; }
        public void save() { st.push(m.clone()); } public void restore() { m = st.pop(); }
        public void translate(float x, float y) { m = mul(m, new double[]{1, 0, 0, 1, x, y}); }
        public void rotate(float r) { double c = Math.cos(r), s = Math.sin(r); m = mul(m, new double[]{c, s, -s, c, 0, 0}); }
        public void scale(float x, float y) { m = mul(m, new double[]{x, 0, 0, y, 0, 0}); }
        static double[] mul(double[] a, double[] b) { return new double[]{a[0] * b[0] + a[2] * b[1], a[1] * b[0] + a[3] * b[1], a[0] * b[2] + a[2] * b[3], a[1] * b[2] + a[3] * b[3], a[0] * b[4] + a[2] * b[5] + a[4], a[1] * b[4] + a[3] * b[5] + a[5]}; }
        public void layer(String n) { layer = n; cur = null; } public void endLayer() { layer = "extra"; cur = null; }
        public void clear(int c) {} public void glow(float x, float y, float r, int c) {} public void additive(boolean on) {}
        public void beginPixels(int w, int h) {} public void endPixels(float x, float y, int s) {}
        public void text(String s, float x, float y, float z, int c, int a) {} public float textW(String s, float z) { return 0; }
        public void tile(int id, int[] px, int size, boolean ch, float x, float y) {}
        void add(String shape) {
            if (cur == null || !cur.layer.equals(layer) || !Arrays.equals(cur.m, m)) { cur = new Item(); cur.layer = layer; cur.m = m.clone(); items.add(cur); }
            cur.shapes.add(shape);
        }
        public void poly(float[] p, int f, int s, float sw) { add(group(path(p, true), f, s, sw, false)); }
        public void shape(float[][] ps, int f, boolean eo, int s, float sw) { StringBuilder b = new StringBuilder(); for (float[] p : ps) { if (b.length() > 0) b.append(','); b.append(path(p, true)); } add(group(b.toString(), f, s, sw, eo)); }
        public void grad(float[] p, int c0, int c1, float x0, float x1, int s, float sw) { poly(p, c0, s, sw); }
        public void circle(float x, float y, float r, int f, int s, float sw) { add(group("{\"ty\":\"el\",\"nm\":\"Elipse\",\"p\":" + st2(x * PX, y * PX) + ",\"s\":" + st2(r * 2 * PX, r * 2 * PX) + "}", f, s, sw, false)); }
        public void line(float a, float b, float c, float d, int k, float w) { add(group(path(new float[]{a, b, c, d}, false), 0, k, w, false)); }
        public void rect(float x, float y, float w, float h, int c) { rrect(x, y, w, h, 0, c, 0, 0); }
        public void rrect(float x, float y, float w, float h, float r, int f, int s, float sw) { add(group("{\"ty\":\"rc\",\"nm\":\"Rectángulo\",\"p\":" + st2((x + w / 2) * PX, (y + h / 2) * PX) + ",\"s\":" + st2(w * PX, h * PX) + ",\"r\":{\"a\":0,\"k\":" + n(r * PX) + "}}", f, s, sw, false)); }
    }
    static String n(double v) { String s = String.format(Locale.ROOT, "%.3f", v); s = s.replaceAll("0+$", "").replaceAll("\\.$", ""); return s.equals("-0") ? "0" : s; }
    static String st2(double a, double b) { return "{\"a\":0,\"k\":[" + n(a) + "," + n(b) + "]}"; }
    static String path(float[] p, boolean closed) {
        StringBuilder v = new StringBuilder(), z = new StringBuilder();
        for (int i = 0; i < p.length; i += 2) { if (i > 0) { v.append(','); z.append(','); } v.append('[').append(n(p[i] * PX)).append(',').append(n(p[i + 1] * PX)).append(']'); z.append("[0,0]"); }
        return "{\"ty\":\"sh\",\"nm\":\"Trazado\",\"ks\":{\"a\":0,\"k\":{\"c\":" + closed + ",\"v\":[" + v + "],\"i\":[" + z + "],\"o\":[" + z + "]}}}";
    }
    static String col(int c) { return "{\"a\":0,\"k\":[" + n((c >> 16 & 255) / 255.0) + "," + n((c >> 8 & 255) / 255.0) + "," + n((c & 255) / 255.0) + ",1]}"; }
    /** Grupo con su relleno y contorno; si el relleno es el color del traje, el grupo se llama "traje". */
    static String group(String shapes, int f, int s, float sw, boolean eo) {
        boolean suit = f != 0 && (f & 0xFFFFFF) == SUIT;
        StringBuilder b = new StringBuilder("{\"ty\":\"gr\",\"nm\":\"" + (suit ? "traje" : "forma") + "\",\"it\":[" + shapes);
        if (s != 0 && sw > 0) b.append(",{\"ty\":\"st\",\"nm\":\"Contorno\",\"c\":").append(col(s)).append(",\"o\":{\"a\":0,\"k\":").append(n((s >>> 24) * 100 / 255.0)).append("},\"w\":{\"a\":0,\"k\":").append(n(sw * PX)).append("},\"lc\":2,\"lj\":2,\"ml\":4}");
        if (f != 0) b.append(",{\"ty\":\"fl\",\"nm\":\"Relleno\",\"c\":").append(col(f)).append(",\"o\":{\"a\":0,\"k\":").append(n((f >>> 24) * 100 / 255.0)).append("},\"r\":").append(eo ? 2 : 1).append('}');
        b.append(",{\"ty\":\"tr\",\"p\":{\"a\":0,\"k\":[0,0]},\"a\":{\"a\":0,\"k\":[0,0]},\"s\":{\"a\":0,\"k\":[100,100]},\"r\":{\"a\":0,\"k\":0},\"o\":{\"a\":0,\"k\":100}}]}");
        return b.toString();
    }
    static String kf(List<Integer> ts, List<double[]> vs, boolean anim) {
        if (!anim) { double[] v = vs.get(0); StringBuilder b = new StringBuilder("{\"a\":0,\"k\":"); b.append(v.length == 1 ? n(v[0]) : arr(v)); return b.append('}').toString(); }
        StringBuilder b = new StringBuilder("{\"a\":1,\"k\":[");
        for (int i = 0; i < ts.size(); i++) { if (i > 0) b.append(','); b.append("{\"t\":").append(ts.get(i)).append(",\"s\":").append(arr(vs.get(i))).append(",\"h\":1}"); }
        return b.append("]}").toString();
    }
    static String arr(double[] v) { StringBuilder b = new StringBuilder("["); for (int i = 0; i < v.length; i++) { if (i > 0) b.append(','); b.append(n(v[i])); } return b.append(']').toString(); }

    public static void main(String[] a) throws Exception {
        Arte art; try (InputStream in = new FileInputStream(a[0])) { art = Arte.load(in); }
        File out = new File(a[1]); out.mkdirs(); RobotArt.exportAll = true;
        for (String[] an : RobotArt.ANIMS) {
            String name = an[0]; boolean ball = name.equals("doble_salto");
            List<List<Rec.Item>> frames = new ArrayList<>(); List<Integer> times = new ArrayList<>(); int t = 0;
            for (int i = 2; i < an.length; i++) {
                String[] f = an[i].split(":"); RobotArt.View v = RobotArt.frame(f[0]); Rec r = new Rec();
                // el doble salto es una bola, no las piezas del dibujo: va en vector
                Arte.Pen pen = new Arte.Pen(r, art); pen.suit = 0xFF000000 | SUIT; Gfx g = ball ? r : pen;
                r.translate(OX, OY); r.scale(PX, PX);
                RobotArt.drawPose(g, v, (1 << com.wap.core.Robot.PIECES) - 1, 0xFF000000 | SUIT, 0);
                frames.add(r.items); times.add(t); t += Integer.parseInt(f[1]);
            }
            int total = t;
            // capas: cada pieza (capa del dibujo + tramo con la misma matriz); el orden de dibujo se une entre cuadros
            List<String> order = new ArrayList<>(); Map<String, String> shapes = new HashMap<>(), layerOf = new HashMap<>();
            List<Map<String, double[]>> mats = new ArrayList<>();
            for (List<Rec.Item> fr : frames) {
                Map<String, Integer> seen = new HashMap<>(); Map<String, double[]> mm = new HashMap<>(); int at = 0;
                for (Rec.Item it : fr) {
                    int k = seen.merge(it.layer, 1, Integer::sum); String key = it.layer + "#" + k;
                    mm.put(key, it.m); layerOf.put(key, it.layer);
                    shapes.putIfAbsent(key, String.join(",", it.shapes));
                    int idx = order.indexOf(key); if (idx < 0) { order.add(at, key); at++; } else at = idx + 1;
                }
                mats.add(mm);
            }
            StringBuilder layers = new StringBuilder(); int ind = 1;
            // guía oculta: línea del suelo y centro (no se dibuja en el juego)
            layers.append("{\"ddd\":0,\"ind\":").append(ind++).append(",\"ty\":4,\"nm\":\"guia\",\"hd\":true,\"ip\":0,\"op\":").append(total).append(",\"st\":0,\"sr\":1,\"ks\":{\"o\":{\"a\":0,\"k\":40},\"r\":{\"a\":0,\"k\":0},\"p\":{\"a\":0,\"k\":[0,0]},\"a\":{\"a\":0,\"k\":[0,0]},\"s\":{\"a\":0,\"k\":[100,100]}},\"shapes\":[")
                .append(group(path(new float[]{0, OY / (float) PX, W / (float) PX, OY / (float) PX}, false), 0, 0xFF19C2A0, .2f, false)).append(',').append(group(path(new float[]{OX / (float) PX, 0, OX / (float) PX, H / (float) PX}, false), 0, 0xFF19C2A0, .2f, false)).append("]}");
            for (int li = order.size() - 1; li >= 0; li--) {
                String key = order.get(li);
                List<double[]> P = new ArrayList<>(), Rr = new ArrayList<>(), S = new ArrayList<>(), O = new ArrayList<>(); double last = Double.NaN; double[] prev = null;
                for (int f = 0; f < frames.size(); f++) {
                    double[] m = mats.get(f).get(key); boolean vis = m != null; if (m == null) m = prev != null ? prev : findAny(mats, key); prev = m;
                    double sx = Math.hypot(m[0], m[1]), det = m[0] * m[3] - m[1] * m[2], sy = det / sx, rot = Math.toDegrees(Math.atan2(m[1], m[0]));
                    if (!Double.isNaN(last)) while (rot - last > 180) rot -= 360; while (!Double.isNaN(last) && rot - last < -180) rot += 360; last = rot;
                    P.add(new double[]{m[4], m[5]}); Rr.add(new double[]{rot}); S.add(new double[]{sx * 100 / PX, sy * 100 / PX}); O.add(new double[]{vis ? 100 : 0});
                }
                boolean anim = frames.size() > 1;
                layers.append(",{\"ddd\":0,\"ind\":").append(ind++).append(",\"ty\":4,\"nm\":\"").append(layerOf.get(key)).append("\",\"ip\":0,\"op\":").append(total).append(",\"st\":0,\"sr\":1,\"ks\":{")
                    .append("\"o\":").append(kf(times, O, anim && O.stream().anyMatch(o -> o[0] == 0))).append(",\"r\":").append(kf(times, Rr, anim)).append(",\"p\":").append(kf(times, P, anim))
                    .append(",\"a\":{\"a\":0,\"k\":[0,0]},\"s\":").append(kf(times, S, anim)).append("},\"shapes\":[").append(shapes.get(key)).append("]}");
            }
            String json = "{\"v\":\"5.7.0\",\"fr\":60,\"ip\":0,\"op\":" + total + ",\"w\":" + W + ",\"h\":" + H + ",\"nm\":\"" + name + "\",\"ddd\":0,\"assets\":[],\"layers\":[" + layers + "]}";
            java.nio.file.Files.write(new File(out, name + ".json").toPath(), json.getBytes(StandardCharsets.UTF_8));
            System.out.println(name + ".json  " + frames.size() + " poses, " + total + " cuadros, " + order.size() + " capas");
        }
    }
    static double[] findAny(List<Map<String, double[]>> mats, String key) { for (Map<String, double[]> m : mats) if (m.containsKey(key)) return m.get(key); return new double[]{1, 0, 0, 1, 0, 0}; }
}
