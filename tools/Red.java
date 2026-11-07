import com.wap.core.*; import com.wap.net.*;
import java.io.*; import java.net.*; import java.util.*; import java.util.concurrent.*;
/**
 * Prueba de red realista: dos "teléfonos" a 60 cuadros por segundo conectados por un proxy que imita el WiFi de
 * un teléfono (latencia base, variación y picos por ahorro de energía). Mide cuántos ticks avanza la partida por
 * segundo (60 = sin lentitud) y cuántas veces se congela. Uso: java Red <ms base> <ms variación> <ms pico> <segundos>
 */
public class Red {
    /** Reenvía bytes de un socket a otro con retraso, respetando el orden. */
    static void pipe(Socket a, Socket b, int base, int jit, int spike, long seed) {
        Thread t = new Thread(() -> {
            Random r = new Random(seed); DelayQueue<Item> q = new DelayQueue<>(); long[] last = {0};
            Thread out = new Thread(() -> { try { OutputStream o = b.getOutputStream(); while (true) { Item it = q.take(); if (it.data == null) return; o.write(it.data); o.flush(); } } catch (Exception e) { } });
            out.setDaemon(true); out.start();
            try {
                InputStream in = a.getInputStream(); byte[] buf = new byte[4096]; int n;
                while ((n = in.read(buf)) > 0) {
                    long lat = base + r.nextInt(jit + 1) + (r.nextInt(100) < 4 ? spike : 0); // 4 % de los paquetes con pico
                    long at = Math.max(last[0] + 1, System.nanoTime() + lat * 1_000_000L); last[0] = at;
                    q.put(new Item(Arrays.copyOf(buf, n), at));
                }
            } catch (Exception e) { }
            q.put(new Item(null, Long.MAX_VALUE / 2));
        }); t.setDaemon(true); t.start();
    }
    static final class Item implements Delayed { final byte[] data; final long at; Item(byte[] d, long t) { data = d; at = t; }
        public long getDelay(TimeUnit u) { return u.convert(at - System.nanoTime(), TimeUnit.NANOSECONDS); }
        public int compareTo(Delayed o) { return Long.compare(at, ((Item) o).at); } }
    public static double[] run(int base, int jit, int spike, int secs) throws Exception {
        ServerSocket mid = new ServerSocket(0);
        // el invitado se conecta al proxy (mid), y el proxy al anfitrión real
        ServerSocket real = new ServerSocket(0); Link[] H = new Link[1];
        Thread t = new Thread(() -> { try { H[0] = Link.accept(real); } catch (Exception e) { } }); t.start();
        Thread px = new Thread(() -> { try { Socket g = mid.accept(); Socket h = new Socket("127.0.0.1", real.getLocalPort()); g.setTcpNoDelay(true); h.setTcpNoDelay(true); pipe(g, h, base, jit, spike, 1); pipe(h, g, base, jit, spike, 2); } catch (Exception e) { } });
        px.start(); Link C = Link.connect("127.0.0.1", mid.getLocalPort()); t.join(); px.join();
        Wpn[] A = new Wpn[4]; for (int i = 0; i < 4; i++) A[i] = Wpn.ALL[i * 3];
        World[] w = {new World(5, 5, A, A), new World(5, 5, A, A)}; Link[] L = {H[0], C}; L[0].startReader(); L[1].startReader();
        Lockstep[] ls = {new Lockstep(L[0], 0), new Lockstep(L[1], 1)}; Bot[] bot = {new Bot(1, 0, 1), new Bot(2, 1, 1)};
        int[] frozen = new int[2]; int[] longest = new int[2];
        Thread[] ph = new Thread[2]; long end = System.nanoTime() + secs * 1_000_000_000L;
        for (int k = 0; k < 2; k++) { final int me = k; ph[k] = new Thread(() -> {
            long step = 16_666_667L, last = System.nanoTime(), accu = 0; int still = 0;
            while (System.nanoTime() < end) {
                long now = System.nanoTime(); accu += Math.min(now - last, 100_000_000L); last = now; int before = w[me].tick, n = 0;
                while (accu >= step && n++ < 4) { accu -= step; App_tick(ls[me], w[me], bot[me].think(w[me])); }
                if (w[me].tick == before && n > 0) { still++; if (still == 3) frozen[me]++; longest[me] = Math.max(longest[me], still); } else still = 0;
                try { Thread.sleep(Math.max(0, (step - (System.nanoTime() - now)) / 1_000_000L)); } catch (InterruptedException e) { return; }
            } }); ph[k].start(); }
        for (Thread p : ph) p.join();
        double tps = Math.min(w[0].tick, w[1].tick) / (double) secs;
        boolean same = true; for (Lockstep l : ls) same &= !l.desync;
        L[0].close(); L[1].close();
        return new double[]{tps, frozen[0] + frozen[1], Math.max(longest[0], longest[1]) * 16.7, same ? 1 : 0, ls[0].delay(), ls[1].delay()};
    }
    /** Igual que App.tick en línea. */
    static void App_tick(Lockstep l, World w, int mask) {
        l.advance(w, mask, w.fx::clear);
    }
    public static void main(String[] a) throws Exception {
        int[][] cases = {{2, 3, 0}, {25, 30, 120}, {45, 60, 250}, {80, 80, 300}};
        String[] name = {"WiFi ideal", "WiFi normal de teléfono", "WiFi cargado", "WiFi malo"};
        int secs = a.length > 0 ? Integer.parseInt(a[0]) : 8;
        for (int i = 0; i < cases.length; i++) {
            double[] r = run(cases[i][0], cases[i][1], cases[i][2], secs);
            System.out.printf(Locale.ROOT, "%-24s ticks/s %5.1f   congelones %3.0f   el más largo %4.0f ms   retraso %2.0f/%2.0f   %s%n", name[i], r[0], r[1], r[2], r[4], r[5], r[3] == 1 ? "en sincronía" : "DESINCRONIZADO");
        }
        System.exit(0);
    }
}
