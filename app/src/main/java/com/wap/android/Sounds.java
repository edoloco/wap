package com.wap.android;
import android.content.Context; import android.media.AudioAttributes; import android.media.SoundPool;
import com.wap.render.Audio;
import java.io.ByteArrayOutputStream; import java.io.File; import java.io.FileOutputStream; import java.util.Random;
/** Sonidos sintetizados al iniciar: no hay archivos de audio en la APK. */
final class Sounds implements Audio {
    private final SoundPool pool; private final int[] ids = new int[10]; private final long[] lastT = new long[10]; private static final int RATE = 22050;
    Sounds(Context ctx) {
        pool = new SoundPool.Builder().setMaxStreams(8).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        float[][] d = {{.09f,.7f,520,140},{.38f,1,120,28},{.9f,1,90,16},{.07f,.25f,300,200},{.12f,.15f,1500,700},{.1f,0,300,620},{.08f,.1f,900,300},{.18f,0,620,1240},{.5f,.8f,180,40},{.04f,0,700,700}};
        for (int i = 0; i < d.length; i++) {
            try { File f = new File(ctx.getCacheDir(), "s" + i + ".wav"); try (FileOutputStream o = new FileOutputStream(f)) { o.write(wav(d[i][0], d[i][1], d[i][2], d[i][3], i)); } ids[i] = pool.load(f.getPath(), 1); }
            catch (Exception e) { ids[i] = 0; }
        }
    }
    private static byte[] wav(float dur, float noise, float f0, float f1, int seed) throws Exception {
        int n = (int) (dur * RATE); Random r = new Random(seed); ByteArrayOutputStream b = new ByteArrayOutputStream(44 + n * 2);
        le(b, 0x46464952, 4); le(b, 36 + n * 2, 4); le(b, 0x45564157, 4); le(b, 0x20746D66, 4); le(b, 16, 4); le(b, 1, 2); le(b, 1, 2); le(b, RATE, 4); le(b, RATE * 2, 4); le(b, 2, 2); le(b, 16, 2); le(b, 0x61746164, 4); le(b, n * 2, 4);
        double ph = 0; float hold = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) n, f = f0 + (f1 - f0) * t, env = (1 - t) * (1 - t); ph += 2 * Math.PI * f / RATE;
            if (i % Math.max(1, (int) (RATE / (f * 2 + 1))) == 0) hold = r.nextFloat() * 2 - 1;
            float tone = (float) Math.signum(Math.sin(ph)) * .6f, v = (tone * (1 - noise) + hold * noise) * env * .8f;
            le(b, (int) (v * 32000) & 0xFFFF, 2);
        }
        return b.toByteArray();
    }
    private static void le(ByteArrayOutputStream b, int v, int n) { for (int i = 0; i < n; i++) b.write(v >> (8 * i) & 255); }
    @Override public void play(int id) {
        long t = System.nanoTime(); if (t - lastT[id] < 45_000_000L || ids[id] == 0) return; lastT[id] = t;
        pool.play(ids[id], .7f, .7f, 1, 0, 1);
    }
    void release() { pool.release(); }
}
