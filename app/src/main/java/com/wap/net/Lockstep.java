package com.wap.net;
import com.wap.core.World;
import java.util.HashMap; import java.util.Map;
/** Partida sincronizada: cada teléfono simula lo mismo con las entradas de ambos, enviadas con unos ticks de adelanto. */
public final class Lockstep {
    public static final int DELAY = 4;
    final Link link; public final int me; int made; byte[] local = new byte[8192]; final HashMap<Integer, Integer> mine = new HashMap<>();
    public boolean desync; public int stall;
    public Lockstep(Link link, int me) { this.link = link; this.me = me; }
    /** Envía la entrada propia para los próximos ticks. */
    public void pump(World w, int mask) {
        while (made < w.tick + DELAY) {
            int m = made < DELAY ? 0 : mask;
            if (made == local.length) local = java.util.Arrays.copyOf(local, local.length * 2);
            local[made++] = (byte) m; link.sendInput(m);
        }
    }
    /** El otro teléfono va adelantado: se puede avanzar un tick extra para alcanzarlo. */
    public boolean behind(World w) { return link.count() > w.tick + DELAY + 1; }
    /** Avanza un tick si ya llegaron las entradas de ambos. */
    public boolean step(World w) {
        int t = w.tick;
        if (made <= t || link.count() <= t) { stall++; return false; }
        stall = 0;
        int[] in = new int[2]; in[me] = local[t] & 0xFF; in[1 - me] = link.input(t); w.step(in);
        if (w.tick % 60 == 0) { int h = w.hash(); mine.put(w.tick, h); link.sendHash(w.tick, h); }
        for (Map.Entry<Integer, Integer> e : link.hashes.entrySet()) {
            Integer m = mine.get(e.getKey());
            if (m != null) { if (!m.equals(e.getValue())) desync = true; mine.remove(e.getKey()); link.hashes.remove(e.getKey()); }
        }
        return true;
    }
}
