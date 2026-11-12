package com.wap.android;
import android.app.Activity; import android.content.SharedPreferences; import android.hardware.input.InputManager; import android.os.Build; import android.os.Bundle; import android.view.InputDevice; import android.view.KeyEvent; import android.view.MotionEvent; import android.view.View; import android.view.WindowManager;
import com.wap.core.Input; import com.wap.render.App; import com.wap.render.Store;
/** Pantalla completa horizontal, mando Bluetooth, teclado y ciclo de vida. */
public class MainActivity extends Activity implements InputManager.InputDeviceListener {
    private App app; private Sounds snd; private GameView view; private int keyMask, axisMask; private InputManager im;
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 28) getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        final SharedPreferences p = getSharedPreferences("wap", MODE_PRIVATE);
        snd = new Sounds(this);
        app = new App(new Store() {
            @Override public String get(String k, String d) { return p.getString(k, d); }
            @Override public void put(String k, String v) { p.edit().putString(k, v).apply(); }
        }, snd);
        app.seed((int) System.nanoTime());
        final android.net.wifi.WifiManager wifi = (android.net.wifi.WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        final android.net.wifi.WifiManager.MulticastLock lock = wifi != null ? wifi.createMulticastLock("wap-salas") : null;
        if (lock != null) lock.setReferenceCounted(false);
        final android.net.wifi.WifiManager.WifiLock fast = wifi != null ? wifi.createWifiLock(Build.VERSION.SDK_INT >= 29 ? android.net.wifi.WifiManager.WIFI_MODE_FULL_LOW_LATENCY : android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF, "wap-partida") : null;
        if (fast != null) fast.setReferenceCounted(false);
        app.dev = new App.Device() {
            @Override public String name() { return "Sala de " + Build.MODEL; }
            @Override public void listen(boolean on) { if (lock == null) return; try { if (on) lock.acquire(); else if (lock.isHeld()) lock.release(); } catch (RuntimeException e) { } }
            @Override public void exit() { runOnUiThread(() -> finishAndRemoveTask()); }
            @Override public void lowLatency(boolean on) { if (fast == null) return; try { if (on) fast.acquire(); else if (fast.isHeld()) fast.release(); } catch (RuntimeException e) { } }
            @Override public void askText(String title, String current, int max, java.util.function.Consumer<String> done) {
                runOnUiThread(() -> {
                    final android.widget.EditText ed = new android.widget.EditText(MainActivity.this);
                    ed.setSingleLine(true); ed.setText(current); ed.setSelection(ed.getText().length());
                    ed.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(max), new android.text.InputFilter.AllCaps()});
                    android.app.AlertDialog d = new android.app.AlertDialog.Builder(MainActivity.this).setTitle(title).setView(ed)
                        .setPositiveButton("Listo", (x, y) -> { synchronized (app) { done.accept(ed.getText().toString()); } })
                        .setNegativeButton("Cancelar", null).create();
                    d.setOnDismissListener(x -> { immersive(); view.requestFocus(); });
                    d.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                    d.show(); ed.requestFocus();
                });
            }
        };
        // armas cambiadas en el Taller de armas (assets/armas.txt); si no está, las de siempre
        try (java.io.InputStream in = getAssets().open("armas.txt")) {
            com.wap.core.Wpn.load(new String(readAll(in), java.nio.charset.StandardCharsets.UTF_8));
            if (com.wap.core.Wpn.error != null) android.util.Log.w("WAP", "armas.txt: " + com.wap.core.Wpn.error);
        } catch (java.io.IOException e) { /* sin archivo: armas de siempre */ }
        // piezas del personaje hechas en el editor de piezas (assets/personaje.txt); si no está, el personaje de siempre
        try (java.io.InputStream in = getAssets().open("personaje.txt")) {
            com.wap.render.Pieces.load(new String(readAll(in), java.nio.charset.StandardCharsets.UTF_8));
            if (com.wap.render.Pieces.error != null) android.util.Log.w("WAP", "personaje.txt: " + com.wap.render.Pieces.error);
        } catch (java.io.IOException e) { /* sin archivo: personaje de siempre */ }
        app.files = new DiskFiles(new java.io.File(getFilesDir(), "niveles"));
        app.up = new AppUpdater(this); ((AppUpdater) app.up).check();
        view = new GameView(this, app); setContentView(view); view.requestFocus();
        im = (InputManager) getSystemService(INPUT_SERVICE); im.registerInputDeviceListener(this, null); checkPads();
    }
    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
    @Override public void onWindowFocusChanged(boolean f) { super.onWindowFocusChanged(f); if (f) immersive(); else clearInput(); }
    @Override protected void onResume() { super.onResume(); immersive(); checkPads(); }
    @Override protected void onPause() { super.onPause(); synchronized (app) { app.pauseNow(); } clearInput(); }
    @Override protected void onDestroy() { synchronized (app) { app.quit(); } super.onDestroy(); im.unregisterInputDeviceListener(this); snd.release(); }
    private void clearInput() { keyMask = axisMask = 0; push(); synchronized (app) { app.pad.clear(); } }
    private void push() { app.keys = keyMask | axisMask; }
    /** El mando en pantalla se oculta si hay un mando físico y vuelve al desconectarlo. */
    private void checkPads() {
        boolean found = false;
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice d = InputDevice.getDevice(id); if (d == null || d.isVirtual()) continue;
            int s = d.getSources();
            if ((s & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD || (s & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) found = true;
        }
        synchronized (app) { app.padSeen = found; app.pad.visible = !found; }
    }
    @Override public void onInputDeviceAdded(int id) { checkPads(); }
    @Override public void onInputDeviceRemoved(int id) { clearInput(); checkPads(); }
    @Override public void onInputDeviceChanged(int id) { checkPads(); }
    private static int bit(int code) {
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_LEFT: case KeyEvent.KEYCODE_A: return Input.LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT: case KeyEvent.KEYCODE_D: return Input.RIGHT;
            case KeyEvent.KEYCODE_DPAD_UP: case KeyEvent.KEYCODE_W: return Input.UP;
            case KeyEvent.KEYCODE_DPAD_DOWN: case KeyEvent.KEYCODE_S: return Input.DOWN;
            case KeyEvent.KEYCODE_BUTTON_A: case KeyEvent.KEYCODE_BUTTON_R1: case KeyEvent.KEYCODE_BUTTON_R2: case KeyEvent.KEYCODE_J: case KeyEvent.KEYCODE_Z: case KeyEvent.KEYCODE_DPAD_CENTER: return Input.A;
            case KeyEvent.KEYCODE_BUTTON_B: case KeyEvent.KEYCODE_K: case KeyEvent.KEYCODE_X: case KeyEvent.KEYCODE_SPACE: return Input.B;
            case KeyEvent.KEYCODE_BUTTON_X: case KeyEvent.KEYCODE_BUTTON_Y: case KeyEvent.KEYCODE_BUTTON_L1: case KeyEvent.KEYCODE_BUTTON_L2: case KeyEvent.KEYCODE_L: case KeyEvent.KEYCODE_C: case KeyEvent.KEYCODE_SHIFT_LEFT: return Input.X;
            default: return 0;
        }
    }
    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        int c = e.getKeyCode(); boolean down = e.getAction() == KeyEvent.ACTION_DOWN;
        if (c == KeyEvent.KEYCODE_BUTTON_START || c == KeyEvent.KEYCODE_ENTER) {
            if (down && e.getRepeatCount() == 0) synchronized (app) { app.startKey = true; }
            return true;
        }
        if (c == KeyEvent.KEYCODE_BACK || c == KeyEvent.KEYCODE_ESCAPE || c == KeyEvent.KEYCODE_BUTTON_SELECT) {
            if (down && e.getRepeatCount() == 0) synchronized (app) { if (app.screen == App.TITLE) finish(); else app.back(); }
            return true;
        }
        int m = bit(c);
        if (m == 0) return super.dispatchKeyEvent(e);
        int src = e.getSource();
        if (((src & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD || (src & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) && app.pad.visible) synchronized (app) { app.pad.visible = false; app.padSeen = true; }
        if (down) keyMask |= m; else keyMask &= ~m;
        push(); return true;
    }
    @Override public boolean dispatchGenericMotionEvent(MotionEvent e) {
        if ((e.getSource() & InputDevice.SOURCE_JOYSTICK) != InputDevice.SOURCE_JOYSTICK || e.getAction() != MotionEvent.ACTION_MOVE) return super.dispatchGenericMotionEvent(e);
        float hx = e.getAxisValue(MotionEvent.AXIS_HAT_X), hy = e.getAxisValue(MotionEvent.AXIS_HAT_Y), x = e.getAxisValue(MotionEvent.AXIS_X), y = e.getAxisValue(MotionEvent.AXIS_Y);
        int m = 0; final float T = .5f;
        if (hx < -T || x < -T) m |= Input.LEFT; if (hx > T || x > T) m |= Input.RIGHT;
        if (hy < -T || y < -T) m |= Input.UP; if (hy > T || y > T) m |= Input.DOWN;
        if (m != 0 && app.pad.visible) synchronized (app) { app.pad.visible = false; app.padSeen = true; }
        axisMask = m; push(); return true;
    }
    static byte[] readAll(java.io.InputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream(); byte[] b = new byte[8192]; int n;
        while ((n = in.read(b)) > 0) o.write(b, 0, n); return o.toByteArray();
    }
}
