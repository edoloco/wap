package com.wap.render;
/** Dibujo independiente de la plataforma: Canvas en Android, Java2D en las pruebas de escritorio. */
public interface Gfx {
    int w(); int h();
    void save(); void restore(); void translate(float x, float y); void rotate(float rad); void scale(float x, float y);
    void clear(int c);
    void poly(float[] p, int fill, int stroke, float sw);
    void circle(float x, float y, float r, int fill, int stroke, float sw);
    void line(float x0, float y0, float x1, float y1, int c, float w);
    void rect(float x, float y, float w, float h, int c);
    void rrect(float x, float y, float w, float h, float r, int fill, int stroke, float sw);
    void text(String s, float x, float y, float size, int c, int align);
    float textW(String s, float size);
    void tile(int id, int[] px, int size, boolean changed, float x, float y);
}
