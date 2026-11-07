package com.wap.net;
import com.wap.core.World;
import java.util.HashMap; import java.util.Map;
/** Partida sincronizada: cada teléfono simula lo mismo con las entradas de ambos, enviadas con unos ticks de adelanto. */
public final class Lockstep {
    /** Retraso de entradas en ticks: empieza en 4 y se ajusta solo según el ping y los congelones. */
    public static final int DELAY = 4, MIN_DELAY = 3, MAX_DELAY = 8, CATCH_UP = 6;
    int delay = DELAY, extra, calm, lastStallTick = -1;
    public int delay() { return delay; }
    public float ping() { return link.rtt; }
    /** Elige el retraso: la mitad del ping más su variación, más un margen que sube con cada congelón. */
    void adapt(World w) {
        if (w.tick % 15 == 0 && w.tick != lastPing) { lastPing = w.tick; link.ping(); }
        float rtt = link.rtt; int base = rtt < 0 ? DELAY : (int) Math.ceil((rtt / 2 + link.rttDev) / 16.67f) + 1;
        if (++calm > 300 && extra > 0) { extra--; calm = 0; } // 5 s sin congelones: baja el margen
        delay = Math.max(MIN_DELAY, Math.min(MAX_DELAY, base + extra));
    }
    int lastPing = -1;
    /** Ticks que se deben al reloj: tras un congelón se recuperan para que la partida no quede más lenta. */
    int owed;
    /** Un cuadro de juego (1/60 s): avanza lo que corresponde y recupera lo atrasado. each se llama tras cada tick. */
    public int advance(World w, int mask, Runnable each) {
        owed = Math.min(owed + 1, MAX_OWED); int n = 0;
        while (n < CATCH_UP) {
            pump(w, mask);
            if (owed <= 0 && !behind(w)) break;
            if (!step(w)) break;
            owed = Math.max(0, owed - 1); n++; if (each != null) each.run();
        }
        return n;
    }
    static final int MAX_OWED = 45;
    final Link link; public final int me; int made; byte[] local = new byte[8192]; final HashMap<Integer, Integer> mine = new HashMap<>();
    public boolean desync; public int stall;
    public Lockstep(Link link, int me) { this.link = link; this.me = me; }
    /** Envía la entrada propia para los próximos ticks. */
    public void pump(World w, int mask) {
        adapt(w);
        while (made < w.tick + delay) {
            int m = made < DELAY ? 0 : mask;
            if (made == local.length) local = java.util.Arrays.copyOf(local, local.length * 2);
            local[made++] = (byte) m; link.sendInput(m);
        }
    }
    /** El otro teléfono va adelantado: se puede avanzar un tick extra para alcanzarlo. */
    public boolean behind(World w) { return link.count() > w.tick + delay + 1; }
    /** Avanza un tick si ya llegaron las entradas de ambos. */
    public boolean step(World w) {
        int t = w.tick;
        if (made <= t || link.count() <= t) {
            if (link.count() <= t && lastStallTick != t) { lastStallTick = t; extra = Math.min(3, extra + 1); calm = 0; } // congelón: más margen
            stall++; return false;
        }
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
