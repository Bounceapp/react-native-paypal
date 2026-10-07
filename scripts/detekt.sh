#!/bin/sh
# Lints the Android module. Run from the repository root.
#
# detekt runs as its standalone CLI rather than as a Gradle plugin: every app
# that consumes this package compiles android/build.gradle, so a lint plugin
# there would load into each of their builds.
set -eu

DETEKT_VERSION=1.23.8
CACHE_DIR="${XDG_CACHE_HOME:-$HOME/.cache}/react-native-paypal"
RELEASES="https://github.com/detekt/detekt/releases/download/v$DETEKT_VERSION"

sha256() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | cut -d ' ' -f 1
  else
    shasum -a 256 "$1" | cut -d ' ' -f 1
  fi
}

# Downloads a release jar into the cache unless it is already there, and
# refuses one that does not match its pinned checksum.
fetch() {
  jar="$CACHE_DIR/$1"
  if [ ! -f "$jar" ] || [ "$(sha256 "$jar")" != "$2" ]; then
    mkdir -p "$CACHE_DIR"
    curl -sSfL -o "$jar.tmp" "$RELEASES/$1"
    if [ "$(sha256 "$jar.tmp")" != "$2" ]; then
      rm -f "$jar.tmp"
      echo "$1 failed checksum verification" >&2
      exit 1
    fi
    mv "$jar.tmp" "$jar"
  fi
}

fetch "detekt-cli-$DETEKT_VERSION-all.jar" 2ce2ff952e150baf28a29cda70a363b0340b3e81a55f43e51ec5edffc3d066c1
# ktlint's formatting rules, which detekt does not run without this plugin.
fetch "detekt-formatting-$DETEKT_VERSION.jar" a18a3f680d232ad746ccec3d5a251776ce39131b395c455b92e1bb0a9e8fb6ba

"${JAVA_HOME:+$JAVA_HOME/bin/}java" -jar "$CACHE_DIR/detekt-cli-$DETEKT_VERSION-all.jar" \
  --input android/src --config detekt.yml --build-upon-default-config \
  --plugins "$CACHE_DIR/detekt-formatting-$DETEKT_VERSION.jar"

# detekt's UnsafeCallOnNullableType only runs with type resolution, which needs
# the module's full compile classpath -- only available inside an app build.
# A not-null assertion is a plain token, so check for it directly instead,
# skipping lines that are only a comment.
if grep -rn --include='*.kt' '!!' android/src |
  grep -vE '^[^:]+:[0-9]+:[[:space:]]*(//|/?\*)'; then
  echo "Avoid the not-null assertion operator (!!) in the Android module." >&2
  exit 1
fi
