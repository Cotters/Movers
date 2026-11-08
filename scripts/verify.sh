#!/usr/bin/env bash
set -euo pipefail

if (( $# != 0 )); then
    printf 'Usage: %s (no arguments)\n' "$0" >&2
    exit 64
fi

repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_dir"

fail() {
    printf 'Verification prerequisite: %s\nSee docs/workflow/testing.md for setup.\n' "$1" >&2
    exit 2
}

[[ -x ./gradlew ]] || fail 'The Gradle wrapper is missing or not executable.'

java_bin=java
if [[ -n "${JAVA_HOME:-}" ]]; then
    java_bin="$JAVA_HOME/bin/java"
fi
command -v "$java_bin" >/dev/null 2>&1 || fail 'Install JDK 17 and configure JAVA_HOME or PATH.'
java_version="$("$java_bin" -version 2>&1)" || fail 'Java could not start; check JAVA_HOME and PATH.'
version_pattern='version[[:space:]]+"([0-9]+)'
if [[ "$java_version" =~ $version_pattern ]]; then
    [[ "${BASH_REMATCH[1]}" == 17 ]] || fail 'Use JDK 17 for the verified project toolchain.'
else
    fail 'Could not identify Java 17 from java -version.'
fi

sdk_dir="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -n "$sdk_dir" ]]; then
    [[ -d "$sdk_dir" ]] || fail 'The configured Android SDK directory does not exist.'
elif [[ ! -f local.properties ]] || ! grep -Eq '^[[:space:]]*sdk\.dir[[:space:]]*=' local.properties; then
    fail 'Set ANDROID_HOME/ANDROID_SDK_ROOT or sdk.dir in local.properties.'
fi

[[ -f movie/internal/src/main/java/com/jcotters/internal/ApiConstants.kt ]] ||
    fail 'Create the ignored ApiConstants.kt using the example in the testing guide; preserve existing credentials.'

printf 'Checking convention plugins.\n'
./gradlew --no-daemon --console=plain -p build-logic :convention:check

printf 'Running host unit tests, Android lint, and a debug build.\n'
exec ./gradlew --no-daemon --console=plain --continue testDebugUnitTest lintDebug :app:assembleDebug
