# Plantilla del personaje de WAP

Cada archivo `.svg` es un cuadro de animación, generado desde el robot que usa hoy el juego.
Cada pieza está en su propia capa con nombre. Dibuja encima y conserva los nombres.
Luego el juego leerá tus archivos en lugar de su dibujo.

## Cómo usarla
1. Abre el SVG en tu programa. Inkscape lo abre con capas; Illustrator, Affinity y Animate lo importan como grupos.
2. Dibuja en cada capa o reemplaza su contenido. Puedes borrar mis formas.
3. Guarda como SVG con el **mismo nombre de archivo**.

## Reglas del lienzo
- No cambies el tamaño del lienzo: 640 × 720 px (unidades del juego: `viewBox -32 -64 64 72`).
- El personaje mira **a la derecha**.
- Los **pies van sobre la línea verde** y centrados en la línea punteada vertical.
- La caja rosa es el espacio que ocupa el robot en el juego. El dibujo puede sobresalir un poco.
- La capa `guia` no se usa en el juego.
- Todo en vector: evita imágenes incrustadas y convierte los textos a trazos.

## Capas
El orden de las capas es el orden de dibujo: la de abajo queda atrás. Puedes reordenarlas.
"Lejano" es el lado del cuerpo más lejos de la cámara y "cercano" el más próximo.

**Armadura** (se cae pieza por pieza al recibir daño):
`cabeza`, `pecho`, `abdomen`, `pelvis`, `hombro_lejano`, `hombro_cercano`,
`guantelete_lejano`, `guantelete_cercano`, `muslo_lejano`, `muslo_cercano`,
`canilla_lejana`, `canilla_cercana`.

**Esqueleto** (siempre visible):
capas que empiezan con `hueso_`, `mano_`, `bota_` y `rodilla_`.

**Especiales**:
- `craneo`: se ve solo cuando el robot perdió la cabeza. Queda debajo de `cabeza`.
- `punto_disparo` (en `disparo` y `salto_disparo`): el punto celeste marca de dónde sale el disparo. Muévelo a la mano.

Si una capa aparece repetida, lleva `-2`, `-3`, etc. Cuenta igual que la original.
Puedes agregar capas nuevas usando esos mismos nombres.

## Color del traje
El café `#5E5650` se cambia por el color que elige cada jugador.
Para sombras y luces del traje usa `#45403B` y `#7A716A`, que también cambian.
Cualquier otro color queda tal cual.

## Cuadros
| Archivo | Uso | Velocidad |
|---|---|---|
| `de_pie_1`, `de_pie_2` | Descanso, alternando | lenta |
| `correr_1` a `correr_4` | Carrera | 8 cuadros/s (3 cuadros de un video a 24) |
| `preparar_1`, `preparar_2` | Antes del primer disparo | 0,1 s en total |
| `disparo` | Disparo en tierra | mientras dispara |
| `salto_subir`, `salto_caer` | Salto | |
| `salto_disparo` | Disparo en el aire | |
| `doble_salto` | Bola del doble salto (el juego la hace girar) | |
| `estirar` | Al salir de la bola | |
| `barrida_1`, `barrida_2` | Barrida | |
| `pared` | Pegado a la pared | |
| `gancho` | Colgado del gancho (el brazo cercano apunta al gancho) | |
| `lanzado` | Lanzado por Drop (el juego lo hace girar) | |

Si quieres más cuadros, numera seguido: `correr_5`, `correr_6`…
Los destellos, las luces y el polvo los pone el juego; no hace falta dibujarlos.
