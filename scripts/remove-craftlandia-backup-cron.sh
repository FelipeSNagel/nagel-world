#!/usr/bin/env bash
set -euo pipefail

MARKER="# nagel-craftlandia-backup"
TMP_FILE="$(mktemp)"

if ! crontab -l >"$TMP_FILE" 2>/dev/null; then
  echo "No crontab found."
  rm -f "$TMP_FILE"
  exit 0
fi

sed -i.bak "/$MARKER/d" "$TMP_FILE"
crontab "$TMP_FILE"
rm -f "$TMP_FILE" "$TMP_FILE.bak"

echo "Daily Craftlandia backup schedule removed."
