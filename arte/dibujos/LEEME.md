# Dibujos del personaje

`de_pie_1.svg` es el dibujo del usuario sobre la plantilla (`arte/plantilla/de_pie_1.svg`). Cada capa es una pieza
del cuerpo; el juego toma sus formas y las mueve con el esqueleto en todas las poses.

- El marrón `#5F574E` es el color del traje: el juego lo cambia por el color de cada jugador.
- Una capa vacía no se dibuja. Una capa que falta en el SVG usa el dibujo original.
- Si una pieza de un lado quedó en la capa del otro lado (y esa capa está vacía), el importador la pasa sola.

Para actualizar el juego tras cambiar el dibujo: `tools/arte.sh` (genera `app/src/main/resources/com/wap/render/arte.txt`).
