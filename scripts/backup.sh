#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

KEEP_DAYS="${BACKUP_KEEP_DAYS:-14}"
if [ -f .env ]; then
  ENV_KEEP_DAYS="$(sed -n 's/^BACKUP_KEEP_DAYS=//p' .env | tail -n 1)"
  if [ -n "$ENV_KEEP_DAYS" ]; then
    KEEP_DAYS="$ENV_KEEP_DAYS"
  fi
fi
BACKUP_DIR="$ROOT_DIR/backups"
STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_FILE="$BACKUP_DIR/bedrock-$STAMP.tar.gz"

mkdir -p "$BACKUP_DIR"

echo "Stopping server for a consistent backup..."
docker compose stop bedrock

restart_server() {
  echo "Starting server..."
  docker compose up -d bedrock >/dev/null
}
trap restart_server EXIT

echo "Writing $BACKUP_FILE"
tar -czf "$BACKUP_FILE" data

find "$BACKUP_DIR" -name "bedrock-*.tar.gz" -mtime +"$KEEP_DAYS" -delete

echo "Backup complete."
