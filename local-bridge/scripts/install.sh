#!/bin/zsh
set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PHANTOM_VERSION="v0.5.4"
PHANTOM_BIN="$ROOT_DIR/bin/phantom"

cd "$ROOT_DIR"

if [[ -x "$PHANTOM_BIN" ]]; then
  echo "Phantom $PHANTOM_VERSION ja esta instalado."
  exit 0
fi

case "$(uname -s):$(uname -m)" in
  Darwin:arm64)
    asset="phantom-macos-arm8"
    ;;
  Darwin:x86_64)
    asset="phantom-macos"
    ;;
  *)
    echo "Sistema nao suportado automaticamente: $(uname -s) $(uname -m)" >&2
    exit 1
    ;;
esac

mkdir -p "$ROOT_DIR/bin"
download="$PHANTOM_BIN.download"
url="https://github.com/jhead/phantom/releases/download/$PHANTOM_VERSION/$asset"

echo "Baixando Phantom $PHANTOM_VERSION ($asset)..."
curl --retry 4 --retry-delay 2 -fL "$url" -o "$download"
chmod +x "$download"
mv "$download" "$PHANTOM_BIN"

echo "Phantom instalado em $PHANTOM_BIN"
