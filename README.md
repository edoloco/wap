# WAP

Juego de acción 2D en pixel art para Android: escenario plataformero destructible, armas de Liero y movimiento de Mega Man.

## Instalar
Descarga `wap.apk` desde la sección **Releases** del repositorio, en el teléfono Android, y permite la instalación de fuentes externas. Cada push a `main` compila una versión nueva, que se puede instalar desde el menú del juego.

## Controles
| Entrada | Acción |
|---|---|
| ◀ ▶ | Caminar |
| A | Disparar (arma 4) |
| B | Saltar |
| ▲ + A | Arma 3 |
| ▼ + A | Arma 1 |
| ◀▶ + A | Arma 2 |
| ▲ + B | Gancho: mantén B y te lleva solo; suelta B para soltarte |
| ▼ + B | Deslizarse |

En el aire, empujar contra una pared te afirma; B rebota hacia el lado contrario.

La Plasma Cannon se activa una vez para cargar y, cuando el robot brilla, otra vez para lanzarla.

Dirección y A casi a la vez activan el arma lateral; si ya vienes corriendo, A dispara. Lado + B siempre salta.
Las cajas, computadoras y bloques de piedra tienen resistencia propia y se rompen enteros en pedazos.

Todas las armas salen rectas, desde la mano del robot, tras una preparación corta. El robot no escarba: el escenario se destruye con las armas.

## Estructura
- `core`: simulación determinista, sin dependencias de Android. La entrada de cada tick es una máscara de bits, base del futuro modo online.
- `render`: dibujo, pantallas y mando en pantalla, sobre la interfaz `Gfx`.
- `android`: actividad, Canvas, mando Bluetooth y sonido.
- `tools`: pruebas de escritorio y capturas sin dispositivo (`tools/run.sh`).

La clave en `keystore/` es solo de desarrollo y pública; para Play Store hay que usar otra.
