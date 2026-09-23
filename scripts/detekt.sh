#!/bin/sh
# Lints the Android module. Run from the repository root.
#
# detekt runs as its standalone CLI rather than as a Gradle plugin: every app
# that consumes this package compiles android/build.gradle, so a lint plugin
# there would load into each of their builds.
set -eu

DETEKT_VERSION=1.23.8
DETEKT_SHA256=2ce2ff952e150baf28a29cda70a363b0340b3e81a55f43e51ec5edffc3d066c1
CACHE_DIR="${XDG_CACHE_HOME:-$HOME/.cache}/react-native-paypal"
JAR="$CACHE_DIR/detekt-cli-$DETEKT_VERSION-all.jar"

sha256() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | cut -d ' ' -f 1
  else
    shasum -a 256 "$1" | cut -d ' ' -f 1
  fi
}

if [ ! -f "$JAR" ] || [ "$(sha256 "$JAR")" != "$DETEKT_SHA256" ]; then
  mkdir -p "$CACHE_DIR"
  curl -sSfL -o "$JAR.tmp" \
    "https://github.com/detekt/detekt/releases/download/v$DETEKT_VERSION/detekt-cli-$DETEKT_VERSION-all.jar"
  if [ "$(sha256 "$JAR.tmp")" != "$DETEKT_SHA256" ]; then
    rm -f "$JAR.tmp"
    echo "detekt $DETEKT_VERSION failed checksum verification" >&2
    exit 1
  fi
  mv "$JAR.tmp" "$JAR"
fi

"${JAVA_HOME:+$JAVA_HOME/bin/}java" -jar "$JAR" \
  --input android/src --config detekt.yml --build-upon-default-config

# detekt's UnsafeCallOnNullableType only runs with type resolution, which needs
# the module's full compile classpath -- only available inside an app build.
# A not-null assertion is a plain token, so check for it directly instead.
if grep -rn --include='*.kt' '!!' android/src; then
  echo "Avoid the not-null assertion operator (!!) in the Android module." >&2
  exit 1
fi
