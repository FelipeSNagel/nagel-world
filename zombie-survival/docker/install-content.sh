#!/bin/sh
set -eu

GEYSER_DIR="/data/plugins/Geyser-Spigot"
mkdir -p "/data/plugins" "$GEYSER_DIR/packs" "$GEYSER_DIR/custom_mappings"
rm -f \
  /data/plugins/Essentials*.jar \
  /data/plugins/LuckPerms-*.jar

if [ -f /data/bukkit.yml ]; then
  sed -i 's/^  connection-throttle:.*/  connection-throttle: 0/' /data/bukkit.yml
else
  printf 'settings:\n  connection-throttle: 0\n' > /data/bukkit.yml
fi

cp /content/NagelZombieSurvival.jar /data/plugins/NagelZombieSurvival.jar
cp /content/NagelZombieBedrock.mcpack "$GEYSER_DIR/packs/NagelZombieBedrock.mcpack"
cp /content/nagel-zombie-items.json "$GEYSER_DIR/custom_mappings/nagel-zombie-items.json"

if [ ! -f "$GEYSER_DIR/config.yml" ]; then
  cp /content/geyser-config.yml "$GEYSER_DIR/config.yml"
else
  sed -i \
    -e 's/^  enable-custom-content:.*/  enable-custom-content: true/' \
    -e 's/^  force-resource-packs:.*/  force-resource-packs: true/' \
    "$GEYSER_DIR/config.yml"
fi

chown -R 1000:1000 /data 2>/dev/null || true
echo "Plugin, mapeamentos e pacote Bedrock instalados."
