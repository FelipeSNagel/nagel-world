#!/bin/zsh
set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

if [[ -f .env ]]; then
  set -a
  source .env
  set +a
fi

PHANTOM_BIN="$ROOT_DIR/bin/phantom"
TARGET_HOST="${TARGET_HOST:-31.97.252.93}"
TARGET_PORT="${TARGET_PORT:-19132}"

if [[ ! -x "$PHANTOM_BIN" ]]; then
  echo "Phantom nao instalado. Rode: make install" >&2
  exit 1
fi

args=(-server "$TARGET_HOST:$TARGET_PORT")

if [[ -n "${BIND_PORT:-}" ]]; then
  args+=(-bind_port "$BIND_PORT")
fi

if [[ "${PHANTOM_DEBUG:-false}" == "true" ]]; then
  args+=(-debug)
fi

echo "Expondo $TARGET_HOST:$TARGET_PORT como servidor LAN..."
exec "$PHANTOM_BIN" "${args[@]}"
