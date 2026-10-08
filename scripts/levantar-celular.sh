#!/usr/bin/env bash
set -e

export PATH=$PATH:$HOME/Android/Sdk/platform-tools

echo "→ Conectando puerto 8080 del backend con el celular..."
adb reverse tcp:8080 tcp:8080

echo "→ Abriendo Parkeo en tu celular..."
adb shell am start -n pe.parkeo/.MainActivity

echo "✔ ¡Parkeo abierto y conectado con éxito!"
