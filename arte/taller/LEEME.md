# Taller de piezas

Editor (artefacto web) para reemplazar las piezas del personaje con dibujos vectoriales.

- Lee PDF, EPS, AI (Illustrator 7 o anterior; los AI nuevos se leen como PDF) y SVG.
- Exporta `app/src/main/assets/personaje.txt`, que el juego dibuja como vectores (clase `render/Pieces.java`).
- Si `personaje.txt` no está, el personaje es el de siempre. El juego ignora un archivo de más de 6000 puntos.

Para regenerar los datos de base (piezas y poses actuales) después de cambiar `RobotArt.java`:

    javac -d /tmp/b app/src/main/java/com/wap/*/*.java tools/BasePiezas.java && java -cp /tmp/b BasePiezas > base.json

y reemplazar `__BASE__` en `taller_src.html` con ese JSON.

# Taller de armas

Editor (artefacto web) para cambiar valores, nombre, descripción y aspecto de las armas, crearlas o quitarlas del juego.

Aspecto: `arma NOMBRE aspecto=COHETE color=RRGGBB tamano=1.5 luz=0` y bloques `dibujo NOMBRE` / `forma relleno=AARRGGBB contorno=AARRGGBB grosor=N parimpar=0` / `p x y ...` / `fin` (unidades del juego, apuntando a la derecha, máx. 600 puntos por arma). El dibujo se importa desde PDF, EPS, AI o SVG.

- Exporta `app/src/main/assets/armas.txt`, que el juego lee al iniciar (`Wpn.load`). Sin archivo, las armas de siempre.
- Formato: `nueva NOMBRE BASE` (crea un arma copiando otra), `arma NOMBRE clave=valor ...`, `nombre NOMBRE texto`, `descripcion NOMBRE texto`, `quitar NOMBRE`. En los nombres, los espacios van como `_`.
- Un archivo con errores se ignora entero. Los valores se limitan a rangos seguros (`Wpn.LIMITS`).
- En línea: con armas distintas los teléfonos no se conectan (`World.rules()` incluye la huella de armas.txt).

Para regenerar los datos de base: `java -Dstdout.encoding=UTF-8 BaseArmas > armas_base.json` (en UTF-8, por los acentos) (tools/BaseArmas.java) y reemplazar `__ARMAS__` en `armas_src.html`; las imágenes de cada aspecto salen de `java LookSprites > looks.json` y reemplazan `__LOOKS__`.
