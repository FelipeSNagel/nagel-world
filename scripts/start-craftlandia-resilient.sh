#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

docker compose stop bedrock

if [ ! -d craftlandia-data ]; then
  echo "craftlandia-data nao existe; tentando restaurar o ultimo backup da nuvem..."
  ./scripts/restore-latest-craftlandia-cloud.sh
else
  echo "craftlandia-data existe; usando mundo local."
fi

docker compose --profile craftlandia up -d craftlandia
