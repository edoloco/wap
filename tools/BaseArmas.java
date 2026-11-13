import com.wap.core.*; import java.util.*;
/** Exporta las armas de siempre para el Taller de armas (artefacto). Uso: java BaseArmas > armas_base.json */
public class BaseArmas {
    static String n(double v) { String s = String.format(Locale.ROOT, "%.4f", v); s = s.replaceAll("0+$", "").replaceAll("\\.$", ""); return s.equals("-0") ? "0" : s; }
    static String q(String s) { return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }
    public static void main(String[] a) {
        StringBuilder o = new StringBuilder("{\"keys\":["); for (int i = 0; i < Wpn.KEYS.length; i++) o.append(i > 0 ? "," : "").append(q(Wpn.KEYS[i]));
        o.append("],\"looks\":["); for (int i = 0; i < Wpn.LOOKS.length; i++) o.append(i > 0 ? "," : "").append(q(Wpn.LOOKS[i]));
        o.append("],\"weapons\":[");
        for (int i = 0; i < Wpn.ALL.length; i++) { Wpn w = Wpn.ALL[i];
            o.append(i > 0 ? "," : "").append("{\"name\":").append(q(w.name)).append(",\"label\":").append(q(w.label)).append(",\"desc\":").append(q(w.desc)).append(",\"look\":").append(w.look)
             .append(",\"splinter\":").append(w.splinter != null).append(",\"sticky\":").append(w.sticky).append(",\"beam\":").append(w.beam).append(",\"lasso\":").append(w.lasso)
             .append(",\"trap\":").append(w.trap).append(",\"fan\":").append(w.fan).append(",\"punch\":").append(w.punch).append(",\"infinite\":").append(w.infinite).append(",\"drone\":").append(w.drone).append(",\"v\":{");
            for (int k = 0; k < Wpn.KEYS.length; k++) o.append(k > 0 ? "," : "").append(q(Wpn.KEYS[k])).append(':').append(n(w.get(Wpn.KEYS[k])));
            o.append("}}");
        }
        o.append("]}"); System.out.println(o);
    }
}
