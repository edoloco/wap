package com.wap.net;
import java.io.*; import java.net.*; import java.util.Arrays; import java.util.concurrent.ConcurrentHashMap; import java.util.concurrent.LinkedBlockingQueue;
/** Conexión TCP entre dos teléfonos: saludo inicial, entradas de cada tick y huellas del estado. */
public final class Link implements Closeable {
    public static final int PORT = 47812;
    static final int HELLO = 1, INPUT = 2, HASH = 3, BYE = 4, PING = 5, PONG = 6;
    /** Datos de cada jugador al empezar: reglas, semilla, vidas, color y armas. */
    public static final class Hello {
        public int rules, seed, lives, color; public int[] slots = new int[0]; public String name = "";
        /** Nivel del anfitrión (vacío: aleatorio). El invitado lo usa solo para esta partida. */
        public byte[] level = new byte[0];
        void write(DataOutputStream o) throws IOException { o.writeInt(rules); o.writeInt(seed); o.writeInt(lives); o.writeInt(color); o.writeByte(slots.length); for (int s : slots) o.writeShort(s); o.writeUTF(name); o.writeInt(level.length); o.write(level); }
        static Hello read(DataInputStream i) throws IOException {
            Hello h = new Hello(); h.rules = i.readInt(); h.seed = i.readInt(); h.lives = i.readInt(); h.color = i.readInt();
            int n = i.readUnsignedByte(); if (n > 8) throw new IOException("saludo inválido"); h.slots = new int[n];
            for (int k = 0; k < n; k++) h.slots[k] = i.readShort(); h.name = i.readUTF();
            int len = i.readInt(); if (len < 0 || len > 16 << 20) throw new IOException("nivel demasiado grande");
            h.level = new byte[len]; i.readFully(h.level); return h;
        }
    }
    private final Socket s; private final DataInputStream in; private final DataOutputStream out; private final Object wl = new Object();
    private volatile byte[] remote = new byte[8192]; private volatile int count;
    public volatile boolean closed; public volatile String error;
    public final ConcurrentHashMap<Integer, Integer> hashes = new ConcurrentHashMap<>();
    private Link(Socket s) throws IOException {
        this.s = s; s.setTcpNoDelay(true);
        in = new DataInputStream(new BufferedInputStream(s.getInputStream())); out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
    }
    public static Link accept(ServerSocket ss) throws IOException { return new Link(ss.accept()); }
    public static Link connect(String host, int port) throws IOException { Socket s = new Socket(); s.connect(new InetSocketAddress(host, port), 4000); return new Link(s); }
    public void send(Hello h) throws IOException { synchronized (wl) { out.writeByte(HELLO); h.write(out); out.flush(); } }
    public Hello readHello() throws IOException {
        s.setSoTimeout(15000); // el saludo puede traer un nivel
        if (in.readByte() != HELLO) throw new IOException("protocolo distinto");
        Hello h = Hello.read(in); s.setSoTimeout(0); return h;
    }
    /** Desde aquí un hilo recibe todo lo que manda el otro teléfono. */
    public void startReader() {
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    int type = in.readByte();
                    if (type == INPUT) put(in.readByte());
                    else if (type == PING) { long t0 = in.readLong(); queue(PONG, t0); } // responde al instante
                    else if (type == PONG) sample((System.nanoTime() - in.readLong()) / 1e6f);
                    else if (type == HASH) { int k = in.readInt(); hashes.put(k, in.readInt()); }
                    else if (type == BYE) { closed = true; return; }
                    else throw new IOException("mensaje desconocido");
                }
            } catch (IOException e) { if (!closed) error = "Se perdió la conexión con el otro teléfono"; closed = true; }
        }, "partida-red");
        t.setDaemon(true); t.start();
        writer = true;
        Thread wt = new Thread(() -> { // los envíos van en su propio hilo: la red lenta nunca frena el dibujo
            try {
                while (true) {
                    long[] m = outQ.take(); if (m == null || m[0] == BYE) break;
                    synchronized (wl) {
                        out.writeByte((int) m[0]);
                        if (m[0] == INPUT) out.writeByte((int) m[1]); else if (m[0] == HASH) { out.writeInt((int) m[1]); out.writeInt((int) m[2]); } else out.writeLong(m[1]);
                        if (outQ.isEmpty()) out.flush();
                    }
                }
            } catch (Exception e) { if (!closed) error = "Se perdió la conexión con el otro teléfono"; closed = true; }
        }, "partida-envio");
        wt.setDaemon(true); wt.start();
    }
    private final LinkedBlockingQueue<long[]> outQ = new LinkedBlockingQueue<>();
    private void queue(long... m) { if (!closed) outQ.add(m); }
    /** Ping de ida y vuelta (ms): promedio y variación, para ajustar el retraso de entradas. */
    public volatile float rtt = -1, rttDev = 0;
    private void sample(float ms) {
        if (rtt < 0) { rtt = ms; rttDev = ms / 4; return; }
        float d = Math.abs(ms - rtt); rttDev += (d - rttDev) * .2f; rtt += (ms - rtt) * .15f;
    }
    public void ping() { queue(PING, System.nanoTime()); }
    private void put(byte b) {
        byte[] r = remote; if (count == r.length) { r = Arrays.copyOf(r, r.length * 2); remote = r; }
        r[count] = b; count = count + 1;
    }
    public int count() { return count; }
    public int input(int i) { return remote[i] & 0xFF; }
    public void sendInput(int m) { if (started()) queue(INPUT, m); else write(INPUT, m, 0, true); }
    public void sendHash(int tick, int h) { if (started()) queue(HASH, tick, h); else write(HASH, tick, h, false); }
    private volatile boolean writer; private boolean started() { return writer; }
    private void write(int type, int a, int b, boolean small) {
        if (closed) return;
        try { synchronized (wl) { out.writeByte(type); if (small) out.writeByte(a); else { out.writeInt(a); out.writeInt(b); } out.flush(); } }
        catch (IOException e) { error = "Se perdió la conexión con el otro teléfono"; closed = true; }
    }
    @Override public void close() {
        outQ.add(new long[]{BYE});
        try { synchronized (wl) { out.writeByte(BYE); out.flush(); } } catch (IOException e) { }
        closed = true; try { s.close(); } catch (IOException e) { }
    }
}
