#!/bin/zsh
set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LABEL="com.nagel.local-bridge"
DOMAIN="gui/$(id -u)"
PLIST="$HOME/Library/LaunchAgents/$LABEL.plist"
cd "$ROOT_DIR"
"$ROOT_DIR/scripts/install.sh"

mkdir -p "$HOME/Library/LaunchAgents" "$ROOT_DIR/logs"

cat > "$PLIST" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key>
  <string>$LABEL</string>
  <key>ProgramArguments</key>
  <array>
    <string>/bin/zsh</string>
    <string>$ROOT_DIR/scripts/run.sh</string>
  </array>
  <key>RunAtLoad</key>
  <true/>
  <key>KeepAlive</key>
  <true/>
  <key>WorkingDirectory</key>
  <string>$ROOT_DIR</string>
  <key>StandardOutPath</key>
  <string>$ROOT_DIR/logs/local-bridge.log</string>
  <key>StandardErrorPath</key>
  <string>$ROOT_DIR/logs/local-bridge.error.log</string>
</dict>
</plist>
EOF

launchctl bootout "$DOMAIN" "$PLIST" 2>/dev/null || true
launchctl bootstrap "$DOMAIN" "$PLIST"
launchctl enable "$DOMAIN/$LABEL"
launchctl kickstart -k "$DOMAIN/$LABEL"

echo "Local bridge instalado e iniciado."
echo "Status: make service-status"
echo "Logs:   make service-logs"
