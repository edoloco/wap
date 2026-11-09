package com.wap.android;
import android.content.Context; import android.graphics.Canvas;
import com.airbnb.lottie.LottieComposition; import com.airbnb.lottie.LottieCompositionFactory; import com.airbnb.lottie.LottieDrawable; import com.airbnb.lottie.LottieProperty; import com.airbnb.lottie.LottieResult;
import com.airbnb.lottie.model.KeyPath; import com.airbnb.lottie.utils.MiscUtils; import com.airbnb.lottie.value.LottieFrameInfo; import com.airbnb.lottie.value.LottieValueCallback;
import com.wap.core.Robot; import com.wap.render.Gfx; import com.wap.render.RobotArt;
import java.util.HashMap;
/**
 * Animaciones del personaje hechas a mano en Glaxnimate y guardadas como Lottie en assets/animaciones/NOMBRE.json.
 * Lienzo de 640 x 720 px, 10 px por unidad del juego, pies en (320, 640) mirando a la derecha.
 * Los grupos llamados "traje" toman el color del jugador; las capas con nombre de pieza de armadura se esconden
 * cuando esa pieza se cae; "punto_disparo" y "guia" nunca se ven en el juego.
 */
final class LottieSkin implements RobotArt.Skin {
    static final int W = 640, H = 720, OX = 320, OY = 640; static final float PX = 10;
    static final String[] PARTS = {"cabeza", "craneo", "pecho", "abdomen", "pelvis", "hombro_lejano", "hombro_cercano", "guantelete_lejano", "guantelete_cercano",
        "muslo_lejano", "muslo_cercano", "canilla_lejana", "canilla_cercana", "punto_disparo", "guia"};
    /** Pieza de armadura de cada capa; -2: el cráneo, que se ve solo sin casco; -1: nunca se ve. */
    static final int[] SLOT = {Robot.HELMET, -2, Robot.CHEST, Robot.BELLY, Robot.HIP, Robot.SH_F, Robot.SH_B, Robot.BR_F, Robot.BR_B,
        Robot.TH_F, Robot.TH_B, Robot.SN_F, Robot.SN_B, -1, -1};
    private final HashMap<String, LottieDrawable> anims = new HashMap<>(); private int color, armor;

    static LottieSkin load(Context ctx) {
        LottieSkin s = new LottieSkin(); String[] files;
        try { files = ctx.getAssets().list("animaciones"); } catch (Exception e) { return null; }
        if (files == null) return null;
        for (String f : files) {
            if (!f.endsWith(".json")) continue;
            try {
                LottieResult<LottieComposition> r = LottieCompositionFactory.fromAssetSync(ctx, "animaciones/" + f);
                if (r.getValue() == null) continue;
                LottieDrawable d = new LottieDrawable(); d.setComposition(r.getValue()); d.setBounds(0, 0, W, H);
                d.addValueCallback(new KeyPath("**", "traje", "**"), LottieProperty.COLOR, new LottieValueCallback<Integer>() {
                    @Override public Integer getValue(LottieFrameInfo<Integer> i) { return s.color; } });
                for (int k = 0; k < PARTS.length; k++) { final int slot = SLOT[k];
                    d.addValueCallback(new KeyPath(PARTS[k]), LottieProperty.TRANSFORM_OPACITY, new LottieValueCallback<Integer>() {
                        @Override public Integer getValue(LottieFrameInfo<Integer> i) {
                            boolean on = slot == -2 ? (s.armor >> Robot.HELMET & 1) == 0 : slot >= 0 && (s.armor >> slot & 1) != 0;
                            if (!on) return 0;
                            Integer a = i.getStartValue(), b = i.getEndValue(); if (a == null) return 100; if (b == null) return a;
                            return MiscUtils.lerp(a, b, i.getInterpolatedKeyframeProgress());
                        } });
                }
                s.anims.put(f.substring(0, f.length() - 5), d);
            } catch (RuntimeException e) { android.util.Log.w("WAP", "animación " + f + " no se pudo leer", e); }
        }
        return s.anims.isEmpty() ? null : s;
    }

    @Override public boolean draw(Gfx g, RobotArt.View v, int ar, int c) {
        LottieDrawable d = anims.get(v.anim); if (d == null || !(g instanceof AndroidGfx)) return false;
        LottieComposition comp = d.getComposition(); float start = comp.getStartFrame(), n = Math.max(1, comp.getEndFrame() - start);
        float f = v.anim.startsWith("correr") ? RobotArt.runProgress(v) * n : RobotArt.loops(v.anim) ? v.animT % n : Math.min(v.animT, n - 1);
        color = c; armor = ar; d.setFrame((int) (start + Math.min(f, n - 1)));
        Canvas cv = ((AndroidGfx) g).c; cv.save(); cv.scale(1 / PX, 1 / PX); cv.translate(-OX, -OY); d.draw(cv); cv.restore();
        return true;
    }
}
