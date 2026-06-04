#!/usr/bin/env bash
set -euo pipefail

PORT="${BEDROCK_PORT:-19132}"

sudo ufw allow "$PORT/udp"
sudo ufw status
