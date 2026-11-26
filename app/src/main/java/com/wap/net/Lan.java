package com.wap.net;
import java.io.Closeable; import java.io.IOException; import java.net.*; import java.nio.charset.StandardCharsets; import java.util.*; import java.util.concurrent.ConcurrentHashMap;
/** Salas en la red local: quien crea la sala la anuncia por difusión UDP y los demás la ven en su lista. */
public final class Lan {
    public static final int DISC = 47811; static final String TAG = "WAP-SALA";
    private Lan() {}
    /** Identifica a este teléfono en los anuncios: un mismo teléfono nunca muestra dos salas, aunque cree otra con otro puerto. */
    static final long DEVICE = new java.util.Random().nextLong() & 0xFFFFFFFFFFFFL;
    public static final class Room {
        public final String name, ip; public final int port, rules; final long born; volatile long seen;
        Room(String name, String ip, int port, int rules, long born) { this.name = name; this.ip = ip; this.port = port; this.rules = rules; this.born = born; }
    }
    static List<InetAddress> targets() {
        List<InetAddress> out = new ArrayList<>();
        try { out.add(InetAddress.getByName("255.255.255.255")); } catch (IOException e) { }
        try {
            for (NetworkInterface n : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!n.isUp() || n.isLoopback()) continue;
                for (InterfaceAddress a : n.getInterfaceAddresses()) if (a.getBroadcast() != null && !out.contains(a.getBroadcast())) out.add(a.getBroadcast());
            }
        } catch (IOException | RuntimeException e) { }
        try { out.add(InetAddress.getByName("127.0.0.1")); } catch (IOException e) { } // para jugar y probar en el mismo equipo
        return out;
    }
    /** Anuncia la sala cada 0,7 segundos hasta cerrarse. */
    public static final class Beacon implements Closeable {
        private final DatagramSocket s; private volatile boolean on = true;
        public Beacon(String name, int port, int rules) throws IOException {
            s = new DatagramSocket(); s.setBroadcast(true);
            byte[] msg = (TAG + "|" + rules + "|" + port + "|" + Long.toHexString(DEVICE) + "|" + System.nanoTime() + "|" + name.replace('|', ' ')).getBytes(StandardCharsets.UTF_8);
            Thread t = new Thread(() -> {
                List<InetAddress> to = targets(); int n = 0;
                while (on) {
                    if (++n % 10 == 0) to = targets();
                    for (InetAddress a : to) try { s.send(new DatagramPacket(msg, msg.length, a, DISC)); } catch (IOException e) { }
                    try { Thread.sleep(700); } catch (InterruptedException e) { return; }
                }
            }, "sala-anuncio");
            t.setDaemon(true); t.start();
        }
        @Override public void close() { on = false; s.close(); }
    }
    /** Escucha anuncios y mantiene la lista de salas vistas en los últimos 3 segundos. */
    public static final class Browser implements Closeable {
        private final DatagramSocket s; private volatile boolean on = true; private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
        public Browser() throws IOException {
            s = new DatagramSocket(null); s.setReuseAddress(true); s.setBroadcast(true); s.bind(new InetSocketAddress(DISC)); s.setSoTimeout(500);
            Thread t = new Thread(() -> {
                byte[] buf = new byte[512];
                while (on) {
                    try {
                        DatagramPacket p = new DatagramPacket(buf, buf.length); s.receive(p);
                        String[] f = new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8).split("\\|", 6);
                        if (f.length != 6 || !f[0].equals(TAG)) continue;
                        String ip = p.getAddress().getHostAddress(); int port = Integer.parseInt(f[2]); long born = Long.parseLong(f[4]);
                        Room r = new Room(f[5], ip, port, Integer.parseInt(f[1]), born); r.seen = System.currentTimeMillis();
                        String key = f[3]; // un teléfono, una sala: el anuncio de una sala vieja o el mismo anuncio por otra red no la duplica
                        Room old = rooms.get(key);
                        if (old == null || born > old.born || born == old.born && old.ip.startsWith("127.") && !ip.startsWith("127.")) rooms.put(key, r);
                        else if (born == old.born) old.seen = r.seen;
                    } catch (SocketTimeoutException e) { } catch (IOException | RuntimeException e) { if (!on) return; }
                }
            }, "sala-busqueda");
            t.setDaemon(true); t.start();
        }
        public List<Room> rooms() {
            long now = System.currentTimeMillis(); List<Room> out = new ArrayList<>();
            for (Map.Entry<String, Room> e : rooms.entrySet()) if (now - e.getValue().seen > 3000) rooms.remove(e.getKey()); else out.add(e.getValue());
            out.sort(Comparator.comparing(r -> r.name)); return out;
        }
        @Override public void close() { on = false; s.close(); }
    }
}
