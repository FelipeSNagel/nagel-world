#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

BACKUP_FILE="${1:-}"

if [ -z "$BACKUP_FILE" ]; then
  echo "Uso: ./scripts/restore.sh backups/bedrock-YYYYMMDD-HHMMSS.tar.gz"
  echo
  echo "Backups recentes:"
  find "$ROOT_DIR/backups" -name "bedrock-*.tar.gz" -type f 2>/dev/null | sort | tail -n 10
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

if ! tar -tzf "$BACKUP_FILE" | grep -q '^data/'; then
  echo "Arquivo invalido: o backup precisa conter a pasta data/"
  exit 1
fi

STAMP="$(date +%Y%m%d-%H%M%S)"
SAFETY_BACKUP="$ROOT_DIR/backups/pre-restore-$STAMP.tar.gz"
OLD_DATA_DIR="$ROOT_DIR/data.before-restore-$STAMP"

mkdir -p "$ROOT_DIR/backups"

echo "Stopping server before restore..."
docker compose stop bedrock

restart_server() {
  echo "Starting server..."
  docker compose up -d bedrock >/dev/null
}
trap restart_server EXIT

if [ -d data ]; then
  echo "Saving current data to $SAFETY_BACKUP"
  tar -czf "$SAFETY_BACKUP" data

  echo "Moving current data to $OLD_DATA_DIR"
  mv data "$OLD_DATA_DIR"
fi

restore_failed() {
  echo "Restore failed."
  if [ ! -d data ] && [ -d "$OLD_DATA_DIR" ]; then
    echo "Restoring previous data directory..."
    mv "$OLD_DATA_DIR" data
  fi
}
trap restore_failed ERR

echo "Restoring $BACKUP_FILE"
tar -xzf "$BACKUP_FILE"

trap - ERR

echo "Restore complete."
echo "Previous data directory: $OLD_DATA_DIR"
echo "Previous data backup: $SAFETY_BACKUP"
