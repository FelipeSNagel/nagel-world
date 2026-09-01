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
mkdir -p "$BUILD/classes" "$BUILD/generated-textures" "$BUILD/generated-sounds-wav" "$BUILD/generated-sounds" "$DIST"

CLASSPATH="$API_JAR:$(find "$LIBRARIES" -type f -name '*.jar' -print | paste -sd: -)"
"$BUILD_JAVA_HOME/bin/javac" -proc:none --release 21 -classpath "$CLASSPATH" \
  -d "$BUILD/classes" \
  "$PROJECT/plugin/src/main/java/com/nagelworld/zombie/WeaponType.java" \
  "$PROJECT/plugin/src/main/java/com/nagelworld/zombie/NagelZombieSurvivalPlugin.java"
cp "$PROJECT/plugin/src/main/resources/plugin.yml" "$BUILD/classes/plugin.yml"
cp "$PROJECT/plugin/src/main/resources/config.yml" "$BUILD/classes/config.yml"
"$BUILD_JAVA_HOME/bin/jar" --create --file "$DIST/NagelZombieSurvival.jar" -C "$BUILD/classes" .

"$BUILD_JAVA_HOME/bin/javac" --release 21 -d "$BUILD/tools" \
  "$PROJECT/tools/GenerateTextures.java" "$PROJECT/tools/GenerateSounds.java"
"$BUILD_JAVA_HOME/bin/java" -Djava.awt.headless=true -cp "$BUILD/tools" GenerateTextures "$BUILD/generated-textures"
"$BUILD_JAVA_HOME/bin/java" -cp "$BUILD/tools" GenerateSounds "$BUILD/generated-sounds-wav"
if ! command -v oggenc >/dev/null 2>&1; then
  echo "oggenc nao encontrado; instale vorbis-tools para gerar os sons OGG." >&2
  exit 1
fi
serial=12001
for wav in "$BUILD/generated-sounds-wav"/*.wav; do
  name="$(basename "$wav" .wav)"
  oggenc -Q -q 5 -s "$serial" -o "$BUILD/generated-sounds/$name.ogg" "$wav"
  serial=$((serial + 1))
done

cp -R "$PROJECT/resource-packs/java" "$BUILD/java-pack"
mkdir -p "$BUILD/java-pack/assets/nagelzombie/textures/item"
cp "$BUILD/generated-textures"/pistol.png "$BUILD/generated-textures"/shotgun.png \
  "$BUILD/generated-textures"/rifle.png "$BUILD/generated-textures"/sniper.png \
  "$BUILD/generated-textures"/light_ammo.png "$BUILD/generated-textures"/shell.png \
  "$BUILD/generated-textures"/rifle_ammo.png "$BUILD/generated-textures"/sniper_ammo.png \
  "$BUILD/generated-textures"/gun_dark.png "$BUILD/generated-textures"/gun_metal.png \
  "$BUILD/generated-textures"/gun_wood.png "$BUILD/generated-textures"/gun_olive.png \
  "$BUILD/generated-textures"/gun_scope.png \
  "$BUILD/java-pack/assets/nagelzombie/textures/item/"
mkdir -p "$BUILD/java-pack/assets/minecraft/textures/entity/zombie"
cp "$BUILD/generated-textures/zombie.png" "$BUILD/java-pack/assets/minecraft/textures/entity/zombie/zombie.png"
cp "$BUILD/generated-textures/husk.png" "$BUILD/java-pack/assets/minecraft/textures/entity/zombie/husk.png"
cp "$BUILD/generated-textures/drowned.png" "$BUILD/java-pack/assets/minecraft/textures/entity/zombie/drowned.png"
cp "$BUILD/generated-textures/drowned_outer_layer.png" "$BUILD/java-pack/assets/minecraft/textures/entity/zombie/drowned_outer_layer.png"
mkdir -p "$BUILD/java-pack/assets/nagelzombie/sounds"
cp "$BUILD/generated-sounds"/*.ogg "$BUILD/java-pack/assets/nagelzombie/sounds/"

for item in pistol shotgun rifle sniper light_ammo shell rifle_ammo sniper_ammo; do
  mkdir -p "$BUILD/java-pack/assets/nagelzombie/items" "$BUILD/java-pack/assets/nagelzombie/models/item"
  printf '{"model":{"type":"minecraft:model","model":"nagelzombie:item/%s"}}\n' "$item" \
    > "$BUILD/java-pack/assets/nagelzombie/items/$item.json"
  case "$item" in
    pistol|shotgun|rifle|sniper) ;;
    *) printf '{"parent":"minecraft:item/generated","textures":{"layer0":"nagelzombie:item/%s"}}\n' "$item" \
      > "$BUILD/java-pack/assets/nagelzombie/models/item/$item.json" ;;
  esac
done
find "$BUILD/java-pack" -type f -exec touch -t 202601010000 {} +
(cd "$BUILD/java-pack" && find . -type f -print | LC_ALL=C sort | zip -q "$DIST/NagelZombieJava.zip" -@)

cp -R "$PROJECT/resource-packs/bedrock" "$BUILD/bedrock-pack"
mkdir -p "$BUILD/bedrock-pack/textures/items"
cp "$BUILD/generated-textures"/pistol.png "$BUILD/generated-textures"/shotgun.png \
  "$BUILD/generated-textures"/rifle.png "$BUILD/generated-textures"/sniper.png \
  "$BUILD/generated-textures"/light_ammo.png "$BUILD/generated-textures"/shell.png \
  "$BUILD/generated-textures"/rifle_ammo.png "$BUILD/generated-textures"/sniper_ammo.png \
  "$BUILD/bedrock-pack/textures/items/"
mkdir -p "$BUILD/bedrock-pack/textures/entity/zombie"
cp "$BUILD/generated-textures/zombie.png" "$BUILD/bedrock-pack/textures/entity/zombie/zombie.png"
cp "$BUILD/generated-textures/husk.png" "$BUILD/bedrock-pack/textures/entity/zombie/husk.png"
cp "$BUILD/generated-textures/drowned.png" "$BUILD/bedrock-pack/textures/entity/zombie/drowned.png"
cp "$BUILD/generated-textures/drowned_outer_layer.png" "$BUILD/bedrock-pack/textures/entity/zombie/drowned_outer_layer.png"
mkdir -p "$BUILD/bedrock-pack/sounds"
cp "$BUILD/generated-sounds"/*.ogg "$BUILD/bedrock-pack/sounds/"
find "$BUILD/bedrock-pack" -type f -exec touch -t 202601010000 {} +
(cd "$BUILD/bedrock-pack" && find . -type f -print | LC_ALL=C sort | zip -q "$DIST/NagelZombieBedrock.mcpack" -@)

shasum -a 1 "$DIST/NagelZombieJava.zip" | awk '{print $1}' > "$DIST/NagelZombieJava.sha1"
echo "Artefatos criados em $DIST"
