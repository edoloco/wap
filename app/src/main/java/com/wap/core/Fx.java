package com.wap.core;
/** Eventos visuales y de sonido. La simulación los emite, pero nunca depende de ellos. */
public final class Fx {
    public static final int EXPLODE = 0, CHIPS = 1, SMOKE = 2, FLASH = 3, HIT = 4, ARMOR = 5, DEATH = 6, BEAM = 7, JUMP = 8, PICK = 9, HOOK = 10, SPAWN = 11, SLIDE = 12, EMPTY = 13, CHARGE = 14, WALL = 15, PROP = 16;
    public int type, who; public float x, y, a, b, c, d;
}
