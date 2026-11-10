import com.wap.render.*; import java.util.*;
/**
 * Exporta para el editor de piezas (artefacto) las piezas de siempre como vectores y, para cada pose del juego,
 * dónde va cada pieza (matriz) y en qué orden se dibuja. Uso: java BasePiezas > base.json
 */
public class BasePiezas {
    static final int SUIT = 0xFF5F574E;
    static String n(double v) { String s = String.format(Locale.ROOT, "%.3f", v); s = s.replaceAll("0+$", "").replaceAll("\\.$", ""); return s.equals("-0") ? "0" : s; }
    /** Sigue la matriz actual; como PartSink anota cada pieza; como Gfx anota las formas de una pieza. */
    static class Rec implements Gfx, RobotArt.PartSink {
        double[] m = {1, 0, 0, 1, 0, 0}; final ArrayDeque<double[]> st = new ArrayDeque<>();
        final StringBuilder out = new StringBuilder(); boolean first = true;
        static double[] mul(double[] a, double[] b) { return new double[]{a[0] * b[0] + a[2] * b[1], a[1] * b[0] + a[3] * b[1], a[0] * b[2] + a[2] * b[3], a[1] * b[2] + a[3] * b[3], a[0] * b[4] + a[2] * b[5] + a[4], a[1] * b[4] + a[3] * b[5] + a[5]}; }
        public int w() { return 640; } public int h() { return 720; }
        public void save() { st.push(m.clone()); } public void restore() { m = st.pop(); }
        public void translate(float x, float y) { m = mul(m, new double[]{1, 0, 0, 1, x, y}); }
        public void rotate(float r) { double c = Math.cos(r), s = Math.sin(r); m = mul(m, new double[]{c, s, -s, c, 0, 0}); }
        public void scale(float x, float y) { m = mul(m, new double[]{x, 0, 0, y, 0, 0}); }
        public void clear(int c) {} public void glow(float x, float y, float r, int c) {} public void additive(boolean on) {}
        public void beginPixels(int w, int h) {} public void endPixels(float x, float y, int s) {}
        public void text(String s, float x, float y, float z, int c, int a) {} public float textW(String s, float z) { return 0; }
        public void tile(int id, int[] px, int size, boolean ch, float x, float y) {}
        void sep() { if (!first) out.append(','); first = false; }
        public void part(String name, int suit, boolean lit) {
            sep(); out.append("{\"n\":\"").append(name).append("\",\"m\":[");
            for (int i = 0; i < 6; i++) out.append(i > 0 ? "," : "").append(n(m[i]));
            out.append("],\"ink\":").append(suit != SUIT).append('}');
        }
        // formas de una pieza, en coordenadas de la pieza (con la matriz actual aplicada, que aquí es la identidad o la del hombro)
        void shape(float[][] ps, int f, int s, float sw, boolean eo, boolean open) {
            sep(); out.append("{\"fill\":\"").append(String.format("%08X", f)).append("\",\"stroke\":\"").append(String.format("%08X", s)).append("\",\"sw\":").append(n(sw))
                .append(",\"eo\":").append(eo).append(",\"open\":").append(open).append(",\"suit\":").append(f != 0 && f == SUIT).append(",\"polys\":[");
            for (int k = 0; k < ps.length; k++) { out.append(k > 0 ? "," : "").append('['); for (int i = 0; i < ps[k].length; i += 2) { double x = ps[k][i], y = ps[k][i + 1];
                double X = m[0] * x + m[2] * y + m[4], Y = m[1] * x + m[3] * y + m[5]; out.append(i > 0 ? "," : "").append(n(X)).append(',').append(n(Y)); } out.append(']'); }
            out.append("]}");
        }
        public void poly(float[] p, int f, int s, float sw) { shape(new float[][]{p}, f, s, sw, false, false); }
        public void shape(float[][] ps, int f, boolean eo, int s, float sw) { shape(ps, f, s, sw, eo, false); }
        public void grad(float[] p, int c0, int c1, float x0, float x1, int s, float sw) { poly(p, c0, s, sw); }
        public void circle(float x, float y, float r, int f, int s, float sw) { float[] p = new float[64]; for (int i = 0; i < 32; i++) { double a = i * Math.PI / 16; p[i * 2] = x + (float) (Math.cos(a) * r); p[i * 2 + 1] = y + (float) (Math.sin(a) * r); } poly(p, f, s, sw); }
        public void line(float a, float b, float c, float d, int k, float w) { shape(new float[][]{{a, b, c, d}}, 0, k, w, false, true); }
        public void rect(float x, float y, float w, float h, int c) { poly(new float[]{x, y, x + w, y, x + w, y + h, x, y + h}, c, 0, 0); }
        public void rrect(float x, float y, float w, float h, float r, int f, int s, float sw) {
            r = Math.min(r, Math.min(w, h) / 2); ArrayList<Float> q = new ArrayList<>(); float[][] cs = {{x + w - r, y + r, -90}, {x + w - r, y + h - r, 0}, {x + r, y + h - r, 90}, {x + r, y + r, 180}};
            for (float[] c : cs) for (int i = 0; i <= 4; i++) { double a = Math.toRadians(c[2] + i * 22.5); q.add(c[0] + (float) (Math.cos(a) * r)); q.add(c[1] + (float) (Math.sin(a) * r)); }
            float[] p = new float[q.size()]; for (int i = 0; i < p.length; i++) p[i] = q.get(i); poly(p, f, s, sw);
        }
    }
    public static void main(String[] a) {
        StringBuilder o = new StringBuilder("{\"pieces\":{");
        for (int i = 0; i < RobotArt.PARTS.length; i++) {
            Rec r = new Rec(); RobotArt.vec(r, RobotArt.PARTS[i], SUIT, false);
            o.append(i > 0 ? "," : "").append('"').append(RobotArt.PARTS[i]).append("\":[").append(r.out).append(']');
        }
        o.append("},\"poses\":{"); boolean f = true;
        for (String fr : RobotArt.FRAMES) {
            Rec r = new Rec(); RobotArt.View v = RobotArt.frame(fr);
            RobotArt.drawPose(r, v, (1 << com.wap.core.Robot.PIECES) - 1, SUIT, 0);
            o.append(f ? "" : ",").append('"').append(fr).append("\":[").append(r.out).append(']'); f = false;
        }
        o.append("}}"); System.out.println(o);
    }
}
