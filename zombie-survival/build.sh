#!/bin/sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)"
PROJECT="$ROOT/zombie-survival"
BUILD="$PROJECT/build"
DIST="$PROJECT/dist"
API_JAR="$ROOT/plugin-src/nagel-aliases/lib/paper-api-1.21.4.jar"
LIBRARIES="$ROOT/craftlandia-data/libraries"

BUILD_JAVA_HOME="${BUILD_JAVA_HOME:-$(/usr/libexec/java_home -v 21 2>/dev/null || true)}"
if [ -z "$BUILD_JAVA_HOME" ] || [ ! -x "$BUILD_JAVA_HOME/bin/javac" ]; then
  echo "JDK 21 ou superior nao encontrado." >&2
  exit 1
fi
if [ ! -f "$API_JAR" ] || [ ! -d "$LIBRARIES" ]; then
  echo "Paper API ou bibliotecas locais nao encontradas." >&2
  exit 1
fi

rm -rf "$BUILD" "$DIST"
mkdir -p "$BUILD/classes" "$BUILD/generated-textures" "$DIST"

CLASSPATH="$API_JAR:$(find "$LIBRARIES" -type f -name '*.jar' -print | paste -sd: -)"
"$BUILD_JAVA_HOME/bin/javac" -proc:none --release 21 -classpath "$CLASSPATH" \
  -d "$BUILD/classes" \
  "$PROJECT/plugin/src/main/java/com/nagelworld/zombie/WeaponType.java" \
  "$PROJECT/plugin/src/main/java/com/nagelworld/zombie/NagelZombieSurvivalPlugin.java"
cp "$PROJECT/plugin/src/main/resources/plugin.yml" "$BUILD/classes/plugin.yml"
cp "$PROJECT/plugin/src/main/resources/config.yml" "$BUILD/classes/config.yml"
"$BUILD_JAVA_HOME/bin/jar" --create --file "$DIST/NagelZombieSurvival.jar" -C "$BUILD/classes" .

"$BUILD_JAVA_HOME/bin/javac" --release 21 -d "$BUILD/tools" "$PROJECT/tools/GenerateTextures.java"
"$BUILD_JAVA_HOME/bin/java" -Djava.awt.headless=true -cp "$BUILD/tools" GenerateTextures "$BUILD/generated-textures"

cp -R "$PROJECT/resource-packs/java" "$BUILD/java-pack"
mkdir -p "$BUILD/java-pack/assets/nagelzombie/textures/item"
cp "$BUILD/generated-textures"/*.png "$BUILD/java-pack/assets/nagelzombie/textures/item/"

for item in pistol shotgun rifle sniper light_ammo shell rifle_ammo sniper_ammo; do
  mkdir -p "$BUILD/java-pack/assets/nagelzombie/items" "$BUILD/java-pack/assets/nagelzombie/models/item"
  printf '{"model":{"type":"minecraft:model","model":"nagelzombie:item/%s"}}\n' "$item" \
    > "$BUILD/java-pack/assets/nagelzombie/items/$item.json"
  parent="minecraft:item/generated"
  case "$item" in pistol|shotgun|rifle|sniper) parent="minecraft:item/handheld" ;; esac
  printf '{"parent":"%s","textures":{"layer0":"nagelzombie:item/%s"}}\n' "$parent" "$item" \
    > "$BUILD/java-pack/assets/nagelzombie/models/item/$item.json"
done
find "$BUILD/java-pack" -type f -exec touch -t 202601010000 {} +
(cd "$BUILD/java-pack" && find . -type f -print | LC_ALL=C sort | zip -q "$DIST/NagelZombieJava.zip" -@)

cp -R "$PROJECT/resource-packs/bedrock" "$BUILD/bedrock-pack"
mkdir -p "$BUILD/bedrock-pack/textures/items"
cp "$BUILD/generated-textures"/*.png "$BUILD/bedrock-pack/textures/items/"
find "$BUILD/bedrock-pack" -type f -exec touch -t 202601010000 {} +
(cd "$BUILD/bedrock-pack" && find . -type f -print | LC_ALL=C sort | zip -q "$DIST/NagelZombieBedrock.mcpack" -@)

shasum -a 1 "$DIST/NagelZombieJava.zip" | awk '{print $1}' > "$DIST/NagelZombieJava.sha1"
echo "Artefatos criados em $DIST"
