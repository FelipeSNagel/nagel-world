#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

DRY_RUN="${1:-}"

# shellcheck source=/dev/null
source "$ROOT_DIR/scripts/craftlandia-cloud-common.sh"
load_cloud_config

mkdir -p "$ROOT_DIR/craftlandia-backups"

latest_name="$(
  rclone lsf "$CRAFT_CLOUD_DEST" --files-only 2>/dev/null \
    | grep '^craftlandia-[0-9]\{8\}-[0-9]\{6\}\.tar\.gz$' \
    | sort \
    | tail -n 1
)"

if [ -z "$latest_name" ]; then
  echo "Nenhum backup encontrado em $CRAFT_CLOUD_DEST"
  exit 1
fi

echo "$latest_name"

if [ "$DRY_RUN" = "--dry-run" ]; then
  exit 0
fi

if [ -f "$ROOT_DIR/craftlandia-backups/$latest_name" ]; then
  echo "Backup ja existe localmente: craftlandia-backups/$latest_name"
  exit 0
fi

echo "Downloading $latest_name from $CRAFT_CLOUD_DEST"
rclone copyto "$CRAFT_CLOUD_DEST/$latest_name" "$ROOT_DIR/craftlandia-backups/$latest_name"

echo "Downloaded: craftlandia-backups/$latest_name"
