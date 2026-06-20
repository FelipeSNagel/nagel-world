#!/bin/zsh
set -eu

LABEL="com.nagel.local-bridge"
DOMAIN="gui/$(id -u)"
PLIST="$HOME/Library/LaunchAgents/$LABEL.plist"

launchctl bootout "$DOMAIN" "$PLIST" 2>/dev/null || true
rm -f "$PLIST"

echo "Local bridge removido do inicio automatico."
