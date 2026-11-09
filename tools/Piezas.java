import com.wap.render.*; import java.awt.image.BufferedImage; import javax.imageio.ImageIO; import java.io.File;
/** Exporta las piezas vectoriales del personaje como PNG (punto de partida para redibujarlas): tools/piezas.sh */
public class Piezas {
    public static void main(String[] a) throws Exception {
        File dir = new File(a.length > 0 ? a[0] : "app/src/main/assets/personaje"); dir.mkdirs();
        Sprites.load(n -> null); // solo prepara las cajas
        for (Sprites.Piece p : Sprites.PIECES.values()) {
            BufferedImage b = new BufferedImage(p.pw(), p.ph(), BufferedImage.TYPE_INT_ARGB); Sim.J g = new Sim.J(b);
            g.scale(Sprites.PPU, Sprites.PPU); g.translate(-p.x0, -p.y0);
            RobotArt.vec(g, p.name, 0xFFFF00FF, false); // el traje en magenta
            ImageIO.write(b, "png", new File(dir, p.name + ".png"));
            System.out.println(p.name + ".png  " + b.getWidth() + "x" + b.getHeight());
        }
    }
}
