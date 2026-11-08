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

PROTON RAY (reemplaza a Larpa) es un rayo continuo: mientras mantienes el disparo sale un chorro naranja que se retuerce, con arcos eléctricos celestes, y la carga se agota lentamente (4 s). Si alcanza al rival lo atrapa en un lazo: queda quieto, cae despacio y pierde energía hasta que salta para soltarse. ROBOT TRAP (reemplaza a Blowtorch) se carga como el Plasma Cannon y al terminar dice "TRAMPA LISTA". Entonces A con la cruceta elige dónde cae: abajo, a los pies; adelante (o sola), a media distancia; arriba, lejos. No se pega: rebota en las paredes y siempre termina en el suelo. Allí se abre con un haz de luz en triángulo (punta en la trampa, base ancha y brillante arriba) y atrae todo lo cercano, incluido quien la lanzó: quien llega al centro muere al instante.

PERSONALIZAR (antes "Color del robot") cambia el color y el nombre. En línea, el nombre aparece sobre cada personaje. Cuando alguien muere, arriba se avisa "X SE BANEÓ DE LA VIDA" (se mató solo) o "X FUE ELIMINADO POR Y".

El importador de dibujos (`tools/arte.sh`, `arte/dibujos/`) queda listo, pero el juego usa por ahora el personaje original.

## Editor de niveles

En el menú, **EDITOR DE NIVELES** crea un nivel vacío o parte de uno aleatorio. Un dedo dibuja; dos dedos mueven la vista y el pellizco hace zoom. Herramientas: pincel, línea, cuadrado, círculo y triángulo (relleno o contorno), balde (toca tierra o madera para cambiarle el color a toda la zona, o un hueco cerrado para llenarlo) y borrar, todas con **tierra** (con vetas) o **madera** (color liso) en 10 colores y 3 grosores; además bloque morado, acero indestructible, caja, metal, computador, piedra, ducto y agua (se toca la altura, o "SIN AGUA"). Con mando: la cruceta mueve un cursor, A aplica (en las formas, apretar marca el inicio y soltar las dibuja), X cambia de herramienta y B sale. El nivel se guarda al salir o al probarlo; tocar el nombre lo renombra.

La madera no se recorta como la tierra: está dividida en trozos irregulares de unos 20 px que aguantan golpes (un poco menos que una caja), muestran grietas y se rompen en astillas de su color.

**SELECCIONA NIVEL** muestra cuadritos con la miniatura y el nombre de cada nivel (y el aleatorio).

Bajo el agua se avanza más lento (65 %), la gravedad es menor y el salto es más lento pero de la misma altura; el fuego se apaga y los disparos se frenan.

En línea, si el anfitrión eligió un nivel suyo, se manda al otro teléfono en el saludo y se usa solo para esa partida; al final se pregunta si conservarlo.

## Rivales

**RIVALES** elige de 1 a 4 rivales de la máquina, el modo (todos contra todos o equipos con colores: rojo, azul, verde, amarillo) y, por rival, la dificultad, las armas (al azar o elegidas) y el color o el equipo. En equipos no hay fuego amigo y gana el último equipo con vidas. En línea sigue siendo 1 contra 1.

## En línea (red local)

Retraso adaptable (versión 17): cada teléfono mide el ping y sube o baja solo el adelanto de entradas (3 a 8 ticks, 50 a 133 ms). Si hubo un congelón, después se recuperan los ticks atrasados para que la partida no quede más lenta. Los envíos van en su propio hilo, el WiFi se pone en baja latencia durante la partida y arriba a la izquierda se ve el ping ("RED 45 MS"). Si un teléfono no alcanza a dibujar a tiempo, baja partículas y luces solo. `tools/Red.java` simula dos teléfonos con WiFi lento y mide la velocidad real de la partida.
Ambos teléfonos en la misma red Wi-Fi. Uno elige "Crear sala" y el otro la ve en la lista y se une.
Cada teléfono simula la misma partida con las entradas de los dos; deben tener la misma versión del juego.
