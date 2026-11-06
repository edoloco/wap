package com.wap.core;
/** Entrada de un jugador en un tick: una máscara de bits. Es lo único que viajará por red en el modo online. */
public final class Input {
    public static final int LEFT = 1, RIGHT = 2, UP = 4, DOWN = 8, A = 16, B = 32, X = 64;
    private Input() {}
}
