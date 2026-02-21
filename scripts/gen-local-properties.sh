#!/usr/bin/env bash
set -euo pipefail

# Generates or updates local.properties with Android SDK path.
# Preserves existing keys (for example GOOGLE_AI_API_KEY and signing config).
# Priority: ANDROID_SDK_ROOT > ANDROID_HOME

SDK_DIR="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"

if [ -z "${SDK_DIR}" ]; then
  echo "ERROR: ANDROID_SDK_ROOT or ANDROID_HOME is not set." >&2
  echo "Set ANDROID_SDK_ROOT to your SDK path, then rerun." >&2
  exit 1
fi

LOCAL_PROPERTIES_FILE="local.properties"
TMP_FILE="$(mktemp)"

if [ -f "${LOCAL_PROPERTIES_FILE}" ]; then
  # Keep everything except sdk.dir so we can write the latest SDK path.
  grep -v '^sdk\.dir=' "${LOCAL_PROPERTIES_FILE}" > "${TMP_FILE}" || true
fi

{
  echo "sdk.dir=${SDK_DIR}"
  cat "${TMP_FILE}" 2>/dev/null || true
} > "${LOCAL_PROPERTIES_FILE}"

if ! grep -q '^GOOGLE_AI_API_KEY=' "${LOCAL_PROPERTIES_FILE}"; then
  {
    echo ""
    echo "## Google AI API key for Gemini cloud fallback"
    echo "## Get your key from: https://aistudio.google.com/apikey"
    echo "GOOGLE_AI_API_KEY="
  } >> "${LOCAL_PROPERTIES_FILE}"
fi

rm -f "${TMP_FILE}"
echo "Updated ${LOCAL_PROPERTIES_FILE} with sdk.dir=${SDK_DIR} (existing entries preserved)"
