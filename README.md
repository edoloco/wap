# WAP

Juego de acción 2D para Android: terreno destructible y armas de Liero, movimiento de Mega Man.

## Instalar
Descarga `wap.apk` desde la sección **Releases** del repositorio, en el teléfono Android, y permite la instalación de fuentes externas. Cada push a `main` compila una versión nueva.

## Controles
| Entrada | Acción |
|---|---|
| ◀ ▶ | Caminar |
| A | Disparar (arma 5) |
| B | Saltar |
| ▲ + A | Gancho |
| ▼ + A | Arma 1 |
| ◀▶ + A | Arma 2 |
| ▲ + B | Arma 3 |
| ◀▶ + B | Arma 4 |
| ▼ + B | Deslizarse |

Dirección y botón casi a la vez activan el arma. Si ya vienes corriendo, A dispara y B salta.
Empujar contra la tierra, o mantener ▲ o ▼, cava.

## Estructura
- `core`: simulación determinista, sin dependencias de Android. La entrada de cada tick es una máscara de bits, base del futuro modo online.
- `render`: dibujo, pantallas y mando en pantalla, sobre la interfaz `Gfx`.
- `android`: actividad, Canvas, mando Bluetooth y sonido.
- `tools`: pruebas de escritorio y capturas sin dispositivo (`tools/run.sh`).

La clave en `keystore/` es solo de desarrollo y pública; para Play Store hay que usar otra.
