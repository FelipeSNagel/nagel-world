#!/usr/bin/env bash

load_cloud_config() {
  CRAFT_CLOUD_REMOTE="${CRAFT_CLOUD_REMOTE:-}"
  CRAFT_CLOUD_PATH="${CRAFT_CLOUD_PATH:-minecraft-nagel/craftlandia-backups}"

  if [ -f .env ]; then
    env_remote="$(sed -n 's/^CRAFT_CLOUD_REMOTE=//p' .env | tail -n 1)"
    env_path="$(sed -n 's/^CRAFT_CLOUD_PATH=//p' .env | tail -n 1)"

    if [ -n "$env_remote" ]; then
      CRAFT_CLOUD_REMOTE="$env_remote"
    fi

    if [ -n "$env_path" ]; then
      CRAFT_CLOUD_PATH="$env_path"
    fi
  fi

  if [ -z "$CRAFT_CLOUD_REMOTE" ]; then
    echo "CRAFT_CLOUD_REMOTE nao configurado."
    echo "Configure no .env, por exemplo:"
    echo "CRAFT_CLOUD_REMOTE=gdrive"
    echo "CRAFT_CLOUD_PATH=minecraft-nagel/craftlandia-backups"
    exit 1
  fi

  if ! command -v rclone >/dev/null 2>&1; then
    echo "rclone nao encontrado."
    echo "Instale e configure um remote primeiro: rclone config"
    exit 1
  fi

  CRAFT_CLOUD_DEST="${CRAFT_CLOUD_REMOTE}:${CRAFT_CLOUD_PATH}"
}
