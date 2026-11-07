package com.wap.android;
import com.wap.render.Files;
import java.io.*; import java.util.Arrays;
/** Niveles guardados en la carpeta privada de la app. */
final class DiskFiles implements Files {
    private final File dir;
    DiskFiles(File dir) { this.dir = dir; dir.mkdirs(); }
    @Override public String[] list() { String[] n = dir.list(); if (n == null) return new String[0]; Arrays.sort(n); return n; }
    @Override public byte[] read(String name) {
        File f = new File(dir, name); if (!f.isFile()) return null;
        try (InputStream in = new FileInputStream(f)) { ByteArrayOutputStream b = new ByteArrayOutputStream(); byte[] k = new byte[16384]; int n; while ((n = in.read(k)) > 0) b.write(k, 0, n); return b.toByteArray(); }
        catch (IOException e) { return null; }
    }
    @Override public void write(String name, byte[] data) {
        File tmp = new File(dir, name + ".tmp"); // se escribe aparte y se renombra: un corte no deja el nivel a medias
        try (OutputStream o = new FileOutputStream(tmp)) { o.write(data); } catch (IOException e) { return; }
        tmp.renameTo(new File(dir, name));
    }
    @Override public void delete(String name) { new File(dir, name).delete(); }
}
