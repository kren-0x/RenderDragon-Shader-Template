#!/bin/sh
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

LIB_DIR="$SCRIPT_DIR/tools/lib"
LOCAL_LIB="$HOME/.local/lib"
TERMUX_PREFIX="${PREFIX:-/data/data/com.termux/files/usr}"
TERMUX_SO="$TERMUX_PREFIX/lib/libc++_shared.so"
TERMUX_BIN="$TERMUX_PREFIX/bin"

mkdir -p "$LIB_DIR"

if [ ! -f "$LIB_DIR/libc++_shared.so" ] && [ -f "$TERMUX_SO" ]; then
    cp "$TERMUX_SO" "$LIB_DIR/libc++_shared.so" 2>/dev/null || true
fi

if [ ! -f "$LIB_DIR/libandroid.so" ]; then
    if which clang >/dev/null 2>&1; then
        clang -shared -o "$LIB_DIR/libandroid.so" -x c /dev/null 2>/dev/null || true
    elif which gcc >/dev/null 2>&1; then
        gcc -shared -o "$LIB_DIR/libandroid.so" -x c /dev/null 2>/dev/null || true
    fi
fi

if [ -d "/data/data/com.termux" ] || [ -n "$TERMUX_VERSION" ] || [ -n "$PREFIX" ]; then
    if ! which glslangValidator >/dev/null 2>&1 && ! which glslang >/dev/null 2>&1 && [ ! -f "$LIB_DIR/.glslang_installed" ]; then
        pkg install -y glslang 2>/dev/null || apt-get install -y glslang 2>/dev/null || true
        touch "$LIB_DIR/.glslang_installed" 2>/dev/null || true
    fi
    if [ -f "$TERMUX_BIN/glslangValidator" ] && [ ! -f "$TERMUX_BIN/glslang" ]; then
        ln -s "$TERMUX_BIN/glslangValidator" "$TERMUX_BIN/glslang" 2>/dev/null || true
    fi
fi

SYSTEM_LZMA=""
for lzma_candidate in \
    /apex/com.android.runtime/lib64/bionic/liblzma.so \
    /apex/com.android.art/lib64/liblzma.so \
    /system/lib64/liblzma.so \
    /apex/com.android.runtime/lib/bionic/liblzma.so \
    /apex/com.android.art/lib/liblzma.so \
    /system/lib/liblzma.so; do
    if [ -f "$lzma_candidate" ]; then
        SYSTEM_LZMA="$lzma_candidate"
        if [ ! -f "$LIB_DIR/liblzma.so" ]; then
            cp "$lzma_candidate" "$LIB_DIR/liblzma.so" 2>/dev/null || true
        fi
        break
    fi
done

if [ -n "$SYSTEM_LZMA" ]; then
    export LD_PRELOAD="$SYSTEM_LZMA${LD_PRELOAD:+:$LD_PRELOAD}"
fi

export LD_LIBRARY_PATH="$LIB_DIR:$LOCAL_LIB:/apex/com.android.runtime/lib64/bionic:/apex/com.android.art/lib64:/system/lib64:/apex/com.android.runtime/lib/bionic:/system/lib"

which python3 >/dev/null 2>&1 && PYTHON=python3 || PYTHON=python
$PYTHON tools/build.py "$@"
