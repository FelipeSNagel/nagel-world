#!/usr/bin/env bash
set -euo pipefail

DATA_DIR="${CRAFT_DATA_DIR:-/data/minecraft-nagel/craftlandia-data}"
BACKUP_DIR="${CRAFT_BACKUP_DIR:-/data/minecraft-nagel/craftlandia-backups}"
CLOUD_REMOTE="${CRAFT_CLOUD_REMOTE:-gdrive}"
CLOUD_PATH="${CRAFT_CLOUD_PATH:-minecraft-nagel/craftlandia-backups}"
KEEP_DAYS="${CRAFT_BACKUP_KEEP_DAYS:-14}"
SERVICE_NAME="${CRAFT_COMPOSE_SERVICE:-craftlandia}"
LOCK_FILE="${CRAFT_BACKUP_LOCK:-/tmp/nagel-craftlandia-backup.lock}"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker nao encontrado."
  exit 1
fi

if ! command -v rclone >/dev/null 2>&1; then
  echo "rclone nao encontrado."
  exit 1
fi

if [ ! -d "$DATA_DIR" ]; then
  echo "Diretorio do servidor nao encontrado: $DATA_DIR"
  exit 1
fi

mkdir -p "$BACKUP_DIR"

exec 9>"$LOCK_FILE"
if ! flock -n 9; then
  echo "Outro backup ja esta rodando."
  exit 0
fi

container_id="$(
  docker ps \
    --filter "label=com.docker.compose.service=$SERVICE_NAME" \
    --format '{{.ID}}' \
    | head -n 1
)"

save_disabled=0
if [ -n "$container_id" ]; then
  echo "Forcando save do servidor antes do backup..."
  docker exec "$container_id" mc-send-to-console "save-off" || true
  save_disabled=1
  sleep 1
  docker exec "$container_id" mc-send-to-console "save-all flush" || docker exec "$container_id" mc-send-to-console "save-all" || true
  sleep 2
else
  echo "Container $SERVICE_NAME nao encontrado rodando. Fazendo backup do disco mesmo assim."
fi

enable_save() {
  if [ "$save_disabled" -eq 1 ] && [ -n "$container_id" ]; then
    docker exec "$container_id" mc-send-to-console "save-on" || true
  fi
}
trap enable_save EXIT

stamp="$(date +%Y%m%d-%H%M%S)"
backup_file="$BACKUP_DIR/craftlandia-$stamp.tar.gz"

echo "Criando backup: $backup_file"
tar -C "$(dirname "$DATA_DIR")" -czf "$backup_file" "$(basename "$DATA_DIR")"

enable_save
trap - EXIT

echo "Enviando backup para $CLOUD_REMOTE:$CLOUD_PATH"
rclone copy "$backup_file" "$CLOUD_REMOTE:$CLOUD_PATH"

find "$BACKUP_DIR" -name "craftlandia-*.tar.gz" -mtime +"$KEEP_DAYS" -delete

echo "Backup concluido: $(basename "$backup_file")"
