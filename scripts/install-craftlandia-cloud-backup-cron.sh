#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCHEDULE="${1:-0 5 * * *}"
MARKER="# nagel-craftlandia-cloud-backup"
LOG_FILE="$ROOT_DIR/craftlandia-backups/cloud-backup.log"
CRON_LINE="$SCHEDULE cd $ROOT_DIR && ./scripts/backup-craftlandia-cloud.sh >> $LOG_FILE 2>&1 $MARKER"
TMP_FILE="$(mktemp)"

mkdir -p "$ROOT_DIR/craftlandia-backups"

if crontab -l >"$TMP_FILE" 2>/dev/null; then
  sed -i.bak "/$MARKER/d" "$TMP_FILE"
else
  : >"$TMP_FILE"
fi

printf '%s\n' "$CRON_LINE" >>"$TMP_FILE"
crontab "$TMP_FILE"
rm -f "$TMP_FILE" "$TMP_FILE.bak"

echo "Daily Craftlandia cloud backup scheduled:"
echo "$CRON_LINE"
echo
echo "Logs: $LOG_FILE"
