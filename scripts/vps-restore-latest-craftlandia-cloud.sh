#!/usr/bin/env bash
set -euo pipefail

DATA_ROOT="${CRAFT_DATA_ROOT:-/data/minecraft-nagel}"
DATA_DIR="${CRAFT_DATA_DIR:-$DATA_ROOT/craftlandia-data}"
BACKUP_DIR="${CRAFT_BACKUP_DIR:-$DATA_ROOT/craftlandia-backups}"
CLOUD_REMOTE="${CRAFT_CLOUD_REMOTE:-gdrive}"
CLOUD_PATH="${CRAFT_CLOUD_PATH:-minecraft-nagel/craftlandia-backups}"
SERVICE_NAME="${CRAFT_COMPOSE_SERVICE:-craftlandia}"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker nao encontrado."
  exit 1
fi

if ! command -v rclone >/dev/null 2>&1; then
  echo "rclone nao encontrado."
  exit 1
fi

mkdir -p "$DATA_ROOT" "$BACKUP_DIR"

latest="$(
  rclone lsf "$CLOUD_REMOTE:$CLOUD_PATH" --files-only \
    | grep '^craftlandia-[0-9]\{8\}-[0-9]\{6\}\.tar\.gz$' \
    | sort \
    | tail -n 1
)"

if [ -z "$latest" ]; then
  echo "Nenhum backup encontrado em $CLOUD_REMOTE:$CLOUD_PATH"
  exit 1
fi

container_id="$(
  docker ps -a \
    --filter "label=com.docker.compose.service=$SERVICE_NAME" \
    --format '{{.ID}}' \
    | head -n 1
)"

was_running=0
if [ -n "$container_id" ] && docker ps --format '{{.ID}}' | grep -q "^$container_id$"; then
  was_running=1
  echo "Parando container $SERVICE_NAME antes do restore..."
  docker stop "$container_id"
fi

local_backup="$BACKUP_DIR/$latest"

echo "Baixando $CLOUD_REMOTE:$CLOUD_PATH/$latest"
rclone copyto "$CLOUD_REMOTE:$CLOUD_PATH/$latest" "$local_backup"

if [ -d "$DATA_DIR" ]; then
  safety_dir="$DATA_DIR.before-restore-$(date +%Y%m%d-%H%M%S)"
  echo "Movendo mundo atual para $safety_dir"
  mv "$DATA_DIR" "$safety_dir"
fi

echo "Restaurando backup em $DATA_ROOT"
tar -C "$DATA_ROOT" -xzf "$local_backup"
chown -R 1000:1000 "$DATA_DIR"

if [ -n "$container_id" ] && [ "$was_running" -eq 1 ]; then
  echo "Iniciando container $SERVICE_NAME..."
  docker start "$container_id"
fi

echo "Restore concluido: $latest"
