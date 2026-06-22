#!/usr/bin/env bash
set -euo pipefail

PORT="${BEDROCK_PORT:-19132}"
JAVA_PORT="${CRAFT_JAVA_PORT:-25565}"

sudo ufw allow "$PORT/udp"
sudo ufw allow "$JAVA_PORT/tcp"
sudo ufw status
