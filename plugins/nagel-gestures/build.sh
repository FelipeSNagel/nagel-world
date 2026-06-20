#!/bin/sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname "$0")/../.." && pwd)"
PLUGIN_DIR="$ROOT_DIR/plugins/nagel-gestures"
BUILD_DIR="$PLUGIN_DIR/build"
OUTPUT_JAR="$ROOT_DIR/config/craftlandia/NagelGestures.jar"
LIBRARIES_DIR="$ROOT_DIR/craftlandia-data/libraries"

if [ -z "${PAPER_API_JAR:-}" ]; then
  PAPER_API_JAR="$(find "$ROOT_DIR/craftlandia-data/libraries/io/papermc/paper/paper-api" \
    -type f -name 'paper-api-*.jar' 2>/dev/null | sort | tail -n 1)"
fi

if [ -z "${PAPER_API_JAR:-}" ] || [ ! -f "$PAPER_API_JAR" ]; then
  echo "Paper API nao encontrada. Defina PAPER_API_JAR=/caminho/paper-api.jar" >&2
  exit 1
fi

if [ ! -d "$LIBRARIES_DIR" ]; then
  echo "Bibliotecas do Paper nao encontradas em $LIBRARIES_DIR" >&2
  exit 1
fi

if command -v javac >/dev/null 2>&1; then
  java_major="$(javac -version 2>&1 | sed 's/^javac //' | cut -d. -f1)"
else
  java_major=0
fi

if [ "$java_major" -lt 25 ]; then
  temp_dir="$(mktemp -d "${TMPDIR:-/tmp}/nagel-gestures-build.XXXXXX")"
  trap 'rm -rf "$temp_dir"' EXIT

  docker run --rm \
    --entrypoint /bin/sh \
    --user "$(id -u):$(id -g)" \
    -v "$PLUGIN_DIR/src:/workspace/src:ro" \
    -v "$LIBRARIES_DIR:/workspace/libraries:ro" \
    -v "$temp_dir:/workspace/output" \
    -w /workspace \
    eclipse-temurin:25-jdk \
    -c '
      set -eu
      mkdir -p /tmp/nagel-gestures/classes
      classpath="$(find /workspace/libraries -type f -name "*.jar" -print | paste -sd: -)"
      javac \
        --release 17 \
        -classpath "$classpath" \
        -d /tmp/nagel-gestures/classes \
        /workspace/src/main/java/com/nagelworld/gestures/NagelGesturesPlugin.java
      cp /workspace/src/main/resources/plugin.yml /tmp/nagel-gestures/classes/plugin.yml
      jar --create --file /workspace/output/NagelGestures.jar \
        -C /tmp/nagel-gestures/classes .
    '

  mv "$temp_dir/NagelGestures.jar" "$OUTPUT_JAR"
  echo "Plugin criado em $OUTPUT_JAR"
  exit 0
fi

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/classes"
classpath="$(find "$LIBRARIES_DIR" -type f -name '*.jar' -print | paste -sd: -)"

javac \
  --release 17 \
  -classpath "$classpath" \
  -d "$BUILD_DIR/classes" \
  "$PLUGIN_DIR/src/main/java/com/nagelworld/gestures/NagelGesturesPlugin.java"

cp "$PLUGIN_DIR/src/main/resources/plugin.yml" "$BUILD_DIR/classes/plugin.yml"
jar --create --file "$OUTPUT_JAR" -C "$BUILD_DIR/classes" .

echo "Plugin criado em $OUTPUT_JAR"
