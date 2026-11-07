import com.wap.render.*;
import com.wap.core.Robot;
import java.awt.geom.AffineTransform; import java.awt.geom.Point2D;
import java.util.*;
/**
 * Mide la pose "de_pie_1" con la misma regla de tramos que usa el juego (Arte.Pen) y escribe, por capa y tramo,
 * la matriz del hueso y el centro de lo que dibuja. tools/importar_arte.py lo usa para pasar el dibujo del
 * usuario a coordenadas locales de cada hueso.
 */
public class Molde {
    static class Mat implements Gfx {
        AffineTransform t = new AffineTransform(); final ArrayDeque<AffineTransform> st = new ArrayDeque<>();
        public int w() { return 640; } public int h() { return 720; }
        public void save() { st.push(new AffineTransform(t)); } public void restore() { t = st.pop(); }
        public void translate(float x, float y) { t.translate(x, y); } public void rotate(float r) { t.rotate(r); } public void scale(float x, float y) { t.scale(x, y); }
        public void clear(int c) {} public void glow(float x, float y, float r, int c) {} public void additive(boolean on) {}
        public void beginPixels(int w, int h) {} public void endPixels(float x, float y, int s) {}
        public void poly(float[] p, int f, int s, float sw) {} public void grad(float[] p, int a, int b, float x0, float x1, int s, float sw) {}
        public void circle(float x, float y, float r, int f, int s, float sw) {} public void line(float a, float b, float c, float d, int k, float w) {}
        public void rect(float x, float y, float w, float h, int c) {} public void rrect(float x, float y, float w, float h, float r, int f, int s, float sw) {}
        public void text(String s, float x, float y, float z, int c, int a) {} public float textW(String s, float z) { return 0; }
        public void tile(int id, int[] px, int size, boolean ch, float x, float y) {}
    }
    public static void main(String[] a) {
        String frame = a.length > 0 ? a[0] : "de_pie_1";
        RobotArt.exportAll = true; Mat m = new Mat(); StringBuilder out = new StringBuilder("{");
        Map<String, Map<Integer, double[]>> rec = new LinkedHashMap<>();
        Arte.Pen pen = new Arte.Pen(m, null) {
            @Override protected boolean hit(boolean lit, float[] box) {
                super.hit(lit, box); if (layer == null) return false;
                double[] r = rec.computeIfAbsent(layer, k -> new LinkedHashMap<>()).computeIfAbsent(j, k -> { double[] q = new double[9]; m.t.getMatrix(q); return q; });
                double sx = 0, sy = 0; int n = box.length / 2;
                for (int i = 0; i < box.length; i += 2) { Point2D p = m.t.transform(new Point2D.Double(box[i], box[i + 1]), null); sx += p.getX(); sy += p.getY(); }
                r[6] += sx / n; r[7] += sy / n; r[8]++;
                return false;
            }
        };
        RobotArt.drawPose(pen, RobotArt.frame(frame), (1 << Robot.PIECES) - 1, RobotArt.COLOR[0], 0);
        boolean first = true;
        for (Map.Entry<String, Map<Integer, double[]>> e : rec.entrySet()) {
            out.append(first ? "" : ",").append("\n\"").append(e.getKey()).append("\":["); first = false; boolean f2 = true;
            for (double[] r : e.getValue().values()) {
                out.append(f2 ? "" : ",").append(String.format(Locale.ROOT, "[%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.4f,%.4f]", r[0], r[1], r[2], r[3], r[4], r[5], r[6] / r[8], r[7] / r[8])); f2 = false;
            }
            out.append("]");
        }
        System.out.println(out.append("\n}"));
    }
}
