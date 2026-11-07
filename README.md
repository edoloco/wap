# WAP

Juego de acción 2D en pixel art para Android: escenario plataformero destructible, armas de Liero y movimiento de Mega Man.

## Instalar
Descarga `wap.apk` desde la sección **Releases** del repositorio, en el teléfono Android, y permite la instalación de fuentes externas. Cada push a `main` compila una versión nueva, que se puede instalar desde el menú del juego.

## Controles
| Entrada | Acción |
|---|---|
| ◀ ▶ | Caminar |
| A | Disparar (arma 4) |
| B | Saltar; en el aire, otra vez para el doble salto |
| ◀▶ con X mantenido | Correr |
| X mantenido + ▲▼ | Apuntar el gancho |
| ▲ + A | Arma 3 |
| ▼ + A | Arma 1 |
| ◀▶ + A | Arma 2 |
| ▲ + B | Gancho en la dirección de la mira: mantén B y te lleva solo; suelta B para soltarte |
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

## Reglas entre personajes
Los personajes no se atraviesan: uno puede pararse sobre el otro. Caer encima es un pisotón: quita poca vida y aturde.
Una barrida lanza al rival. Si te golpean mientras te deslizas, sales lanzado y pierdes la mitad del daño.
CANNON PUNCH lanza un guante de box del color del traje; si golpea al rival lo despide en el sentido del golpe y al chocar pierde entre 12 % y 25 % de vida. DEATH RAY dispara cinco cargas celestes que forman un rayo; BOUNCY LASER rebota en diagonal hasta 10 veces; FAN no hace daño, desvía balas y nunca se acaba. Las cajas y objetos se empujan con los golpes, caen si pierden apoyo y se rompen al agotar su resistencia.

## En línea (red local)
Ambos teléfonos en la misma red Wi-Fi. Uno elige "Crear sala" y el otro la ve en la lista y se une.
Cada teléfono simula la misma partida con las entradas de los dos; deben tener la misma versión del juego.
