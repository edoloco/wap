import com.wap.render.*;
import com.wap.core.Robot;
import java.awt.*; import java.awt.geom.*; import java.awt.image.BufferedImage; import javax.imageio.ImageIO; import java.io.File;
/** Genera el ícono de la app: el personaje disparando, con traje rojo. Uso: java Icono app/src/main/res */
public class Icono {
    static final int RED = 0xFFE8231E, BG0 = 0xFF1B1F4A, BG1 = 0xFF2E4BA8;
    /** Personaje disparando en una imagen transparente, recortado a su contenido. */
    static BufferedImage hero(int size) {
        BufferedImage b = new BufferedImage(size * 2, size * 2, BufferedImage.TYPE_INT_ARGB); Sim.J g = new Sim.J(b);
        RobotArt.View v = RobotArt.frame("disparo"); v.shotT = 5;
        float s = size / 70f; g.save(); g.translate(size * .8f, size * 1.5f); g.scale(s, s);
        RobotArt.drawPose(g, v, (1 << Robot.PIECES) - 1, RED, 0); g.restore();
        int x0 = b.getWidth(), y0 = b.getHeight(), x1 = 0, y1 = 0;
        for (int y = 0; y < b.getHeight(); y++) for (int x = 0; x < b.getWidth(); x++) if ((b.getRGB(x, y) >>> 24) > 20) { x0 = Math.min(x0, x); y0 = Math.min(y0, y); x1 = Math.max(x1, x); y1 = Math.max(y1, y); }
        return b.getSubimage(x0, y0, x1 - x0 + 1, y1 - y0 + 1);
    }
    /** Dibuja el personaje centrado dentro de un cuadro de lado 'box', con un fogonazo detrás de la mano. */
    static void place(Graphics2D c, BufferedImage h, float cx, float cy, float box) {
        float k = Math.min(box / h.getWidth(), box / h.getHeight()); int w = Math.round(h.getWidth() * k), hh = Math.round(h.getHeight() * k);
        int x = Math.round(cx - w / 2f), y = Math.round(cy - hh / 2f);
        float fx = x + w * .93f, fy = y + hh * .42f, fr = box * .32f; // luz del disparo
        c.setPaint(new RadialGradientPaint(fx, fy, fr, new float[]{0, .35f, 1}, new Color[]{new Color(255, 250, 210, 240), new Color(255, 214, 80, 170), new Color(255, 160, 40, 0)}));
        c.fill(new Ellipse2D.Float(fx - fr, fy - fr, fr * 2, fr * 2));
        c.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        c.drawImage(h, x, y, w, hh, null);
    }
    static void background(Graphics2D c, int n, boolean round) {
        Shape sh = round ? new RoundRectangle2D.Float(0, 0, n, n, n * .36f, n * .36f) : new Rectangle2D.Float(0, 0, n, n);
        c.setPaint(new GradientPaint(0, 0, new Color(BG1), 0, n, new Color(BG0))); c.fill(sh);
        c.setPaint(new Color(0x8214FF)); c.fill(new Rectangle2D.Float(0, n * .86f, n, n * .14f)); // plataforma morada del juego
        c.setPaint(new Color(0xB46BFF)); c.fill(new Rectangle2D.Float(0, n * .86f, n, Math.max(1, n * .015f)));
        if (round) { c.setComposite(AlphaComposite.DstIn); c.setPaint(Color.WHITE); c.fill(sh); c.setComposite(AlphaComposite.SrcOver); }
    }
    static Graphics2D gfx(BufferedImage b) { Graphics2D c = b.createGraphics(); c.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); return c; }
    public static void main(String[] a) throws Exception {
        File res = new File(a.length > 0 ? a[0] : "app/src/main/res"); BufferedImage h = hero(900);
        String[] dens = {"mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"}; int[] px = {48, 72, 96, 144, 192};
        for (int i = 0; i < dens.length; i++) {
            int n = px[i]; File d = new File(res, "mipmap-" + dens[i]); d.mkdirs();
            BufferedImage big = new BufferedImage(n * 4, n * 4, BufferedImage.TYPE_INT_ARGB); Graphics2D c = gfx(big);
            int N = n * 4; c.setClip(new RoundRectangle2D.Float(0, 0, N, N, N * .36f, N * .36f)); background(c, N, false);
            place(c, h, N * .47f, N * .5f, N * .84f); c.dispose();
            ImageIO.write(scale(big, n), "png", new File(d, "ic_launcher.png"));
            // capa delantera del ícono adaptable: 108 dp, con el personaje dentro de la zona segura central
            int f = Math.round(n * 108 / 48f); File dd = new File(res, "drawable-" + dens[i]); dd.mkdirs();
            BufferedImage fg = new BufferedImage(f * 4, f * 4, BufferedImage.TYPE_INT_ARGB); Graphics2D q = gfx(fg); int F = f * 4;
            place(q, h, F * .48f, F * .5f, F * .6f); q.dispose();
            ImageIO.write(scale(fg, f), "png", new File(dd, "ic_launcher_fg.png"));
            BufferedImage bg = new BufferedImage(f * 4, f * 4, BufferedImage.TYPE_INT_ARGB); Graphics2D r = gfx(bg); background(r, f * 4, false); r.dispose();
            ImageIO.write(scale(bg, f), "png", new File(dd, "ic_launcher_bg.png"));
        }
        BufferedImage prev = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB); Graphics2D c = gfx(prev);
        c.setClip(new RoundRectangle2D.Float(0, 0, 512, 512, 184, 184)); background(c, 512, false); place(c, h, 512 * .47f, 256, 512 * .84f); c.dispose();
        ImageIO.write(prev, "png", new File(a.length > 1 ? a[1] : "icono.png"));
    }
    static BufferedImage scale(BufferedImage src, int n) {
        Image s = src.getScaledInstance(n, n, Image.SCALE_AREA_AVERAGING); BufferedImage o = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        Graphics2D c = o.createGraphics(); c.drawImage(s, 0, 0, null); c.dispose(); return o;
    }
}
