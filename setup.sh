#!/usr/bin/env bash
set -euo pipefail

# Small helper to surface missing tools with a CachyOS/Arch-friendly hint.
INSTALL_HINT="sudo apt-get install openjdk-17-jdk wget unzip zip git"
if command -v pacman >/dev/null 2>&1; then
  INSTALL_HINT="sudo pacman -S --needed jdk17-openjdk wget unzip zip git base-devel"
fi

require_cmd() {
  local bin="$1"
  if ! command -v "$bin" >/dev/null 2>&1; then
    echo "❌ Missing dependency: $bin. Install with: $INSTALL_HINT" >&2
    exit 1
  fi
}

require_cmd wget
require_cmd unzip
require_cmd zip
require_cmd java

# Warn if Java is not 17
JAVA_VERSION_OUTPUT=$(java -version 2>&1 | head -n 1 || true)
if ! grep -qE '"17\.' <<<"$JAVA_VERSION_OUTPUT"; then
  echo "⚠️  Java 17 recommended. Detected: $JAVA_VERSION_OUTPUT" >&2
fi

# ----- Android SDK + Build Tools -----
: "${TARGET_API:=35}"      # change to 36 if you want Android 16 preview
: "${BUILD_TOOLS:=35.0.0}" # latest stable
DEFAULT_SDK_ROOT="${SDK_ROOT:-$HOME/android-sdk}"
SDK_ROOT="$DEFAULT_SDK_ROOT"
# If the default SDK_ROOT isn't writable (e.g., sandboxed $HOME), fall back to a local directory.
if ! mkdir -p "$SDK_ROOT" 2>/dev/null || ! touch "$SDK_ROOT/.rwtest" 2>/dev/null; then
  SDK_ROOT="$PWD/.android-sdk"
  mkdir -p "$SDK_ROOT"
  echo "ℹ️ Using SDK directory at $SDK_ROOT"
else
  rm -f "$SDK_ROOT/.rwtest" 2>/dev/null || true
fi
ANDROID_SDK_HOME="${ANDROID_SDK_HOME:-$SDK_ROOT/.android}"

export SDK_ROOT
export ANDROID_SDK_ROOT="$SDK_ROOT"
export ANDROID_HOME="$SDK_ROOT"
export ANDROID_SDK_HOME

TOOLS_ROOT="$SDK_ROOT/cmdline-tools"
TOOLS_LATEST="$TOOLS_ROOT/latest"

if [ ! -x "$TOOLS_LATEST/bin/sdkmanager" ]; then
  echo "Installing Android cmdline-tools..."
  mkdir -p "$TOOLS_ROOT"
  rm -f /tmp/cmdtools.zip
  wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdtools.zip
  unzip -q /tmp/cmdtools.zip -d "$TOOLS_ROOT"
  rm /tmp/cmdtools.zip
  mv "$TOOLS_ROOT/cmdline-tools" "$TOOLS_LATEST"
fi

export PATH="$TOOLS_LATEST/bin:$SDK_ROOT/platform-tools:$PATH"

# sdkmanager expects this file to exist; place it alongside the SDK to avoid $HOME permission issues.
if mkdir -p "$ANDROID_SDK_HOME" 2>/dev/null; then
  touch "$ANDROID_SDK_HOME/repositories.cfg" 2>/dev/null || true
fi

# Accept licenses and install required SDK components; handle sdkmanager closing stdin early.
echo "Accepting licenses..."
set +o pipefail
yes 2>/dev/null | sdkmanager --sdk_root="$SDK_ROOT" --licenses >/dev/null || true
set -o pipefail

echo "Installing platform-tools, platform ${TARGET_API}, build-tools ${BUILD_TOOLS}..."
set +o pipefail
yes 2>/dev/null | sdkmanager --sdk_root="$SDK_ROOT" \
  "platform-tools" \
  "platforms;android-${TARGET_API}" \
  "build-tools;${BUILD_TOOLS}" || true
set -o pipefail

# ----- Project wiring -----
# Write local.properties next to a detected Gradle wrapper.
PROJECT_DIR="$PWD"
if [ ! -f "$PROJECT_DIR/gradlew" ] && [ -f "/workspace/gradlew" ]; then
  PROJECT_DIR="/workspace"
fi

if [ -f "$PROJECT_DIR/gradlew" ]; then
  chmod +x "$PROJECT_DIR/gradlew"
  echo "sdk.dir=$SDK_ROOT" > "$PROJECT_DIR/local.properties"
fi

echo "✅ Android SDK ready (API $TARGET_API, Build-Tools $BUILD_TOOLS)"
