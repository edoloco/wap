#!/bin/sh
# Reimporta el dibujo del personaje: tools/arte.sh [arte/dibujos/de_pie_1.svg]
# Requiere: pip install svgelements
SVG=${1:-arte/dibujos/de_pie_1.svg}; B=$(mktemp -d)
javac -nowarn -d "$B" app/src/main/java/com/wap/core/*.java app/src/main/java/com/wap/render/*.java app/src/main/java/com/wap/net/*.java tools/Molde.java || exit 1
java -cp "$B" Molde de_pie_1 > "$B/molde.json" && python3 tools/importar_arte.py "$SVG" "$B/molde.json" app/src/main/resources/com/wap/render/arte.txt
