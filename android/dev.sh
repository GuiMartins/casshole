#!/usr/bin/env bash
# Atalhos de build/deploy do app Android, tipo um Makefile (nao tem `make`
# instalado no Git Bash deste PC). Uso: ./dev.sh <comando>
#
# Comandos:
#   emulator   abre o emulador (AVD fatura_test) em background
#   build      builda o APK debug
#   install    instala o APK debug ja buildado no dispositivo/emulador conectado
#   start      abre o app no dispositivo/emulador conectado
#   run        build + install + start, tudo de uma vez (o mais usado)
#   logs       segue o logcat do processo do app (precisa estar rodando)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

export JAVA_HOME="${JAVA_HOME:-/c/Program Files/Microsoft/jdk-17.0.19.10-hotspot}"
ANDROID_HOME="${ANDROID_HOME:-/c/Users/guilh/AndroidSdk}"

ADB="$ANDROID_HOME/platform-tools/adb.exe"
EMULATOR="$ANDROID_HOME/emulator/emulator.exe"
AVD_NOME="money_hole_test"
PACOTE="com.casshole"
ACTIVITY="$PACOTE/.MainActivity"
APK="$SCRIPT_DIR/app/build/outputs/apk/debug/app-debug.apk"

buildar() {
    (cd "$SCRIPT_DIR" && ./gradlew assembleDebug)
}

instalar() {
    "$ADB" install -r "$APK"
}

iniciar() {
    "$ADB" shell am start -n "$ACTIVITY"
}

comando="${1:-run}"

case "$comando" in
    emulator)
        echo "Abrindo o emulador $AVD_NOME (a janela demora uns 30s-1min pra subir)..."
        "$EMULATOR" -avd "$AVD_NOME" &
        disown
        ;;
    build)
        buildar
        ;;
    install)
        instalar
        ;;
    start)
        iniciar
        ;;
    run)
        buildar
        instalar
        iniciar
        ;;
    logs)
        pid=$("$ADB" shell pidof -s "$PACOTE")
        echo "Seguindo logcat do PID $pid (Ctrl+C pra sair)..."
        "$ADB" logcat --pid="$pid"
        ;;
    *)
        echo "Uso: ./dev.sh [emulator|build|install|start|run|logs]"
        exit 1
        ;;
esac
