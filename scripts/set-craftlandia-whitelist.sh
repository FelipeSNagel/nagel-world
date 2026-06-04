#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
USERS_CSV="${1:-}"

cd "$ROOT_DIR"

if [[ -z "$USERS_CSV" ]]; then
  echo 'Usage: ./scripts/set-craftlandia-whitelist.sh ".Gamertag1,.Gamertag2"'
  exit 1
fi

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

trim() {
  printf '%s' "$1" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'
}

run_rcon() {
  docker compose --profile craftlandia exec -T craftlandia rcon-cli "$1"
}

server_running() {
  docker compose --profile craftlandia ps --status running --services 2>/dev/null | grep -qx craftlandia
}

set_env "CRAFT_ENABLE_WHITELIST" "true"
set_env "CRAFT_ENFORCE_WHITELIST" "true"
set_env "CRAFT_WHITELIST" "$USERS_CSV"

echo "Craftlandia whitelist saved to .env."

if ! server_running; then
  echo "Craftlandia is not running. Start it with: make craft-start"
  exit 0
fi

run_rcon "whitelist on"

IFS=',' read -ra USERS <<<"$USERS_CSV"
for raw_user in "${USERS[@]}"; do
  user="$(trim "$raw_user")"
  if [[ -n "$user" ]]; then
    run_rcon "whitelist add $user"
  fi
done

run_rcon "whitelist reload"

echo "Craftlandia whitelist enabled."
echo "Allowed users: $USERS_CSV"
