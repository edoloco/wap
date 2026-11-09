#!/bin/sh
# Vuelve a generar los PNG del personaje desde el dibujo vectorial (¡sobrescribe los que hayas dibujado!).
# Uso: sh tools/piezas.sh [carpeta]   (por defecto app/src/main/assets/personaje)
B=$(mktemp -d); javac -nowarn -d "$B" app/src/main/java/com/wap/core/*.java app/src/main/java/com/wap/render/*.java app/src/main/java/com/wap/net/*.java tools/Sim.java tools/Piezas.java && java -Djava.awt.headless=true -cp "$B" Piezas "$@"
