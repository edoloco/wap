package com.wap.render;
/** Dibujo independiente de la plataforma: Canvas en Android, Java2D en las pruebas de escritorio. */
public interface Gfx {
    int w(); int h();
    void save(); void restore(); void translate(float x, float y); void rotate(float rad); void scale(float x, float y);
    void clear(int c);
    /** Capas con nombre: solo las usa el exportador de plantillas SVG; en el juego no hacen nada. */
    default void layer(String name) {}
    default void endLayer() {}
    /** Luz: círculo con degradado radial que se desvanece hacia el borde. */
    void glow(float x, float y, float r, int argb);
    /** Mezcla aditiva para luces (los colores se suman al fondo). */
    void additive(boolean on);
    /** Desde aquí se dibuja en una capa de baja resolución, sin suavizado: pixel art. */
    void beginPixels(int w, int h);
    /** Termina la capa y la amplía por un factor entero, sin filtrar. */
    void endPixels(float x, float y, int scale);
    void poly(float[] p, int fill, int stroke, float sw);
    /** Polígono con degradado horizontal entre x0 y x1. */
    void grad(float[] p, int c0, int c1, float x0, float x1, int stroke, float sw);
    void circle(float x, float y, float r, int fill, int stroke, float sw);
    void line(float x0, float y0, float x1, float y1, int c, float w);
    void rect(float x, float y, float w, float h, int c);
    void rrect(float x, float y, float w, float h, float r, int fill, int stroke, float sw);
    void text(String s, float x, float y, float size, int c, int align);
    float textW(String s, float size);
    void tile(int id, int[] px, int size, boolean changed, float x, float y);
}
