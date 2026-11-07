import com.wap.render.*;
import com.wap.core.Robot;
import java.awt.geom.AffineTransform;
import java.io.*; import java.nio.charset.StandardCharsets; import java.util.*;
/** Exporta la plantilla del personaje: un SVG por cuadro, con una capa con nombre por pieza. */
public class Plantilla {
    /** Lienzo en unidades del juego: x de -32 a 32, y de -64 a 8; los pies en (0, 0). Se muestra a 10 píxeles por unidad. */
    static final float VX = -32, VY = -64, VW = 64, VH = 72, PX = 10;
    static class Svg implements Gfx {
        final StringBuilder out = new StringBuilder(); AffineTransform t = new AffineTransform(); final ArrayDeque<AffineTransform> st = new ArrayDeque<>();
        final Map<String, Integer> used = new HashMap<>(); boolean open;
        public int w() { return (int) (VW * PX); } public int h() { return (int) (VH * PX); }
        public void save() { st.push(new AffineTransform(t)); } public void restore() { t = st.pop(); }
        public void translate(float x, float y) { t.translate(x, y); } public void rotate(float r) { t.rotate(r); } public void scale(float x, float y) { t.scale(x, y); }
        public void clear(int c) {} public void beginPixels(int w, int h) {} public void endPixels(float x, float y, int s) {}
        public void glow(float x, float y, float r, int c) {} public void additive(boolean on) {}
        public void text(String s, float x, float y, float z, int c, int a) {} public float textW(String s, float z) { return 0; }
        public void tile(int id, int[] px, int size, boolean ch, float x, float y) {}
        static String hex(int c) { return String.format("#%06X", c & 0xFFFFFF); }
        static String f(double v) { return String.format(Locale.ROOT, "%.3f", v).replaceAll("0+$", "").replaceAll("\\.$", ""); }
        String m() { double[] q = new double[6]; t.getMatrix(q); return " transform=\"matrix(" + f(q[0]) + " " + f(q[1]) + " " + f(q[2]) + " " + f(q[3]) + " " + f(q[4]) + " " + f(q[5]) + ")\""; }
        String paint(int fill, int stroke, float sw) {
            StringBuilder s = new StringBuilder();
            s.append(" fill=\"").append(fill == 0 ? "none" : hex(fill)).append('"');
            if (fill != 0 && (fill >>> 24) != 255) s.append(" fill-opacity=\"").append(f((fill >>> 24) / 255.0)).append('"');
            if (stroke != 0 && sw > 0) s.append(" stroke=\"").append(hex(stroke)).append("\" stroke-width=\"").append(f(sw)).append("\" stroke-linejoin=\"round\" stroke-linecap=\"round\"");
            return s.toString();
        }
        void need() { if (!open) layer("otros"); }
        public void layer(String name) {
            endLayer(); int n = used.merge(name, 1, Integer::sum); String id = n == 1 ? name : name + "-" + n;
            out.append("  <g id=\"").append(id).append("\" inkscape:groupmode=\"layer\" inkscape:label=\"").append(id).append("\">\n"); open = true;
        }
        public void endLayer() { if (open) out.append("  </g>\n"); open = false; }
        public void poly(float[] p, int fill, int stroke, float sw) {
            need(); StringBuilder d = new StringBuilder("M");
            for (int i = 0; i < p.length; i += 2) d.append(i == 0 ? "" : " L").append(f(p[i])).append(' ').append(f(p[i + 1]));
            out.append("    <path").append(m()).append(" d=\"").append(d).append(" Z\"").append(paint(fill, stroke, sw)).append("/>\n");
        }
        public void grad(float[] p, int c0, int c1, float x0, float x1, int s, float sw) { poly(p, c0, s, sw); }
        public void circle(float x, float y, float r, int fill, int stroke, float sw) { need(); out.append("    <circle").append(m()).append(" cx=\"").append(f(x)).append("\" cy=\"").append(f(y)).append("\" r=\"").append(f(r)).append('"').append(paint(fill, stroke, sw)).append("/>\n"); }
        public void line(float a, float b, float c, float d, int k, float w) { need(); out.append("    <line").append(m()).append(" x1=\"").append(f(a)).append("\" y1=\"").append(f(b)).append("\" x2=\"").append(f(c)).append("\" y2=\"").append(f(d)).append('"').append(paint(0, k, w)).append("/>\n"); }
        public void rect(float x, float y, float w, float h, int c) { need(); out.append("    <rect").append(m()).append(" x=\"").append(f(x)).append("\" y=\"").append(f(y)).append("\" width=\"").append(f(w)).append("\" height=\"").append(f(h)).append('"').append(paint(c, 0, 0)).append("/>\n"); }
        public void rrect(float x, float y, float w, float h, float r, int fill, int stroke, float sw) { need(); out.append("    <rect").append(m()).append(" x=\"").append(f(x)).append("\" y=\"").append(f(y)).append("\" width=\"").append(f(w)).append("\" height=\"").append(f(h)).append("\" rx=\"").append(f(r)).append('"').append(paint(fill, stroke, sw)).append("/>\n"); }
        String doc(String name) {
            endLayer();
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!-- WAP · cuadro \"" + name + "\". Dibuja encima de cada capa y conserva sus nombres. Pies en (0,0), mirando a la derecha. -->\n"
                + "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:inkscape=\"http://www.inkscape.org/namespaces/inkscape\" width=\"" + (int) (VW * PX) + "\" height=\"" + (int) (VH * PX) + "\" viewBox=\"" + f(VX) + " " + f(VY) + " " + f(VW) + " " + f(VH) + "\">\n"
                + "  <g id=\"guia\" inkscape:groupmode=\"layer\" inkscape:label=\"guia\" sodipodi:insensitive=\"true\" xmlns:sodipodi=\"http://sodipodi.sourceforge.net/DTD/sodipodi-0.dtd\" opacity=\"0.5\">\n"
                + "    <line x1=\"-32\" y1=\"0\" x2=\"32\" y2=\"0\" stroke=\"#3DDC97\" stroke-width=\"0.3\"/>\n"
                + "    <line x1=\"0\" y1=\"-64\" x2=\"0\" y2=\"8\" stroke=\"#3DDC97\" stroke-width=\"0.2\" stroke-dasharray=\"1 1\"/>\n"
                + "    <rect x=\"-7\" y=\"-54\" width=\"14\" height=\"54\" fill=\"none\" stroke=\"#FF6FA0\" stroke-width=\"0.2\" stroke-dasharray=\"1 1\"/>\n"
                + "  </g>\n" + out + "</svg>\n";
        }
    }
    public static void main(String[] a) throws Exception {
        File dir = new File(a.length > 0 ? a[0] : "arte/plantilla"); dir.mkdirs();
        RobotArt.exportAll = true; int all = (1 << Robot.PIECES) - 1;
        for (String n : RobotArt.FRAMES) {
            Svg g = new Svg(); RobotArt.drawPose(g, RobotArt.frame(n), all, RobotArt.COLOR[0], 0);
            try (Writer w = new OutputStreamWriter(new FileOutputStream(new File(dir, n + ".svg")), StandardCharsets.UTF_8)) { w.write(g.doc(n)); }
        }
        System.out.println(RobotArt.FRAMES.length + " cuadros en " + dir);
    }
}
