package com.wap.core;
/** Objeto del escenario con material y resistencia: recibe daño entero y al final se rompe en pedazos. */
public final class Prop {
    public int x, y, w, h, kind, hitT; public float hp, max, fx, fy, vx, vy; public boolean gone, moving;
    Prop(int[] q) {
        x = q[0]; y = q[1]; w = q[2]; h = q[3]; kind = q[4];
        fx = x; fy = y;
        max = hp = kind == Terrain.METAL ? 90 : kind == Terrain.STONE ? 140 : kind == Terrain.COMPUTER ? 50 : 30;
    }
    /** Masa según el material: la madera vuela lejos y la piedra apenas se mueve. */
    float mass() { return kind == Terrain.STONE ? 4 : kind == Terrain.METAL ? 2.5f : kind == Terrain.COMPUTER ? 1.8f : 1; }
    boolean has(float px, float py) { return px >= x - 2 && py >= y - 2 && px <= x + w + 2 && py <= y + h + 2; }
}
