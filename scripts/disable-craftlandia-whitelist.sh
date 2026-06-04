#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT_DIR"

set_env() {
  local key="$1"
  local value="$2"
  local tmp_file

  touch .env

  if grep -q "^${key}=" .env; then
    tmp_file="$(mktemp)"
    awk -v key="$key" -v value="$value" '
      BEGIN { updated = 0 }
      $0 ~ "^" key "=" {
        print key "=" value
        updated = 1
        next
      }
      { print }
      END {
        if (updated == 0) {
          print key "=" value
        }
      }
    ' .env >"$tmp_file"
    mv "$tmp_file" .env
  else
    printf '%s=%s\n' "$key" "$value" >> .env
  fi
}

run_rcon() {
  docker compose --profile craftlandia exec -T craftlandia rcon-cli "$1"
}

server_running() {
  docker compose --profile craftlandia ps --status running --services 2>/dev/null | grep -qx craftlandia
}

set_env "CRAFT_ENABLE_WHITELIST" "false"
set_env "CRAFT_WHITELIST" ""

if server_running; then
  run_rcon "whitelist off"
fi

echo "Craftlandia whitelist disabled."
