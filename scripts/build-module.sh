#!/usr/bin/env bash
# Builds the Catch Magisk module zip from a Catch APK.
#
#   scripts/build-module.sh <Catch.apk> <version-name> <version-code> [out.zip]
#
# Needs an Android SDK (ANDROID_HOME) with build-tools and a platform installed,
# plus keytool and zip.
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
APK=$(realpath "$1")
VERSION=$2
CODE=$3
OUT=$(realpath -m "${4:-$ROOT/dist/catch-magisk-$VERSION.zip}")

SDK=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}
BUILD_TOOLS=$(printf "%s\n" "$SDK"/build-tools/* | sort -V | tail -1)
ANDROID_JAR=$(printf "%s\n" "$SDK"/platforms/android-* | sort -V | tail -1)/android.jar

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
STAGE=$WORK/module

mkdir -p "$STAGE"
cp -r "$ROOT/magisk/META-INF" "$ROOT/magisk/system" "$STAGE/"
cp "$ROOT/magisk/customize.sh" "$ROOT/magisk/service.sh" "$ROOT/magisk/sepolicy.rule" "$STAGE/"
sed -e "s/@VERSION@/$VERSION/" -e "s/@VERSION_CODE@/$CODE/" \
  "$ROOT/magisk/module.prop" > "$STAGE/module.prop"
install -D -m 644 "$APK" "$STAGE/system/priv-app/Catch/Catch.apk"

# Static resource overlays live on /product and are not signature-checked
# against their targets, so a throwaway key is enough.
KEY=$WORK/overlay.jks
keytool -genkeypair -keystore "$KEY" -storepass catchrro -keypass catchrro -alias rro \
  -keyalg RSA -keysize 2048 -validity 10000 -dname CN=CatchOverlay >/dev/null 2>&1

build_overlay() {
  local src=$ROOT/magisk/overlays/$1 name=$2 tmp=$WORK/$2
  mkdir -p "$tmp" "$STAGE/system/product/overlay/$name"
  "$BUILD_TOOLS/aapt2" compile --dir "$src/res" -o "$tmp/res.zip"
  "$BUILD_TOOLS/aapt2" link -o "$tmp/unsigned.apk" -I "$ANDROID_JAR" \
    --manifest "$src/AndroidManifest.xml" --min-sdk-version 29 --target-sdk-version 29 \
    "$tmp/res.zip"
  "$BUILD_TOOLS/zipalign" -f 4 "$tmp/unsigned.apk" "$tmp/aligned.apk"
  "$BUILD_TOOLS/apksigner" sign --ks "$KEY" --ks-pass pass:catchrro --v4-signing-enabled false \
    --out "$STAGE/system/product/overlay/$name/$name.apk" "$tmp/aligned.apk"
}

build_overlay fallback-home CatchFallbackHome
build_overlay systemui-nav CatchSysuiHome

mkdir -p "$(dirname "$OUT")"
rm -f "$OUT"
(cd "$STAGE" && zip -qr9 "$OUT" .)
echo "$OUT"
