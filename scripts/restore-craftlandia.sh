#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

BACKUP_FILE="${1:-}"

if [ -z "$BACKUP_FILE" ]; then
  echo "Uso: ./scripts/restore-craftlandia.sh craftlandia-backups/craftlandia-YYYYMMDD-HHMMSS.tar.gz"
  echo
  echo "Backups recentes:"
  find "$ROOT_DIR/craftlandia-backups" -name "craftlandia-*.tar.gz" -type f 2>/dev/null | sort | tail -n 10
  exit 1
fi

case "$BACKUP_FILE" in
  /*) ;;
  *) BACKUP_FILE="$ROOT_DIR/$BACKUP_FILE" ;;
esac

if [ ! -f "$BACKUP_FILE" ]; then
  echo "Backup nao encontrado: $BACKUP_FILE"
  exit 1
fi

if ! tar -tzf "$BACKUP_FILE" | grep -q '^craftlandia-data/'; then
  echo "Arquivo invalido: o backup precisa conter a pasta craftlandia-data/"
  exit 1
fi

STAMP="$(date +%Y%m%d-%H%M%S)"
SAFETY_BACKUP="$ROOT_DIR/craftlandia-backups/pre-restore-$STAMP.tar.gz"
OLD_DATA_DIR="$ROOT_DIR/craftlandia-data.before-restore-$STAMP"

mkdir -p "$ROOT_DIR/craftlandia-backups"

echo "Stopping Craftlandia server before restore..."
docker compose --profile craftlandia stop craftlandia

restart_server() {
  echo "Starting Craftlandia server..."
  docker compose --profile craftlandia up -d craftlandia >/dev/null
}
trap restart_server EXIT

if [ -d craftlandia-data ]; then
  echo "Saving current Craftlandia data to $SAFETY_BACKUP"
  tar -czf "$SAFETY_BACKUP" craftlandia-data

  echo "Moving current Craftlandia data to $OLD_DATA_DIR"
  mv craftlandia-data "$OLD_DATA_DIR"
fi

restore_failed() {
  echo "Restore failed."
  if [ ! -d craftlandia-data ] && [ -d "$OLD_DATA_DIR" ]; then
    echo "Restoring previous Craftlandia data directory..."
    mv "$OLD_DATA_DIR" craftlandia-data
  fi
}
trap restore_failed ERR

echo "Restoring $BACKUP_FILE"
tar -xzf "$BACKUP_FILE"

trap - ERR

echo "Craftlandia restore complete."
echo "Previous data directory: $OLD_DATA_DIR"
echo "Previous data backup: $SAFETY_BACKUP"
