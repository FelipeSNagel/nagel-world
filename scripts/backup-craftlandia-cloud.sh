#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

# shellcheck source=/dev/null
source "$ROOT_DIR/scripts/craftlandia-cloud-common.sh"
load_cloud_config

./scripts/backup-craftlandia.sh

latest_backup="$(find "$ROOT_DIR/craftlandia-backups" -name "craftlandia-*.tar.gz" -type f | sort | tail -n 1)"

if [ -z "$latest_backup" ]; then
  echo "Nenhum backup local encontrado para enviar."
  exit 1
fi

echo "Uploading latest backup to $CRAFT_CLOUD_DEST"
rclone copy "$latest_backup" "$CRAFT_CLOUD_DEST"

echo "Cloud backup complete: $(basename "$latest_backup")"
