#!/bin/sh
# Compila y ejecuta las pruebas de escritorio: tools/run.sh <carpeta_salida>
OUT=${1:-out}; B=$OUT/classes; rm -rf "$B"; mkdir -p "$B"
javac -nowarn -d "$B" app/src/main/java/com/wap/core/*.java app/src/main/java/com/wap/render/*.java app/src/main/java/com/wap/net/*.java tools/Sim.java && cp -r app/src/main/resources/. "$B"/ 2>/dev/null; java -Djava.awt.headless=true -cp "$B" Sim "$OUT/shots"
