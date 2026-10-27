#!/bin/sh
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

which python3 >/dev/null 2>&1 && PYTHON=python3 || PYTHON=python
$PYTHON tools/build.py "$@"
