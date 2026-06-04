#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

USERS="${1:-}"

if [ -z "$USERS" ]; then
  echo 'Uso: ./scripts/set-allowlist.sh "Gamertag1,Gamertag2,Gamertag3"'
  echo
  echo "Use os Gamertags exatos da conta Microsoft/Xbox."
  exit 1
fi

touch .env

set_env_value() {
  local key="$1"
  local value="$2"
  local tmp_file
  tmp_file="$(mktemp)"

  if grep -q "^$key=" .env; then
    awk -v key="$key" -v value="$value" '
      BEGIN { prefix = key "=" }
      index($0, prefix) == 1 { print key "=" value; next }
      { print }
    ' .env >"$tmp_file"
  else
    cp .env "$tmp_file"
    printf '%s=%s\n' "$key" "$value" >>"$tmp_file"
  fi

  mv "$tmp_file" .env
}

set_env_value "ALLOW_LIST" "true"
set_env_value "ALLOW_LIST_USERS" "$USERS"

echo "Allowlist enabled for:"
echo "$USERS"
echo
echo "Restarting server to apply changes..."
docker compose up -d bedrock
