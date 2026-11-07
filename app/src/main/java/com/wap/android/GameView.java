package com.wap.android;
import android.content.Context; import android.graphics.Canvas; import android.os.Build; import android.view.MotionEvent; import android.view.SurfaceHolder; import android.view.SurfaceView;
import com.wap.render.App;
/** Bucle principal: simula a 60 ticks fijos por segundo y dibuja cada cuadro. */
final class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    final App app; private final AndroidGfx gfx = new AndroidGfx(); private float drawMs; private Thread th; private volatile boolean run;
    GameView(Context c, App a) { super(c); app = a; getHolder().addCallback(this); setFocusable(true); setFocusableInTouchMode(true); }
    @Override public void surfaceCreated(SurfaceHolder h) { run = true; th = new Thread(this, "game"); th.start(); }
    @Override public void surfaceChanged(SurfaceHolder h, int f, int w, int hh) { }
    @Override public void surfaceDestroyed(SurfaceHolder h) { run = false; app.pauseNow(); try { if (th != null) th.join(800); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    @Override public void run() {
        final long step = 16_666_667L; long last = System.nanoTime(), acc = 0;
        SurfaceHolder h = getHolder();
        while (run) {
            long now = System.nanoTime(); acc += Math.min(now - last, 100_000_000L); last = now;
            Canvas c = null;
            try {
                c = Build.VERSION.SDK_INT >= 26 ? h.lockHardwareCanvas() : h.lockCanvas();
                if (c == null) continue;
                synchronized (app) {
                    int n = 0;
                    while (acc >= step && n++ < 4) { app.tick(); acc -= step; }
                    long d0 = System.nanoTime(); gfx.begin(c, c.getWidth(), c.getHeight()); app.draw(gfx);
                    drawMs += ((System.nanoTime() - d0) / 1e6f - drawMs) * .05f; // promedio del tiempo de dibujo
                    if (!app.ren.lite && drawMs > 14) app.ren.lite = true; else if (app.ren.lite && drawMs < 6) app.ren.lite = false;
                }
            } finally { if (c != null) try { h.unlockCanvasAndPost(c); } catch (IllegalStateException e) { } }
        }
    }
    @Override public boolean onTouchEvent(MotionEvent e) {
        synchronized (app) {
            app.pad.visible = true;
            int a = e.getActionMasked(), ix = e.getActionIndex(); boolean play = app.screen == App.PLAY;
            if (a == MotionEvent.ACTION_DOWN) app.down(e.getX(0), e.getY(0));
            if (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_MOVE) app.drag(e.getX(0), e.getY(0));
            if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) app.up();
            if (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_POINTER_DOWN) { if (!app.tap(e.getX(ix), e.getY(ix)) && play) app.pointer(e.getPointerId(ix), e.getX(ix), e.getY(ix), true); else if (play) performClick(); }
            else if (a == MotionEvent.ACTION_MOVE && play) { for (int i = 0; i < e.getPointerCount(); i++) app.pointer(e.getPointerId(i), e.getX(i), e.getY(i), true); }
            else if (a == MotionEvent.ACTION_POINTER_UP) app.pointer(e.getPointerId(ix), 0, 0, false);
            else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) app.pad.clear();
        }
        return true;
    }
    @Override public boolean performClick() { return super.performClick(); }
}
