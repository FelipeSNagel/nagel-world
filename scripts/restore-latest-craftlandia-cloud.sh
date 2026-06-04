#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

latest_name="$(./scripts/download-latest-craftlandia-backup.sh --dry-run | tail -n 1)"

if [ -z "$latest_name" ]; then
  echo "Nao foi possivel determinar o backup mais recente."
  exit 1
fi

./scripts/download-latest-craftlandia-backup.sh
./scripts/restore-craftlandia.sh "craftlandia-backups/$latest_name"
