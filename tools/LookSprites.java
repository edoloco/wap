import com.wap.core.*; import com.wap.render.*; import java.awt.image.BufferedImage; import javax.imageio.ImageIO; import java.io.*; import java.util.Base64;
/** Dibuja cada aspecto de disparo del juego como imagen (PNG en base64) para el Taller de armas. Uso: java LookSprites > looks.json */
public class LookSprites {
    public static void main(String[] a) throws Exception {
        World w = new World(5, 3, set(), set()); Renderer rd = new Renderer(); Wpn base = Wpn.ALL[Wpn.index("BAZOOKA")]; int orig = base.look;
        StringBuilder o = new StringBuilder("{");
        for (int part = 0; part < 2; part++) { // cuerpo y luz por separado: el juego dibuja la luz encima, y un dibujo propio reemplaza solo el cuerpo
            o.append(part == 0 ? "\"body\":[" : ",\"light\":[");
            for (int L = 0; L < Wpn.LOOKS.length; L++) {
                base.look = L; w.shots.clear(); Shot s = w.spawn(base, 0, 0, 0, 6, 0); s.age = 9; s.ang = 0; s.life = 40;
                BufferedImage im = new BufferedImage(240, 150, BufferedImage.TYPE_INT_ARGB); Sim.J g = new Sim.J(im);
                g.save(); g.translate(120, 75); g.scale(3.2f, 3.2f); rd.drawShotPart(g, s, part == 1); g.restore();
                ByteArrayOutputStream b = new ByteArrayOutputStream(); ImageIO.write(im, "png", b);
                o.append(L > 0 ? "," : "").append("\"data:image/png;base64,").append(Base64.getEncoder().encodeToString(b.toByteArray())).append('"');
            }
            o.append(']');
        }
        base.look = orig; System.out.println(o.append('}'));
    }
    static Wpn[] set() { Wpn[] r = new Wpn[Robot.SLOTS]; java.util.Arrays.fill(r, Wpn.ALL[0]); return r; }
}
