# Taller de piezas

Editor (artefacto web) para reemplazar las piezas del personaje con dibujos vectoriales.

- Lee PDF, EPS, AI (Illustrator 7 o anterior; los AI nuevos se leen como PDF) y SVG.
- Exporta `app/src/main/assets/personaje.txt`, que el juego dibuja como vectores (clase `render/Pieces.java`).
- Si `personaje.txt` no está, el personaje es el de siempre. El juego ignora un archivo de más de 6000 puntos.

Para regenerar los datos de base (piezas y poses actuales) después de cambiar `RobotArt.java`:

    javac -d /tmp/b app/src/main/java/com/wap/*/*.java tools/BasePiezas.java && java -cp /tmp/b BasePiezas > base.json

y reemplazar `__BASE__` en `taller_src.html` con ese JSON.
