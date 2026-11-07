package com.wap.render;
import java.util.HashMap; import java.util.Map; import java.util.TreeSet;
/** Archivos de la app (niveles guardados). En Android van a la carpeta privada de la app. */
public interface Files {
    String[] list(); byte[] read(String name); void write(String name, byte[] data); void delete(String name);
    /** En memoria: para las pruebas de escritorio. */
    final class Memory implements Files {
        final Map<String, byte[]> m = new HashMap<>();
        public String[] list() { return new TreeSet<>(m.keySet()).toArray(new String[0]); }
        public byte[] read(String n) { return m.get(n); }
        public void write(String n, byte[] d) { m.put(n, d); }
        public void delete(String n) { m.remove(n); }
    }
}
