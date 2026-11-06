package com.wap.core;
/** Generador determinista: misma semilla, misma partida en cualquier dispositivo. */
public final class Rng {
    public int s;
    public Rng(int seed) { s = seed == 0 ? 0x9E3779B9 : seed; }
    public int next() { int x = s; x ^= x << 13; x ^= x >>> 17; x ^= x << 5; s = x; return x; }
    public int i(int n) { return (next() >>> 1) % n; }
    public float f() { return (next() >>> 8) / 16777216f; }
    public float r(float a, float b) { return a + (b - a) * f(); }
}
