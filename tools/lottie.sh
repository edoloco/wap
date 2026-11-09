#!/bin/sh
# Rehace las animaciones Lottie del personaje desde el dibujo SVG, con las poses del juego.
# ¡OJO! Sobrescribe app/src/main/assets/animaciones/*.json (lo que hayas animado a mano se pierde).
# Uso: sh tools/lottie.sh [arte/dibujos/de_pie_1.svg] [carpeta_salida]     Requiere: pip install svgelements
SVG=${1:-arte/dibujos/de_pie_1.svg}; OUT=${2:-app/src/main/assets/animaciones}; B=$(mktemp -d)
javac -nowarn -d "$B" app/src/main/java/com/wap/core/*.java app/src/main/java/com/wap/render/*.java app/src/main/java/com/wap/net/*.java tools/Molde.java tools/Lottie.java || exit 1
java -cp "$B" Molde de_pie_1 > "$B/molde.json" && python3 tools/importar_arte.py "$SVG" "$B/molde.json" "$B/arte.txt" && java -cp "$B" Lottie "$B/arte.txt" "$OUT"
