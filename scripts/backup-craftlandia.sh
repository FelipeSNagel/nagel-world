#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

KEEP_DAYS="${CRAFT_BACKUP_KEEP_DAYS:-14}"
if [ -f .env ]; then
  ENV_KEEP_DAYS="$(sed -n 's/^CRAFT_BACKUP_KEEP_DAYS=//p' .env | tail -n 1)"
  if [ -n "$ENV_KEEP_DAYS" ]; then
    KEEP_DAYS="$ENV_KEEP_DAYS"
  fi
fi

BACKUP_DIR="$ROOT_DIR/craftlandia-backups"
STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_FILE="$BACKUP_DIR/craftlandia-$STAMP.tar.gz"

mkdir -p "$BACKUP_DIR"

echo "Stopping Craftlandia server for a consistent backup..."
docker compose --profile craftlandia stop craftlandia

restart_server() {
  echo "Starting Craftlandia server..."
  docker compose --profile craftlandia up -d craftlandia >/dev/null
}
trap restart_server EXIT

echo "Writing $BACKUP_FILE"
tar -czf "$BACKUP_FILE" craftlandia-data

find "$BACKUP_DIR" -name "craftlandia-*.tar.gz" -mtime +"$KEEP_DAYS" -delete

echo "Craftlandia backup complete."
