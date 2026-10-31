#!/bin/sh
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

LIB_DIR="$SCRIPT_DIR/tools/lib"
LOCAL_LIB="$HOME/.local/lib"
TERMUX_SO="/data/data/com.termux/files/usr/lib/libc++_shared.so"

if [ ! -f "$LIB_DIR/libc++_shared.so" ] && [ -f "$TERMUX_SO" ]; then
    mkdir -p "$LIB_DIR"
    cp "$TERMUX_SO" "$LIB_DIR/libc++_shared.so"
fi

export LD_LIBRARY_PATH="$LIB_DIR:$LOCAL_LIB"

which python3 >/dev/null 2>&1 && PYTHON=python3 || PYTHON=python
$PYTHON tools/build.py "$@"
