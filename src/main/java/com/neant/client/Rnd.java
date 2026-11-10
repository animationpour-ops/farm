package com.neant.client;

/** Hasard deterministe : le meme (seed, index) donne toujours la meme valeur, donc aucune particule a stocker. */
public final class Rnd {

    public static int hash(int seed, int i) {
        int h = seed * 0x9E3779B1 + i * 0x85EBCA6B;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        h *= 0x297A2D39;
        h ^= h >>> 15;
        return h;
    }

    /** Valeur dans [0, 1). */
    public static float f(int seed, int i) {
        return (hash(seed, i) >>> 8) / (float) (1 << 24);
    }

    /** Valeur dans [-1, 1). */
    public static float s(int seed, int i) {
        return f(seed, i) * 2.0F - 1.0F;
    }

    public static float range(int seed, int i, float min, float max) {
        return min + f(seed, i) * (max - min);
    }

    public static float clamp01(float v) {
        return v < 0.0F ? 0.0F : Math.min(v, 1.0F);
    }

    public static float smooth(float v) {
        v = clamp01(v);
        return v * v * (3.0F - 2.0F * v);
    }

    /** Rampe : 0 avant a, 1 apres b. */
    public static float ramp(float t, float a, float b) {
        return clamp01((t - a) / (b - a));
    }

    private Rnd() {}
}
